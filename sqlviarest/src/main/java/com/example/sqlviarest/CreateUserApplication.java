package com.example.sqlviarest;

import java.util.Objects;

import com.example.common.oci.OciConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This demo shows how to use the REST Enabled SQL Service: a unique endpoint used to run SQL or PL/SQL code.
 * <p>
 * In this example, we create a brand new user inside the database and provide development related privileges
 * using a PL/SQL anonymous block.
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class CreateUserApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(CreateUserApplication.class);

	private OciConfiguration ociConfiguration;

	private DataSourceProperties dataSourceProperties;

	public CreateUserApplication(OciConfiguration ociConfiguration, DataSourceProperties dataSourceProperties) {
		this.ociConfiguration = ociConfiguration;
		this.dataSourceProperties = dataSourceProperties;
	}

	@Override
	public void run(String... args)  {
		LOG.info("=".repeat(126));

		WebClient client = WebClient.builder()
				.baseUrl(String.format("https://%s.adb.%s.oraclecloudapps.com",
						ociConfiguration.getDatabase().replace('_', '-'),
						ociConfiguration.getRegion()))
				.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/sql")
				.defaultHeaders(header -> header.setBasicAuth(dataSourceProperties.getUsername(), dataSourceProperties.getPassword()))
				.build();

		RESTEnabledSQLServiceResponse result = client.post()
				.uri("/ords/admin/_/sql")
				.bodyValue(String.format("""
						DECLARE
							username varchar2(60) := '%s';
							password varchar2(60) := '%s';
						BEGIN
						    execute immediate 'create user ' || username || ' identified by "'|| password ||'" DEFAULT TABLESPACE DATA TEMPORARY TABLESPACE TEMP';
						    execute immediate 'alter user ' || username || ' quota unlimited on data';
						    execute immediate 'grant dwrole, create session, soda_app, graph_developer, oml_developer, alter session, select_catalog_role to ' || username;
						    execute immediate 'alter user ' || username || ' grant connect through GRAPH$PROXY_USER';
						    execute immediate 'alter user ' || username || ' grant connect through OML$PROXY';
						    
						    begin
						      execute immediate 'grant PYQADMIN to ' || username;
						    exception when others then null;
						    end;

						    execute immediate 'grant execute on CTX_DDL to ' || username;
						    execute immediate 'grant select on dba_rsrc_consumer_group_privs to ' || username;
						    execute immediate 'grant execute on DBMS_SESSION to ' || username;
						    execute immediate 'grant select on sys.v_$services to ' || username;
							execute immediate 'grant select any dictionary to ' || username;
							execute immediate 'grant execute on DBMS_AUTO_INDEX to ' || username;
							execute immediate 'grant execute on DBMS_LOCK to ' || username; -- used for demo3

						    ords_admin.enable_schema(p_enabled => TRUE, p_schema => upper(username), p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => lower(username), p_auto_rest_auth => TRUE);
						END;

						/
						""",ociConfiguration.getSampleUsername(), ociConfiguration.getSamplePassword()))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(RESTEnabledSQLServiceResponse.class)
				.block();

		for (String responseLine : Objects.requireNonNull(result).getItems()[0].getResponse()) {
			LOG.info(responseLine.replaceAll(dataSourceProperties.getPassword(), "************"));
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(CreateUserApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
