package com.example.sqlviarest;

import com.example.configuration.OciConfiguration;
import com.example.sqlviarest.model.RESTEnabledSQLServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * This demo shows how to use the REST Enabled SQL Service: a unique endpoint used to run SQL or PL/SQL code.
 * <p>
 * In this example, we create a new user inside the database and provide development related privileges
 * using a PL/SQL anonymous block.
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication(scanBasePackages = "com.example")
@Component
public class CreateUserApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(CreateUserApplication.class);

	private final OciConfiguration ociConfiguration;

	/**
	 * The content of the PL/SQL script will be injected via the #setCreateDatabaseUserScriptContent method.
	 */
	private String plsqlCreateDatabaseUserScriptContent;

	public CreateUserApplication(OciConfiguration ociConfiguration) {
		this.ociConfiguration = ociConfiguration;
	}

	@Value("classpath:db/create_database_user.sql")
	public void setCreateDatabaseUserScriptContent(Resource script) throws IOException {
		this.plsqlCreateDatabaseUserScriptContent = StreamUtils.copyToString(script.getInputStream(), StandardCharsets.UTF_8);
	}

	@Override
	public void run(String... args) {
		LOG.info("=".repeat(126));

		WebClient client = WebClient.builder()
				.baseUrl(String.format("https://%s.adb.%s.oraclecloudapps.com",
						ociConfiguration.getDatabaseName().replace('_', '-'),
						ociConfiguration.getRegion()))
				.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/sql")
				.defaultHeaders(header -> header.setBasicAuth(ociConfiguration.getDatabaseAdminUsername(),
						ociConfiguration.getDatabaseAdminPassword()))
				.build();

		RESTEnabledSQLServiceResponse result = client.post()
				.uri("/ords/admin/_/sql")
				.bodyValue(String.format(plsqlCreateDatabaseUserScriptContent,
						ociConfiguration.getDatabaseUsername(),
						ociConfiguration.getDatabasePassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(RESTEnabledSQLServiceResponse.class)
				.block();

		for (String responseLine : Objects.requireNonNull(result).getItems()[0].getResponse()) {
			LOG.info(responseLine.replaceAll(ociConfiguration.getDatabasePassword(), "************"));
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(CreateUserApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
