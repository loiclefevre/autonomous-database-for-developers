package com.example.transactionalqueue.service;

import oracle.AQ.AQDequeueOption;
import oracle.AQ.AQException;
import oracle.AQ.AQMessage;
import oracle.AQ.AQOracleSession;
import oracle.AQ.AQQueue;
import oracle.AQ.AQSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

@Component
public class AQDequeueService implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(AQDequeueService.class);

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private AQSession aqSessionForDequeue;

	@EventListener(ApplicationReadyEvent.class)
	public void start() {
		AQDequeueService dequeueTask = applicationContext.getBean(AQDequeueService.class);
		taskExecutor.execute(dequeueTask );
		LOG.info("Dequeue task started");
	}

	@Override
	public void run() {
		try {
			final Connection connection = ((AQOracleSession) aqSessionForDequeue).getDBConnection();
			final AQQueue queue = aqSessionForDequeue.getQueue("DEMOS", "AQ_NOTIFICATIONS_QUEUE");
			final AQDequeueOption dequeueOption = new AQDequeueOption();

			while (true) {
				try {
					AQMessage newMessage = queue.dequeue(dequeueOption);
					connection.commit();

					LOG.warn("Received message: {}", new String(newMessage.getRawPayload().getBytes()));
				}
				catch (SQLException sqle) {
					try {
						connection.rollback();
					}
					catch (SQLException ignored) {
					}
				}

				Thread.yield();
			}
		}
		catch (AQException aqe) {
			LOG.error("Dequeuing!", aqe);
		}
		finally {
			if(aqSessionForDequeue != null) {
				aqSessionForDequeue.close();
			}
		}
	}
}
