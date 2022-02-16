package com.example.geojson;

import com.example.geojson.model.GeoJSONFeature;
import com.example.geojson.model.GeoJSONFeatures;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * This demo will focus on GeoJSON and how you can analyze such data.
 * <p>
 * First, we load the GeoJSON data into a table with a JSON column. We'll need to create a Spatial index to be able to
 * use Spatial operators (such as SDO_CONTAINS).
 *
 *
 * @author Loïc Lefèvre
 * @see <a href="https://geojson.org/">The GeoJSON Specification (RFC 7946)</a>
 * @see <a href="https://epsg.io/">Coordinate Systems Worldwide</a>
 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/spatl/coordinate-systems-concepts.html">
 *     Coordinate Systems (Spatial Reference Systems)</a>
 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/spatl/spatial-operators-reference.html">
 *     Spatial Operators (Oracle database 19c)</a>
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class GeoJSONApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(GeoJSONApplication.class);

	// Number of countries to load
	private static final int COUNTRY_NUMBER = 239;

	private String geoJSONCountryPolygons;

	private final TransactionTemplate transactionTemplate;

	private final JdbcTemplate jdbcTemplate;

	public GeoJSONApplication(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate) {
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Value("classpath:db/countries_polygons.json")
	public void setGeoJSONCountryPolygons(Resource geoJSONFile) throws IOException {
		this.geoJSONCountryPolygons = StreamUtils.copyToString(geoJSONFile.getInputStream(), StandardCharsets.UTF_8);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		final boolean geoJSONLoaded = Objects.requireNonNull(jdbcTemplate.queryForObject(
				"select count(*) from country_polygons",
				Integer.class)) == COUNTRY_NUMBER;

		if (!geoJSONLoaded) {
			// Read GeoJson from file...
			final GeoJSONFeatures geoJSONFeatures = new ObjectMapper().readValue(geoJSONCountryPolygons, GeoJSONFeatures.class);

			// ...and load into the database
			loadGeoJSONFeatures(geoJSONFeatures.features());

			LOG.info("{} country polygons loaded!", geoJSONFeatures.features().size());

			LOG.info("=".repeat(126));
		}
		else {
			LOG.info("{} country polygons in database", COUNTRY_NUMBER);

			LOG.info("=".repeat(126));
		}

		// GPS coordinates from https://epsg.io/map#srs=4326&x=5.380583&y=43.280427&z=12&layer=streets
		// Using EPSG:4326 (aka SRID for Spatial Reference system ID)
		// EPSG: European Petroleum Survey Group
		// For instance, Google Maps uses EPSG:3857
		final double marseilleLongitude = -5.380583d;
		final double marseilleLatitude = 43.280427d;

		LOG.warn("Is Marseille really in France?");
		LOG.info("Marseille coordinates (from https://epsg.io/):");
		LOG.info("- longitude: {}", marseilleLongitude );
		LOG.info("- latitude: {}", marseilleLatitude );

		final String sql = String.format(Locale.US, """
						SELECT c.country_id
						FROM country_polygons c
						WHERE SDO_CONTAINS(
								sdo_util.from_geojson(c.geometry),
								sdo_util.from_geojson('{"type": "Point", "coordinates": [%f, %f]}')
						) = 'TRUE'
					""",
				marseilleLongitude, marseilleLatitude);

		final String countryISO3Code = jdbcTemplate.queryForObject(sql, String.class);

		LOG.info("Country code (ISO3) containing Marseille: {}", countryISO3Code);
	}

	/**
	 * Loads GeoJSON data.
	 *
	 * @param geoJSONFeatures a list of country polygons and country ISO3 codes
	 */
	public void loadGeoJSONFeatures(List<GeoJSONFeature> geoJSONFeatures) {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jdbcTemplate.batchUpdate("insert into country_polygons (country_id, geometry) values (?,?)",
						new BatchPreparedStatementSetter() {
							public void setValues(PreparedStatement ps, int i) throws SQLException {
								ps.setString(1, geoJSONFeatures.get(i).getProperties().id());
								ps.setString(2, geoJSONFeatures.get(i).getGeometry());
							}

							public int getBatchSize() {
								return geoJSONFeatures.size();
							}
						}
				);
			}
		});
	}

	public static void main(String[] args) {
		SpringApplication.run(GeoJSONApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
