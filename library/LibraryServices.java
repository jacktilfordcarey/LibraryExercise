package library;
import java.util.*;

import loans.*;
import audit.AuditLogger;
import subscribers.*;
import resources.*;

public class LibraryServices {
	private final AuditLogger auditLogger;

	public LibraryServices(AuditLogger auditLogger) {
		if (auditLogger == null) {
			throw new NullPointerException("Audit logger can not be null");
		}
		this.auditLogger = auditLogger;
	}

	public LibraryServices() {
		this(new audit.ConsoleAuditLogger());
	}
	public void addResource(Resource resource) {
		ResourceExtent.INSTANCE.createResource(resource);
	}
	public void addPensioner(Pensioner pensioner) {
		SubscriberExtent.INSTANCE.createPensioner(pensioner);
	}
	public void addStudent(Student student) {
		SubscriberExtent.INSTANCE.createStudent(student);
	}
	public void addPremiumMember(PremiumMember premiumMember) {
		SubscriberExtent.INSTANCE.createPremiumMember(premiumMember);
	}

	public void reserveResource(Resource resource, Object subscriber) {
		if (resource == null) {
			throw new NullPointerException("Resource parameter can not be null");
		}
		if (!(subscriber instanceof Student) && !(subscriber instanceof Pensioner)
				&& !(subscriber instanceof PremiumMember)) {
			throw new IllegalArgumentException("Unsupported subscriber type");
		}
		if ("Game".equals(resource.type) && !(subscriber instanceof Student)) {
			throw new IllegalArgumentException("Games may only be reserved by Students");
		}
		synchronized (LoanExtent.INSTANCE) {
			resource.addReservation(subscriber);
		}
	}

	public void cancelReservation(Resource resource, Object subscriber) {
		if (resource == null) {
			throw new NullPointerException("Resource parameter can not be null");
		}
		synchronized (LoanExtent.INSTANCE) {
			resource.cancelReservation(subscriber);
		}
	}
	
	public Loan loanResourceToSubscriber(Resource resource, Object subscriber, GregorianCalendar gregorianCalendar) throws Exception {
		// Keep validation, auditing, and the state change in one serialized operation.
		synchronized (LoanExtent.INSTANCE) {
			try {
				LoanExtent.INSTANCE.validateLoan(resource, subscriber, gregorianCalendar);
			} catch (Exception exception) {
				auditLogger.log("LOAN", "LOAN_REJECTED", loanDetail(resource, subscriber, exception.getMessage()));
				throw exception;
			}
			auditLogger.log("LOAN", "LOAN_CREATED", loanDetail(resource, subscriber, null));
			return LoanExtent.INSTANCE.createLoan(resource, subscriber, gregorianCalendar);
		}
	}

	public double returnedResource(Loan loan, GregorianCalendar gregorianCalendar) {
		// Record the assessed fee before closing the loan.
		synchronized (LoanExtent.INSTANCE) {
			try {
				if (loan == null || LoanExtent.INSTANCE.findByPrimaryKey(loan.getId()) != loan) {
					throw new IllegalArgumentException("Loan is not registered");
				}
				if (gregorianCalendar == null) {
					throw new NullPointerException("Return date can not be null");
				}
				if (loan.getReturnDate() != null) {
					throw new IllegalStateException("Return date has already been assigned");
				}
				if (loan.getLoanDate() == null || gregorianCalendar.before(loan.getLoanDate())) {
					throw new IllegalArgumentException("Return date can not be before the loan date");
				}
			} catch (RuntimeException exception) {
				auditLogger.log("LOAN", "LOAN_RETURN_REJECTED",
						loanDetail(loan == null ? null : loan.getResource(), loan == null ? null : loan.getSubscriber(), exception.getMessage()));
				throw exception;
			}
			double surcharge = loan.getSurcharge(gregorianCalendar);
			auditLogger.log("LOAN", "LOAN_RETURNED",
					"loanId=" + loan.getId() + "; surcharge=" + surcharge);
			loan.setReturnDate(gregorianCalendar);
			return surcharge;
		}
	}

	private String loanDetail(Resource resource, Object subscriber, String reason) {
		String detail = "resourceId=" + (resource == null ? "null" : resource.getId())
				+ "; subscriberId=" + subscriberId(subscriber);
		if (reason != null) {
			detail += "; reason=" + reason;
		}
		return detail;
	}

	private String subscriberId(Object subscriber) {
		if (subscriber instanceof Student) return ((Student)subscriber).getId();
		if (subscriber instanceof Pensioner) return ((Pensioner)subscriber).getId();
		if (subscriber instanceof PremiumMember) return ((PremiumMember)subscriber).getId();
		return subscriber == null ? "null" : "unsupported";
	}

}
