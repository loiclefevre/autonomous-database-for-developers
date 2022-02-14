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
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.sql.DataSource;
import java.math.BigDecimal;

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
	private TaskExecutor myTasksExecutor;

	@EventListener(ApplicationReadyEvent.class)
	public void startDequeueServices() {
		for (int i = 1; i <= tasksNumber; i++) {
			AQDequeueService dequeueTask = new AQDequeueService(ociConfiguration,dataSource);
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
					i == 50 ? HIGH_PRIORITY : DEFAULT_PRIORITY );
		}
	}

	public static void main(String[] args) {
		System.setProperty("aq.drivers", "oracle.AQ.AQOracleDriver");
		SpringApplication.run(TransactionalQueueApplication.class, args);
		LOG.info("=".repeat(126));
	}
}