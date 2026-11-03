# H2 Database Persistence & Console Bugfix Design

## Overview

The LoanMS application (Struts 1 + iBATIS 2.3.4.726 + H2 2.2.224) is supposed to store
customer data in an H2 database through iBATIS, but today it does not persist anything.
Three layered defects, confirmed by reading the code, combine to produce the bug:

1. **In-memory datasource** — `src/main/resources/SqlMapConfig.xml` sets the JDBC URL to
   `jdbc:h2:mem:yodadb;DB_CLOSE_DELAY=-1`. An in-memory database lives only for the JVM
   lifetime, so the database vanishes when the embedded Tomcat process (`ServerLauncher`)
   stops.
2. **No browser access** — `ServerLauncher.main(...)` starts only embedded Tomcat; no H2
   Web Console is started, so there is no URL to browse or query the database.
3. **The data layer is never wired to the database** — `CustomerAction` performs all CRUD
   against a `static CopyOnWriteArrayList<Customer>` seeded in a `static {}` block. There is
   no `SqlMapClient` bootstrap anywhere, the iBATIS map `Customer.xml` referenced by
   `SqlMapConfig.xml` does not exist, and no `CUSTOMER` table is ever created. Because of
   this, even a file-based URL on its own would persist an empty, unused database while the
   UI keeps mutating memory.

This design therefore covers **true end-to-end persistence**, not just datasource
configuration. The earlier "data-layer wiring is out of scope" framing is dropped and
replaced with a concrete design for that wiring. The fix spans:

- **`SqlMapConfig.xml`** — switch to a file-based, mixed-mode datasource.
- **`ServerLauncher.java`** — start the H2 Web Console (port 8082) and initialize the schema
  before Tomcat begins serving.
- **`Customer.xml`** (new) — the iBATIS SQL map with select/insert/update/delete statements.
- **`SqlMapClientFactory`** (new) — a singleton that builds one `SqlMapClient` from
  `SqlMapConfig.xml`.
- **`CustomerDao`** (new) — a thin DAO wrapping the `SqlMapClient` CRUD calls plus schema
  init/seed.
- **`CustomerAction.java`** (modified) — replace the static list with DAO calls while
  preserving every observable UI behavior (flows, validation, success messages, form reset,
  `customerList` attribute, `list` forward).

The existing `Customer` model and Struts forms are retained unchanged.

### Confirmed iBATIS 2.3.4.726 API facts

The dependency is `org.apache.ibatis:ibatis-sqlmap:2.3.4.726`. Despite the `org.apache.ibatis`
Maven groupId, the jar repackages the classic iBATIS 2.3.4 and the **Java packages are still
`com.ibatis.*`** (verified by inspecting the jar). The relevant API, confirmed via `javap`:

- `com.ibatis.common.resources.Resources.getResourceAsReader(String)` → `java.io.Reader`
- `com.ibatis.sqlmap.client.SqlMapClientBuilder.buildSqlMapClient(Reader)` → `SqlMapClient`
- `com.ibatis.sqlmap.client.SqlMapClient` executor methods (all throw `java.sql.SQLException`):
  - `List queryForList(String id)` and `List queryForList(String id, Object param)`
  - `Object queryForObject(String id, Object param)`
  - `Object insert(String id, Object param)`
  - `int update(String id, Object param)`
  - `int delete(String id, Object param)`
- `SqlMapClient` also exposes (via `SqlMapTransactionManager`):
  - `javax.sql.DataSource getDataSource()`
  - `java.sql.Connection getCurrentConnection()` / `getUserConnection()` / `setUserConnection(Connection)`
  - `startTransaction()` / `commitTransaction()` / `endTransaction()`

`getDataSource()` returning a `javax.sql.DataSource` is the key fact that lets us run DDL
(the `CREATE TABLE`) via a plain JDBC `Statement` from the same datasource iBATIS uses,
without needing a separate H2 `RunScript` call or a `jdbc.jar`-level connection.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug — data must survive a restart,
  OR the user needs browser access to the database, OR a customer write made through the UI
  is expected to actually reach the persistent database.
- **Property (P)**: The desired behavior — a persistent file-based datasource, a reachable
  H2 Web Console connected to that same database, and a customer data layer where every
  list/add/edit/delete flows through iBATIS/H2 and survives restarts.
- **Preservation**: Observable UI behavior that must stay unchanged — the same actions
  (`list`, `save`/`add`, `edit`, `delete`), the same `CustomerForm.validateFields()`
  validation and errors, the same success messages (`customer.success.added`,
  `customer.success.deleted`), the same form reset, the same `customerList` request attribute
  and `list` forward, the same `org.h2.Driver`/`sa`/empty-password connection, and embedded
  Tomcat on port 8080 at `/struts1-app`.
- **F**: The original application — in-memory datasource, no console, CRUD against a static
  `CopyOnWriteArrayList`, no schema, missing `Customer.xml`, no `SqlMapClient`.
- **F'**: The fixed application — file-based datasource, running H2 Web Console, idempotent
  `CUSTOMER` schema with conditional seed, a `Customer.xml` map, a singleton `SqlMapClient`,
  a `CustomerDao`, and a `CustomerAction` that performs all CRUD through the DAO.
- **In-memory database**: An H2 DB using `jdbc:h2:mem:`; exists only for the JVM lifetime.
- **File-based database**: An H2 DB using `jdbc:h2:file:`; stored on disk, survives restarts.
- **AUTO_SERVER=TRUE (mixed mode)**: An H2 URL flag that lets the first (embedded) connection
  auto-start a server so other processes — here the Web Console — can share the same file.
- **H2 Web Console**: `org.h2.tools.Server.createWebServer(...)`, a browser UI (default port
  8082) for browsing tables and running SQL.
- **SqlMapClient**: The iBATIS `com.ibatis.sqlmap.client.SqlMapClient` — the single entry
  point for all mapped statement execution.
- **SqlMapClientFactory**: `com.example.struts.db.SqlMapClientFactory` (new) — a singleton
  holder that builds the one `SqlMapClient`.
- **CustomerDao**: `com.example.struts.db.CustomerDao` (new) — wraps `SqlMapClient` CRUD and
  owns schema init/seed.
- **ServerLauncher**: `com.example.struts.ServerLauncher`, the `main` class booting embedded
  Tomcat; the place to start the console and run schema init before Tomcat serves traffic.
- **SqlMapConfig.xml**: `src/main/resources/SqlMapConfig.xml`, the iBATIS datasource config
  holding the JDBC URL being changed. (`target/` copies are Maven build artifacts and are
  never edited directly.)

## Bug Details

### Bug Condition

The bug manifests whenever (a) the application restarts after data was written, (b) the user
needs to browse/query the database from a browser, or (c) a customer write made through the
UI is expected to actually persist. The datasource is in-memory, no console is started, and
the customer CRUD path never touches the database at all.

**Formal Specification:**
```
FUNCTION isBugCondition(X)
  INPUT: X of type AppEvent
  OUTPUT: boolean

  RETURN X.isRestartAfterDataWrite = true            // persistence defect
      OR X.requiresBrowserDbAccess = true            // console defect
      OR X.isCustomerWriteExpectedToPersist = true   // data-layer-wiring defect
END FUNCTION
```

### Examples

- **Persistence defect**: Start the app, add a customer, stop the app, restart — the data is
  gone. *Expected*: the customer is still present after restart.
- **Console defect**: While the app runs, open `http://localhost:8082` — nothing responds
  because no Web Console was started. *Expected*: the H2 Web Console login page loads.
- **Data-layer-wiring defect**: Add a customer through the UI; inspect the configured H2
  database directly — the `CUSTOMER` table does not even exist, and the new row lives only in
  `CustomerAction`'s static list. *Expected*: the row exists in the `CUSTOMER` table, written
  via the `SqlMapClient`, and is visible in the Web Console.
- **Edge case (id generation after delete)**: With rows `CUST-001`, `CUST-002`, `CUST-003`,
  delete `CUST-002`, then add a new customer. Using `count + 1` would regenerate `CUST-003`
  and collide with the existing primary key. *Expected*: the new id does not collide (see
  Fix Implementation — id is derived from the max numeric suffix + 1).
- **Edge case (file-lock concurrency)**: Opening plain `jdbc:h2:file:./data/yodadb` (no mixed
  mode) from the console while the app holds the file embedded fails with
  "database may be already in use". *Expected*: `AUTO_SERVER=TRUE` lets both share the file.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors (observable at the UI level):**
- The customer actions `list`, `save`/`add`, `edit`, and `delete` continue to work with the
  same navigation and the same `list` forward (Req 3.3).
- `CustomerForm.validateFields()` still runs on add/edit, and the same validation errors
  (`errors.required`, `errors.email`) are shown (Req 3.4).
- The same success messages — `customer.success.added` on add/edit and
  `customer.success.deleted` on delete — are displayed, and the form is reset after a
  successful add/edit exactly as today (Req 3.5).
- The request attribute `customerList` is still set and the JSP renders the list identically
  (Req 3.6).
- The datasource still uses `org.h2.Driver`, username `sa`, empty password (Req 3.1).
- Embedded Tomcat still serves on port 8080 at `/struts1-app` (Req 3.2).
- The `Customer` model keeps the same fields (`id`, `name`, `email`, `phone`, `city`) and
  stays `Serializable` (Req 3.7).

**Revised preservation note:** Because the in-memory `CopyOnWriteArrayList` is being
*replaced* by DB-backed CRUD, the previous guarantee about the list itself no longer applies.
Preservation is asserted on observable UI behavior, which must be equivalent to today — now
backed by H2 instead of the static list (Req 3.6).

**Scope:**
Inputs that do not involve persistence, the console, or a persisted customer write should be
unaffected. This includes serving JSP pages, login/logout, the HTTP connector/context/port,
and the driver/credentials in the iBATIS config.

**Note:** The expected *correct* behavior for buggy inputs is defined in the Correctness
Properties section below. This section focuses on what must NOT change.

## Hypothesized Root Cause

Based on the code read during investigation, the causes are:

1. **In-memory JDBC URL (confirmed)**: `SqlMapConfig.xml` sets `JDBC.ConnectionURL` to
   `jdbc:h2:mem:yodadb;DB_CLOSE_DELAY=-1`. The `mem:` scheme is volatile; `DB_CLOSE_DELAY=-1`
   only keeps it alive while the JVM runs, it does not write to disk. Direct cause of the
   persistence defect (Req 1.1, 1.2).

2. **No H2 Web Console started (confirmed)**: `ServerLauncher.main(...)` starts only embedded
   Tomcat; there is no `org.h2.tools.Server.createWebServer(...)` call, so port 8082 is never
   opened. Direct cause of the console defect (Req 1.3).

3. **Data layer not wired to H2 (confirmed)**: `CustomerAction` holds a static
   `CopyOnWriteArrayList<Customer>` seeded in a `static {}` block and performs all CRUD on it.
   No code loads `SqlMapConfig.xml`, builds a `SqlMapClient`, or opens a JDBC connection; the
   referenced `Customer.xml` does not exist; no `CUSTOMER` table is ever created. Direct cause
   of the data-layer-wiring defect (Req 1.4, 1.5, 1.6). This is the root cause that makes the
   persistence fix meaningful rather than cosmetic.

4. **Concurrency constraint on file DBs (design consideration)**: A file-based H2 DB opened
   embedded by the app cannot be opened simultaneously by the console unless mixed mode
   (`AUTO_SERVER=TRUE`) is used. Ignoring this would turn the persistence fix into a file-lock
   failure.

5. **Fragile id generation (confirmed, latent)**: New ids are built as
   `"CUST-" + String.format("%03d", CUSTOMERS.size() + 1)`. Derived from the collection size,
   this already collides after a delete-then-add. Moving to the database makes this a
   primary-key violation, so the fix derives the next id from the max existing numeric suffix.

## Correctness Properties

Property 1: Bug Condition - Persistent Datasource

_For any_ input where the bug condition holds (isBugCondition returns true), the fixed
application SHALL have its iBATIS datasource configured to a file-based H2 database whose
contents are written to disk and survive JVM restarts, while still using the `org.h2.Driver`
driver, username `sa`, and empty password.

**Validates: Requirements 2.1, 2.2**

Property 2: Bug Condition - Reachable Console

_For any_ input where the bug condition holds (isBugCondition returns true), the fixed
application SHALL run an H2 Web Console reachable in a browser at `http://localhost:8082` that
can connect to the same persistent database using the configured JDBC URL, username `sa`, and
empty password, so tables can be browsed and queried.

**Validates: Requirements 2.3, 2.4**

Property 3: Bug Condition - End-to-End Data Wiring

_For any_ input where the bug condition holds (isBugCondition returns true), the fixed
application SHALL ensure a `CUSTOMER` table exists (created idempotently with
`CREATE TABLE IF NOT EXISTS`), seed exactly the two demo rows only when the table is empty,
and perform every customer list/add/edit/delete through the `SqlMapClient` (via `CustomerDao`)
against that table, such that a write made through the UI is present in the database
immediately and still present after a restart, and demo rows are never re-seeded or
duplicated when the table already contains data.

**Validates: Requirements 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11**

Property 4: Preservation - Existing UI Behavior and Connectivity

_For any_ input where the bug condition does NOT hold (isBugCondition returns false), the
fixed application SHALL produce the same observable result as the original application,
preserving the customer actions and `list` forward, `CustomerForm.validateFields()` behavior
and error messages, the success messages (`customer.success.added`, `customer.success.deleted`),
form reset, the `customerList` request attribute, the `Customer` model fields and
serializability, the `org.h2.Driver`/`sa`/empty-password connection, and embedded Tomcat on
port 8080 at `/struts1-app`.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**

### Formal behavior specification

```
FUNCTION expectedBehavior(result)
  INPUT: result of a fixed-application operation
  OUTPUT: boolean

  RETURN result.configuredUrl STARTS WITH "jdbc:h2:file:"
     AND result.consoleReachableAt = "http://localhost:8082"
     AND result.customerTableExists = true
     AND result.writeExecutedVia = "SqlMapClient"
     AND result.rowPresentInDbAfterUiAction = true
     AND result.rowStillPresentAfterRestart = true
     AND (result.tableWasEmptyAtStartup
            IMPLIES result.demoRowsSeeded = {CUST-001, CUST-002})
     AND (NOT result.tableWasEmptyAtStartup
            IMPLIES result.demoRowsReseeded = false)
     AND result.generatedIdCollides = false
END FUNCTION
```

## Fix Implementation

### Changes Required

Six files are touched (two new config/map files, three new Java classes, one modified Java
class). Per the build layout, only `src/` is edited; Maven re-copies resources to
`target/classes` on the next build, so the `target/` copies are left alone. `SqlMapConfig.xml`
already declares `<sqlMap resource="Customer.xml"/>`, and because `Customer.xml` will live in
`src/main/resources`, it lands on the classpath at `target/classes/Customer.xml` and resolves
via `Resources.getResourceAsReader("SqlMapConfig.xml")` / the `<sqlMap resource=...>` lookup.

---

**File 1 — `src/main/resources/SqlMapConfig.xml` (modified)**

Change the single `JDBC.ConnectionURL` property:

- Replace `jdbc:h2:mem:yodadb;DB_CLOSE_DELAY=-1`
  with `jdbc:h2:file:./data/yodadb;AUTO_SERVER=TRUE`.
  - `file:./data/yodadb` writes `data/yodadb.mv.db` under the process working directory
    (project root under `exec:java`/`ServerLauncher`). A relative `./data/` path keeps the DB
    out of `target/` so `mvn clean` does not wipe it.
  - `AUTO_SERVER=TRUE` enables mixed mode so the Web Console can share the file.
- Leave `JDBC.Driver` (`org.h2.Driver`), `JDBC.Username` (`sa`), and `JDBC.Password` (empty)
  unchanged (preserves Req 3.1).
- Leave the `<sqlMap resource="Customer.xml"/>` declaration intact — it now resolves to a real
  file.

---

**File 2 — `src/main/resources/Customer.xml` (new, iBATIS sqlMap)**

An iBATIS 2 SQL map using the SQL Map 2.0 DTD, namespace `Customer`, a `resultMap` over
`com.example.struts.model.Customer`, and five statements. The column names match the model
fields exactly (`id`, `name`, `email`, `phone`, `city`), so a `resultMap` keeps mapping
explicit and robust.

Structure (pseudostructure, not final XML):

```
<sqlMap namespace="Customer">

  <resultMap id="customerResult" class="com.example.struts.model.Customer">
    <result property="id"    column="id"/>
    <result property="name"  column="name"/>
    <result property="email" column="email"/>
    <result property="phone" column="phone"/>
    <result property="city"  column="city"/>
  </resultMap>

  <select id="selectAllCustomers" resultMap="customerResult">
    SELECT id, name, email, phone, city FROM CUSTOMER ORDER BY id
  </select>

  <select id="selectCustomerById" parameterClass="java.lang.String"
          resultMap="customerResult">
    SELECT id, name, email, phone, city FROM CUSTOMER WHERE id = #value#
  </select>

  <insert id="insertCustomer" parameterClass="com.example.struts.model.Customer">
    INSERT INTO CUSTOMER (id, name, email, phone, city)
    VALUES (#id#, #name#, #email#, #phone#, #city#)
  </insert>

  <update id="updateCustomer" parameterClass="com.example.struts.model.Customer">
    UPDATE CUSTOMER SET name = #name#, email = #email#, phone = #phone#, city = #city#
    WHERE id = #id#
  </update>

  <delete id="deleteCustomer" parameterClass="java.lang.String">
    DELETE FROM CUSTOMER WHERE id = #value#
  </delete>

</sqlMap>
```

Notes / assumptions:
- For a single scalar `String` parameter iBATIS 2 uses the `#value#` placeholder with
  `parameterClass="java.lang.String"`. This is the documented iBATIS 2 convention; stated as
  an assumption since the exact inline-parameter token matters.
- No `CREATE TABLE` statement is placed in the map. DDL is handled in Java (see File 4) via
  the datasource connection, which is simpler to run exactly once at startup and keeps the map
  purely about CRUD.

---

**File 3 — `src/main/java/com/example/struts/db/SqlMapClientFactory.java` (new)**

A singleton that builds exactly one `SqlMapClient` from `SqlMapConfig.xml`:

```
package com.example.struts.db;

import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;
import java.io.Reader;

public final class SqlMapClientFactory {
    private static volatile SqlMapClient instance;

    private SqlMapClientFactory() {}

    public static SqlMapClient getSqlMapClient() {
        if (instance == null) {
            synchronized (SqlMapClientFactory.class) {
                if (instance == null) {
                    try (Reader reader =
                             Resources.getResourceAsReader("SqlMapConfig.xml")) {
                        instance = SqlMapClientBuilder.buildSqlMapClient(reader);
                    } catch (Exception e) {
                        throw new RuntimeException(
                            "Failed to initialize iBATIS SqlMapClient", e);
                    }
                }
            }
        }
        return instance;
    }
}
```

- Uses the confirmed `com.ibatis.*` classes. `getResourceAsReader` loads
  `SqlMapConfig.xml` from the classpath (it is in `target/classes`).
- Double-checked locking with a `volatile` field keeps it a true singleton without
  serializing every call.
- Wrapping the checked exception in a `RuntimeException` is deliberate — a missing/invalid
  config at bootstrap is fatal and should surface loudly (see Error Handling).

---

**File 4 — `src/main/java/com/example/struts/db/CustomerDao.java` (new)**

A thin DAO over the `SqlMapClient`, plus schema init/seed. All mapped-statement methods
translate iBATIS `SQLException` into an unchecked `DataAccessException` (a small
`RuntimeException` subclass, declared in this package) so `CustomerAction` stays close to its
current shape and does not have to declare `SQLException` beyond its existing
`throws Exception`.

```
package com.example.struts.db;

import com.example.struts.model.Customer;
import com.ibatis.sqlmap.client.SqlMapClient;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class CustomerDao {

    private final SqlMapClient sqlMap = SqlMapClientFactory.getSqlMapClient();

    @SuppressWarnings("unchecked")
    public List<Customer> list() {
        try { return sqlMap.queryForList("selectAllCustomers"); }
        catch (SQLException e) { throw new DataAccessException("list failed", e); }
    }

    public Customer findById(String id) {
        try { return (Customer) sqlMap.queryForObject("selectCustomerById", id); }
        catch (SQLException e) { throw new DataAccessException("findById failed", e); }
    }

    public void insert(Customer c) {
        try { sqlMap.insert("insertCustomer", c); }
        catch (SQLException e) { throw new DataAccessException("insert failed", e); }
    }

    public void update(Customer c) {
        try { sqlMap.update("updateCustomer", c); }
        catch (SQLException e) { throw new DataAccessException("update failed", e); }
    }

    public void delete(String id) {
        try { sqlMap.delete("deleteCustomer", id); }
        catch (SQLException e) { throw new DataAccessException("delete failed", e); }
    }

    /** Idempotent schema creation + conditional seed. Call once at startup. */
    public void initSchemaAndSeed() {
        try (Connection conn = sqlMap.getDataSource().getConnection();
             Statement st = conn.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS CUSTOMER (" +
                "  id    VARCHAR(32) PRIMARY KEY," +
                "  name  VARCHAR(255)," +
                "  email VARCHAR(255)," +
                "  phone VARCHAR(64)," +
                "  city  VARCHAR(255))");
        } catch (SQLException e) {
            throw new DataAccessException("schema init failed", e);
        }
        if (list().isEmpty()) {
            insert(new Customer("CUST-001", "Budi Santoso",
                    "budi@example.com", "08123456789", "Jakarta"));
            insert(new Customer("CUST-002", "Siti Aminah",
                    "siti@example.com", "08234567890", "Surabaya"));
        }
    }

    /** Next id from the max existing numeric suffix + 1, collision-safe after deletes. */
    public String nextId() {
        int max = 0;
        for (Customer c : list()) {
            String id = c.getId();
            if (id != null && id.startsWith("CUST-")) {
                try { max = Math.max(max, Integer.parseInt(id.substring(5))); }
                catch (NumberFormatException ignore) { /* skip non-conforming ids */ }
            }
        }
        return String.format("CUST-%03d", max + 1);
    }
}
```

Design decisions:
- **DDL approach (chosen): plain JDBC `Statement` from `sqlMap.getDataSource().getConnection()`.**
  `getDataSource()` is confirmed on the `SqlMapClient` API, so DDL runs against the exact same
  datasource (same file, same credentials) iBATIS uses — no second connection config, no H2
  `RunScript`, no extra `.sql` file to keep in sync. `CREATE TABLE IF NOT EXISTS` is idempotent
  so restarts never fail (Req 2.5). Alternatives considered and rejected: an `<insert>`-style
  "initialize" statement in the map (iBATIS 2 has no clean DDL hook and mixing DDL into CRUD
  maps is awkward), and H2 `RunScript` (needs a separate `.sql` resource and duplicates the
  schema definition).
- **Conditional seed (Req 2.6):** seed the two demo rows only when `list().isEmpty()`. On any
  later restart the table is non-empty, so no re-seed and no duplicate/overwrite.
- **Id generation (Req 2.11 + concurrency):** `nextId()` uses `max(numeric suffix) + 1` rather
  than `count + 1`. This is collision-safe after deletes (the confirmed latent bug in the
  original `size()+1` scheme). Known residual edge: under truly concurrent adds, two requests
  could compute the same `max+1` and the second `INSERT` would hit the primary-key constraint;
  this surfaces as a `DataAccessException` rather than silent corruption. For this
  single-instance demo app that race is acceptable and documented; a production fix would use a
  DB sequence/identity or a `SELECT ... FOR UPDATE`. We deliberately do not introduce a sequence
  here to keep the `id` format (`CUST-%03d`) and the `Customer` model unchanged (Req 3.7).

---

**File 5 — `src/main/java/com/example/struts/db/DataAccessException.java` (new, small)**

```
package com.example.struts.db;

public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Unchecked wrapper so the iBATIS `SQLException` does not ripple through `CustomerAction`'s
signature. (Listed separately for completeness; could be a nested class of `CustomerDao` if a
single new file is preferred.)

---

**File 6 — `src/main/java/com/example/struts/ServerLauncher.java` (modified)**

In `main(String[] args)`, before `tomcat.start()`:

1. **Initialize schema + seed** so the DB is ready before any request can arrive:
   ```
   new com.example.struts.db.CustomerDao().initSchemaAndSeed();
   ```
   Running this in `ServerLauncher` (not lazily in the action) guarantees the table and demo
   data exist up front and makes startup failures obvious. This also triggers the first
   (embedded) H2 connection, which — with `AUTO_SERVER=TRUE` — starts the shared server the
   console will attach to.
2. **Start the H2 Web Console**:
   ```
   org.h2.tools.Server webConsole =
       org.h2.tools.Server.createWebServer("-webPort", "8082").start();
   System.out.println("🗄️  H2 Console: http://localhost:8082");
   ```
   `-webAllowOthers` is omitted for a localhost-only console; add it only if remote browser
   access is required.
3. **Register a shutdown hook** to `webConsole.stop()` so port 8082 is released cleanly:
   ```
   Runtime.getRuntime().addShutdownHook(new Thread(webConsole::stop));
   ```
4. **Leave the Tomcat port, context path (`/struts1-app`), connector setup, and the final
   `tomcat.start()` / `tomcat.getServer().await()` unchanged** (preserves Req 3.2).

Ordering: build/seed the DB and start the console, then start Tomcat, so the app never serves
a request against a missing table.

---

**File 7 — `src/main/java/com/example/struts/action/CustomerAction.java` (modified)**

Replace the static list with a `CustomerDao`. The control flow, validation, messages, form
reset, request attribute, and forward are preserved exactly.

- **Remove** the `static final List<Customer> CUSTOMERS` field, the `static {}` seed block, and
  the `CopyOnWriteArrayList`/`UUID` imports.
- **Add** `private final CustomerDao dao = new CustomerDao();` (the DAO reuses the singleton
  `SqlMapClient`, so constructing it per request is cheap; it holds no per-request state).
- **`save`/`add`** (unchanged validation + messages):
  - Still call `customerForm.validateFields()`; on errors, `saveErrors`, set
    `request.setAttribute("customerList", dao.list())`, and `findForward("list")` — identical
    shape to today, only the list source changes.
  - If `id` is present and non-blank → build a `Customer` from the form and `dao.update(c)`
    (replaces the in-place field mutation of the matching list element).
  - Else (new) → `String newId = dao.nextId();` then
    `dao.insert(new Customer(newId, name, email, phone, city));` (replaces
    `"CUST-" + (size()+1)`).
  - Still add `customer.success.added`, `saveMessages`, and `customerForm.reset(...)`.
- **`delete`**: still read `request.getParameter("id")`; if non-null call `dao.delete(id)` and
  add `customer.success.deleted` (replaces `CUSTOMERS.removeIf(...)`).
- **`edit`**: still read `request.getParameter("id")`; if non-null call
  `Customer c = dao.findById(id)` and, when found, copy its fields onto the form exactly as
  today (replaces the list scan).
- **End of `execute`**: `request.setAttribute("customerList", dao.list());` then
  `return mapping.findForward("list");` — same attribute name, same forward.

The method keeps its existing `throws Exception`; DAO errors arrive as `DataAccessException`
(unchecked), so the signature is unchanged.

### Error Handling

- **iBATIS `SQLException` wrapping**: every DAO method catches the checked `SQLException` from
  the `SqlMapClient` and rethrows a `DataAccessException` (unchecked). This keeps
  `CustomerAction` close to its current form and lets Struts' normal exception handling / the
  container surface the error, rather than swallowing it.
- **DB unavailable at startup**: `SqlMapClientFactory.getSqlMapClient()` throws a
  `RuntimeException` if `SqlMapConfig.xml` cannot be read or the client cannot be built, and
  `initSchemaAndSeed()` throws `DataAccessException` if the connection or DDL fails. Because
  both run in `ServerLauncher` before `tomcat.start()`, a misconfigured datasource fails fast
  and loudly at launch instead of producing a half-started server — the preferred behavior for
  a bootstrap-level problem.
- **Missing `Customer.xml` on the classpath**: surfaces as a build failure of the
  `SqlMapClient` inside `SqlMapClientFactory` at startup (fast-fail), which is why File 2 must
  land in `src/main/resources`.

## Testing Strategy

### Validation Approach

Two phases: first surface counterexamples that demonstrate the bug on the unfixed code, then
verify the fix works and preserves existing UI behavior. Because parts of this bug are
runtime/configuration wiring (a running JVM, a browser-reachable port, a file on disk, a real
DB round-trip), several checks are integration-style and a few are manual.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples before implementing the fix and confirm the root-cause
analysis; if refuted, re-hypothesize.

**Test Plan**: Inspect the configured URL, probe the console port, and inspect the database
directly against the current (unfixed) code.

**Test Cases**:
1. **In-memory URL assertion**: Parse `SqlMapConfig.xml`; assert the URL begins with
   `jdbc:h2:mem:` (holds on unfixed code — persistence defect).
2. **No console port**: With the unfixed app running, attempt a socket/HTTP connection to
   `localhost:8082`; observe connection refused.
3. **No table / no wiring**: With the unfixed app running, add a customer through the UI, then
   open a direct JDBC connection to the configured DB and query `CUSTOMER`; observe the table
   does not exist and no `SqlMapClient` was ever built (data-layer defect).
4. **Restart loses data (edge)**: Even with a file URL, because the UI writes to the static
   list, a restart shows the re-seeded demo rows and loses UI edits.
5. **Id collision (edge)**: Add three customers, delete the middle one, add another; observe
   the `size()+1` scheme regenerates an existing id.

**Expected Counterexamples**: volatile `mem:` URL; port 8082 closed; `CUSTOMER` table absent;
`SqlMapClient` never instantiated; id collision after delete.

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed application
produces the expected behavior (persistent datasource + reachable console + end-to-end DB
wiring).

**Pseudocode:**
```
FOR ALL X WHERE isBugCondition(X) DO
  result := fixedApp(X)
  ASSERT configuredUrl(result) STARTS WITH "jdbc:h2:file:"
  ASSERT httpReachable("http://localhost:8082") = true
  ASSERT consoleConnectsTo(configuredUrl(result), "sa", "") = true
  ASSERT tableExists("CUSTOMER") = true
  ASSERT writeExecutedVia(result) = "SqlMapClient"
  ASSERT rowPresentInDbAfterUiAction(result) = true
  ASSERT rowStillPresentAfterRestart(result) = true
  ASSERT (tableEmptyAtStartup(result) IMPLIES seededRows(result) = {CUST-001, CUST-002})
  ASSERT (NOT tableEmptyAtStartup(result) IMPLIES reseeded(result) = false)
  ASSERT generatedIdCollides(result) = false
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed
application behaves identically to the original at the UI level.

**Pseudocode:**
```
FOR ALL X WHERE NOT isBugCondition(X) DO
  ASSERT uiResult(originalApp(X)) = uiResult(fixedApp(X))
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation because it
generates many sequences across the domain, catches edge cases manual tests miss, and gives
stronger guarantees that observable behavior is unchanged. The practical domain is sequences
of UI customer operations plus the static datasource/connectivity attributes.

**Test Plan**: Observe the unfixed behavior first (validation errors, success messages, form
reset, list contents within a session, port-8080 reachability, driver/user/password), then
assert the fixed code matches — now backed by a test H2 DB.

**Test Cases**:
1. **Validation preservation**: Submit an add with a blank name / invalid email; assert the
   same `errors.required` / `errors.email` messages and that the list is re-rendered.
2. **Message + reset preservation**: A valid add/edit still shows `customer.success.added` and
   resets the form; a delete still shows `customer.success.deleted`.
3. **List-render preservation**: Assert `customerList` is set and the forward is `list` on
   every path.
4. **Connectivity preservation**: Assert driver `org.h2.Driver`, user `sa`, empty password;
   assert `http://localhost:8080/struts1-app/welcome.do` still returns 200.

### Unit Tests

- **Map parses**: Build a `SqlMapClient` from `SqlMapConfig.xml` in a test (pointing at an
  in-memory test URL) and assert `Customer.xml` loads without error and all five statement ids
  (`selectAllCustomers`, `selectCustomerById`, `insertCustomer`, `updateCustomer`,
  `deleteCustomer`) are registered.
- **Config assertions**: Parse `SqlMapConfig.xml`; assert the JDBC URL uses `jdbc:h2:file:` and
  contains `AUTO_SERVER=TRUE`, and that driver/user/password are unchanged.
- **`nextId()` logic**: Given rows with suffixes {1, 2, 3}, deleting #2 then computing
  `nextId()` yields `CUST-004` (max+1), never a collision.
- **Console port**: Assert the launcher requests web port 8082.

### Property-Based Tests

- **DAO CRUD round-trip**: Against a fresh test H2 database, generate random valid `Customer`
  values and random sequences of insert/update/delete; assert `list()`/`findById()` always
  reflect the applied operations (model-based oracle).
- **Preservation of UI flows**: Generate random sequences of add/edit/delete (including invalid
  submissions) and assert the sequence of validation errors, success messages, and resulting
  `customerList` contents matches a reference model equivalent to today's behavior.
- **Seed idempotency**: Starting from a non-empty table, run `initSchemaAndSeed()` an arbitrary
  number of times; assert the row set is unchanged (no duplicates, no overwrites).

### Integration Tests

- **End-to-end persistence**: Boot `ServerLauncher`; add a customer through the UI
  (`customer.do` with `action=add`); confirm the row is in `CUSTOMER` via a direct JDBC query;
  stop the JVM; restart; confirm the row is still present (Req 2.10).
- **Console reachability + shared file**: GET `http://localhost:8082` returns the H2 console
  page, and the console can connect to `jdbc:h2:file:./data/yodadb;AUTO_SERVER=TRUE` as `sa`
  while Tomcat runs — proving `AUTO_SERVER=TRUE` resolves the file-lock constraint.
- **Tomcat preservation**: GET `http://localhost:8080/struts1-app/welcome.do` returns 200 with
  the same response as the unfixed app.
- **Clean shutdown**: Stop the JVM; confirm port 8082 is released by the shutdown hook.
