package com.example.transactionalqueue.concurrent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configure the thread pool task executor.
 *
 * @author Loïc Lefèvre
 */
@Configuration
public class ThreadPoolConfiguration {
	@Value("${aq.dequeue.tasks}")
	public int tasksNumber;

	@Bean(name = "myTasksExecutor")
	public ThreadPoolTaskExecutor taskExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(tasksNumber);
		executor.setMaxPoolSize(tasksNumber * 2);
		executor.setQueueCapacity(tasksNumber * 8);
		return executor;
	}
}
