package vn.viettel.vds.promotion.validation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service containing core validation business logic.
 * Pure business logic without infrastructure dependencies.
 * <p>
 * This is a pure domain service - no Spring annotations.
 * Bean wiring is done via DomainServicesConfig in adapter layer.
 * </p>
 */
public class ValidationDomainService {

    private static final Logger log = LoggerFactory.getLogger(ValidationDomainService.class);

    /**
     * Validate a request against a set of rules
     */
    public ValidationResult validate(
            ValidationRequest request,
            List<Rule> rules) {

        log.debug("Starting validation for transaction: {}",
                request.getTransactionId());

        long startTime = System.currentTimeMillis();

        // Check if request is valid
        if (!request.hasValidContext()) {
            return ValidationResult.deny(
                    request.getTransactionId(),
                    "INVALID_CONTEXT",
                    "Validation context is invalid or missing"
            );
        }

        // Check time window
        if (!request.isWithinTimeWindow(300)) { // 5 minutes
            return ValidationResult.deny(
                    request.getTransactionId(),
                    "REQUEST_EXPIRED",
                    "Validation request has expired"
            );
        }

        // Apply validation rules
        List<String> failedRules = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        for (Rule rule : rules) {
            boolean ruleResult = evaluateRule(rule, request);
            if (!ruleResult) {
                failedRules.add(rule.getRuleCode());
                explanations.add(rule.getDescription());
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        // Build result based on rule evaluation
        if (failedRules.isEmpty()) {
            return ValidationResult.builder()
                    .validationId(request.getTransactionId())
                    .decision(ValidationResult.Decision.ALLOW)
                    .reasonCodes(new ArrayList<>())
                    .explanations(new ArrayList<>())
                    .timestamp(Instant.now())
                    .processingTimeMs(processingTime)
                    .build();
        } else {
            return ValidationResult.builder()
                    .validationId(request.getTransactionId())
                    .decision(ValidationResult.Decision.DENY)
                    .reasonCodes(failedRules)
                    .explanations(explanations)
                    .timestamp(Instant.now())
                    .processingTimeMs(processingTime)
                    .build();
        }
    }

    /**
     * Pre-validate request before delegating to Rule-Engine fast-check.
     * This method only performs basic input validation and eligibility checks.
     * Actual fast-check logic (time constraints, blacklist, rate limiting) is handled by Rule-Engine.
     *
     * @param request the validation request to pre-validate
     * @return ValidationResult.ALLOW if pre-validation passes, DENY otherwise
     */
    public ValidationResult preValidateFastCheck(ValidationRequest request) {
        log.debug("Pre-validating fast check request for promotion: {}",
                request.getPromotionId());

        // Validate basic eligibility for fast-check
        if (!request.isFastCheckEligible()) {
            return ValidationResult.deny(
                    request.getTransactionId(),
                    "NOT_ELIGIBLE",
                    "Request not eligible for fast check"
            );
        }

        // Validate required customer context
        if (request.getValidationContext() == null ||
                request.getValidationContext().getCustomer() == null) {
            return ValidationResult.deny(
                    request.getTransactionId(),
                    "MISSING_CUSTOMER",
                    "Customer context is required for fast check"
            );
        }

        // Pre-validation passed - delegate to Rule-Engine for actual fast-check
        log.debug("Pre-validation passed for transaction: {}", request.getTransactionId());
        return ValidationResult.allow(request.getTransactionId());
    }

    /**
     * Evaluate a single rule against the request
     * <p>
     * Note: This is a simplified implementation. In production, this would delegate to
     * a RuleEvaluationService that performs actual rule evaluation against ValidationContext.
     * For now, we skip evaluation for applicable rules as the actual validation logic
     * is handled by the validation-engine module with Drools.
     */
    private boolean evaluateRule(
            Rule rule,
            ValidationRequest request) {

        // Check if rule is active
        if (!rule.isActive()) {
            return true; // Inactive rules pass by default
        }

        // Check if rule applies to this request
        if (!rule.appliesTo(request.getCustomerSegment())) {
            return true; // Rule doesn't apply to this segment
        }

        // Actual rule evaluation delegated to validation-engine module with Drools
        // This method will be enhanced when RuleEvaluationService integration is complete
        // For basic validations, we can check simple conditions
        if (rule.getNodes() != null && !rule.getNodes().isEmpty()) {
            return evaluateBasicConditions(rule);
        }

        // Active and applicable rules without conditions pass by default
        return true;
    }

    /**
     * Evaluate basic rule conditions
     *
     * @param rule the rule to evaluate
     * @return true if all basic conditions pass, false if validation fails
     */
    private boolean evaluateBasicConditions(Rule rule) {
        // Complex rule evaluation is delegated to validation-engine module with Drools
        // This is a simplified placeholder that performs basic structural validation

        if (rule.getNodes() == null || rule.getNodes().isEmpty()) {
            // Rules without nodes are invalid - they need at least one condition
            log.debug("Rule {} has no nodes defined", rule.getId());
            return false;
        }

        // Check if any node has an invalid structure
        boolean hasValidNodes = rule.getNodes().stream()
                .anyMatch(node -> node != null && node.getOperatorName() != null);

        if (!hasValidNodes) {
            log.debug("Rule {} has no valid operator nodes", rule.getId());
            return false;
        }

        // Note: Complex rule evaluation is delegated to validation-engine module with Drools
        // This method performs basic structural validation only
        // Rules with valid node structure pass this check and will be evaluated by validation-engine
        return true;
    }

    /**
     * Check if validation should be bypassed
     */
    public boolean shouldBypassValidation(ValidationRequest request) {
        // Example bypass logic for testing or emergency
        if (request.getContext() != null) {
            Object bypass = request.getContext().get("bypass_validation");
            if (Boolean.TRUE.equals(bypass)) {
                log.warn("Validation bypassed for transaction: {}",
                        request.getTransactionId());
                return true;
            }
        }
        return false;
    }

    /**
     * Determine validation strategy based on request
     */
    public ValidationStrategy determineStrategy(ValidationRequest request) {
        if (request.isHighValueTransaction()) {
            return ValidationStrategy.STRICT;
        } else if (request.isFastCheckEligible()) {
            return ValidationStrategy.FAST;
        } else {
            return ValidationStrategy.STANDARD;
        }
    }

    public enum ValidationStrategy {
        FAST,     // Quick validation for low-risk transactions
        STANDARD, // Normal validation flow
        STRICT    // Enhanced validation for high-risk transactions
    }
}