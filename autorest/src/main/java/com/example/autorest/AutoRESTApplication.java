package com.example.autorest;

import com.example.autorest.model.Country;
import com.example.autorest.model.ORDSClientInfo;
import com.example.autorest.model.ORDSOAuth2Token;
import com.example.autorest.model.PageOfSecretData;
import com.example.autorest.model.SecretData;
import com.example.configuration.OciConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.FileUrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URL;

/**
 * This demo shows how to enable REST access automatically for database TABLES. This allows loading these files using
 * REST endpoints. The 2 tables used as example are protected using BASIC authentication.
 * <p>
 * In the second part of the demo, we present another way to protect and access data using OAuth. Doing so, we create
 * a new module named <i>Secret Data!</i> which is based on a simple SQL query with a parameter. We then access the data
 * using paging capabilities.
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class AutoRESTApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(AutoRESTApplication.class);

	private final OciConfiguration ociConfiguration;

	private final String regionsCSVURL = "https://raw.githubusercontent.com/gvenzl/sample-data/main/countries-cities-currencies/regions.csv";

	private final String countriesCSVURL = "https://raw.githubusercontent.com/gvenzl/sample-data/main/countries-cities-currencies/countries.csv";

	private JdbcTemplate jdbcTemplate;

	public AutoRESTApplication(OciConfiguration ociConfiguration, JdbcTemplate jdbcTemplate) {
		this.ociConfiguration = ociConfiguration;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		LOG.warn("Tables Regions and Countries are now REST enabled!");

		// WebClient used to do REST calls
		WebClient client = WebClient.builder()
				.baseUrl(String.format("https://%s.adb.%s.oraclecloudapps.com",
						ociConfiguration.getDatabaseName().replace('_', '-'),
						ociConfiguration.getRegion()))
				.defaultHeader(HttpHeaders.CONTENT_TYPE, "text/csv")
				.defaultHeaders(header -> header.setBasicAuth(
						ociConfiguration.getDatabaseUsername(),
						ociConfiguration.getDatabasePassword()))
				.build();

		// Load CSV file retrieved from GitHub
		loadCSVDataIntoTableUsingREST(client, "regions", regionsCSVURL);

		// Load CSV file retrieved from GitHub
		loadCSVDataIntoTableUsingREST(client, "countries", countriesCSVURL);

		// Get a country by its ID
		LOG.warn("GETting the country with code \"FRA\" using REST...");

		Country france = client.get()
				.uri(String.format("/ords/%s/countries/FRA",
						ociConfiguration.getDatabaseUsername().toLowerCase()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(Country.class)
				.block();

		LOG.info("France data: {}", france);

		// Now retrieving ORDS client info (id and secret)
		LOG.warn("=".repeat(126));
		LOG.warn("Now accessing Secret Data! using OAuth2 token...");

		final ORDSClientInfo clientInfo = jdbcTemplate.queryForObject("""
						SELECT client_id, client_secret
						FROM user_ords_clients
						where name='ORDS AutoREST demo'""",
				(rs, rowNum) -> new ORDSClientInfo(rs.getString(1), rs.getString(2)));

		LOG.info("- client id    : {}", clientInfo.id());
		LOG.info("- client secret: {}", clientInfo.secret());

		// Retrieve OAuth2 token (valid for 1 hour)
		WebClient clientForOAuth2TokenAcquisition = WebClient.builder()
				.baseUrl(String.format("https://%s.adb.%s.oraclecloudapps.com",
						ociConfiguration.getDatabaseName().replace('_', '-'),
						ociConfiguration.getRegion()))
				.defaultHeaders(header -> header.setBasicAuth(
						clientInfo.id(),
						clientInfo.secret()))
				.build();

		ORDSOAuth2Token oauth2Token = clientForOAuth2TokenAcquisition.post()
				.uri(String.format("/ords/%s/oauth/token",
						ociConfiguration.getDatabaseUsername().toLowerCase()))
				.bodyValue("grant_type=client_credentials")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(ORDSOAuth2Token.class)
				.block();

		LOG.warn("OAuth2 token: {}", oauth2Token);

		// Now proceed with our "Secret data!" module
		LOG.info("Ready to GET the secret data...");

		final String protectedURI = String.format("/ords/%s/secret_data/countries/Europe?offset=2",
				ociConfiguration.getDatabaseUsername().toLowerCase());

		LOG.warn("Protected URI: {}", protectedURI);

		WebClient clientWithOAuth2 = WebClient.builder()
				.baseUrl(String.format("https://%s.adb.%s.oraclecloudapps.com",
						ociConfiguration.getDatabaseName().replace('_', '-'),
						ociConfiguration.getRegion()))
				.defaultHeaders(header -> header.setBearerAuth(oauth2Token.token()))
				.build();

		PageOfSecretData secretData = clientWithOAuth2.get()
				.uri(protectedURI)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(PageOfSecretData.class)
				.block();

		LOG.info("Secret data for Europe (paging by {} items, starting at item number {}):",
				secretData.limit(), secretData.offset());

		for(SecretData sd: secretData.items()) {
			LOG.info("- {}", sd);
		}

		LOG.info("Has more items: {}", secretData.hasMore());
	}

	private void loadCSVDataIntoTableUsingREST(WebClient client, String tableName, String dataCSVURL) throws IOException {
		LOG.warn("Loading table {} from CSV file {} using REST call...", tableName, dataCSVURL);

		final String load = client.post()
				.uri(String.format("/ords/%s/%s/batchload?batchRows=1000",
						ociConfiguration.getDatabaseUsername().toLowerCase(),
						tableName))
				.bodyValue(new FileUrlResource(new URL(dataCSVURL)).getInputStream().readAllBytes())
				.accept(MediaType.TEXT_PLAIN)
				.retrieve()
				.bodyToMono(String.class)
				.block();

		LOG.info("Table {} loaded:\n{}", tableName, load);
	}

	public static void main(String[] args) {
		SpringApplication.run(AutoRESTApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
