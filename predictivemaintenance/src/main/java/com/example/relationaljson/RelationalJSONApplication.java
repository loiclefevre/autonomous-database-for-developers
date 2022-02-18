package com.example.relationaljson;

import com.example.relationaljson.model.Device;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.soda.OracleCollection;
import oracle.soda.OracleDatabase;
import oracle.soda.OracleException;
import oracle.soda.rdbms.OracleRDBMSClient;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 *
 * @author Loïc Lefèvre
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class RelationalJSONApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(RelationalJSONApplication.class);

	private static final int DEVICE_NUMBER = 1423;

	private final TransactionTemplate transactionTemplate;

	private final JdbcTemplate jdbcTemplate;

	private String devicesAsJSONArray;

	public RelationalJSONApplication(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate) {
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Value("classpath:db/devices.json")
	public void setDevicesAsJSONArray(Resource devicesAsJSONArrayFile) throws IOException {
		this.devicesAsJSONArray = StreamUtils.copyToString(devicesAsJSONArrayFile.getInputStream(), StandardCharsets.UTF_8);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		// Create the Simple Oracle Document Access collection named "devices"
		initializeSODACollection("devices");

		// Load JSON data into the collection devices with client side OSON encoding
		loadDevicesJSONDataIfNeeded("devices");

		//
	}

	private void loadDevicesJSONDataIfNeeded(String collectionName) throws JsonProcessingException {
		final boolean devicesLoaded = Objects.requireNonNull(jdbcTemplate.queryForObject(
				String.format("select count(*) from %s",collectionName),
				Integer.class)) == DEVICE_NUMBER;

		if(!devicesLoaded) {
			// First read the JSON array describing fictive devices
			final Device[] devices = new ObjectMapper().readValue(devicesAsJSONArray, Device[].class);
			LOG.warn("Now loading {} devices into the database... using OSON encoding!", devices.length);

			loadDevices(devices);
		}
	}

	private void initializeSODACollection(String collectionName) throws SQLException, OracleException {
		final Properties props = new Properties();
		props.put("oracle.soda.sharedMetadataCache", "true");
		props.put("oracle.soda.localMetadataCache", "true");

		final OracleRDBMSClient cl = new OracleRDBMSClient(props);

		try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
			final OracleDatabase db = cl.getDatabase(connection);
			OracleCollection sodaCollection = db.openCollection(collectionName);

			if (sodaCollection == null) {
				sodaCollection = db.admin().createCollection(collectionName);
				if (sodaCollection == null) {
					throw new IllegalStateException("Can't create SODA collection: " + collectionName);
				}
			}
		}
	}

	private void loadDevices(Device[] devices) {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jdbcTemplate.batchUpdate("INSERT INTO devices (id, version, json_document) VALUES (SYS_GUID(), '1', ?)",
						new BatchPreparedStatementSetter() {
							public void setValues(PreparedStatement ps, int i) throws SQLException {
								ps.setBytes(1, getOSONEncoding( devices[i] ));
							}

							public int getBatchSize() {
								return devices.length;
							}
						}
				);
			}
		});
	}

	private final OracleJsonFactory factory = new OracleJsonFactory();
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

	/**
	 * Generates an OSON encoded JSON document.
	 *
	 * @param device the object to encode
	 * @return the OSON representation of the object.
	 */
	private byte[] getOSONEncoding(Device device) {
		out.reset();

		final OracleJsonGenerator gen = factory.createJsonBinaryGenerator(out);
		gen.writeStartObject();
		// {id_machine":"A54","country":"United States","state":"District of Columbia",
		// "city":"Washington","geometry":{"type":"Point","coordinates":[-77.0162,38.905]}}
		gen.write("id_machine", device.id());
		gen.write("country", device.country());
		gen.write("state", device.state());
		gen.write("city", device.city());

		// GeoJSON
		gen.writeStartObject("geometry" );
		gen.write("type", device.geometry().type());

		gen.writeStartArray("coordinates" );
		for(double v:device.geometry().coordinates()) {
			gen.write(v);
		}
		gen.writeEnd(); // coordinates

		gen.writeEnd(); // geometry

		gen.writeEnd();
		gen.close();

		return out.toByteArray();
	}

	public static void main(String[] args) {
		SpringApplication.run(RelationalJSONApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
