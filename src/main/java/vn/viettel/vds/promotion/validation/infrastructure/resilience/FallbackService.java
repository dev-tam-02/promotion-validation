package vn.viettel.vds.promotion.validation.infrastructure.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FallbackService {

    private static final Logger logger = LoggerFactory.getLogger(FallbackService.class);

    private final Map<String, FallbackStrategy> fallbackStrategies = new ConcurrentHashMap<>();
    private final AtomicLong fallbackExecutions = new AtomicLong(0);
    private final Map<String, AtomicLong> strategyUsageCount = new ConcurrentHashMap<>();

    public FallbackService() {
        initializeFallbackStrategies();
    }

    private void initializeFallbackStrategies() {
        // Default fallback strategy - allow with warnings
        fallbackStrategies.put("default", new DefaultFallbackStrategy());

        // Conservative fallback strategy - deny all
        fallbackStrategies.put("conservative", new ConservativeFallbackStrategy());

        // Rule-based fallback strategy - basic validation
        fallbackStrategies.put("rule-based", new RuleBasedFallbackStrategy());

        // Cache-based fallback strategy - use cached results
        fallbackStrategies.put("cache-based", new CacheBasedFallbackStrategy());
    }

    public ValidationResponse executeValidationFallback(ValidationRequest request, String strategyName, Exception originalException) {
        fallbackExecutions.incrementAndGet();
        strategyUsageCount.computeIfAbsent(strategyName, k -> new AtomicLong(0)).incrementAndGet();

        logger.warn("Executing fallback validation strategy '{}' for request: {}, original error: {}",
                strategyName, request.getTransactionId(), originalException.getMessage());

        FallbackStrategy strategy = fallbackStrategies.getOrDefault(strategyName, fallbackStrategies.get("default"));

        try {
            ValidationResponse response = strategy.execute(request, originalException);
            logger.info("Fallback validation completed successfully using strategy: {}", strategyName);
            return response;
        } catch (Exception e) {
            logger.error("Fallback strategy '{}' failed, using default conservative approach", strategyName, e);
            return fallbackStrategies.get("conservative").execute(request, e);
        }
    }

    public ValidationResponse executeCompilationFallback(Rule rule, Exception originalException) {
        fallbackExecutions.incrementAndGet();
        String strategyName = "compilation-fallback";
        strategyUsageCount.computeIfAbsent(strategyName, k -> new AtomicLong(0)).incrementAndGet();

        logger.warn("Executing compilation fallback for rule: {}, original error: {}",
                rule.getId(), originalException.getMessage());

        return ValidationResponse.builder()
                .transactionId("compilation-fallback-" + System.currentTimeMillis())
                .valid(false)
                .message("Rule compilation failed - using fallback validation")
                .errorCode("COMPILATION_FALLBACK")
                .timestamp(Instant.now())
                .executionTimeMs(0L)
                .rulesFired(List.of())
                .build();
    }

    public FallbackStats getFallbackStats() {
        Map<String, Long> strategyStats = new ConcurrentHashMap<>();
        strategyUsageCount.forEach((strategy, count) -> strategyStats.put(strategy, count.get()));

        return new FallbackStats(
                fallbackExecutions.get(),
                strategyStats,
                Instant.now()
        );
    }

    public void resetFallbackStats() {
        fallbackExecutions.set(0);
        strategyUsageCount.clear();
        logger.info("Fallback statistics reset");
    }

    // Fallback strategy interface
    public interface FallbackStrategy {
        ValidationResponse execute(ValidationRequest request, Exception originalException);
    }

    // Default fallback strategy - allow with warnings
    private static class DefaultFallbackStrategy implements FallbackStrategy {
        @Override
        public ValidationResponse execute(ValidationRequest request, Exception originalException) {
            return ValidationResponse.builder()
                    .transactionId(request.getTransactionId())
                    .valid(true)
                    .message("Validation completed using fallback strategy - validation engine unavailable")
                    .errorCode("FALLBACK_VALIDATION")
                    .timestamp(Instant.now())
                    .executionTimeMs(0L)
                    .rulesFired(List.of("fallback-rule"))
                    .build();
        }
    }

    // Conservative fallback strategy - deny all
    private static class ConservativeFallbackStrategy implements FallbackStrategy {
        @Override
        public ValidationResponse execute(ValidationRequest request, Exception originalException) {
            return ValidationResponse.builder()
                    .transactionId(request.getTransactionId())
                    .valid(false)
                    .message("Validation failed - validation engine unavailable, using conservative approach")
                    .errorCode("CONSERVATIVE_FALLBACK")
                    .timestamp(Instant.now())
                    .executionTimeMs(0L)
                    .rulesFired(List.of())
                    .build();
        }
    }

    // Rule-based fallback strategy - basic validation
    private static class RuleBasedFallbackStrategy implements FallbackStrategy {
        @Override
        public ValidationResponse execute(ValidationRequest request, Exception originalException) {
            // Implement basic rule validation logic
            boolean isValid = performBasicValidation(request);

            return ValidationResponse.builder()
                    .transactionId(request.getTransactionId())
                    .valid(isValid)
                    .message(isValid ? "Basic validation passed" : "Basic validation failed")
                    .errorCode(isValid ? "BASIC_VALIDATION_SUCCESS" : "BASIC_VALIDATION_FAILED")
                    .timestamp(Instant.now())
                    .executionTimeMs(5L) // Simulate basic processing time
                    .rulesFired(List.of("basic-validation-rule"))
                    .build();
        }

        private boolean performBasicValidation(ValidationRequest request) {
            // Basic validation logic
            if (request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()) {
                return false;
            }

            if (request.getOrderValue() != null && request.getOrderValue().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return false;
            }

            // Add more basic validations as needed
            return true;
        }
    }

    // Cache-based fallback strategy - use cached results if available
    private static class CacheBasedFallbackStrategy implements FallbackStrategy {
        private final Map<String, ValidationResponse> responseCache = new ConcurrentHashMap<>();

        @Override
        public ValidationResponse execute(ValidationRequest request, Exception originalException) {
            String cacheKey = generateCacheKey(request);
            ValidationResponse cachedResponse = responseCache.get(cacheKey);

            if (cachedResponse != null) {
                // Return cached response with updated timestamp
                return ValidationResponse.builder()
                        .transactionId(request.getTransactionId())
                        .valid(cachedResponse.isValid())
                        .message("Validation result from cache - " + cachedResponse.getMessage())
                        .errorCode("CACHED_FALLBACK")
                        .timestamp(Instant.now())
                        .executionTimeMs(1L)
                        .rulesFired(cachedResponse.getRulesFired())
                        .build();
            }

            // No cached result available, use conservative approach
            return new ConservativeFallbackStrategy().execute(request, originalException);
        }

        private String generateCacheKey(ValidationRequest request) {
            return String.format("%s:%s:%s",
                    request.getCustomerId(),
                    request.getPromotionId(),
                    request.getOrderValue() != null ? request.getOrderValue().toString() : "0");
        }
    }

    // Statistics class
    public static class FallbackStats {
        private final long totalFallbackExecutions;
        private final Map<String, Long> strategyUsage;
        private final Instant timestamp;

        public FallbackStats(long totalFallbackExecutions, Map<String, Long> strategyUsage, Instant timestamp) {
            this.totalFallbackExecutions = totalFallbackExecutions;
            this.strategyUsage = strategyUsage;
            this.timestamp = timestamp;
        }

        // Getters
        public long getTotalFallbackExecutions() {
            return totalFallbackExecutions;
        }

        public Map<String, Long> getStrategyUsage() {
            return strategyUsage;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public String getMostUsedStrategy() {
            return strategyUsage.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("none");
        }

        public double getFallbackRate() {
            // This would need to be calculated based on total requests
            // For now, return the fallback execution count
            return totalFallbackExecutions;
        }
    }
}