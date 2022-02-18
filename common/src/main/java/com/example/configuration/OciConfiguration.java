package com.example.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "oci.tenant")
@Configuration
public class OciConfiguration {

	@Value("${oci.tenant.region}")
	private String region;

	@Value("${oci.tenant.database.name}")
	private String databaseName;

	@Value("${oci.tenant.database.ssl-server-cert-dn}")
	private String databaseSSLServerCertificateDistinguishedName;

	// Primary Data Source configuration

	@Value("${oci.tenant.database.consumer-group}")
	private String databaseConsumerGroup;

	@Value("${oci.tenant.database.url}")
	private String databaseUrl;

	@Value("${oci.tenant.database.username}")
	private String databaseUsername;

	@Value("${oci.tenant.database.password}")
	private String databasePassword;

	// Secondary (ADMIN) Data Source configuration

	@Value("${oci.tenant.database.admin-consumer-group}")
	private String databaseAdminConsumerGroup;

	@Value("${oci.tenant.database.admin-url}")
	private String databaseAdminUrl;

	@Value("${oci.tenant.database.admin-username}")
	private String databaseAdminUsername;

	@Value("${oci.tenant.database.admin-password}")
	private String databaseAdminPassword;

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getDatabaseSSLServerCertificateDistinguishedName() {
		return databaseSSLServerCertificateDistinguishedName;
	}

	public void setDatabaseSSLServerCertificateDistinguishedName(String databaseSSLServerCertificateDistinguishedName) {
		this.databaseSSLServerCertificateDistinguishedName = databaseSSLServerCertificateDistinguishedName;
	}

	public String getDatabaseUrl() {
		return databaseUrl;
	}

	public void setDatabaseUrl(String databaseUrl) {
		this.databaseUrl = databaseUrl;
	}

	public String getDatabaseUsername() {
		return databaseUsername;
	}

	public void setDatabaseUsername(String databaseUsername) {
		this.databaseUsername = databaseUsername;
	}

	public String getDatabasePassword() {
		return databasePassword;
	}

	public void setDatabasePassword(String databasePassword) {
		this.databasePassword = databasePassword;
	}

	public String getDatabaseAdminUrl() {
		return databaseAdminUrl;
	}

	public void setDatabaseAdminUrl(String databaseAdminUrl) {
		this.databaseAdminUrl = databaseAdminUrl;
	}

	public String getDatabaseAdminUsername() {
		return databaseAdminUsername;
	}

	public void setDatabaseAdminUsername(String databaseAdminUsername) {
		this.databaseAdminUsername = databaseAdminUsername;
	}

	public String getDatabaseAdminPassword() {
		return databaseAdminPassword;
	}

	public void setDatabaseAdminPassword(String databaseAdminPassword) {
		this.databaseAdminPassword = databaseAdminPassword;
	}

	public String getDatabaseConsumerGroup() {
		return databaseConsumerGroup;
	}

	public void setDatabaseConsumerGroup(String databaseConsumerGroup) {
		this.databaseConsumerGroup = databaseConsumerGroup;
	}

	public String getDatabaseAdminConsumerGroup() {
		return databaseAdminConsumerGroup;
	}

	public void setDatabaseAdminConsumerGroup(String databaseAdminConsumerGroup) {
		this.databaseAdminConsumerGroup = databaseAdminConsumerGroup;
	}

	@Override
	public String toString() {
		return "OciConfiguration{" +
				"region='" + region + '\'' +
				", databaseName='" + databaseName + '\'' +
				", databaseSSLServerCertificateDistinguishedName='" + databaseSSLServerCertificateDistinguishedName + '\'' +
				", databaseServiceName='" + databaseConsumerGroup + '\'' +
				", databaseUrl='" + databaseUrl + '\'' +
				", databaseUsername='" + databaseUsername + '\'' +
				", databasePassword='" + databasePassword + '\'' +
				", databaseAdminServiceName='" + databaseAdminConsumerGroup + '\'' +
				", databaseAdminUrl='" + databaseAdminUrl + '\'' +
				", databaseAdminUsername='" + databaseAdminUsername + '\'' +
				", databaseAdminPassword='" + databaseAdminPassword + '\'' +
				'}';
	}
}
