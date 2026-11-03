# Bugfix Requirements Document

## Introduction

The LoanMS application (Struts 1 + iBATIS + H2) is meant to store customer data in an H2 database through iBATIS, but today it does not actually persist anything. There are two layered defects:

1. **In-memory datasource** — the JDBC connection URL in `src/main/resources/SqlMapConfig.xml` points to an **in-memory** H2 database (`jdbc:h2:mem:yodadb;DB_CLOSE_DELAY=-1`). An in-memory database exists only for the lifetime of the JVM, so when the embedded Tomcat process (`ServerLauncher`) stops, the database and its contents vanish.

2. **The data layer is never wired to the database at all** — `CustomerAction` performs all CRUD (list/add/edit/delete) against a `static CopyOnWriteArrayList<Customer>` seeded with two demo rows in a static block. There is no `SqlMapClient` bootstrap anywhere, the iBATIS map `Customer.xml` referenced by `SqlMapConfig.xml` does not exist, and no `CUSTOMER` table schema is ever created. Because of this, even a file-based URL alone would persist an empty, unused database while the UI continues to operate purely in memory.

This fix is therefore **expanded beyond datasource configuration** to deliver true end-to-end persistence: customer data entered through the Struts UI must be written to a file-based H2 database through iBATIS and must survive application restarts.

Three outcomes are required:
1. The H2 database must be **file-based (persistent)** so data is written to disk and survives restarts.
2. The user must be able to **browse and query the database from a web browser** via the H2 Web Console (default `http://localhost:8082`).
3. The customer data layer must be **wired end-to-end to iBATIS/H2** — a `CUSTOMER` table, an iBATIS SQL map, a bootstrapped `SqlMapClient`, and a `CustomerAction` that performs CRUD through that client instead of the in-memory list — so that UI changes are actually persisted.

The scope of this fix spans: the datasource configuration (`SqlMapConfig.xml`), the application startup path (`ServerLauncher`, where the H2 Web Console is started and the schema is initialized), a new iBATIS map (`Customer.xml`), a `SqlMapClient` bootstrap/DAO, and `CustomerAction` (switched from the static list to DB-backed CRUD). The existing `Customer` model, Struts forms, UI flows, validation, and success messages are retained.

## Bug Analysis

### Current Behavior (Defect)

The application uses an in-memory H2 database, provides no browser-accessible console, and never actually reads from or writes to the database — all customer CRUD happens against a static in-memory list.

1.1 WHEN the application is stopped and restarted THEN the system loses all previously stored customer data
1.2 WHEN the JDBC connection URL uses the `jdbc:h2:mem:` (in-memory) scheme THEN the system holds data only in JVM memory and discards it when the process ends
1.3 WHEN the user attempts to open the database in a web browser THEN the system provides no web console URL to connect to, because no H2 Web Console is started
1.4 WHEN a customer is added, edited, or deleted through the Struts UI THEN the system mutates a `static CopyOnWriteArrayList<Customer>` in `CustomerAction` instead of the H2 database, so no change is ever written to persistent storage
1.5 WHEN the application starts THEN the system has no `CUSTOMER` table schema and no bootstrapped `SqlMapClient`, and the iBATIS map `Customer.xml` referenced by `SqlMapConfig.xml` does not exist, so the database is never usable by the application
1.6 WHEN the customer list is displayed THEN the system always shows the two demo rows re-seeded in a static block (`CUST-001` Budi Santoso, `CUST-002` Siti Aminah) rather than data read from the database

### Expected Behavior (Correct)

The application uses a file-based (persistent) H2 database, exposes a web console for browsing, and performs all customer CRUD through iBATIS against that database so changes persist across restarts.

2.1 WHEN the application is stopped and restarted THEN the system SHALL retain all previously stored customer data
2.2 WHEN the JDBC connection URL is configured THEN the system SHALL use a file-based H2 database (e.g. `jdbc:h2:file:...`) so that data is written to disk and survives process restarts
2.3 WHEN the application is running THEN the system SHALL start the H2 Web Console and make it reachable from a browser at a documented URL (default `http://localhost:8082`)
2.4 WHEN the user opens the H2 Web Console URL and connects using the same JDBC URL, username (`sa`), and password THEN the system SHALL allow the user to browse tables and run queries against the persistent database
2.5 WHEN the application starts THEN the system SHALL ensure a `CUSTOMER` table exists in the persistent H2 database with columns `id` (primary key), `name`, `email`, `phone`, and `city`, creating it idempotently (e.g. `CREATE TABLE IF NOT EXISTS`) so startup does not fail when the table already exists
2.6 WHEN the application starts AND the `CUSTOMER` table is empty THEN the system SHALL seed exactly the two demo rows (`CUST-001` Budi Santoso budi@example.com 08123456789 Jakarta; `CUST-002` Siti Aminah siti@example.com 08234567890 Surabaya); WHEN the table already contains rows THEN the system SHALL NOT re-seed, so that restarts neither duplicate demo data nor overwrite user edits
2.7 WHEN the application needs database access THEN the system SHALL provide an iBATIS `Customer.xml` SQL map defining select-all, select-by-id, insert, update, and delete statements mapped to the `Customer` model
2.8 WHEN the application starts THEN the system SHALL bootstrap a single (singleton) `SqlMapClient` built from `SqlMapConfig.xml` and reuse it for all customer operations
2.9 WHEN a customer list/add/edit/delete action is requested THEN the system SHALL perform the operation through the `SqlMapClient` (via a DAO) against the H2 database instead of the static in-memory list
2.10 WHEN a customer is added, edited, or deleted through the UI and the application is then restarted THEN the system SHALL still reflect that change after restart (true end-to-end persistence)
2.11 WHEN a new customer is added THEN the system SHALL generate its id consistently with the existing scheme (`CUST-` followed by a zero-padded sequence) based on the persisted data

### Unchanged Behavior (Regression Prevention)

Existing connectivity, UI flows, and server configuration must be preserved. Because the in-memory list is being **replaced** by DB-backed CRUD, the previous preservation guarantee about the in-memory list itself no longer applies and is revised to a behavioral-equivalence guarantee at the UI level.

3.1 WHEN the application starts THEN the system SHALL CONTINUE TO connect to the H2 database using the `org.h2.Driver` driver, username `sa`, and the existing (empty) password
3.2 WHEN the embedded Tomcat server starts via `ServerLauncher` THEN the system SHALL CONTINUE TO serve the web application on port 8080 at `/struts1-app`
3.3 WHEN a user interacts with the customer screens THEN the system SHALL CONTINUE TO support the same UI flows and actions (`list`, `save`/`add`, `edit`, `delete`) with the same navigation/forwards as today
3.4 WHEN form fields are submitted THEN the system SHALL CONTINUE TO apply the existing `CustomerForm` validation and display the same validation errors as today
3.5 WHEN an add/edit/delete succeeds THEN the system SHALL CONTINUE TO display the existing success messages (`customer.success.added`, `customer.success.deleted`) and reset the form as it does today
3.6 WHEN customer CRUD is performed within a single running session THEN the system SHALL CONTINUE TO work and produce UI results equivalent to today's behavior — now backed by the H2 database rather than the static in-memory list (revised from the former "same-session in-memory-list CRUD untouched" guarantee)
3.7 WHEN the `Customer` model is used THEN the system SHALL CONTINUE TO expose the same fields (`id`, `name`, `email`, `phone`, `city`) and remain `Serializable`

## Bug Condition and Properties

**Key Definitions**
- **F**: The original application — in-memory datasource, no web console, no data layer wiring (CRUD against a static `CopyOnWriteArrayList`), no `CUSTOMER` schema, missing `Customer.xml`, no `SqlMapClient`.
- **F'**: The fixed application — file-based datasource, enabled H2 Web Console, `CUSTOMER` schema created idempotently on startup with conditional demo seeding, an iBATIS `Customer.xml` map, a singleton `SqlMapClient`, and a `CustomerAction` that performs all CRUD through the `SqlMapClient`/DAO.

**Bug Condition** — identifies inputs/states that trigger the bug:

```pascal
FUNCTION isBugCondition(X)
  INPUT: X of type AppEvent
  OUTPUT: boolean

  // The bug is triggered whenever data must survive a restart, whenever the user
  // needs browser access to the DB, or whenever a customer write is expected to
  // actually reach the persistent database.
  RETURN X.isRestartAfterDataWrite = true
      OR X.requiresBrowserDbAccess = true
      OR X.isCustomerWriteExpectedToPersist = true
END FUNCTION
```

**Property: Fix Checking** — correct behavior for buggy inputs:

```pascal
// Persistence across restarts
FOR ALL X WHERE X.isRestartAfterDataWrite = true DO
  result ← F'(X)
  ASSERT dataWrittenBeforeRestart IS PRESENT IN result.databaseAfterRestart
END FOR

// Browser-accessible console
FOR ALL X WHERE X.requiresBrowserDbAccess = true DO
  result ← F'(X)
  ASSERT result.h2ConsoleReachableAt = "http://localhost:8082"
     AND result.consoleCanConnectToPersistentDb = true
END FOR

// End-to-end data layer wiring: UI writes reach the database and survive restart
FOR ALL X WHERE X.isCustomerWriteExpectedToPersist = true DO
  result ← F'(X)
  ASSERT result.customerTableExists = true          // CREATE TABLE IF NOT EXISTS
     AND result.writeExecutedVia = "SqlMapClient"    // not the static in-memory list
     AND result.rowPresentInDbAfterUiAction = true
     AND result.rowStillPresentAfterRestart = true
     AND (result.tableWasEmptyAtStartup
            IMPLIES result.demoRowsSeeded = {CUST-001, CUST-002})
     AND (NOT result.tableWasEmptyAtStartup
            IMPLIES result.demoRowsReseeded = false) // no duplicate/overwrite on restart
END FOR
```

**Property: Preservation Checking** — non-buggy inputs behave identically at the UI level:

```pascal
// Property: Preservation Checking
FOR ALL X WHERE NOT isBugCondition(X) DO
  ASSERT uiResult(F(X)) = uiResult(F'(X))
END FOR
```

Because the fix intentionally **replaces** the in-memory storage mechanism, preservation is asserted on observable UI behavior (same flows, same validation, same success messages, same server on port 8080 at `/struts1-app`, same `org.h2.Driver`/`sa`/empty-password connection) rather than on internal storage. For single-session customer CRUD, the fixed application produces results equivalent to the original — now backed by the H2 database instead of the static list.
