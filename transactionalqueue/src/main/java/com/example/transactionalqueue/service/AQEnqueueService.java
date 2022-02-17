package com.example.transactionalqueue.service;

import com.example.configuration.OciConfiguration;
import oracle.AQ.AQDriverManager;
import oracle.AQ.AQEnqueueOption;
import oracle.AQ.AQException;
import oracle.AQ.AQMessage;
import oracle.AQ.AQMessageProperty;
import oracle.AQ.AQOracleSQLException;
import oracle.AQ.AQQueue;
import oracle.AQ.AQSession;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Enqueue service managing transactional events to send into the queue. The priority of events is also demonstrated.
 *
 * @author Loïc Lefèvre
 */
@Component
@Service
public class AQEnqueueService {

	public static final int HIGH_PRIORITY = -1;
	public static final int DEFAULT_PRIORITY = 0;

	private OciConfiguration ociConfiguration;

	private DataSource dataSource;

	public AQEnqueueService(OciConfiguration ociConfiguration, DataSource dataSource) {
		this.ociConfiguration = ociConfiguration;
		this.dataSource = dataSource;
	}

	public void sendJSONEventInTransaction(String queueName, BigDecimal orderAmount, int priority) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			try {
				final AQSession aqSession = AQDriverManager.createAQSession(connection.unwrap(oracle.jdbc.OracleConnection.class));
				final AQQueue queue = aqSession.getQueue(ociConfiguration.getDatabaseUsername(), queueName);
				final AQEnqueueOption enqueueOption = new AQEnqueueOption();

				final AQMessage message = queue.createMessage();
				final AQMessageProperty messageProperty = new AQMessageProperty();
				// 0: default priority
				// > 0 : lower priority
				// < 0 : higher priority
				messageProperty.setPriority(priority);
				message.setMessageProperty(messageProperty);

				final String jsonEvent = String.format(Locale.US, """
						{"order": {"amount": %.2f} }""", orderAmount);
				message.getRawPayload().setStream(jsonEvent.getBytes(), jsonEvent.getBytes().length);

				try {
					queue.enqueue(enqueueOption, message);
				}
				catch (AQOracleSQLException aqsqle) {
					if (aqsqle.getErrorCode() == 25207) {
						// queue not started, start it...
						queue.startEnqueue();
						// ...and retry!
						queue.enqueue(enqueueOption, message);
					}
				}

				connection.commit();
			}
			catch (AQException aqe) {
				connection.rollback();
				throw new SQLException(aqe);
			}
		}
	}
}
