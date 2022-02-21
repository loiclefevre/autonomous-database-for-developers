package com.example.relationaljson;

import com.example.relationaljson.model.Order;
import com.example.relationaljson.model.Product;
import com.example.relationaljson.model.report.TotalValueAndQuantityPerProduct;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * This demo shows how to load JSON data inside a Simple Oracle Document Access <b>OracleCollection</b> with client-side
 * <b>OSON encoding</b>. It also highlights some standard SQL operators to work on JSON data:
 * <ul>
 *     <li>JSON_TABLE (and its simplest syntax using NESTED "JSON column") to unnest JSON arrays into relational rows</li>
 *     <li>JSON_OBJECT to create JSON documents</li>
 * </ul>
 * And there are numerous others to discover.
 * <p>
 * Finally, it shows how to join JSON fields with Relational columns to leverage all the power of SQL analytics.
 *
 * @author Loïc Lefèvre
 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/adjsn/query-json-data.html">JSON Developer's Guide - Query JSON Data</a>
 */
@SpringBootApplication(scanBasePackages = "com.example")
public class RelationalJSONApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(RelationalJSONApplication.class);

	private static final int ORDER_NUMBER = 4;

	private final TransactionTemplate transactionTemplate;

	private final JdbcTemplate jdbcTemplate;

	private String ordersAsJSONArray;

	private String collectionName = "orders";

	public RelationalJSONApplication(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate) {
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Value("classpath:db/orders.json")
	public void setOrdersAsJSONArray(Resource ordersAsJSONArrayFile) throws IOException {
		this.ordersAsJSONArray = StreamUtils.copyToString(ordersAsJSONArrayFile.getInputStream(), StandardCharsets.UTF_8);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		// Create the Simple Oracle Document Access collection named "orders"
		initializeSODACollection();

		// Load JSON data into the collection orders with client side OSON encoding
		loadOSONDataIfNeeded();

		// Number of orders
		LOG.info("There is a total of {} order(s)", Objects.requireNonNull(
				jdbcTemplate.queryForObject("""
								SELECT count(*)
								  FROM orders
								""",
						Integer.class)));

		// Total number of products
		LOG.info("There is a total of {} product(s)", Objects.requireNonNull(
				jdbcTemplate.queryForObject("""
								SELECT count(*)
								  FROM orders NESTED json_document COLUMNS (
								    	      order_id,
									   NESTED products[*] COLUMNS (
												  prod_id,
												  name,
												  price
											  )
								       )
								""",
						Integer.class)));

		LOG.warn("=".repeat(126));
		// Total number of products
		LOG.info("The total value and quantity of expensive (price>15) products sold:");
		List<TotalValueAndQuantityPerProduct> reportExpensiveProductsSold = Objects.requireNonNull(
				jdbcTemplate.query("""
								SELECT o.prod_id,
                                       o.name,
                                       sum(o.price),
                                       count(*)							       
								  FROM orders NESTED json_document COLUMNS (
								    	      order_id,
									   NESTED products[*] COLUMNS (
												  prod_id,
												  name,
												  price
											  )
								       ) o
                                 WHERE o.price > 15
                              GROUP BY o.prod_id, o.name
                              ORDER BY sum(o.price) DESC
						""",
				(rs, rowNum) -> new TotalValueAndQuantityPerProduct(
						rs.getString(1),
						rs.getString(2),
						rs.getBigDecimal(3),
						rs.getInt(4))));

		LOG.info("Product ID\tProduct Name                    Total sold\tQuantity");
		for(TotalValueAndQuantityPerProduct r:reportExpensiveProductsSold) {
			LOG.info("{}\t{}\t{}\t\t{}", r.productId(), String.format("%-30s", r.productName()), r.totalValue(), r.quantity());
		}

		LOG.warn("=".repeat(126));
		LOG.info("Now as JSON objects:");
		LOG.info("The total value and quantity of expensive (price>15) products sold:");
		List<String> reportExpensiveProductsSoldAsJSON = Objects.requireNonNull(
				jdbcTemplate.query("""
								SELECT JSON_OBJECT(
								       'product_id': o.prod_id,
                                       'product_name': o.name,
                                       'total': sum(o.price),
                                       'quantity': count(*)
                                  )     								       
								  FROM orders NESTED json_document COLUMNS (
								    	      order_id,
									   NESTED products[*] COLUMNS (
												  prod_id,
												  name,
												  price
											  )
								       ) o
                                 WHERE o.price > 15
                              GROUP BY o.prod_id, o.name
                              ORDER BY sum(o.price) DESC
						""",
						(rs, rowNum) -> rs.getString(1)));

		LOG.info("JSON");
		for(String r:reportExpensiveProductsSoldAsJSON) {
			LOG.info("{}", r);
		}

		LOG.warn("=".repeat(126));
		LOG.info("Now computing the real prices by applying VAT per country:");
		reportExpensiveProductsSoldAsJSON = Objects.requireNonNull(
				jdbcTemplate.query("""
								SELECT JSON_OBJECT(
								       'product_id': o.prod_id,
                                       'product_name': o.name,
                                       'totalWithVAT': round(sum(o.price * (1 + ct.tax)),2),
                                       'quantity': count(*)
                                  )
								  FROM orders NESTED json_document COLUMNS (
								    	      order_id,
                                              customer_id NUMBER,
									   NESTED products[*] COLUMNS (
												  prod_id,
												  name,
												  price
											  )
								       ) o 
								       -- JSON documents joined with Relational data
								       JOIN customers c ON c.id = o.customer_id
                                       JOIN country_taxes ct ON c.country = ct.country_name
                                 WHERE o.price > 15
                              GROUP BY o.prod_id, o.name
                              ORDER BY sum(o.price * (1 + ct.tax)) DESC
						""",
						(rs, rowNum) -> rs.getString(1)));

		LOG.info("JSON");
		for(String r:reportExpensiveProductsSoldAsJSON) {
			LOG.info("{}", r);
		}
	}

	private void loadOSONDataIfNeeded() throws JsonProcessingException {
		final boolean devicesLoaded = Objects.requireNonNull(jdbcTemplate.queryForObject(
				String.format("select count(*) from %s", collectionName),
				Integer.class)) == ORDER_NUMBER;

		if (!devicesLoaded) {
			// First read the JSON array describing fictive orders
			final Order[] orders = new ObjectMapper().readValue(ordersAsJSONArray, Order[].class);
			LOG.warn("Now loading {} orders into the database... using OSON encoding!", orders.length);

			loadOrders(orders);
		}
	}

	private void initializeSODACollection() throws SQLException, OracleException {
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

	private void loadOrders(Order[] orders) {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jdbcTemplate.batchUpdate(String.format(
								"INSERT INTO %s (id, version, json_document) VALUES (SYS_GUID(), '1', ?)",
								collectionName),
						new BatchPreparedStatementSetter() {
							public void setValues(PreparedStatement ps, int i) throws SQLException {
								ps.setBytes(1, getOSONEncoding(orders[i]));
							}

							public int getBatchSize() {
								return orders.length;
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
	 * @param order the object to encode
	 * @return the OSON representation of the object.
	 */
	private byte[] getOSONEncoding(Order order) {
		out.reset();

		final OracleJsonGenerator gen = factory.createJsonBinaryGenerator(out);
		gen.writeStartObject();

		gen.write("order_id", order.id());
		gen.write("customer_id", order.customerId());

		gen.writeStartArray("products");
		for (Product p : order.products()) {
			gen.writeStartObject();
			gen.write("prod_id", p.id());
			gen.write("name", p.name());
			gen.write("price", p.price());
			gen.writeEnd(); // object
		}
		gen.writeEnd(); // products

		gen.writeEnd();
		gen.close();

		return out.toByteArray();
	}

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);
		SpringApplication.run(RelationalJSONApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
