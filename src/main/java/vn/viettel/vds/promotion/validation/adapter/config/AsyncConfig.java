package vn.viettel.vds.promotion.validation.adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    private static final int TASK_EXECUTOR_CORE_POOL_SIZE = 5;
    private static final int TASK_EXECUTOR_MAX_POOL_SIZE = 20;
    private static final int TASK_EXECUTOR_QUEUE_CAPACITY = 100;
    private static final int TASK_EXECUTOR_AWAIT_TERMINATION_SECONDS = 30;
    private static final int OUTBOX_EXECUTOR_CORE_POOL_SIZE = 3;
    private static final int OUTBOX_EXECUTOR_MAX_POOL_SIZE = 10;
    private static final int OUTBOX_EXECUTOR_QUEUE_CAPACITY = 50;
    private static final int OUTBOX_EXECUTOR_AWAIT_TERMINATION_SECONDS = 60;
    private static final int PUBLISH_EXECUTOR_CORE_POOL_SIZE = 2;
    private static final int PUBLISH_EXECUTOR_MAX_POOL_SIZE = 5;
    private static final int PUBLISH_EXECUTOR_QUEUE_CAPACITY = 25;
    private static final int PUBLISH_EXECUTOR_AWAIT_TERMINATION_SECONDS = 120;

    /**
     * Task executor for async operations
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(TASK_EXECUTOR_CORE_POOL_SIZE);
        executor.setMaxPoolSize(TASK_EXECUTOR_MAX_POOL_SIZE);
        executor.setQueueCapacity(TASK_EXECUTOR_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("ValidationAsync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(TASK_EXECUTOR_AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }

    /**
     * Task executor specifically for outbox event processing
     */
    @Bean(name = "outboxExecutor")
    public Executor outboxExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(OUTBOX_EXECUTOR_CORE_POOL_SIZE);
        executor.setMaxPoolSize(OUTBOX_EXECUTOR_MAX_POOL_SIZE);
        executor.setQueueCapacity(OUTBOX_EXECUTOR_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("OutboxProcessor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(OUTBOX_EXECUTOR_AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }

    /**
     * Task executor for publishing jobs
     */
    @Bean(name = "publishExecutor")
    public Executor publishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(PUBLISH_EXECUTOR_CORE_POOL_SIZE);
        executor.setMaxPoolSize(PUBLISH_EXECUTOR_MAX_POOL_SIZE);
        executor.setQueueCapacity(PUBLISH_EXECUTOR_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("PublishProcessor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(PUBLISH_EXECUTOR_AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }
}