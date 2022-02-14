package com.example.dbservicenames;

import com.example.dbservicenames.model.ConsumerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import javax.annotation.PostConstruct;
import java.sql.Types;
import java.util.List;
import java.util.Objects;

/**
 * This demo shows the different database consumer groups available and their associated properties.
 * <p>
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class DatabaseConsumerGroupsApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(DatabaseConsumerGroupsApplication.class);

	@Autowired
	@Qualifier("adminJdbcTemplate")
	private JdbcTemplate jdbcTemplate;

	// PL/SQL procedures/functions that we can call
	private SimpleJdbcCall switchConsumerGroup;

	@PostConstruct
	void init() {
		// Allows getting output values without any lower/upper case "problem"
		jdbcTemplate.setResultsMapCaseInsensitive(true);

		switchConsumerGroup = new SimpleJdbcCall(jdbcTemplate)
				.withSchemaName("SYS")
				.withCatalogName("CS_SESSION")
				.withProcedureName("SWITCH_SERVICE")
				.withoutProcedureColumnMetaDataAccess()
				.withNamedBinding()
				.declareParameters(new SqlParameter("service_name", Types.VARCHAR));

	}

	@Override
	public void run(String... args) {
		LOG.info("=".repeat(126));

		final String instanceType = Objects.requireNonNull(jdbcTemplate.queryForObject(
				"select value from v$parameter where name = 'pdb_lockdown'",
				String.class));

		switch (instanceType) {
			case "JDCS" -> LOG.info("Connected to an Autonomous JSON Database");
			case "OLTP" -> LOG.info("Connected to an Autonomous Transaction Processing Database");
			case "DWCS" -> LOG.info("Connected to an Autonomous Data Warehouse");
			default -> throw new IllegalStateException("Unknown instance type: " + instanceType);
		}

		String consumerGroup = Objects.requireNonNull(jdbcTemplate.queryForObject("""
						WITH cs_table AS (SELECT regexp_substr( 
														SYS_CONTEXT('USERENV','SERVICE_NAME'), 
														'(_low.|_medium.|_high.|_tpurgent.|_tp.)' ) cs 
										    FROM dual)
						SELECT upper( substr( cs, 2, length(cs)-2 ) )
						  FROM cs_table""",
				String.class));

		LOG.info("Current ADMIN Consumer Group is: {}", consumerGroup);

		// Display database consumer groups available with their properties
		final List<ConsumerGroup> databaseServiceNameList = jdbcTemplate.query("""
						SELECT consumer_group,
							   concurrency_limit,
						       degree_of_parallelism
						  FROM CS_RESOURCE_MANAGER.LIST_CURRENT_RULES()
						 ORDER BY shares""",
				(rs, rowNum) -> new ConsumerGroup(rs.getString(1), rs.getInt(2), rs.getInt(3)));

		LOG.info("Available database consumer groups:");
		for (ConsumerGroup dsn : databaseServiceNameList) {
			LOG.info("- {}", dsn);
		}

		/*
		SqlParameterSource in = new MapSqlParameterSource()
				.addValue("service_name", "HIGH");

		switchConsumerGroup.execute(in);

		consumerGroup = Objects.requireNonNull(jdbcTemplate.queryForObject("""
						WITH cs_table AS (SELECT regexp_substr( 
														SYS_CONTEXT('USERENV','SERVICE_NAME'), 
														'(_low.|_medium.|_high.|_tpurgent.|_tp.)' ) cs 
										    FROM dual)
						SELECT upper( substr( cs, 2, length(cs)-2 ) )
						  FROM cs_table""",
				String.class));

		LOG.info("Current ADMIN Consumer Group is: {}", consumerGroup);
		*/
	}


	public static void main(String[] args) {
		SpringApplication.run(DatabaseConsumerGroupsApplication.class, args);
		LOG.info("=".repeat(126));
	}
}