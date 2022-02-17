package com.example.transactionalqueue;

import com.example.configuration.OciConfiguration;
import com.example.transactionalqueue.service.AQDequeueService;
import com.example.transactionalqueue.service.AQEnqueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.example.transactionalqueue.service.AQEnqueueService.DEFAULT_PRIORITY;
import static com.example.transactionalqueue.service.AQEnqueueService.HIGH_PRIORITY;

/**
 * This demo illustrates how to use Oracle Advanced Queues to send and receive events within a transaction.
 *
 * @author Loïc Lefèvre
 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/adque/aq-introduction.html#GUID-95868022-ECDA-4685-9D0A-52ED7663C84B">Advanced Queuing</a>
 */
@SpringBootApplication(scanBasePackages = "com.example")
@EnableAsync
public class TransactionalQueueApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(TransactionalQueueApplication.class);

	@Autowired
	private AQEnqueueService aqEnqueueService;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private OciConfiguration ociConfiguration;

	@Value("${aq.dequeue.tasks}")
	private int tasksNumber;

	@Autowired
	private ThreadPoolTaskExecutor myTasksExecutor;

	private CountDownLatch latch = new CountDownLatch(1);

	private List<AQDequeueService> aqDequeueServiceTasks = new ArrayList<>();

	@EventListener(ApplicationReadyEvent.class)
	public void startDequeueServices() {
		try {
			latch.await();
		}
		catch (InterruptedException ignored) {
		}

		for (int i = 1; i <= tasksNumber; i++) {
			AQDequeueService dequeueTask = new AQDequeueService(ociConfiguration, dataSource);
			aqDequeueServiceTasks.add(dequeueTask);
			myTasksExecutor.execute(dequeueTask);
		}
		LOG.info("{} dequeue task(s) started", tasksNumber);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("=".repeat(126));

		// sending 100 events
		for (int i = 1; i <= 100; i++) {
			aqEnqueueService.sendJSONEventInTransaction("AQ_NOTIFICATIONS_QUEUE", new BigDecimal(i),
					i == 50 ? HIGH_PRIORITY : DEFAULT_PRIORITY);

			if (i == 1) {
				// send notification to let AQDequeueServices to start properly
				latch.countDown();
			}
		}
	}

	/**
	 * Stops AQDequeueServices on application exit.
	 */
	@PreDestroy
	public void onExit() {
		LOG.info("Starting application cleanup...");
		for (AQDequeueService aqDequeueServiceTask : aqDequeueServiceTasks) {
			aqDequeueServiceTask.stop();
		}

		try {
			boolean gracefulShutdown = myTasksExecutor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
		}
		catch (InterruptedException ignored) {
		}
		LOG.info("AQ Dequeue services stopped.");

		myTasksExecutor.shutdown();
	}

	public static void main(String[] args) {
		System.setProperty("aq.drivers", "oracle.AQ.AQOracleDriver");
		SpringApplication.run(TransactionalQueueApplication.class, args);
		LOG.info("=".repeat(126));
	}
}
