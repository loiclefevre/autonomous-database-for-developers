package com.example.awrreport;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

/**
 * This demo shows how to configure 2 distinct connection pools to an Autonomous Database
 * using the default HikariCP connection pool type. The primary data source will connect
 * to the normal application user schema while the admin data source will connect to the
 * ADMIN schema and will have more privileges including generating AWR reports used for
 * further performance analysis.
 * <p>
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication
public class AWRReportApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(AWRReportApplication.class);

	@Autowired
	@Qualifier("primaryJdbcTemplate")
	private JdbcTemplate primaryJdbcTemplate;

	@Autowired
	@Qualifier("adminJdbcTemplate")
	private JdbcTemplate adminJdbcTemplate;

	// PL/SQL procedures/functions that we can call
	private SimpleJdbcCall getLockHandleFromName;

	@PostConstruct
	void init() {
		// Allows getting output values without any lower/upper case "problem"
		adminJdbcTemplate.setResultsMapCaseInsensitive(true);

	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		LOG.info("Primary data source username: {}",
				primaryJdbcTemplate.queryForObject("select user from dual",
						(RowMapper<String>) (rs, rowNum) -> rs.getString(1)));

		LOG.info("Admin data source username: {}",
				adminJdbcTemplate.queryForObject("select user from dual",
						(RowMapper<String>) (rs, rowNum) -> rs.getString(1)));

		LOG.info("Taking a snapshot of all internal database metrics using the ADMIN data source...");
		final long startSnapID = Objects.requireNonNull(adminJdbcTemplate.queryForObject(
				"select DBMS_WORKLOAD_REPOSITORY.CREATE_SNAPSHOT() from dual",
				(rs, rowNum) -> rs.getLong(1)));

		// Do some work using the PRIMARY data source connected with the HIGH database service name
		final String sql = "select /*+ result_cache */ count(*) from ssb.lineorder";

		LOG.info("Running some SQL query using the HIGH database service name: {}", sql);
		final long lineOrderRowsNumber = Objects.requireNonNull(primaryJdbcTemplate.queryForObject(
				sql, (rs, rowNum) -> rs.getLong(1)));
		LOG.info("Query counted {} rows from SSB.LINE_ORDER table.", lineOrderRowsNumber);

		// Taking a SECOND snapshot of all internal database metrics using the ADMIN data source
		LOG.info("Taking the SECOND snapshot of all internal database metrics using the ADMIN data source...");
		final long endSnapID = Objects.requireNonNull(adminJdbcTemplate.queryForObject(
				"select DBMS_WORKLOAD_REPOSITORY.CREATE_SNAPSHOT() from dual",
				(rs, rowNum) -> rs.getLong(1)));

		// And now generates the AWR report
		LOG.info("Now generating AWR HTML report using the ADMIN data source...");
		generateAWRReport(startSnapID, endSnapID);
	}

	private void generateAWRReport(long startSnapID, long endSnapID) {
		final NamedParameterJdbcTemplate namedParameterAdminJdbcTemplate =
				new NamedParameterJdbcTemplate(adminJdbcTemplate);

		SqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("start_snap_id", startSnapID);

		final long dbID = Objects.requireNonNull(namedParameterAdminJdbcTemplate.queryForObject(
				"select dbid from DBA_HIST_SNAPSHOT where snap_id = :start_snap_id",
				parameters, Long.class));

		// Now stream the HTML report into a local file!
		try (PrintWriter out =
				     new PrintWriter(new BufferedOutputStream(new FileOutputStream(
						     String.format("awr_report_%d_%d_%d.html", dbID, startSnapID, endSnapID))))) {

			// https://docs.oracle.com/en/database/oracle/oracle-database/19/arpls/DBMS_WORKLOAD_REPOSITORY.html#GUID-A0165998-4E64-4FCE-AFB8-3C7C43146C27
			parameters = new MapSqlParameterSource()
					.addValue("dbid", dbID)
					.addValue("start_snap_id", startSnapID)
					.addValue("end_snap_id", endSnapID)
					.addValue("options", 8);

			namedParameterAdminJdbcTemplate.queryForStream("""
							SELECT output
							  FROM TABLE(
							              DBMS_WORKLOAD_REPOSITORY.AWR_GLOBAL_REPORT_HTML(
							                 :dbid,
							                 '', 
							                 :start_snap_id, 
							                 :end_snap_id, 
							                 :options
							              )
							       )""",
					parameters, (rs, rowNum) -> rs.getString(1))
					.forEach(out::println);
		}
		catch (FileNotFoundException fnfe) {
			LOG.error("While saving AWR HTML report", fnfe);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(AWRReportApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
