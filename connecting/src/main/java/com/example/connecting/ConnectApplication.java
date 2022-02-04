package com.example.connecting;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This demo shows how to connect to an Autonomous Database using the default HikariCP
 * connection pool. It uses the EasyConnect way that allows passing properties to create
 * connections (such as the row fetch size, default to 10 for Oracle JDBC driver...).
 * <p>
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication
public class ConnectApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(ConnectApplication.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		// Connect to the database using the connection pool
		try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
			// Print database version
			LOG.info("Oracle Autonomous Database {}",
					connection.getMetaData().getDatabaseProductVersion().split("\n")[1]);

			// Print configured Row Fetch Size using Easy Connect: passing arguments via the JDBC connection string
			try (Statement statement = connection.createStatement()) {
				LOG.info("Row Fetch Size configured to {}", statement.getFetchSize());

				try(ResultSet resultSet = statement.executeQuery("select current_timestamp from dual")) {
					if(resultSet.next()) {
						LOG.info("Database current date and time is: {}", resultSet.getTimestamp(1));
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(ConnectApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
