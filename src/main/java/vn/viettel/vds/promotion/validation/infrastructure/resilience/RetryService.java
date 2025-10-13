package vn.viettel.vds.promotion.validation.infrastructure.resilience;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Service
public class RetryService {

    private static final Logger logger = LoggerFactory.getLogger(RetryService.class);

    private static final String RETRY_EXECUTION_FAILED = "Retry execution failed";

    private final RetryRegistry retryRegistry;
    private final ResilienceConfiguration config;

    public RetryService(ResilienceConfiguration config) {
        this.config = config;
        this.retryRegistry = createRetryRegistry();
    }

    private RetryRegistry createRetryRegistry() {
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(config.getRetry().getMaxAttempts())
                .waitDuration(Duration.ofMillis(config.getRetry().getWaitDuration()))
                .intervalFunction(config.getRetry().isEnableRandomJitter()
                        ? IntervalFunction.ofExponentialRandomBackoff(
                        config.getRetry().getWaitDuration(), // long value in milliseconds
                        config.getRetry().getExponentialBackoffMultiplier(),
                        0.1,
                        config.getRetry().getMaxWaitDuration()) // long value in milliseconds
                        : IntervalFunction.ofExponentialBackoff(
                        Duration.ofMillis(config.getRetry().getWaitDuration()),
                        config.getRetry().getExponentialBackoffMultiplier()))
                .retryOnException(throwable ->
                        throwable instanceof java.io.IOException ||
                                throwable instanceof java.net.SocketTimeoutException ||
                                throwable instanceof java.util.concurrent.TimeoutException ||
                                throwable instanceof org.springframework.web.client.ResourceAccessException ||
                                throwable instanceof org.springframework.web.client.HttpServerErrorException ||
                                throwable instanceof CircuitBreakerService.CircuitBreakerExecutionException)
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        IllegalStateException.class,
                        org.springframework.web.client.HttpClientErrorException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(defaultConfig);

        // Event listeners for monitoring are disabled to avoid API compatibility issues
        // TODO: Implement with correct Resilience4j event API

        return registry;
    }

    public Retry getRetry(String name) {
        return retryRegistry.retry(name);
    }

    public <T> T executeWithRetry(String retryName, Supplier<T> supplier) {
        Retry retry = getRetry(retryName);
        Supplier<T> decoratedSupplier = Retry.decorateSupplier(retry, supplier);

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            throw new RetryExecutionException(RETRY_EXECUTION_FAILED + " for: " + retryName, e, retryName);
        }
    }

    public <T> T executeWithRetry(String retryName, Callable<T> callable) {
        Retry retry = getRetry(retryName);
        Callable<T> decoratedCallable = Retry.decorateCallable(retry, callable);

        try {
            return decoratedCallable.call();
        } catch (Exception e) {
            throw new RetryExecutionException(RETRY_EXECUTION_FAILED + " for: " + retryName, e, retryName);
        }
    }

    public void executeWithRetry(String retryName, Runnable runnable) {
        Retry retry = getRetry(retryName);
        Runnable decoratedRunnable = Retry.decorateRunnable(retry, runnable);

        try {
            decoratedRunnable.run();
        } catch (Exception e) {
            throw new RetryExecutionException(RETRY_EXECUTION_FAILED + " for: " + retryName, e, retryName);
        }
    }

    public RetryStatus getRetryStatus(String name) {
        Retry retry = retryRegistry.retry(name);

        return new RetryStatus(
                name,
                retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt(),
                retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt(),
                retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt(),
                retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()
        );
    }

    public void resetRetry(String name) {
        // Retry doesn't have reset functionality like circuit breaker
        // But we can remove and recreate if needed
        logger.info("Retry registry doesn't support reset for: {}", name);
    }

    // Exception class
    public static class RetryExecutionException extends RuntimeException {
        private final String retryName;

        public RetryExecutionException(String message, Throwable cause, String retryName) {
            super(message, cause);
            this.retryName = retryName;
        }

        public String getRetryName() {
            return retryName;
        }
    }

    // Status class
    public static class RetryStatus {
        private final String name;
        private final long numberOfSuccessfulCallsWithoutRetry;
        private final long numberOfSuccessfulCallsWithRetry;
        private final long numberOfFailedCallsWithoutRetry;
        private final long numberOfFailedCallsWithRetry;

        public RetryStatus(String name, long numberOfSuccessfulCallsWithoutRetry,
                           long numberOfSuccessfulCallsWithRetry, long numberOfFailedCallsWithoutRetry,
                           long numberOfFailedCallsWithRetry) {
            this.name = name;
            this.numberOfSuccessfulCallsWithoutRetry = numberOfSuccessfulCallsWithoutRetry;
            this.numberOfSuccessfulCallsWithRetry = numberOfSuccessfulCallsWithRetry;
            this.numberOfFailedCallsWithoutRetry = numberOfFailedCallsWithoutRetry;
            this.numberOfFailedCallsWithRetry = numberOfFailedCallsWithRetry;
        }

        // Getters
        public String getName() {
            return name;
        }

        public long getNumberOfSuccessfulCallsWithoutRetry() {
            return numberOfSuccessfulCallsWithoutRetry;
        }

        public long getNumberOfSuccessfulCallsWithRetry() {
            return numberOfSuccessfulCallsWithRetry;
        }

        public long getNumberOfFailedCallsWithoutRetry() {
            return numberOfFailedCallsWithoutRetry;
        }

        public long getNumberOfFailedCallsWithRetry() {
            return numberOfFailedCallsWithRetry;
        }

        public long getTotalCalls() {
            return numberOfSuccessfulCallsWithoutRetry + numberOfSuccessfulCallsWithRetry +
                    numberOfFailedCallsWithoutRetry + numberOfFailedCallsWithRetry;
        }

        public long getTotalSuccessfulCalls() {
            return numberOfSuccessfulCallsWithoutRetry + numberOfSuccessfulCallsWithRetry;
        }

        public long getTotalFailedCalls() {
            return numberOfFailedCallsWithoutRetry + numberOfFailedCallsWithRetry;
        }

        public double getSuccessRate() {
            long total = getTotalCalls();
            return total > 0 ? (double) getTotalSuccessfulCalls() / total * 100 : 0.0;
        }
    }
}