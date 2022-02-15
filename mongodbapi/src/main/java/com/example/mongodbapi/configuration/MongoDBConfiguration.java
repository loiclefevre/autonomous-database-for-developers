package com.example.mongodbapi.configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoDBConfiguration {
	private static final Logger LOG = LoggerFactory.getLogger(MongoDBConfiguration.class);

	@Value("${oci.tenant.region}")
	private String region;

	@Value("${oci.tenant.database.name}")
	private String databaseName;

	@Value("${oci.tenant.database.username}")
	private String databaseUsername;

	@Value("${oci.tenant.database.password}")
	private String databasePassword;

	public @Bean MongoClient mongoClient() {
		final String uri = String.format("mongodb://%s:%s@%s.adb.%s.oraclecloudapps.com:27016/?authMechanism=PLAIN&authSource=$external&ssl=true&retryWrites=false",
				databaseUsername, databasePassword,
				databaseName.replaceAll("_", "-"), region );
		return MongoClients.create(uri);
	}

	public @Bean MongoTemplate mongoTemplate() {
		return new MongoTemplate(mongoClient(), databaseUsername);
	}
}
