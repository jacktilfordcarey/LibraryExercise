# Decisions

## Fixed
- Loan creation is validated and serialized: a resource cannot be loaned twice, only Students may borrow games, null/unknown inputs are rejected, and subscriber limits are enforced before state changes. The documented Student limit is five; Premium members retain the required limit of ten.
- Loan and return attempts are audited through the injected `AuditLogger` interface, including rejected attempts. The simulator uses a console adapter; production should inject TraceAudit's adapter.
- Overdue days now compare calendar dates across month/year boundaries. Game return surcharges are calculated per overdue day using the existing platform rates; books have no surcharge.
- Reservations are held per resource in FIFO order, with Premium members inserted ahead of non-premium subscribers. A reservation is fulfilled only when its holder borrows the resource; holders can cancel.
- File reports now use UTF-8, close each writer, and append each report section. The simulator registers file and console output services.
- Added a standalone build/test command and tests for borrowing limits, game eligibility, premium reservation priority, returns/fees, calendar boundaries, concurrent duplicate loans, and file output. These scenarios target the stated borrowing rules, transaction invariants, date arithmetic, and 50-librarian concurrency risk.

## Deliberately left / assumptions
- Existing platform rates (X-Box 1.00, PS2 1.50, Amstrad 0.50) are assumed to be currency units per overdue day. Rates and currency were not specified.
- "Games may only be borrowed by Students" is applied literally, including Premium members.
- Reservation duration, expiry, and priority among non-premium subscribers are unspecified. I assumed reservations do not expire automatically, FIFO order applies among non-premium subscribers, cancellation is explicit, and only the holder may borrow while a resource has a reservation queue. I would confirm these with Trace before deploying.
- Premium monthly subscription remains recorded as a member attribute; billing and payment processing are outside the stated loan-service scope.
- Platform events are an integration design, not implemented against a nonexistent broker: publish `LoanCreated` (loan/resource/subscriber IDs and loan date), `LoanRejected` (attempt IDs and reason code), `LoanReturned` (loan ID, return date and assessed surcharge), `ResourceReserved`/`ReservationCancelled` (resource, subscriber and queue position), and optionally `ResourceRegistered`. Use a transactional outbox written with the domain transaction, stable event IDs, at-least-once delivery, idempotent consumers, retries, and dead-letter/alerting. Rejected attempts need durable audit even when the domain transaction is rejected.

## Rebuild / validation
- I would use explicit domain types, immutable identifiers and dates, a transactional persistence layer, and integration contracts for audit and events rather than process-wide singleton extents and public mutable fields.
- Assumed Java 8+ (the implementation uses `java.time`). Run `build.bat test` to compile and test, or `build.bat run` to compile and run the simulator.
- I used an AI assistant to inspect the starter code, implement changes, and draft/run focused tests. I reviewed the resulting changes and remain responsible for them.
- The supplied workspace was not a Git repository, so I initialized one and recorded the implementation separately from its build, tests and decisions.
