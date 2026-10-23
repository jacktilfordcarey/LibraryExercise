/*
 * Created on 02-Feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package library;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import loans.Loan;
import loans.LoanExtent;
import resources.ResourceExtent;
import reporting.OutputService;
import subscribers.SubscriberExtent;

/**
 * @author Simon
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LibraryReportingService {
	private final List<OutputService> outputServices = new CopyOnWriteArrayList<OutputService>();
	public void outputSubscribersReport() {
		for (OutputService outputService : outputServices) {
			try {
				outputService.outputExtent("Subscribers", SubscriberExtent.INSTANCE.getSubscribers());
			}
			catch (Exception e) {
				System.out.println("Exception while outputing subscriber to service " + outputService.getName() + " - Original message: "+e.getMessage());
			}
		}
	}
	
	/**
	 * Generates a report on resources using the registered output services
	 *
	 */
	public void outputResourcesReport() {
		for (OutputService outputService : outputServices) {
			try {
				outputService.outputExtent("Resources", ResourceExtent.INSTANCE.getResources());
			}
			catch (Exception e) {
				System.out.println("Exception while outputing resource to service " + outputService.getName() + " - Original message: "+e.getMessage());
			}
		}
	}
	
	public void outputOutstandingLoansReport() {
		Collection<Loan> loans = LoanExtent.INSTANCE.getLoans();
		List<Loan> outstandingLoans = new ArrayList<Loan>();
		for (Loan element : loans) {
			if ( element.getReturnDate() == null ) {
				outstandingLoans.add(element);
			}
		}
		Collections.sort(outstandingLoans, new Comparator<Loan>() {
			public int compare(Loan first, Loan second) {
				return first.getFormattedString().compareTo(second.getFormattedString());
			}
		});
		for (OutputService outputService : outputServices) {
			try {
				outputService.outputExtent("Loans", outstandingLoans);
			}
			catch (Exception e) {
				System.out.println("Exception while outputing loan to service " + outputService.getName() + " - Original message: "+e.getMessage());
			}
		}
	}
	public void addOutputService(OutputService outputService) {
		if (outputService == null) throw new NullPointerException("Output service can not be null");
		outputServices.add(outputService);
	}
	
	public void removeOutputService(OutputService outputService) {
		outputServices.remove(outputService);
	}
	

}
