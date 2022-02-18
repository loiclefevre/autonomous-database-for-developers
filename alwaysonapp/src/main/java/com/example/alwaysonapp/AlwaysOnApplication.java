package com.example.alwaysonapp;

import com.example.alwaysonapp.model.ApplicationContinuityConfiguration;
import com.example.configuration.OciConfiguration;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;

import java.util.Map;

import static java.sql.Types.*;

/**
 * This demo shows how to connect to an Autonomous Database using the default HikariCP
 * connection pool. It uses the EasyConnect way that allows passing properties to create
 * connections (such as the row fetch size, default to 10 for Oracle JDBC driver...).
 * <p>
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class AlwaysOnApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(AlwaysOnApplication.class);

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

		// best practices: https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/application-continuity-code.html#GUID-9479BBBA-C058-482D-85CC-904CEE1B4FF8

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

		if(!"AUTO".equals(alwaysOnConfig.failoverType())) {
			final ApplicationContinuityConfiguration finalAlwaysOnConfig = alwaysOnConfig;
			transactionTemplate.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					SqlParameterSource in = new MapSqlParameterSource()
							.addValue("service_name", finalAlwaysOnConfig.name())
							.addValue("failover_restore", "AUTO")
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
				""", ociConfiguration.getDatabaseConsumerGroup()), (rs, rowNum) ->
					new ApplicationContinuityConfiguration(rs.getString(1), rs.getString(2)));

			LOG.info("Current consumer group has been configured to {}", alwaysOnConfig.failoverType());
		}

		while (true) {
			insertRows(10);

			Thread.sleep(50L);
		}
	}

	private void insertRows(final int numberOfRows) {
		final long startTransactionTime = System.currentTimeMillis();
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				for (int i = 0; i < numberOfRows; i++) {
					final long startTime = System.currentTimeMillis();
					jdbcTemplate.update("insert into always_on (data) values (?)",
							ps -> ps.setString(1, String.format("Hello %d!", ++number))
					);
					final long endTime = System.currentTimeMillis();
					if(endTime - startTime > 200) {
						LOG.warn("\t- Inserted 1 row in {}ms", endTime - startTime);
					}
				}
			}
		});

		final long endTransactionTime = System.currentTimeMillis();
		if(endTransactionTime - startTransactionTime > 500) {
			LOG.warn("- /!\\ WARNING /!\\ Transaction with {} row(s) lasted {}s", numberOfRows, String.format("%.3f", (double)(endTransactionTime - startTransactionTime)/1000d));
		} else {
			LOG.info("- Transaction with {} row(s) lasted {}ms", numberOfRows, endTransactionTime - startTransactionTime);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(AlwaysOnApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
