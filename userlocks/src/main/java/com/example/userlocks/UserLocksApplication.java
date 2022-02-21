package com.example.userlocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;

import static com.example.userlocks.locks.Locks.*;
import static java.sql.Types.*;

/**
 * This demo shows how to connect to an Autonomous Database using the default HikariCP
 * connection pool. It uses the EasyConnect way that allows passing properties to create
 * connections (such as the row fetch size, default to 10 for Oracle JDBC driver...).
 * <p>
 * It also shows how to access one of the 300+ PL/SQL packages made available to developers.
 * Here focusing on SYS.DBMS_LOCK that brings User Locks capabilities (from the documentation):
 * The DBMS_LOCK package has many beneficial uses. These uses include the following:
 * <ul>
 *     <li>Providing exclusive access to a device, such as a terminal</li>
 *     <li>Providing application-level enforcement of read locks</li>
 *     <li>Detecting when a lock is released and cleanup after the application stops</li>
 *     <li>Synchronizing applications and enforcing sequential processing</li>
 * </ul>
 * <p>
 * The database user must be granted the EXECUTE privilege on the SYS.DBMS_LOCK PL/SQL package,
 * for example connected as ADMIN: <b>grant execute on SYS.DBMS_LOCK to <user>;</b>
 *
 * @author Loïc Lefèvre
 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/arpls/DBMS_LOCK.html#GUID-4DECD58D-6D07-4723-B275-40E97902032C">DBMS_LOCK overview</a>
 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/arpls/">all PL/SQL packages for 19c</a>
 * @see <a href="https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/appendix-database-pl-sql-packages-restrictions.html#GUID-829A7D07-1EA4-4F59-AA60-F780FABAFDEC">here for any limitation</a>
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class UserLocksApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(UserLocksApplication.class);

	// Use this for passing PL/SQL Boolean parameters
	private static final int PLSQL_BOOLEAN = 252;

	private final JdbcTemplate jdbcTemplate;

	public UserLocksApplication(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	// PL/SQL procedures/functions that we can call
	private SimpleJdbcCall getLockHandleFromName;
	private SimpleJdbcCall requestLockByName;
	private SimpleJdbcCall releaseLockByName;

	@PostConstruct
	void init() {
		// Allows getting output values without any lower/upper case "problem"
		jdbcTemplate.setResultsMapCaseInsensitive(true);

		// Preparing Stored Procedures/Functions call using SimpleJdbcCall
		getLockHandleFromName = new SimpleJdbcCall(jdbcTemplate)
				.withSchemaName("SYS")
				.withCatalogName("dbms_lock")
				.withProcedureName("allocate_unique_autonomous");

		requestLockByName = new SimpleJdbcCall(jdbcTemplate)
				.withSchemaName("SYS")
				.withCatalogName("dbms_lock")
				.withFunctionName("request")
				// no validation against database dictionary because Spring
				// doesn't support polymorphic functions... yet
				.withoutProcedureColumnMetaDataAccess()
				.declareParameters(
						new SqlOutParameter("return", NUMERIC),
						new SqlParameter("lockhandle", VARCHAR),
						new SqlParameter("lockmode", NUMERIC),
						new SqlParameter("timeout", NUMERIC),
						new SqlParameter("release_on_commit", OTHER /* for PLSQL_BOOLEAN */)
				);

		releaseLockByName = new SimpleJdbcCall(jdbcTemplate)
				.withSchemaName("SYS")
				.withCatalogName("dbms_lock")
				.withFunctionName("release")
				// no validation against database dictionary because Spring
				// doesn't support polymorphic functions... yet
				.withoutProcedureColumnMetaDataAccess()
				.declareParameters(
						new SqlOutParameter("return", NUMERIC),
						new SqlParameter("lockhandle", VARCHAR)
				);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		// Using a named lock to prevent any clash with other apps
		// by using just numbers to identify a specific user lock
		String lockName = "MyUserLock";

		LOG.info("Lock name is: {}", lockName);

		//=== 1 === Get a lock handle mapped to this lock *name* and keep it for the next 10 days
		SqlParameterSource in = new MapSqlParameterSource()
				.addValue("lockname", lockName)
				.addValue("expiration_secs", AllocationExpirationSecs);

		final Map<String, Object> out = getLockHandleFromName.execute(in);

		final String lockHandle = (String) out.get("lockhandle");

		LOG.info("Lock handle: {}", lockHandle);

		//=== 2 === Request an exclusive lock using the lock handle
		in = new MapSqlParameterSource()
				.addValue("lockhandle", lockHandle)
				.addValue("lockmode", ExclusiveMode)
				.addValue("timeout", 0 /*LockTimeout.MaximumWait*/) // Not waiting any second!
				// Do not release the lock upon commit
				.addValue("release_on_commit", false, PLSQL_BOOLEAN);

		int result = requestLockByName.executeFunction(BigDecimal.class, in).intValue();

		printRequestStatus(result, lockName);

		//=== 3 === Wait 5 seconds if lock was acquired without any problem
		// (e.g. no one else acquired it already)
		if (result != Timeout) {
			final long secondsToSleep = 5;
			LOG.info("Sleeping for {} seconds...", secondsToSleep);
			Thread.sleep(secondsToSleep * 1000L);
		}

		//=== 4 === Release the lock using its handle
		in = new MapSqlParameterSource()
				.addValue("lockhandle", lockHandle);

		result = releaseLockByName.executeFunction(BigDecimal.class, in).intValue();

		printReleaseStatus(result, lockName);
	}

	/**
	 * Translates returned code into message and display it.
	 * @param result returned code from calling sys.dbms_lock.request()
	 * @param lockHandle the lock handle
	 */
	private static void printRequestStatus(int result, String lockHandle) {
		switch (result) {
			case Success -> LOG.info("Exclusive lock {} acquired!", lockHandle);
			case AlreadyOwning -> LOG.info("Exclusive lock {} already owned by me!", lockHandle);
			case Deadlock -> LOG.info("Deadlock trying to acquire lock {}!", lockHandle);
			case Timeout -> LOG.info("Exclusive lock NOT acquired because of Timeout!");
			case IllegalLockHandle -> LOG.info("Lock {} NOT acquired because of illegal lock handle!", lockHandle);
			default -> throw new IllegalStateException("Unexpected value: " + result);
		}
	}

	/**
	 * Translates returned code into message and display it.
	 * @param result returned code from calling sys.dbms_lock.release()
	 * @param lockHandle the lock handle
	 */
	private static void printReleaseStatus(int result, String lockHandle) {
		switch (result) {
			case Success -> LOG.info("Lock {} released!", lockHandle);
			case ParameterError -> LOG.info("Lock {} can't be released because of invocation parameter error!", lockHandle);
			case NotOwning -> LOG.info("Lock {} NOT owned by me!", lockHandle);
			case IllegalLockHandle -> LOG.info("Lock {} NOT released because of illegal lock handle!", lockHandle);
			default -> throw new IllegalStateException("Unexpected value: " + result);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(UserLocksApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
