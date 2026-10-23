/*
 * Created on 02-Feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package loans;

import java.util.*;

import resources.*;
import subscribers.*;

public class LoanExtent {
	private static Map<String, Loan> loans = new Hashtable<String, Loan>();
	private static int idCounter = 0;
	public static LoanExtent INSTANCE = new LoanExtent();
	
	private LoanExtent() {
		try {
			//Initialise and register resources currently on loan
			createLoan(ResourceExtent.INSTANCE.findByPrimaryKey("R1"), (Student)SubscriberExtent.INSTANCE.findByPrimaryKey("S3"),new GregorianCalendar(2004, 00, 20));
			createLoan(ResourceExtent.INSTANCE.findByPrimaryKey("R5"), (Student)SubscriberExtent.INSTANCE.findByPrimaryKey("S3"),new GregorianCalendar(2004, 00, 01));
			createLoan(ResourceExtent.INSTANCE.findByPrimaryKey("R4"), (Student)SubscriberExtent.INSTANCE.findByPrimaryKey("S2"),new GregorianCalendar(2004, 00, 01));
		} 
		catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		} 
	}
	public synchronized Loan createLoan(Resource resource, Object subscriber, GregorianCalendar gregorianCalendar) throws Exception {
		// Validate before changing either the resource or subscriber loan lists.
		validateLoan(resource, subscriber, gregorianCalendar);
		Loan loan = new Loan();
		loan.setResource(resource);
		loan.setSubscriber(subscriber);
		loan.setLoanDate(gregorianCalendar);
		loan.setId(getNextId());
		resource.setLoan(loan);
		if ( subscriber instanceof Student ) {
			((Student)subscriber).addLoan(loan);
		}
		else if ( subscriber instanceof Pensioner ) {
			((Pensioner)subscriber).addLoan(loan);
		}
		else {
			((PremiumMember)subscriber).addLoan(loan);
		}
		resource.fulfillReservation(subscriber);
		loans.put(loan.getId(), loan);
		return loan;
	}

	public synchronized void validateLoan(Resource resource, Object subscriber, GregorianCalendar gregorianCalendar) throws Exception {
		if ( resource == null ) {
			throw new NullPointerException("Resource parameter can not be null");
		}
		if ( subscriber == null ) {
			throw new NullPointerException("Subscriber parameter can not be null");
		}
		if (gregorianCalendar == null) {
			throw new NullPointerException("Loan date parameter can not be null");
		}
		if (!"Book".equals(resource.type) && !"Game".equals(resource.type)) {
			throw new IllegalArgumentException("Unsupported resource type");
		}
		if (!(subscriber instanceof Student) && !(subscriber instanceof Pensioner)
				&& !(subscriber instanceof PremiumMember)) {
			throw new IllegalArgumentException("Unsupported subscriber type");
		}
		if ("Game".equals(resource.type) && !(subscriber instanceof Student)) {
			throw new IllegalArgumentException("Games may only be borrowed by Students");
		}
		if (resource.getLoan() != null) {
			throw new IllegalStateException("This resource is already on loan: "+resource.getLoan().getFormattedString());
		}
		if (resource.hasReservations() && !resource.isNextReservation(subscriber)) {
			throw new IllegalStateException("This resource is reserved for another subscriber");
		}
		int loanCount;
		int loanLimit;
		if (subscriber instanceof Student) {
			loanCount = ((Student)subscriber).getLoans().size();
			loanLimit = ((Student)subscriber).getMaxBorrowing();
		}
		else if (subscriber instanceof Pensioner) {
			loanCount = ((Pensioner)subscriber).getLoans().size();
			loanLimit = ((Pensioner)subscriber).getMaxBorrowing();
		}
		else {
			loanCount = ((PremiumMember)subscriber).getLoans().size();
			loanLimit = ((PremiumMember)subscriber).getMaxBorrowing();
		}
		if (loanCount >= loanLimit) {
			throw new IllegalStateException(subscriberName(subscriber)+" has reached their borrowing limit.");
		}
	}

	private String subscriberName(Object subscriber) {
		if (subscriber instanceof Student) return ((Student)subscriber).getName();
		if (subscriber instanceof Pensioner) return ((Pensioner)subscriber).getName();
		return ((PremiumMember)subscriber).getName();
	}
	public synchronized Loan findByPrimaryKey(String id) {
		return loans.get(id);
	}
	public synchronized Collection<Loan> getLoans() {
		return new ArrayList<Loan>(loans.values());
	}
	public synchronized String getNextId() {
		return "L" + (idCounter++);
	}
}
