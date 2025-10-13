package vn.viettel.vds.promotion.validation.infrastructure.resilience;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Service
public class TimeoutService {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutService.class);

    private static final String TIMEOUT_EXECUTION_FAILED = "Timeout execution failed";
    private static final String FOR_SEPARATOR = " for: ";

    private final TimeLimiterRegistry timeLimiterRegistry;
    private final ResilienceConfiguration config;
    private final ExecutorService executorService;

    public TimeoutService(ResilienceConfiguration config) {
        this.config = config;
        this.timeLimiterRegistry = createTimeLimiterRegistry();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    private TimeLimiterRegistry createTimeLimiterRegistry() {
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(config.getTimeout().getDefaultTimeoutDuration()))
                .cancelRunningFuture(config.getTimeout().isCancelRunningFuture())
                .build();

        TimeLimiterConfig compilationConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(config.getTimeout().getCompilationTimeoutDuration()))
                .cancelRunningFuture(config.getTimeout().isCancelRunningFuture())
                .build();

        TimeLimiterConfig executionConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(config.getTimeout().getExecutionTimeoutDuration()))
                .cancelRunningFuture(config.getTimeout().isCancelRunningFuture())
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(defaultConfig);
        registry.timeLimiter("compilation", compilationConfig);
        registry.timeLimiter("execution", executionConfig);

        // Event listeners for monitoring are disabled to avoid API compatibility issues
        // TODO: Implement with correct Resilience4j event API

        return registry;
    }

    public TimeLimiter getTimeLimiter(String name) {
        return timeLimiterRegistry.timeLimiter(name);
    }

    public <T> T executeWithTimeout(String timeLimiterName, Supplier<T> supplier) {
        TimeLimiter timeLimiter = getTimeLimiter(timeLimiterName);

        try {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier, executorService);
            return timeLimiter.executeFutureSupplier(() -> future);
        } catch (Exception e) {
            throw new TimeoutExecutionException(TIMEOUT_EXECUTION_FAILED + FOR_SEPARATOR + timeLimiterName, e, timeLimiterName);
        }
    }

    public <T> T executeWithTimeout(String timeLimiterName, Callable<T> callable) {
        TimeLimiter timeLimiter = getTimeLimiter(timeLimiterName);

        try {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

            return timeLimiter.executeFutureSupplier(() -> future);
        } catch (Exception e) {
            throw new TimeoutExecutionException(TIMEOUT_EXECUTION_FAILED + FOR_SEPARATOR + timeLimiterName, e, timeLimiterName);
        }
    }

    public void executeWithTimeout(String timeLimiterName, Runnable runnable) {
        TimeLimiter timeLimiter = getTimeLimiter(timeLimiterName);

        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(runnable, executorService);
            timeLimiter.executeFutureSupplier(() -> future);
        } catch (Exception e) {
            throw new TimeoutExecutionException(TIMEOUT_EXECUTION_FAILED + FOR_SEPARATOR + timeLimiterName, e, timeLimiterName);
        }
    }

    public TimeoutStatus getTimeoutStatus(String name) {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(name);
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();

        return new TimeoutStatus(
                name,
                config.getTimeoutDuration(),
                config.shouldCancelRunningFuture(),
                0L, // metrics not available in this version
                0L, // metrics not available in this version
                0L  // metrics not available in this version
        );
    }

    public void shutdown() {
        logger.info("Shutting down timeout service executor");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    // Exception class
    public static class TimeoutExecutionException extends RuntimeException {
        private final String timeLimiterName;

        public TimeoutExecutionException(String message, Throwable cause, String timeLimiterName) {
            super(message, cause);
            this.timeLimiterName = timeLimiterName;
        }

        public String getTimeLimiterName() {
            return timeLimiterName;
        }
    }

    // Status class
    public static class TimeoutStatus {
        private final String name;
        private final Duration timeoutDuration;
        private final boolean cancelRunningFuture;
        private final long numberOfSuccessfulCalls;
        private final long numberOfFailedCalls;
        private final long numberOfTimeouts;

        public TimeoutStatus(String name, Duration timeoutDuration, boolean cancelRunningFuture,
                             long numberOfSuccessfulCalls, long numberOfFailedCalls, long numberOfTimeouts) {
            this.name = name;
            this.timeoutDuration = timeoutDuration;
            this.cancelRunningFuture = cancelRunningFuture;
            this.numberOfSuccessfulCalls = numberOfSuccessfulCalls;
            this.numberOfFailedCalls = numberOfFailedCalls;
            this.numberOfTimeouts = numberOfTimeouts;
        }

        // Getters
        public String getName() {
            return name;
        }

        public Duration getTimeoutDuration() {
            return timeoutDuration;
        }

        public boolean isCancelRunningFuture() {
            return cancelRunningFuture;
        }

        public long getNumberOfSuccessfulCalls() {
            return numberOfSuccessfulCalls;
        }

        public long getNumberOfFailedCalls() {
            return numberOfFailedCalls;
        }

        public long getNumberOfTimeouts() {
            return numberOfTimeouts;
        }

        public long getTotalCalls() {
            return numberOfSuccessfulCalls + numberOfFailedCalls;
        }

        public double getTimeoutRate() {
            long total = getTotalCalls();
            return total > 0 ? (double) numberOfTimeouts / total * 100 : 0.0;
        }

        public double getSuccessRate() {
            long total = getTotalCalls();
            return total > 0 ? (double) numberOfSuccessfulCalls / total * 100 : 0.0;
        }
    }
}