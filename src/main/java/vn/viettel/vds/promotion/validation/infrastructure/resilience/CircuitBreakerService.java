package vn.viettel.vds.promotion.validation.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Service
public class CircuitBreakerService {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerService.class);

    private static final String CIRCUIT_BREAKER_EXECUTION_FAILED = "Circuit breaker execution failed";

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ResilienceConfiguration config;

    public CircuitBreakerService(ResilienceConfiguration config) {
        this.config = config;
        this.circuitBreakerRegistry = createCircuitBreakerRegistry();
    }

    private CircuitBreakerRegistry createCircuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(config.getCircuitBreaker().getFailureRateThreshold())
                .slowCallRateThreshold(config.getCircuitBreaker().getSlowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(config.getCircuitBreaker().getSlowCallDurationThreshold()))
                .permittedNumberOfCallsInHalfOpenState(config.getCircuitBreaker().getPermittedNumberOfCallsInHalfOpenState())
                .minimumNumberOfCalls(config.getCircuitBreaker().getMinimumNumberOfCalls())
                .slidingWindowSize(config.getCircuitBreaker().getSlidingWindowSize())
                .waitDurationInOpenState(Duration.ofMillis(config.getCircuitBreaker().getWaitDurationInOpenState()))
                .automaticTransitionFromOpenToHalfOpenEnabled(config.getCircuitBreaker().isAutomaticTransitionFromOpenToHalfOpenEnabled())
                .recordExceptions(
                        java.io.IOException.class,
                        java.net.SocketTimeoutException.class,
                        java.util.concurrent.TimeoutException.class,
                        org.springframework.web.client.ResourceAccessException.class,
                        org.springframework.web.client.RestClientException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        IllegalStateException.class
                )
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Event listeners for monitoring are disabled to avoid API compatibility issues
        // TODO: Implement with correct Resilience4j event API

        return registry;
    }

    public CircuitBreaker getCircuitBreaker(String name) {
        return circuitBreakerRegistry.circuitBreaker(name);
    }

    public <T> T executeWithCircuitBreaker(String circuitBreakerName, Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = getCircuitBreaker(circuitBreakerName);
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            throw new CircuitBreakerExecutionException(CIRCUIT_BREAKER_EXECUTION_FAILED + " for: " + circuitBreakerName, e, circuitBreakerName);
        }
    }

    public <T> T executeWithCircuitBreaker(String circuitBreakerName, Callable<T> callable) {
        CircuitBreaker circuitBreaker = getCircuitBreaker(circuitBreakerName);
        Callable<T> decoratedCallable = CircuitBreaker.decorateCallable(circuitBreaker, callable);

        try {
            return decoratedCallable.call();
        } catch (Exception e) {
            throw new CircuitBreakerExecutionException(CIRCUIT_BREAKER_EXECUTION_FAILED + " for: " + circuitBreakerName, e, circuitBreakerName);
        }
    }

    public void executeWithCircuitBreaker(String circuitBreakerName, Runnable runnable) {
        CircuitBreaker circuitBreaker = getCircuitBreaker(circuitBreakerName);
        Runnable decoratedRunnable = CircuitBreaker.decorateRunnable(circuitBreaker, runnable);

        try {
            decoratedRunnable.run();
        } catch (Exception e) {
            throw new CircuitBreakerExecutionException(CIRCUIT_BREAKER_EXECUTION_FAILED + " for: " + circuitBreakerName, e, circuitBreakerName);
        }
    }

    public CircuitBreakerStatus getCircuitBreakerStatus(String name) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);

        return new CircuitBreakerStatus(
                name,
                circuitBreaker.getState(),
                circuitBreaker.getMetrics().getFailureRate(),
                circuitBreaker.getMetrics().getSlowCallRate(),
                circuitBreaker.getMetrics().getNumberOfFailedCalls(),
                circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(),
                circuitBreaker.getMetrics().getNumberOfSlowCalls(),
                circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()
        );
    }

    public void resetCircuitBreaker(String name) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        circuitBreaker.reset();
        logger.info("Circuit breaker reset: {}", name);
    }

    public void transitionToOpenState(String name) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        circuitBreaker.transitionToOpenState();
        logger.info("Circuit breaker manually opened: {}", name);
    }

    public void transitionToClosedState(String name) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        circuitBreaker.transitionToClosedState();
        logger.info("Circuit breaker manually closed: {}", name);
    }

    // Exception class
    public static class CircuitBreakerExecutionException extends RuntimeException {
        private final String circuitBreakerName;

        public CircuitBreakerExecutionException(String message, Throwable cause, String circuitBreakerName) {
            super(message, cause);
            this.circuitBreakerName = circuitBreakerName;
        }

        public String getCircuitBreakerName() {
            return circuitBreakerName;
        }
    }

    // Status class
    public static class CircuitBreakerStatus {
        private final String name;
        private final CircuitBreaker.State state;
        private final float failureRate;
        private final float slowCallRate;
        private final long numberOfFailedCalls;
        private final long numberOfSuccessfulCalls;
        private final long numberOfSlowCalls;
        private final long numberOfNotPermittedCalls;

        public CircuitBreakerStatus(String name, CircuitBreaker.State state, float failureRate,
                                    float slowCallRate, long numberOfFailedCalls, long numberOfSuccessfulCalls,
                                    long numberOfSlowCalls, long numberOfNotPermittedCalls) {
            this.name = name;
            this.state = state;
            this.failureRate = failureRate;
            this.slowCallRate = slowCallRate;
            this.numberOfFailedCalls = numberOfFailedCalls;
            this.numberOfSuccessfulCalls = numberOfSuccessfulCalls;
            this.numberOfSlowCalls = numberOfSlowCalls;
            this.numberOfNotPermittedCalls = numberOfNotPermittedCalls;
        }

        // Getters
        public String getName() {
            return name;
        }

        public CircuitBreaker.State getState() {
            return state;
        }

        public float getFailureRate() {
            return failureRate;
        }

        public float getSlowCallRate() {
            return slowCallRate;
        }

        public long getNumberOfFailedCalls() {
            return numberOfFailedCalls;
        }

        public long getNumberOfSuccessfulCalls() {
            return numberOfSuccessfulCalls;
        }

        public long getNumberOfSlowCalls() {
            return numberOfSlowCalls;
        }

        public long getNumberOfNotPermittedCalls() {
            return numberOfNotPermittedCalls;
        }

        public long getTotalCalls() {
            return numberOfFailedCalls + numberOfSuccessfulCalls;
        }

        public boolean isHealthy() {
            return state == CircuitBreaker.State.CLOSED;
        }
    }
}