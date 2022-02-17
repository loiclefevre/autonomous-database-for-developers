package com.example.relationaljson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class RelationalJSONApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(RelationalJSONApplication.class);

	private final JdbcTemplate jdbcTemplate;

	public RelationalJSONApplication(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));


	}

	public static void main(String[] args) {
		SpringApplication.run(RelationalJSONApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
