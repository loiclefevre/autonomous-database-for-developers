package com.example.dbservicenames.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {
	@Bean
	@Primary
	@ConfigurationProperties("app.datasource.primary")
	public DataSourceProperties primaryDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean(name="primaryDataSource")
	@Primary
	@ConfigurationProperties("app.datasource.primary.configuration")
	public DataSource primaryDataSource() {
		return primaryDataSourceProperties().initializeDataSourceBuilder()
				.type(HikariDataSource.class).build();
	}

	@Bean(name = "primaryJdbcTemplate")
	public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource ds) {
		return new JdbcTemplate(ds);
	}

	@Bean
	@ConfigurationProperties("app.datasource.admin")
	public DataSourceProperties adminDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean(name="adminDataSource")
	@ConfigurationProperties("app.datasource.admin.configuration")
	public DataSource adminDataSource() {
		return adminDataSourceProperties().initializeDataSourceBuilder()
				.type(HikariDataSource.class).build();
	}

	@Bean(name = "adminJdbcTemplate")
	public JdbcTemplate adminJdbcTemplate(@Qualifier("adminDataSource") DataSource ds) {
		return new JdbcTemplate(ds);
	}
}
