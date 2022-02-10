package com.example.transactionalqueue.service;

import oracle.AQ.AQDriverManager;
import oracle.AQ.AQException;
import oracle.AQ.AQSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

@Configuration
public class AQConfiguration {
	@Bean(name = "aqSessionForEnqueue")
	public AQSession aqSession(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
		return prepareAQSession(jdbcTemplate);
	}

	@Bean(name = "aqSessionForDequeue")
	public AQSession aqSessionForDequeue(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
		return prepareAQSession(jdbcTemplate);
	}

	private AQSession prepareAQSession(JdbcTemplate jdbcTemplate) {
		try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
			return AQDriverManager.createAQSession(connection.unwrap(oracle.jdbc.OracleConnection.class));
		}
		catch (SQLException | AQException sqle) {
			throw new ApplicationContextException("AQ configuration failed", sqle);
		}
	}
}
