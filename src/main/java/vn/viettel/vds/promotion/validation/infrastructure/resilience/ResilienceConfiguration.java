package vn.viettel.vds.promotion.validation.infrastructure.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "validation.resilience")
public class ResilienceConfiguration {

    private boolean enabled = true;
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    private RetryConfig retry = new RetryConfig();
    private TimeoutConfig timeout = new TimeoutConfig();
    private BulkheadConfig bulkhead = new BulkheadConfig();

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public CircuitBreakerConfig getCircuitBreaker() { return circuitBreaker; }
    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) { this.circuitBreaker = circuitBreaker; }

    public RetryConfig getRetry() { return retry; }
    public void setRetry(RetryConfig retry) { this.retry = retry; }

    public TimeoutConfig getTimeout() { return timeout; }
    public void setTimeout(TimeoutConfig timeout) { this.timeout = timeout; }

    public BulkheadConfig getBulkhead() { return bulkhead; }
    public void setBulkhead(BulkheadConfig bulkhead) { this.bulkhead = bulkhead; }

    public static class CircuitBreakerConfig {
        private float failureRateThreshold = 50.0f;
        private float slowCallRateThreshold = 100.0f;
        private long slowCallDurationThreshold = 10000; // 10 seconds
        private int permittedNumberOfCallsInHalfOpenState = 3;
        private int minimumNumberOfCalls = 10;
        private int slidingWindowSize = 10;
        private long waitDurationInOpenState = 60000; // 60 seconds
        private boolean automaticTransitionFromOpenToHalfOpenEnabled = true;

        // Getters and setters
        public float getFailureRateThreshold() { return failureRateThreshold; }
        public void setFailureRateThreshold(float failureRateThreshold) { this.failureRateThreshold = failureRateThreshold; }

        public float getSlowCallRateThreshold() { return slowCallRateThreshold; }
        public void setSlowCallRateThreshold(float slowCallRateThreshold) { this.slowCallRateThreshold = slowCallRateThreshold; }

        public long getSlowCallDurationThreshold() { return slowCallDurationThreshold; }
        public void setSlowCallDurationThreshold(long slowCallDurationThreshold) { this.slowCallDurationThreshold = slowCallDurationThreshold; }

        public int getPermittedNumberOfCallsInHalfOpenState() { return permittedNumberOfCallsInHalfOpenState; }
        public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) { this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState; }

        public int getMinimumNumberOfCalls() { return minimumNumberOfCalls; }
        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) { this.minimumNumberOfCalls = minimumNumberOfCalls; }

        public int getSlidingWindowSize() { return slidingWindowSize; }
        public void setSlidingWindowSize(int slidingWindowSize) { this.slidingWindowSize = slidingWindowSize; }

        public long getWaitDurationInOpenState() { return waitDurationInOpenState; }
        public void setWaitDurationInOpenState(long waitDurationInOpenState) { this.waitDurationInOpenState = waitDurationInOpenState; }

        public boolean isAutomaticTransitionFromOpenToHalfOpenEnabled() { return automaticTransitionFromOpenToHalfOpenEnabled; }
        public void setAutomaticTransitionFromOpenToHalfOpenEnabled(boolean automaticTransitionFromOpenToHalfOpenEnabled) { this.automaticTransitionFromOpenToHalfOpenEnabled = automaticTransitionFromOpenToHalfOpenEnabled; }
    }

    public static class RetryConfig {
        private int maxAttempts = 3;
        private long waitDuration = 1000; // 1 second
        private double exponentialBackoffMultiplier = 2.0;
        private long maxWaitDuration = 10000; // 10 seconds
        private boolean enableRandomJitter = true;

        // Getters and setters
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public long getWaitDuration() { return waitDuration; }
        public void setWaitDuration(long waitDuration) { this.waitDuration = waitDuration; }

        public double getExponentialBackoffMultiplier() { return exponentialBackoffMultiplier; }
        public void setExponentialBackoffMultiplier(double exponentialBackoffMultiplier) { this.exponentialBackoffMultiplier = exponentialBackoffMultiplier; }

        public long getMaxWaitDuration() { return maxWaitDuration; }
        public void setMaxWaitDuration(long maxWaitDuration) { this.maxWaitDuration = maxWaitDuration; }

        public boolean isEnableRandomJitter() { return enableRandomJitter; }
        public void setEnableRandomJitter(boolean enableRandomJitter) { this.enableRandomJitter = enableRandomJitter; }
    }

    public static class TimeoutConfig {
        private long defaultTimeoutDuration = 30000; // 30 seconds
        private long compilationTimeoutDuration = 60000; // 60 seconds
        private long executionTimeoutDuration = 10000; // 10 seconds
        private boolean cancelRunningFuture = true;

        // Getters and setters
        public long getDefaultTimeoutDuration() { return defaultTimeoutDuration; }
        public void setDefaultTimeoutDuration(long defaultTimeoutDuration) { this.defaultTimeoutDuration = defaultTimeoutDuration; }

        public long getCompilationTimeoutDuration() { return compilationTimeoutDuration; }
        public void setCompilationTimeoutDuration(long compilationTimeoutDuration) { this.compilationTimeoutDuration = compilationTimeoutDuration; }

        public long getExecutionTimeoutDuration() { return executionTimeoutDuration; }
        public void setExecutionTimeoutDuration(long executionTimeoutDuration) { this.executionTimeoutDuration = executionTimeoutDuration; }

        public boolean isCancelRunningFuture() { return cancelRunningFuture; }
        public void setCancelRunningFuture(boolean cancelRunningFuture) { this.cancelRunningFuture = cancelRunningFuture; }
    }

    public static class BulkheadConfig {
        private int maxConcurrentCalls = 10;
        private long maxWaitDuration = 0; // No wait

        // Getters and setters
        public int getMaxConcurrentCalls() { return maxConcurrentCalls; }
        public void setMaxConcurrentCalls(int maxConcurrentCalls) { this.maxConcurrentCalls = maxConcurrentCalls; }

        public long getMaxWaitDuration() { return maxWaitDuration; }
        public void setMaxWaitDuration(long maxWaitDuration) { this.maxWaitDuration = maxWaitDuration; }
    }
}