package vn.viettel.vds.promotion.validation.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service containing core validation business logic
 * Pure business logic without infrastructure dependencies
 */
@Slf4j
@Service
public class ValidationDomainService {

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
     * Perform fast check validation
     */
    public ValidationResult performFastCheck(ValidationRequest request) {
        log.debug("Performing fast check for promotion: {}",
                request.getPromotionId());

        // Fast check basic eligibility
        if (!request.isFastCheckEligible()) {
            return ValidationResult.deny(
                    request.getTransactionId(),
                    "NOT_ELIGIBLE",
                    "Request not eligible for fast check"
            );
        }

        // Check customer context
        if (request.getValidationContext() == null ||
                request.getValidationContext().getCustomer() == null) {
            return ValidationResult.deny(
                    request.getTransactionId(),
                    "MISSING_CUSTOMER",
                    "Customer context is required"
            );
        }

        // Quick eligibility check
        if (request.isHighValueTransaction()) {
            // High value transactions may need additional checks
            log.debug("High value transaction detected");
        }

        return ValidationResult.allow(request.getTransactionId());
    }

    /**
     * Evaluate a single rule against the request
     *
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
     * @param rule the rule to evaluate
     * @param request the validation request
     * @return true if all basic conditions pass
     */
    private boolean evaluateBasicConditions(Rule rule) {
        // Placeholder for basic condition evaluation
        // Complex rule evaluation handled by validation-engine
        return rule.getNodes() != null && !rule.getNodes().isEmpty();
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