package library;

import java.util.GregorianCalendar;

import audit.ConsoleAuditLogger;
import reporting.ConsoleOutputService;
import reporting.FileOutputService;
import resources.*;
import subscribers.*;

public class LibrarySimulator {
	LibraryServices library;
	LibraryReportingService reporting;
	public LibrarySimulator() throws Exception {
		library = new LibraryServices(new ConsoleAuditLogger());
		reporting = new LibraryReportingService();
		reporting.addOutputService(new ConsoleOutputService());
		reporting.addOutputService(new FileOutputService("library-report.txt"));
	}
	
	public static void main(String[] args) throws Exception {
		LibrarySimulator librarySimulator = new LibrarySimulator();
		librarySimulator.runSimulator();
	}
	
	public void runReports() {
		reporting.outputResourcesReport();
		reporting.outputSubscribersReport();
		reporting.outputOutstandingLoansReport();
	}
	public void runSimulator() {
		reporting.outputOutstandingLoansReport();
		Resource resource = ResourceExtent.INSTANCE.findByPrimaryKey("R1");
		Object subscriber = SubscriberExtent.INSTANCE.findByPrimaryKey("S1");
		try {
			library.loanResourceToSubscriber(resource, subscriber, new GregorianCalendar(2004, 00, 20));
		}
		catch (Exception e) {
			System.out.println("Error - Loan rejected: " + e.getMessage());
		}
		
		reporting.outputOutstandingLoansReport();
		library.returnedResource(resource.getLoan(), new GregorianCalendar(2004, 00, 20));
		
		reporting.outputOutstandingLoansReport();
		try {
			library.loanResourceToSubscriber(resource, subscriber, new GregorianCalendar(2004, 00, 20));
		} 
		catch (Exception e) {
			System.out.println("Error - Loan rejected: " + e.getMessage());
		} 
		reporting.outputOutstandingLoansReport();
	}
}
