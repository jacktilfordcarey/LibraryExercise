/*
 * Created on 02-Feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package subscribers;

import java.util.*;

public class SubscriberExtent {
	static Map<String, Object> subscribers = new Hashtable<String, Object>();
	static int idCounter = 0;
	public static SubscriberExtent INSTANCE = new SubscriberExtent();
	
	private SubscriberExtent() {
		// Initialise and register library subscribers
		createPensioner(new Pensioner("Bill"));
		createPensioner(new Pensioner("Elsie"));
		createStudent(new Student("Zach", "De Montfort University"));
		createStudent(new Student("Amelia", "Christchurch College"));
		createPremiumMember(new PremiumMember("Nina", 19.99));
	}
	public synchronized void createPensioner(Pensioner pensioner) {
		if (pensioner == null) throw new NullPointerException("Pensioner can not be null");
		pensioner.setId(getNextId());
		subscribers.put(pensioner.getId(), pensioner);
	}
	public synchronized void createStudent(Student student) {
		if (student == null) throw new NullPointerException("Student can not be null");
		student.setId(getNextId());
		subscribers.put(student.getId(), student);
	}
	public synchronized void createPremiumMember(PremiumMember premiumMember) {
		if (premiumMember == null) throw new NullPointerException("Premium member can not be null");
		premiumMember.setId(getNextId());
		subscribers.put(premiumMember.getId(), premiumMember);
	}
	public static synchronized String getNextId() {
		return "S" + (idCounter++);
	}
	public synchronized Object findByPrimaryKey(String id) {
		return subscribers.get(id);
	}
	public synchronized Collection<Object> getSubscribers() {
		return new ArrayList<Object>(subscribers.values());
	}
}
