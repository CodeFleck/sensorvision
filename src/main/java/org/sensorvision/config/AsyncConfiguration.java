package org.sensorvision.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for Spring's @Async support
 * Enables asynchronous method execution with proper thread pool configuration
 */
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfiguration.class);

    /**
     * Configure the thread pool executor for async tasks
     * Primarily used for email notifications to avoid blocking HTTP request threads
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum threads to keep alive
        executor.setCorePoolSize(2);

        // Max pool size - maximum number of threads
        executor.setMaxPoolSize(10);

        // Queue capacity - tasks waiting for thread availability
        executor.setQueueCapacity(500);

        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("async-email-");

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Grace period before forceful shutdown
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        logger.info("Async executor configured: core={}, max={}, queue={}",
            executor.getCorePoolSize(),
            executor.getMaxPoolSize(),
            executor.getQueueCapacity());

        return executor;
    }

    /**
     * Handle exceptions from async methods
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            logger.error("Async method {} threw uncaught exception: {}",
                method.getName(),
                throwable.getMessage(),
                throwable);

            // Additional error handling could include:
            // - Sending alerts to monitoring system
            // - Storing failed tasks for retry
            // - Triggering circuit breaker
        };
    }
}
