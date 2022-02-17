package com.example.relationaljson;

import com.example.relationaljson.model.Device;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class RelationalJSONApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(RelationalJSONApplication.class);

	private final JdbcTemplate jdbcTemplate;

	private String devicesAsJSONArray;

	public RelationalJSONApplication(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Value("classpath:db/devices.json")
	public void setDevicesAsJSONArray(Resource devicesAsJSONArrayFile) throws IOException {
		this.devicesAsJSONArray = StreamUtils.copyToString(devicesAsJSONArrayFile.getInputStream(), StandardCharsets.UTF_8);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		final Device[] devices = new ObjectMapper().readValue(devicesAsJSONArray, Device[].class);

		LOG.info("Now loading {} devices into the database...", devices.length);
	}

	public static void main(String[] args) {
		SpringApplication.run(RelationalJSONApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
