package vn.viettel.vds.promotion.validation.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import vn.viettel.vds.promotion.validation.config.validator.PositiveDuration;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "integration.validation-engine")
public class ValidationEngineProperties {

    @NotBlank
    private String url = "http://localhost:8082";

    @PositiveDuration
    private Duration timeout = Duration.ofSeconds(30);

    @Positive
    private int retryAttempts = 3;

    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    // Getters and setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public static class CircuitBreakerConfig {
        private int failureRateThreshold = 50;
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 10;

        // Getters and setters
        public int getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(int failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public Duration getWaitDurationInOpenState() {
            return waitDurationInOpenState;
        }

        public void setWaitDurationInOpenState(Duration waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
        }
    }
}