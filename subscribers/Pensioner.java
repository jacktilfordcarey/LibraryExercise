package subscribers;
import loans.*;

import java.util.*;

import reporting.OutputServiceContributor;

public class Pensioner  implements OutputServiceContributor {
	public String id;
	public String name;
	public Map<String, Loan> loans = new Hashtable<String, Loan>();
	
	public Pensioner(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public Pensioner(String name) {
		this.name = name;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	
	public synchronized void addLoan(Loan loan) throws Exception {
		if (loan == null) throw new NullPointerException("Loan can not be null");
		if ( loans.size() >= getMaxBorrowing() ) {
			throw new Exception(name+" has reached their borrowing limit.");
		}
		loan.setSubscriber(this);
		loans.put(loan.getId(), loan);
	}
	
	public synchronized void removeLoan(Loan loan) {
		loans.remove(loan.getId());
	}
	
	public synchronized Collection<Loan> getLoans() {
		return new ArrayList<Loan>(loans.values());
	}
	
	public int getMaxBorrowing(){
		return 3;
	}
	
	public String getFormattedString() {
		return "Pensioner: " + name;
	}
}
