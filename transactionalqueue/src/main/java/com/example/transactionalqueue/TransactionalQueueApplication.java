package com.example.transactionalqueue;

import oracle.AQ.AQEnqueueOption;
import oracle.AQ.AQMessage;
import oracle.AQ.AQQueue;
import oracle.AQ.AQSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

@SpringBootApplication(scanBasePackages = "com.example")
@EnableAsync
public class TransactionalQueueApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(TransactionalQueueApplication.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AQSession aqSessionForEnqueue;

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		// Connect to the database using the connection pool
		try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
			try {
				AQQueue queue = aqSessionForEnqueue.getQueue("DEMOS", "AQ_NOTIFICATIONS_QUEUE");

				AQMessage message = queue.createMessage();
				String json = "{\"test\": 42}";
				message.getRawPayload().setStream(json.getBytes(),json.getBytes().length);

				AQEnqueueOption option = new AQEnqueueOption();
				queue.enqueue(option, message);
				connection.commit();

/*				AQDequeueOption dequeueOption = new AQDequeueOption();

				AQMessage newMessage = queue.dequeue(dequeueOption);
				connection.commit();

				String newJSON = new String(newMessage.getRawPayload().getBytes());

				LOG.info("Received: {}",newJSON); */
			}
			catch(SQLException sqle) {
				connection.rollback();
			}
			finally {
				if(aqSessionForEnqueue != null) {
					aqSessionForEnqueue.close();
				}
			}
		}
	}

	public static void main(String[] args) {
		System.setProperty("aq.drivers","oracle.AQ.AQOracleDriver");
		SpringApplication.run(TransactionalQueueApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
