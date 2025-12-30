package vn.viettel.vds.promotion.validation.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.in.ValidateDataUseCase;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationEnginePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;
import vn.viettel.vds.promotion.validation.domain.service.ValidationDomainService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use case implementation for data validation
 * Orchestrates validation flow using domain services and ports
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidateDataUseCaseImpl implements ValidateDataUseCase {

    private final ValidationDomainService validationDomainService;
    private final ValidationRuleRepositoryPort ruleRepository;
    private final ValidationEnginePort validationEngine;

    @Override
    public ValidationResult validate(ValidationRequest request) {
        log.info("Starting validation for transaction: {}",
                request.getTransactionId());

        try {
            // Check if validation should be bypassed
            if (validationDomainService.shouldBypassValidation(request)) {
                log.warn("Validation bypassed for transaction: {}",
                        request.getTransactionId());
                return ValidationResult.allow(request.getTransactionId());
            }

            // Determine validation strategy
            var strategy = validationDomainService.determineStrategy(request);
            log.debug("Using validation strategy: {} for transaction: {}",
                    strategy, request.getTransactionId());

            // Execute validation based on strategy
            switch (strategy) {
                case FAST:
                    return performFastCheck(request);
                case STRICT:
                    return executeStrictValidation(request);
                default:
                    return executeStandardValidation(request);
            }

        } catch (Exception e) {
            log.error("Validation failed for transaction: {}",
                    request.getTransactionId(), e);
            return ValidationResult.error(
                    request.getTransactionId(),
                    "Validation failed: " + e.getMessage()
            );
        }
    }

    @Override
    public ValidationResult performFastCheck(ValidationRequest request) {
        log.debug("Performing fast check for transaction: {}",
                request.getTransactionId());

        // Pre-validate request before delegating to Rule-Engine
        ValidationResult preValidationResult =
                validationDomainService.preValidateFastCheck(request);

        if (preValidationResult.isDenied()) {
            log.debug("Pre-validation failed for transaction: {}",
                    request.getTransactionId());
            return preValidationResult;
        }

        // Delegate actual fast-check logic to Rule-Engine service
        // Rule-Engine handles: time constraints, order constraints, blacklist, rate limiting
        if (validationEngine.isAvailable() && request.getPromotionId() != null) {
            try {
                Map<String, Object> context = new HashMap<>();
                context.put("customerId", request.getCustomerId());
                context.put("orderValue", request.getOrderValue());
                context.put("timestamp", request.getTimestamp());
                context.put("customerSegment", request.getCustomerSegment());

                return validationEngine.performFastCheck(
                        request.getPromotionId(),
                        context
                );
            } catch (Exception e) {
                log.error("Fast check via Rule-Engine failed: {}", e.getMessage(), e);
                return ValidationResult.error(
                        request.getTransactionId(),
                        "Fast check service unavailable: " + e.getMessage()
                );
            }
        }

        // If Rule-Engine is not available, fail with clear error
        log.warn("Rule-Engine service not available for fast check");
        return ValidationResult.error(
                request.getTransactionId(),
                "Rule-Engine service not available for fast check"
        );
    }

    @Override
    public ValidationResult executeFullValidation(ValidationRequest request) {
        log.info("Executing full validation for transaction: {}",
                request.getTransactionId());

        // Load all active rules
        List<Rule> rules = ruleRepository.findActiveRules();

        // Filter rules applicable to this request
        List<Rule> applicableRules = filterApplicableRules(rules, request);

        // Sort by priority
        applicableRules.sort(Comparator.comparingInt(Rule::getPriority));

        log.debug("Found {} applicable rules for transaction: {}",
                applicableRules.size(), request.getTransactionId());

        // Execute validation with filtered rules
        return validationDomainService.validate(request, applicableRules);
    }

    @Override
    public ValidationResult validateWithRuleSet(
            ValidationRequest request,
            String ruleSetId) {

        log.info("Validating with rule set: {} for transaction: {}",
                ruleSetId, request.getTransactionId());

        // Load rules for specific rule set
        List<Rule> rules = ruleRepository.findByRuleSetId(ruleSetId);

        if (rules.isEmpty()) {
            log.warn("No rules found for rule set: {}", ruleSetId);
            return ValidationResult.allow(request.getTransactionId());
        }

        // Sort by priority and execute
        rules.sort(Comparator.comparingInt(Rule::getPriority));
        return validationDomainService.validate(request, rules);
    }

    @Override
    public Map<String, ValidationResult> validateBatch(
            Map<String, ValidationRequest> requests) {

        log.info("Starting batch validation for {} requests", requests.size());

        Map<String, ValidationResult> results = new HashMap<>();

        // Process each request
        // Could be parallelized for better performance
        for (Map.Entry<String, ValidationRequest> entry : requests.entrySet()) {
            ValidationResult result = validate(entry.getValue());
            results.put(entry.getKey(), result);
        }

        log.info("Batch validation completed. Allowed: {}, Denied: {}",
                results.values().stream()
                        .filter(ValidationResult::isAllowed).count(),
                results.values().stream()
                        .filter(ValidationResult::isDenied).count()
        );

        return results;
    }

    /**
     * Execute standard validation flow
     */
    private ValidationResult executeStandardValidation(ValidationRequest request) {
        // Load applicable rules
        List<Rule> rules = loadApplicableRules(request);

        // Use domain service for validation
        ValidationResult result = validationDomainService.validate(request, rules);

        // If validation passes and engine is available, double-check with engine
        if (result.isAllowed() && validationEngine.isAvailable()) {
            try {
                ValidationResult engineResult =
                        validationEngine.executeValidation(request);
                // Use more restrictive result
                if (engineResult.isDenied()) {
                    return engineResult;
                }
            } catch (Exception e) {
                log.warn("Engine validation failed, using domain result", e);
            }
        }

        return result;
    }

    /**
     * Execute strict validation with all checks
     */
    private ValidationResult executeStrictValidation(ValidationRequest request) {
        // Load all rules including inactive ones for strict validation
        List<Rule> allRules = ruleRepository.findActiveRules();

        // Add high-priority rules for strict validation
        List<Rule> strictRules =
                ruleRepository.findByType(Rule.RuleType.BLACKLIST);
        allRules.addAll(strictRules);

        // Sort by priority
        allRules.sort(Comparator.comparingInt(Rule::getPriority));

        // Execute validation
        ValidationResult result = validationDomainService.validate(request, allRules);

        // For strict validation, also check with engine if available
        if (validationEngine.isAvailable()) {
            try {
                ValidationResult engineResult =
                        validationEngine.executeValidation(request);
                // Combine results - deny if either denies
                if (result.isAllowed() && engineResult.isDenied()) {
                    return engineResult;
                }
            } catch (Exception e) {
                log.error("Engine validation failed for strict check", e);
                // For strict validation, fail on engine error
                return ValidationResult.error(
                        request.getTransactionId(),
                        "Strict validation requires engine check"
                );
            }
        }

        return result;
    }

    /**
     * Load rules applicable to the request
     */
    private List<Rule> loadApplicableRules(ValidationRequest request) {
        List<Rule> rules;

        // If promotion ID is specified, load promotion-specific rules
        if (request.getPromotionId() != null) {
            rules = ruleRepository.findByPromotionId(request.getPromotionId());
        } else {
            // Otherwise load all active rules
            rules = ruleRepository.findActiveRules();
        }

        // Filter by customer segment if available
        String segment = request.getCustomerSegment();
        if (segment != null) {
            List<Rule> segmentRules =
                    ruleRepository.findByTargetSegment(segment);
            rules.addAll(segmentRules);
        }

        // Remove duplicates and sort by priority
        return rules.stream()
                .distinct()
                .sorted(Comparator.comparingInt(Rule::getPriority))
                .toList();
    }

    /**
     * Filter rules applicable to the request
     */
    private List<Rule> filterApplicableRules(
            List<Rule> rules,
            ValidationRequest request) {

        String customerSegment = request.getCustomerSegment();

        return rules.stream()
                .filter(rule -> rule.isEffective(request.getTimestamp()))
                .filter(rule -> rule.appliesTo(customerSegment))
                .toList();
    }
}