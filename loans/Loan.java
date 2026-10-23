package loans;

import java.util.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import resources.*;
import reporting.OutputServiceContributor;
import subscribers.*;

public class Loan  implements OutputServiceContributor {
	private Resource resource;
	private Object subscriber;
	private GregorianCalendar loanDate;
	private GregorianCalendar returnDate;
	private String id;
	
	public static int idCounter = 0;
	
	public String getFormattedString() {
		String formattedString = (resource==null?"Unknown":resource.getFormattedString()) + " loaned to ";
		if ( subscriber == null ) {
			formattedString += "Nobody";
		}
		else if ( subscriber instanceof Student ) {
			formattedString += ((Student)subscriber).getFormattedString();
		}
		else if ( subscriber instanceof Pensioner ) {
			formattedString += ((Pensioner)subscriber).getFormattedString();
		}
		else if ( subscriber instanceof PremiumMember ) {
			formattedString += ((PremiumMember)subscriber).getFormattedString();
		}
		else {
			formattedString += "Unknown subscriber type.";
		}
		return  formattedString;
	}

	public GregorianCalendar getReturnDate() {
		return returnDate == null ? null : (GregorianCalendar)returnDate.clone();
	}
	void setId(String id) {
		this.id = id;
	}
	public void setReturnDate(GregorianCalendar returnDate) {
		if (returnDate == null) {
			throw new NullPointerException("Return date can not be null");
		}
		if ( this.returnDate != null )
			throw new IllegalStateException("Return date has already been assigned");
		if (loanDate == null || returnDate.before(loanDate)) {
			throw new IllegalArgumentException("Return date can not be before the loan date");
		}
		if (resource == null || (!(subscriber instanceof Student) && !(subscriber instanceof Pensioner)
				&& !(subscriber instanceof PremiumMember))) {
			throw new IllegalStateException("Loan is missing its resource or subscriber");
		}
		if ( subscriber instanceof Student ) {
			((Student)subscriber).removeLoan(this);
		}
		else if ( subscriber instanceof Pensioner ) {
			((Pensioner)subscriber).removeLoan(this);
		}
		else if ( subscriber instanceof PremiumMember ) {
			((PremiumMember)subscriber).removeLoan(this);
		}
		resource.removeLoan();
		this.returnDate = (GregorianCalendar)returnDate.clone();
	}

	public void setLoanDate(GregorianCalendar loanDate) {
		this.loanDate = loanDate == null ? null : (GregorianCalendar)loanDate.clone();
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public void setSubscriber(Object subscriber) {
		this.subscriber = subscriber;
	}

	public Loan() {
		this.id = "L"+idCounter++;
	}
	
	public Loan(String id) {
		this.id = id;
		idCounter ++;
	}
	
	public String getId() {
		return id;
	}

	public GregorianCalendar getLoanDate() {
		return loanDate == null ? null : (GregorianCalendar)loanDate.clone();
	}

	public Resource getResource() {
		return resource;
	}

	public Object getSubscriber() {
		return subscriber;
	}
	
	/**
	 * A loan is overdue when the elapsed days exceed the resource's maximum loan
	 * period. Handles month and year boundaries correctly.
	 */
	public boolean isOverdue(GregorianCalendar today) {
		if ( today == null || loanDate == null || resource == null ) {
			return false;
		}
		long elapsedDays = daysBetween(loanDate, today);
		return elapsedDays > resource.getMaxLoanPeriod();
	}

	public long getDaysOverdue(GregorianCalendar date) {
		if (date == null || loanDate == null || resource == null) {
			return 0;
		}
		return Math.max(0, daysBetween(loanDate, date) - resource.getMaxLoanPeriod());
	}

	public double getSurcharge(GregorianCalendar returnDate) {
		if (resource == null || !"Game".equals(resource.type)) {
			return 0;
		}
		return getDaysOverdue(returnDate) * resource.getSurcharge();
	}

	private long daysBetween(GregorianCalendar start, GregorianCalendar end) {
		LocalDate startDate = LocalDate.of(start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1,
				start.get(Calendar.DAY_OF_MONTH));
		LocalDate endDate = LocalDate.of(end.get(Calendar.YEAR), end.get(Calendar.MONTH) + 1,
				end.get(Calendar.DAY_OF_MONTH));
		return ChronoUnit.DAYS.between(startDate, endDate);
	}
}
