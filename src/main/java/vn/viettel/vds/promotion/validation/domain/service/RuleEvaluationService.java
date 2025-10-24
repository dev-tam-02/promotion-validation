package vn.viettel.vds.promotion.validation.domain.service;

import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationContext;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Domain service responsible for evaluating rules
 * Contains complex business logic for rule evaluation
 */
public class RuleEvaluationService {

    private final ExecutorService executorService;

    public RuleEvaluationService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Evaluate a single rule against a validation context
     */
    public ValidationResult evaluateRule(Rule rule, ValidationContext context) {
        if (!rule.isActive()) {
            return ValidationResult.builder()
                    .validationId(UUID.randomUUID().toString())
                    .ruleId(rule.getId())
                    .ruleCode(rule.getCode())
                    .decision(ValidationResult.Decision.PENDING)
                    .reasonCodes(List.of("RULE_NOT_ACTIVE"))
                    .explanations(List.of("Rule is not in active status"))
                    .timestamp(Instant.now())
                    .build();
        }

        long startTime = System.currentTimeMillis();

        try {
            // Perform the actual evaluation
            ValidationResult ruleResult = (ValidationResult) rule.evaluate(context);

            // Enrich with timing information
            return ValidationResult.builder()
                    .validationId(UUID.randomUUID().toString())
                    .ruleId(rule.getId())
                    .ruleCode(rule.getCode())
                    .decision(mapStatusToDecision(ruleResult))
                    .message(ruleResult.getMessage())
                    .reasonCodes(extractReasonCodes(ruleResult))
                    .explanations(extractExplanations(ruleResult))
                    .timestamp(Instant.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            return ValidationResult.error(
                    UUID.randomUUID().toString(),
                    "Error evaluating rule: " + e.getMessage()
            );
        }
    }

    /**
     * Evaluate multiple rules in parallel
     */
    public List<ValidationResult> evaluateRulesInParallel(List<Rule> rules, ValidationContext context) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<ValidationResult>> futures = rules.stream()
                .map(rule -> CompletableFuture.supplyAsync(
                        () -> evaluateRule(rule, context),
                        executorService
                ))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    /**
     * Evaluate rules with priority ordering
     */
    public ValidationResult evaluateRulesWithPriority(
            List<Rule> rules,
            ValidationContext context,
            EvaluationStrategy strategy
    ) {
        if (rules == null || rules.isEmpty()) {
            return ValidationResult.allow(UUID.randomUUID().toString());
        }

        // Sort rules by priority (could be based on creation date, version, or custom field)
        List<Rule> sortedRules = new ArrayList<>(rules);
        sortedRules.sort(Comparator.comparing(Rule::getCreatedAt));

        switch (strategy) {
            case FAIL_FAST:
                return evaluateFailFast(sortedRules, context);
            case EVALUATE_ALL:
                return evaluateAll(sortedRules, context);
            case PASS_FIRST:
                return evaluatePassFirst(sortedRules, context);
            default:
                throw new UnsupportedOperationException("Strategy not supported: " + strategy);
        }
    }

    /**
     * Fail-fast evaluation: stop on first failure
     */
    private ValidationResult evaluateFailFast(List<Rule> rules, ValidationContext context) {
        for (Rule rule : rules) {
            ValidationResult result = evaluateRule(rule, context);
            if (result.isDenied() || result.hasError()) {
                return result;
            }
        }
        return ValidationResult.allow(UUID.randomUUID().toString());
    }

    /**
     * Evaluate all rules and combine results
     */
    private ValidationResult evaluateAll(List<Rule> rules, ValidationContext context) {
        List<ValidationResult> results = new ArrayList<>();
        List<String> allReasonCodes = new ArrayList<>();
        List<String> allExplanations = new ArrayList<>();
        long totalProcessingTime = 0;

        for (Rule rule : rules) {
            ValidationResult result = evaluateRule(rule, context);
            results.add(result);

            if (result.isDenied()) {
                if (result.getReasonCodes() != null) {
                    allReasonCodes.addAll(result.getReasonCodes());
                }
                if (result.getExplanations() != null) {
                    allExplanations.addAll(result.getExplanations());
                }
            }
            totalProcessingTime += result.getProcessingTimeMs();
        }

        // Determine overall decision
        boolean hasError = results.stream().anyMatch(ValidationResult::hasError);
        boolean hasDenied = results.stream().anyMatch(ValidationResult::isDenied);

        ValidationResult.Decision decision;
        if (hasError) {
            decision = ValidationResult.Decision.ERROR;
        } else if (hasDenied) {
            decision = ValidationResult.Decision.DENY;
        } else {
            decision = ValidationResult.Decision.ALLOW;
        }

        return ValidationResult.builder()
                .validationId(UUID.randomUUID().toString())
                .decision(decision)
                .reasonCodes(allReasonCodes)
                .explanations(allExplanations)
                .timestamp(Instant.now())
                .processingTimeMs(totalProcessingTime)
                .build();
    }

    /**
     * Pass-first evaluation: stop on first pass
     */
    private ValidationResult evaluatePassFirst(List<Rule> rules, ValidationContext context) {
        List<String> allReasonCodes = new ArrayList<>();
        List<String> allExplanations = new ArrayList<>();

        for (Rule rule : rules) {
            ValidationResult result = evaluateRule(rule, context);
            if (result.isAllowed()) {
                return result;
            }
            if (result.getReasonCodes() != null) {
                allReasonCodes.addAll(result.getReasonCodes());
            }
            if (result.getExplanations() != null) {
                allExplanations.addAll(result.getExplanations());
            }
        }

        // All rules failed
        return ValidationResult.builder()
                .validationId(UUID.randomUUID().toString())
                .decision(ValidationResult.Decision.DENY)
                .reasonCodes(allReasonCodes)
                .explanations(allExplanations)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Evaluate rules with caching support
     */
    public ValidationResult evaluateWithCache(
            Rule rule,
            ValidationContext context,
            RuleEvaluationCache cache
    ) {
        String cacheKey = generateCacheKey(rule, context);

        // Check cache first
        Optional<ValidationResult> cachedResult = cache.get(cacheKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        // Evaluate and cache result
        ValidationResult result = evaluateRule(rule, context);
        cache.put(cacheKey, result);

        return result;
    }

    private String generateCacheKey(Rule rule, ValidationContext context) {
        // Generate a unique key based on rule ID and context data
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(rule.getId());
        keyBuilder.append(":");

        // Add relevant context data to key
        if (context.getCustomer() != null) {
            keyBuilder.append(context.getCustomer().getCustomerId());
        }
        if (context.getOrder() != null) {
            keyBuilder.append(":").append(context.getOrder().getOrderId());
        }

        return keyBuilder.toString();
    }

    private ValidationResult.Decision mapStatusToDecision(ValidationResult ruleResult) {
        if (ruleResult.isValid()) {
            return ValidationResult.Decision.ALLOW;
        } else {
            return ValidationResult.Decision.DENY;
        }
    }

    private List<String> extractReasonCodes(ValidationResult ruleResult) {
        if (ruleResult.getReasonCodes() != null) {
            return ruleResult.getReasonCodes();
        }
        return ruleResult.isValid() ?
                Collections.emptyList() :
                List.of("RULE_VALIDATION_FAILED");
    }

    private List<String> extractExplanations(ValidationResult ruleResult) {
        if (ruleResult.getExplanations() != null) {
            return ruleResult.getExplanations();
        }
        if (ruleResult.getMessage() != null) {
            return List.of(ruleResult.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Evaluation strategy enum
     */
    public enum EvaluationStrategy {
        FAIL_FAST,    // Stop on first failure
        EVALUATE_ALL, // Evaluate all rules
        PASS_FIRST    // Stop on first pass
    }

    /**
     * Simple cache interface for rule evaluation
     */
    public interface RuleEvaluationCache {
        Optional<ValidationResult> get(String key);

        void put(String key, ValidationResult result);

        void invalidate(String key);

        void clear();
    }
}