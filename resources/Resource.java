package resources;

import reporting.OutputServiceContributor;
import loans.*;
import java.util.ArrayList;
import java.util.List;

public class Resource  implements OutputServiceContributor {
	public String id;
	public String title;
	public String gameType;
	public String type;
	public String author;
	
	public Loan loan;
	private final List<Object> reservations = new ArrayList<Object>();
	
	public Resource(String type, String title, String extra){
		this(type, null, title, extra);
	}
	public Resource(String type, String id, String title, String extra){
		if (!"Book".equals(type) && !"Game".equals(type)) {
			throw new IllegalArgumentException("Resource type must be Book or Game");
		}
		if (title == null || title.trim().length() == 0 || extra == null || extra.trim().length() == 0) {
			throw new IllegalArgumentException("Resource title and author/platform are required");
		}
		this.title = title;
		this.type = type;
		this.id = id;
		if ( "Book".equals(type)) {
			this.author = extra;
		}
		else {
			this.gameType = extra;
		}
	}
	void setId(String id) {
		this.id = id;
	}
	public String getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	/**
	 * Records the loan against this resource. Games may be loaned to Students
	 * only by Students; books may be loaned to any supported subscriber.
	 */
	public synchronized void setLoan(Loan loan) throws Exception {
		if (loan == null) {
			throw new NullPointerException("Loan parameter can not be null");
		}
		if ( this.loan != null ) {
			throw new Exception("This resource is already on loan: "+this.loan.getFormattedString());
		}
		this.loan = loan;
	}
	public synchronized void removeLoan() {
		loan = null;
	}
	public synchronized Loan getLoan() {
		return loan;
	}

	public synchronized void addReservation(Object subscriber) {
		if (subscriber == null) {
			throw new NullPointerException("Subscriber can not be null");
		}
		if (!(subscriber instanceof subscribers.Student) && !(subscriber instanceof subscribers.Pensioner)
				&& !(subscriber instanceof subscribers.PremiumMember)) {
			throw new IllegalArgumentException("Unsupported subscriber type");
		}
		if ("Game".equals(type) && !(subscriber instanceof subscribers.Student)) {
			throw new IllegalArgumentException("Games may only be reserved by Students");
		}
		if (reservations.contains(subscriber)) {
			throw new IllegalStateException("Subscriber already has a reservation for this resource");
		}
		if (subscriber instanceof subscribers.PremiumMember) {
			// Premium holds precede standard holds; each group remains FIFO.
			int firstNonPremium = 0;
			while (firstNonPremium < reservations.size()
					&& reservations.get(firstNonPremium) instanceof subscribers.PremiumMember) {
				firstNonPremium++;
			}
			reservations.add(firstNonPremium, subscriber);
		} else {
			reservations.add(subscriber);
		}
	}

	public synchronized boolean isNextReservation(Object subscriber) {
		return !reservations.isEmpty() && reservations.get(0) == subscriber;
	}

	public synchronized boolean hasReservations() {
		return !reservations.isEmpty();
	}

	public synchronized void fulfillReservation(Object subscriber) {
		if (isNextReservation(subscriber)) {
			reservations.remove(0);
		}
	}

	public synchronized void cancelReservation(Object subscriber) {
		reservations.remove(subscriber);
	}

	public synchronized List<Object> getReservations() {
		return new ArrayList<Object>(reservations);
	}

	public int getMaxLoanPeriod() {
		if ( "Book".equals(type) ) {
			return 21;
		}
		if ( "X-Box".equals(gameType) ) {
			return 7;
		}
		else if ( "PS2".equals(gameType) ) {
			return 7;
		}
		else if ( "Amstrad".equals(gameType) ) {
			return 14;
		}
		return 28;
	}
	public double getSurcharge() {
		if ( "Book".equals(type) ) {
			return 0;
		}
		if ( "X-Box".equals(gameType) ) {
			return 1.0;
		}
		else if ( "PS2".equals(gameType) ) {
			return 1.5;
		}
		else if ( "Amstrad".equals(gameType) ) {
			return .5;
		}
		return 0;
	}
	public String getFormattedString() {
		if ( "Book".equals(type) )
			return "Book - " + title + " by " + author;
		else
			return "Game - " + title + " for the " + gameType;
	}
}
