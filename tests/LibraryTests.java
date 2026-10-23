package tests;

import audit.AuditLogger;
import library.LibraryReportingService;
import library.LibraryServices;
import loans.Loan;
import reporting.FileOutputService;
import reporting.OutputService;
import reporting.OutputServiceContributor;
import resources.Resource;
import subscribers.Pensioner;
import subscribers.PremiumMember;
import subscribers.Student;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class LibraryTests {
	private static int assertions;

	public static void main(String[] args) throws Exception {
		testBorrowLimitAndRejectedAudit();
		testPremiumBorrowLimit();
		testPremiumReservationPriority();
		testGameEligibilityAndReturnSurcharge();
		testOverdueAcrossMonthBoundary();
		testConcurrentDuplicateLoan();
		testFileOutputWritesMultipleReports();
		testOutstandingLoansReportIsSorted();
		System.out.println("Passed " + assertions + " assertions.");
	}

	private static void testPremiumReservationPriority() throws Exception {
		LibraryServices library = new LibraryServices(new RecordingAuditLogger());
		Resource newResource = book("New resource");
		Student student = new Student("Waiting student", "University");
		PremiumMember premium = new PremiumMember("Priority member", 19.99);
		library.reserveResource(newResource, student);
		library.reserveResource(newResource, premium);
		check(newResource.getReservations().get(0) == premium,
				"Premium reservations must take priority over earlier standard reservations");
		expectFailure(new CheckedAction() {
			public void run() throws Exception {
				library.loanResourceToSubscriber(newResource, student, date(2026, Calendar.JANUARY, 1));
			}
		});
		Loan loan = library.loanResourceToSubscriber(newResource, premium,
				date(2026, Calendar.JANUARY, 1));
		check(loan.getSubscriber() == premium, "Priority reservation must be fulfilled by its holder");
		check(newResource.getReservations().size() == 1,
				"Fulfilling the priority hold must leave later reservations queued");
	}

	private static void testBorrowLimitAndRejectedAudit() throws Exception {
		RecordingAuditLogger audit = new RecordingAuditLogger();
		LibraryServices library = new LibraryServices(audit);
		Student student = new Student("Limit test", "Test University");
		for (int i = 0; i < 5; i++) {
			library.loanResourceToSubscriber(book("Book " + i), student, date(2026, Calendar.JANUARY, 1));
		}
		expectFailure(new CheckedAction() {
			public void run() throws Exception {
				library.loanResourceToSubscriber(book("Sixth book"), student,
						date(2026, Calendar.JANUARY, 1));
			}
		});
		check(student.getLoans().size() == 5, "Student borrowing limit must be five");
		check(audit.containsAction("LOAN_REJECTED"), "Rejected loan attempt must be audited");
	}

	private static void testPremiumBorrowLimit() throws Exception {
		LibraryServices library = new LibraryServices(new RecordingAuditLogger());
		PremiumMember member = new PremiumMember("Premium limit test", 19.99);
		for (int i = 0; i < 10; i++) {
			library.loanResourceToSubscriber(book("Premium book " + i), member,
					date(2026, Calendar.JANUARY, 1));
		}
		expectFailure(new CheckedAction() {
			public void run() throws Exception {
				library.loanResourceToSubscriber(book("Eleventh premium book"), member,
						date(2026, Calendar.JANUARY, 1));
			}
		});
		check(member.getLoans().size() == 10, "Premium borrowing limit must be ten");
	}

	private static void testGameEligibilityAndReturnSurcharge() throws Exception {
		RecordingAuditLogger audit = new RecordingAuditLogger();
		LibraryServices library = new LibraryServices(audit);
		Resource game = new Resource("Game", "Surcharge test", "X-Box");
		expectFailure(new CheckedAction() {
			public void run() throws Exception {
				library.loanResourceToSubscriber(game, new Pensioner("Not eligible"),
						date(2026, Calendar.JANUARY, 1));
			}
		});
		check(audit.containsAction("LOAN_REJECTED"), "Ineligible game loan must be audited");

		Loan loan = library.loanResourceToSubscriber(game, new Student("Eligible", "Test University"),
				date(2026, Calendar.JANUARY, 1));
		expectFailure(new CheckedAction() {
			public void run() {
				library.returnedResource(loan, date(2025, Calendar.DECEMBER, 31));
			}
		});
		check(audit.containsAction("LOAN_RETURN_REJECTED"), "Rejected return must be audited");
		double surcharge = library.returnedResource(loan, date(2026, Calendar.JANUARY, 10));
		check(surcharge == 2.0, "Two overdue X-Box days must incur a 2.0 surcharge");
		check(game.getLoan() == null, "Returned resource must become available");
		check(loan.getReturnDate() != null, "Return date must be recorded");
		check(audit.containsAction("LOAN_RETURNED"), "Return transaction must be audited");
	}

	private static void testOverdueAcrossMonthBoundary() throws Exception {
		LibraryServices library = new LibraryServices(new RecordingAuditLogger());
		Loan loan = library.loanResourceToSubscriber(book("Month boundary"),
				new Student("Calendar test", "Test University"),
				date(2026, Calendar.JANUARY, 20));
		check(loan.isOverdue(date(2026, Calendar.FEBRUARY, 11)),
				"Overdue calculation must include month boundaries");
	}

	private static void testConcurrentDuplicateLoan() throws Exception {
		final LibraryServices library = new LibraryServices(new RecordingAuditLogger());
		final Resource resource = book("Concurrent");
		final CountDownLatch ready = new CountDownLatch(2);
		final CountDownLatch start = new CountDownLatch(1);
		final AtomicInteger successes = new AtomicInteger();
		final AtomicInteger failures = new AtomicInteger();
		Thread first = loanThread(library, resource, new Student("First", "University"), ready, start,
				successes, failures);
		Thread second = loanThread(library, resource, new Student("Second", "University"), ready, start,
				successes, failures);
		first.start();
		second.start();
		ready.await();
		start.countDown();
		first.join();
		second.join();
		check(successes.get() == 1, "Exactly one concurrent loan may succeed");
		check(failures.get() == 1, "The competing loan must be rejected");
		check(resource.getLoan() != null, "Resource must have one active loan");
	}

	private static Thread loanThread(final LibraryServices library, final Resource resource,
			final Student student, final CountDownLatch ready, final CountDownLatch start,
			final AtomicInteger successes, final AtomicInteger failures) {
		return new Thread(new Runnable() {
			public void run() {
				ready.countDown();
				try {
					start.await();
					library.loanResourceToSubscriber(resource, student, date(2026, Calendar.MARCH, 1));
					successes.incrementAndGet();
				} catch (Exception expected) {
					failures.incrementAndGet();
				}
			}
		});
	}

	private static void testFileOutputWritesMultipleReports() throws Exception {
		Path report = Files.createTempFile("library-report-test", ".txt");
		try {
			FileOutputService output = new FileOutputService(report.toString());
			output.outputExtent("First report", Arrays.asList(new Student("Reader", "University")));
			output.outputExtent("Second report", Arrays.asList(book("Reported book")));
			String contents = new String(Files.readAllBytes(report), StandardCharsets.UTF_8);
			check(contents.contains("First report") && contents.contains("Second report"),
					"File output must preserve multiple report sections");
			check(contents.contains("Reported book"), "File output must format report contributors");
		} finally {
			Files.deleteIfExists(report);
		}
	}

	private static void testOutstandingLoansReportIsSorted() {
		final List<String> loanLines = new java.util.ArrayList<String>();
		LibraryReportingService reporting = new LibraryReportingService();
		// Capture the report output to verify its order independently of the console.
		reporting.addOutputService(new OutputService() {
			public void outputExtent(String title, java.util.Collection contributors) {
				if ("Loans".equals(title)) {
					for (Object contributor : contributors) {
						loanLines.add(((OutputServiceContributor)contributor).getFormattedString());
					}
				}
			}

			public String getName() {
				return "Test output service";
			}
		});
		reporting.outputOutstandingLoansReport();
		List<String> sortedLoanLines = new java.util.ArrayList<String>(loanLines);
		Collections.sort(sortedLoanLines);
		check(loanLines.equals(sortedLoanLines),
				"Outstanding loan reports must use a stable alphabetical order");
	}

	private static Resource book(String title) {
		return new Resource("Book", title, "Test Author");
	}

	private static GregorianCalendar date(int year, int month, int day) {
		return new GregorianCalendar(year, month, day);
	}

	private static void expectFailure(CheckedAction action) throws Exception {
		try {
			action.run();
			throw new AssertionError("Expected operation to fail");
		} catch (IllegalStateException expected) {
			assertions++;
		} catch (IllegalArgumentException expected) {
			assertions++;
		} catch (Exception expected) {
			if ("Expected operation to fail".equals(expected.getMessage())) {
				throw expected;
			}
			assertions++;
		}
	}

	private static void check(boolean condition, String message) {
		assertions++;
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private interface CheckedAction {
		void run() throws Exception;
	}

	private static final class RecordingAuditLogger implements AuditLogger {
		private final List<String> actions = Collections.synchronizedList(new java.util.ArrayList<String>());

		public void log(String category, String action, String detail) {
			actions.add(action);
		}

		boolean containsAction(String action) {
			return actions.contains(action);
		}
	}
}
