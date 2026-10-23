package audit;

public class ConsoleAuditLogger implements AuditLogger {
	public synchronized void log(String category, String action, String detail) {
		System.out.println("AUDIT category=" + category + " action=" + action + " detail=" + detail);
	}
}
