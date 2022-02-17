package com.example.transactionalqueue.service;

import com.example.configuration.OciConfiguration;
import oracle.AQ.AQDequeueOption;
import oracle.AQ.AQDriverManager;
import oracle.AQ.AQException;
import oracle.AQ.AQMessage;
import oracle.AQ.AQOracleSession;
import oracle.AQ.AQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Concurrently dequeue events.
 *
 * @author Loïc Lefèvre
 */
@Component
public class AQDequeueService implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(AQDequeueService.class);

	private static final int AQ_TIMEOUT_ERROR_CODE = 25228;

	private OciConfiguration ociConfiguration;

	private AQOracleSession aqSessionForDequeue;

	public AQDequeueService(OciConfiguration ociConfiguration, DataSource dataSource) {
		this.ociConfiguration = ociConfiguration;
		try {
			Connection connection = dataSource.getConnection();
			this.aqSessionForDequeue = (AQOracleSession) AQDriverManager.createAQSession(connection.unwrap(oracle.jdbc.OracleConnection.class));
		}
		catch (SQLException | AQException e) {
			LOG.error("Creating dequeue service", e);
		}
	}

	private boolean running = true;

	public synchronized void stop() {
		LOG.warn("Stop requested!");
		//currentThread.interrupt();
		running = false;
	}

	private synchronized boolean isRunning() {
		return running;
	}

	private Thread currentThread;

	@Override
	public void run() {
		currentThread = Thread.currentThread();

		try {
			final AQQueue queue = aqSessionForDequeue.getQueue(ociConfiguration.getDatabaseUsername(), "AQ_NOTIFICATIONS_QUEUE");

			AQDequeueOption dequeueOption = new AQDequeueOption();
			dequeueOption.setWaitTime(1);
			try {
				while (isRunning()) {
					try {
						final Event event = getMessage(queue, dequeueOption);

						if (event.priority == AQEnqueueService.HIGH_PRIORITY) {
							LOG.warn("Thread {} received HIGH priority message: {}", Thread.currentThread().getName(), event.message());
						}
						else {
							LOG.warn("Thread {} received message: {}", Thread.currentThread().getName(), event.message());
						}

						Thread.yield();
					}
					catch (AQException e) {
						if(e.getErrorCode() != AQ_TIMEOUT_ERROR_CODE) {
							throw e;
						}
					}
				}
			}
			finally {
				// whatever happens stop the queue

				// and rollback before!
				try {
					aqSessionForDequeue.getDBConnection().rollback();
				}
				catch(SQLException ignored) {}

				LOG.warn("Stopping queue...");
				queue.stopEnqueue(false);
				queue.stopDequeue(false);
				LOG.warn("Stopping queue done!");
			}
		}
		catch (AQException | SQLException e) {
			LOG.error("While dequeuing!", e);
		}
		finally {
			if (aqSessionForDequeue != null) {
				LOG.warn("Closing session...");
				aqSessionForDequeue.close();
			}
		}
	}

	record Event(String message, int priority) {};

	Event getMessage(AQQueue queue, AQDequeueOption dequeueOption) throws AQException, SQLException {
		try {
			final AQMessage event = queue.dequeue(dequeueOption);

			aqSessionForDequeue.getDBConnection().commit();

			return new Event(new String(event.getRawPayload().getBytes()), event.getMessageProperty().getPriority());
		}
		catch (AQException aqe) {
			try {
				aqSessionForDequeue.getDBConnection().rollback();
			}
			catch (AQException ignored) {
			}

			throw aqe;
		}
	}
}
