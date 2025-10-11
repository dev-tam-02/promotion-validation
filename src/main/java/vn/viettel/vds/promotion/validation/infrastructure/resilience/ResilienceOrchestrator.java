package vn.viettel.vds.promotion.validation.infrastructure.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

@Service
public class ResilienceOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceOrchestrator.class);

    private final CircuitBreakerService circuitBreakerService;
    private final RetryService retryService;
    private final TimeoutService timeoutService;
    private final BulkheadService bulkheadService;
    private final FallbackService fallbackService;
    private final ResilienceConfiguration config;

    public ResilienceOrchestrator(CircuitBreakerService circuitBreakerService,
                                  RetryService retryService,
                                  TimeoutService timeoutService,
                                  BulkheadService bulkheadService,
                                  FallbackService fallbackService,
                                  ResilienceConfiguration config) {
        this.circuitBreakerService = circuitBreakerService;
        this.retryService = retryService;
        this.timeoutService = timeoutService;
        this.bulkheadService = bulkheadService;
        this.fallbackService = fallbackService;
        this.config = config;
    }

    public ValidationResponse executeValidationWithResilience(ValidationRequest request,
                                                              Supplier<ValidationResponse> validationSupplier) {
        String operationName = "validation-" + request.getPromotionId();
        Instant startTime = Instant.now();

        try {
            if (!config.isEnabled()) {
                logger.debug("Resilience patterns disabled, executing validation directly");
                return validationSupplier.get();
            }

            return executeWithAllPatterns(operationName, () -> {
                logger.debug("Executing validation with resilience patterns for request: {}", request.getTransactionId());
                return validationSupplier.get();
            });

        } catch (Exception e) {
            logger.error("Validation execution failed for request: {}, attempting fallback", request.getTransactionId(), e);
            return fallbackService.executeValidationFallback(request, "default", e);
        } finally {
            Duration executionTime = Duration.between(startTime, Instant.now());
            logger.info("Validation execution completed for request: {} in {}ms",
                    request.getTransactionId(), executionTime.toMillis());
        }
    }

    public ValidationResponse executeCompilationWithResilience(Rule rule,
                                                               Supplier<ValidationResponse> compilationSupplier) {
        String operationName = "compilation-" + rule.getId();
        Instant startTime = Instant.now();

        try {
            if (!config.isEnabled()) {
                logger.debug("Resilience patterns disabled, executing compilation directly");
                return compilationSupplier.get();
            }

            return executeWithAllPatterns(operationName, () -> {
                logger.debug("Executing compilation with resilience patterns for rule: {}", rule.getId());
                return compilationSupplier.get();
            });

        } catch (Exception e) {
            logger.error("Compilation execution failed for rule: {}, attempting fallback", rule.getId(), e);
            return fallbackService.executeCompilationFallback(rule, e);
        } finally {
            Duration executionTime = Duration.between(startTime, Instant.now());
            logger.info("Compilation execution completed for rule: {} in {}ms",
                    rule.getId(), executionTime.toMillis());
        }
    }

    public <T> T executeWithAllPatterns(String operationName, Supplier<T> operation) {
        return executeWithCircuitBreaker(operationName, () ->
                executeWithRetry(operationName, () ->
                        executeWithTimeout(operationName, () ->
                                executeWithBulkhead(operationName, operation))));
    }

    public <T> T executeWithCircuitBreaker(String operationName, Supplier<T> operation) {
        try {
            return circuitBreakerService.executeWithCircuitBreaker(operationName, operation);
        } catch (CircuitBreakerService.CircuitBreakerExecutionException e) {
            logger.error("Circuit breaker execution failed for operation: {}", operationName, e);
            throw new ResilienceExecutionException("Circuit breaker failed", e, operationName, "CIRCUIT_BREAKER");
        }
    }

    public <T> T executeWithRetry(String operationName, Supplier<T> operation) {
        try {
            return retryService.executeWithRetry(operationName, operation);
        } catch (RetryService.RetryExecutionException e) {
            logger.error("Retry execution failed for operation: {}", operationName, e);
            throw new ResilienceExecutionException("Retry failed", e, operationName, "RETRY");
        }
    }

    public <T> T executeWithTimeout(String operationName, Supplier<T> operation) {
        try {
            return timeoutService.executeWithTimeout(operationName, operation);
        } catch (TimeoutService.TimeoutExecutionException e) {
            logger.error("Timeout execution failed for operation: {}", operationName, e);
            throw new ResilienceExecutionException("Timeout occurred", e, operationName, "TIMEOUT");
        }
    }

    public <T> T executeWithBulkhead(String operationName, Supplier<T> operation) {
        try {
            return bulkheadService.executeWithBulkhead(operationName, operation);
        } catch (BulkheadService.BulkheadExecutionException e) {
            logger.error("Bulkhead execution failed for operation: {}", operationName, e);
            throw new ResilienceExecutionException("Bulkhead rejected", e, operationName, "BULKHEAD");
        }
    }

    public ResilienceStatus getResilienceStatus() {
        return new ResilienceStatus(
                config.isEnabled(),
                circuitBreakerService.getCircuitBreakerStatus("default"),
                retryService.getRetryStatus("default"),
                timeoutService.getTimeoutStatus("default"),
                bulkheadService.getBulkheadStatus("default"),
                fallbackService.getFallbackStats(),
                Instant.now()
        );
    }

    public void resetResilienceComponents() {
        logger.info("Resetting all resilience components");
        circuitBreakerService.resetCircuitBreaker("default");
        retryService.resetRetry("default");
        bulkheadService.resetBulkhead("default");
        fallbackService.resetFallbackStats();
    }

    // Custom exception for resilience execution failures
    public static class ResilienceExecutionException extends RuntimeException {
        private final String operationName;
        private final String failureType;

        public ResilienceExecutionException(String message, Throwable cause, String operationName, String failureType) {
            super(message, cause);
            this.operationName = operationName;
            this.failureType = failureType;
        }

        public String getOperationName() {
            return operationName;
        }

        public String getFailureType() {
            return failureType;
        }
    }

    // Comprehensive status class
    public static class ResilienceStatus {
        private final boolean enabled;
        private final CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus;
        private final RetryService.RetryStatus retryStatus;
        private final TimeoutService.TimeoutStatus timeoutStatus;
        private final BulkheadService.BulkheadStatus bulkheadStatus;
        private final FallbackService.FallbackStats fallbackStats;
        private final Instant timestamp;

        public ResilienceStatus(boolean enabled,
                                CircuitBreakerService.CircuitBreakerStatus circuitBreakerStatus,
                                RetryService.RetryStatus retryStatus,
                                TimeoutService.TimeoutStatus timeoutStatus,
                                BulkheadService.BulkheadStatus bulkheadStatus,
                                FallbackService.FallbackStats fallbackStats,
                                Instant timestamp) {
            this.enabled = enabled;
            this.circuitBreakerStatus = circuitBreakerStatus;
            this.retryStatus = retryStatus;
            this.timeoutStatus = timeoutStatus;
            this.bulkheadStatus = bulkheadStatus;
            this.fallbackStats = fallbackStats;
            this.timestamp = timestamp;
        }

        // Getters
        public boolean isEnabled() {
            return enabled;
        }

        public CircuitBreakerService.CircuitBreakerStatus getCircuitBreakerStatus() {
            return circuitBreakerStatus;
        }

        public RetryService.RetryStatus getRetryStatus() {
            return retryStatus;
        }

        public TimeoutService.TimeoutStatus getTimeoutStatus() {
            return timeoutStatus;
        }

        public BulkheadService.BulkheadStatus getBulkheadStatus() {
            return bulkheadStatus;
        }

        public FallbackService.FallbackStats getFallbackStats() {
            return fallbackStats;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public boolean isHealthy() {
            return enabled &&
                    circuitBreakerStatus.isHealthy() &&
                    !bulkheadStatus.isAtCapacity();
        }

        public String getOverallStatus() {
            if (!enabled) return "DISABLED";
            if (!isHealthy()) return "DEGRADED";
            return "HEALTHY";
        }
    }
}