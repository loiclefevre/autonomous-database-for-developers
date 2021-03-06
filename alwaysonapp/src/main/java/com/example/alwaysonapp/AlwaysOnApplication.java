package com.example.alwaysonapp;

import com.example.alwaysonapp.model.ApplicationContinuityConfiguration;
import com.example.configuration.OciConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.logging.LogManager;

import static java.sql.Types.NUMERIC;
import static java.sql.Types.VARCHAR;

/**
 * This demo shows how to configure Consumer Groups to enable Transparent Application Continuity. It requires the
 * Oracle Universal Connection Pool (or UCP) instead of Hikari!
 * <p>
 * Restarting the database while the application runs transactions should produce a slight delay of 12-15 seconds.
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class AlwaysOnApplication implements CommandLineRunner {
	static {
		System.setProperty("oracle.jdbc.defaultConnectionValidation","SOCKET");

		// To display what happens behind the curtain:
		// REMOVE IN PRODUCTION!
		System.setProperty("oracle.jdbc.Trace", "true"); //-1- enable Oracle JDBC driver tracing

		//-2- enable Oracle JDBC driver Replay tracing
		try {
			final LogManager logManager = LogManager.getLogManager();
			try (final InputStream is = AlwaysOnApplication.class.getResourceAsStream("/jdbc_log.properties")) {
				logManager.readConfiguration(is);
			}
		}
		catch (Exception ignored) {
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(AlwaysOnApplication.class);

	private static final String TAC_FAILOVER_TYPE = "AUTO";

	private final OciConfiguration ociConfiguration;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private SimpleJdbcCall enableTransparentApplicationContinuity;

	private int number = 0;

	public AlwaysOnApplication(OciConfiguration ociConfiguration, TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate) {
		this.ociConfiguration = ociConfiguration;
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	@PostConstruct
	void init() {
		// Allows getting output values without any lower/upper case "problem"
		jdbcTemplate.setResultsMapCaseInsensitive(true);

		// Preparing Stored Procedures/Functions call using SimpleJdbcCall
		enableTransparentApplicationContinuity = new SimpleJdbcCall(jdbcTemplate)
				.withSchemaName("SYS")
				.withCatalogName("DBMS_APP_CONT_ADMIN")
				.withProcedureName("enable_tac")
				.withoutProcedureColumnMetaDataAccess()
				.declareParameters(
						new SqlParameter("service_name", VARCHAR),
						new SqlParameter("failover_restore", VARCHAR),
						new SqlParameter("replay_initiation_timeout", NUMERIC)
				);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		LOG.info("oracle.jdbc.defaultConnectionValidation: {}", System.getProperty("oracle.jdbc.defaultConnectionValidation"));

		setupTransparentApplicationContinuity();

		while (true) {
			insertRows(10);

			Thread.sleep(50L);
		}
	}

	private static final int ONE_ROW_DELAY_IN_MILLISECONDS = 500;

	private void insertRows(final int numberOfRows) {
		final long startTransactionTime = System.currentTimeMillis();
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				for (int i = 0; i < numberOfRows; i++) {
					final long startTime = System.currentTimeMillis();
					update();
					final long endTime = System.currentTimeMillis();
					if (endTime - startTime > ONE_ROW_DELAY_IN_MILLISECONDS * 2) {
						LOG.warn("\t- Inserted 1 row in {}ms", endTime - startTime);
					}
				}
			}
		});

		final long endTransactionTime = System.currentTimeMillis();
		if (endTransactionTime - startTransactionTime > ONE_ROW_DELAY_IN_MILLISECONDS * (numberOfRows + 2)) {
			LOG.warn("- /!\\ WARNING /!\\ Transaction with {} row(s) lasted {}s", numberOfRows, String.format(Locale.US, "%.3f", (double) (endTransactionTime - startTransactionTime) / 1000d));
		}
		else {
			LOG.info("- Transaction with {} row(s) lasted {}ms", numberOfRows, endTransactionTime - startTransactionTime);
		}
	}

	private void update() {
		jdbcTemplate.update("insert into always_on (data) values (?)",
				ps -> ps.setString(1, String.format("Hello %d!", ++number))
		);

		try {
			Thread.sleep(ONE_ROW_DELAY_IN_MILLISECONDS);
		}
		catch (InterruptedException ignored) {
		}
	}

	private void setupTransparentApplicationContinuity() {
		LOG.info("Connected with database consumer group: {}", ociConfiguration.getDatabaseConsumerGroup());

		ApplicationContinuityConfiguration alwaysOnConfig = jdbcTemplate.queryForObject(String.format("""
				SELECT name,
				       nvl(failover_type,'Never configured'),
				       failover_retries,
				       failover_delay,
				       failover_restore,
				       commit_outcome,
				       replay_initiation_timeout,
				       session_state_consistency
				  FROM ALL_SERVICES
				 WHERE name like '%%%s%%'
				""", ociConfiguration.getDatabaseConsumerGroup()), (rs, rowNum) ->
				new ApplicationContinuityConfiguration(rs.getString(1), rs.getString(2)));

		LOG.info("Current consumer group configuration is set to {}", alwaysOnConfig.failoverType());

		// Transparent Application Continuity requires the AUTO value for failover type
		if (!TAC_FAILOVER_TYPE.equals(alwaysOnConfig.failoverType())) {
			final ApplicationContinuityConfiguration finalAlwaysOnConfig = alwaysOnConfig;
			transactionTemplate.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					SqlParameterSource in = new MapSqlParameterSource()
							.addValue("service_name", finalAlwaysOnConfig.name())
							.addValue("failover_restore", TAC_FAILOVER_TYPE)
							.addValue("replay_initiation_timeout", 60);

					final Map<String, Object> out = enableTransparentApplicationContinuity.execute(in);
				}
			});

			alwaysOnConfig = jdbcTemplate.queryForObject(String.format("""
					SELECT name,
					       nvl(failover_type,'Never configured'),
					       failover_retries,
					       failover_delay,
					       failover_restore,
					       commit_outcome,
					       replay_initiation_timeout,
					       session_state_consistency
					  FROM ALL_SERVICES
					 WHERE name like '%%%s%%'
					""", ociConfiguration.getDatabaseConsumerGroup().toLowerCase()), (rs, rowNum) ->
					new ApplicationContinuityConfiguration(rs.getString(1), rs.getString(2)));

			LOG.info("Current consumer group has been configured to {}", alwaysOnConfig.failoverType());
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(AlwaysOnApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
