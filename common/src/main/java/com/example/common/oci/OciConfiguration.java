package com.example.common.oci;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author bnasslahsen
 */
@ConfigurationProperties(prefix = "oci.tenant")
@Configuration
public class OciConfiguration {

	private String region;
	private String database;
	private String sampleUsername;
	private String samplePassword;

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getSampleUsername() {
		return sampleUsername;
	}

	public void setSampleUsername(String sampleUsername) {
		this.sampleUsername = sampleUsername;
	}

	public String getSamplePassword() {
		return samplePassword;
	}

	public void setSamplePassword(String samplePassword) {
		this.samplePassword = samplePassword;
	}
}