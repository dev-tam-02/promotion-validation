package vn.viettel.vds.promotion.validation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.schema.redemption.event.ValidateStackableDiscountResultEvent;
import vn.viettel.vds.promotion.validation.application.port.in.ValidateStackableDiscountUseCase;
import vn.viettel.vds.promotion.validation.application.port.in.command.ValidateStackableDiscountCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.*;
import vn.viettel.vds.promotion.validation.application.port.out.PublishValidationResultPort;
import vn.viettel.vds.promotion.validation.domain.fact.DiscountFact;
import vn.viettel.vds.promotion.validation.domain.fact.FactPack;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.StackingContext;
import vn.viettel.vds.promotion.validation.domain.model.StackingResult;
import vn.viettel.vds.promotion.validation.domain.service.StackableDiscountValidationService;
import vn.viettel.vds.promotion.validation.domain.service.StackableRuleEvaluator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
/**
 * Application service implementing the validate stackable discount use case.
 * <p>
 * This service orchestrates the validation flow by:
 * - Building fact packs from commands
 * - Executing domain validation logic
 * - Evaluating validation rules
 * - Publishing results
 * </p>
 * <p>
 * This is the primary entry point for stackable discount validation.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ValidateStackableDiscountService implements ValidateStackableDiscountUseCase {

    private final StackableDiscountValidationService domainValidationService;
    private final StackableRuleEvaluator ruleEvaluator;
    private final RuleService ruleService;
    private final PublishValidationResultPort publishResultPort;
    private final AuditService auditService;
    private final FactPackBuilder factPackBuilder;
    private final ValidationResultEventBuilder eventBuilder;

    @Override
    public ValidateStackableDiscountResult validate(ValidateStackableDiscountCommand command) {
        long startTime = System.currentTimeMillis();

        log.info("Starting stackable discount validation: idempotencyKey={}, customerId={}, discountCount={}",
                command.idempotencyKey(),
                command.customerInfo().customerId(),
                command.getDiscountCount());

        try {
            // Step 1: Build stacking context from command
            StackingContext context = buildStackingContext(command);

            // Validate context
            if (!context.isValid()) {
                log.warn("Invalid stacking context: validationId={}", command.idempotencyKey());
                return handleInvalidContext(command, context, startTime);
            }

            // Step 2: Retrieve applicable validation rules
            List<Rule> rules = retrieveApplicableRules(command);
            log.debug("Retrieved {} validation rules", rules.size());

            // Step 3: Execute domain validation logic
            StackingResult domainResult = domainValidationService.validateStackingRules(context);
            log.debug("Domain validation completed: decision={}", domainResult.getDecision());

            // Step 4: Evaluate rules via rule engine
            FactPack factPack = factPackBuilder.buildFromCommand(command);
            StackableRuleEvaluator.EvaluationResult ruleEvaluation =
                    ruleEvaluator.evaluateStackingRules(factPack, rules);
            log.debug("Rule evaluation completed: passed={}", ruleEvaluation.isPassed());

            // Step 5: Check budget availability if requested
            if (command.shouldCheckBudget()) {
                boolean budgetAvailable = checkBudgetAvailability(context, domainResult);
                if (!budgetAvailable) {
                    log.warn("Insufficient budget: validationId={}", command.idempotencyKey());
                    return handleInsufficientBudget(command, startTime);
                }
            }

            // Step 6: Aggregate results
            ValidateStackableDiscountResult result = aggregateResults(
                    command,
                    domainResult,
                    ruleEvaluation,
                    startTime
            );

            // Step 7: Audit the validation
            auditValidation(command, result);

            // Step 8: Publish result event
            publishValidationResult(command, result);

            log.info("Validation completed: idempotencyKey={}, decision={}, processingTime={}ms",
                    command.idempotencyKey(), result.decision(), result.processingTimeMs());

            return result;

        } catch (Exception e) {
            log.error("Validation failed with exception: idempotencyKey={}, error={}",
                    command.idempotencyKey(), e.getMessage(), e);
            return handleValidationError(command, e, startTime);
        }
    }

    /**
     * Builds stacking context from validation command.
     */
    private StackingContext buildStackingContext(ValidateStackableDiscountCommand command) {
        log.debug("Building stacking context from command");

        FactPack factPack = factPackBuilder.buildFromCommand(command);

        // Build discount facts from requests
        List<DiscountFact> discountFacts = command.discountRequests().stream()
                .map(this::buildDiscountFact)
                .toList();

        return StackingContext.builder()
                .validationId(command.idempotencyKey())
                .correlationId(command.correlationId())
                .customer(factPack.customer())
                .order(factPack.order())
                .discounts(discountFacts)
                .checkBudget(command.validationOptions().checkBudgetAvailability())
                .optimizeOrder(command.validationOptions().optimizeOrder())
                .requestedAt(command.requestedAt())
                .build();
    }

    /**
     * Builds discount fact from discount request.
     */
    private DiscountFact buildDiscountFact(DiscountRequest request) {
        return DiscountFact.builder()
                .discountId(request.objectId())
                .type(request.objectType().name())
                .amount(request.expectedDiscount())
                .percentage(null) // Will be populated from discount metadata
                .status("ACTIVE") // Default status
                .build();
    }

    /**
     * Retrieves applicable validation rules for the command.
     */
    private List<Rule> retrieveApplicableRules(ValidateStackableDiscountCommand command) {
        try {
            String customerSegment = command.customerInfo().segment();
            return ruleService.getActiveRulesForStackableDiscount(customerSegment);
        } catch (Exception e) {
            log.warn("Failed to retrieve validation rules: {}", e.getMessage());
            return new ArrayList<>(); // Continue with empty rules on error
        }
    }

    /**
     * Checks budget availability for the validation.
     */
    private boolean checkBudgetAvailability(StackingContext context, StackingResult domainResult) {
        return domainValidationService.checkBudgetAvailability(
                context.getDiscounts(),
                domainResult.getTotalDiscount()
        );
    }

    /**
     * Aggregates domain and rule evaluation results.
     */
    private ValidateStackableDiscountResult aggregateResults(
            ValidateStackableDiscountCommand command,
            StackingResult domainResult,
            StackableRuleEvaluator.EvaluationResult ruleEvaluation,
            long startTime
    ) {
        long processingTime = System.currentTimeMillis() - startTime;

        // If rule evaluation failed, reject the validation
        if (!ruleEvaluation.isPassed()) {
            return buildRejectedResult(
                    command,
                    domainResult,
                    ruleEvaluation,
                    processingTime
            );
        }

        // If domain validation is approved or partial, use that result
        if (domainResult.isApproved() || domainResult.isPartial()) {
            return buildApprovedOrPartialResult(
                    command,
                    domainResult,
                    processingTime
            );
        }

        // Otherwise, validation is rejected
        return buildRejectedResult(
                command,
                domainResult,
                ruleEvaluation,
                processingTime
        );
    }

    /**
     * Builds approved or partial validation result.
     */
    private ValidateStackableDiscountResult buildApprovedOrPartialResult(
            ValidateStackableDiscountCommand command,
            StackingResult domainResult,
            long processingTime
    ) {
        List<ValidatedDiscount> validatedDiscounts = domainResult.getValidatedDiscounts().stream()
                .map(d -> ValidatedDiscount.of(
                        mapDiscountType(d.getDiscountType()),
                        d.getDiscountId(),
                        d.getDiscountAmount(),
                        d.getApplicationOrder()
                ))
                .toList();

        List<RejectedDiscount> rejectedDiscounts = domainResult.getRejectedDiscounts().stream()
                .map(d -> RejectedDiscount.of(
                        mapDiscountType(d.getDiscountType()),
                        d.getDiscountId(),
                        d.getRejectionReasons()
                ))
                .toList();

        List<ValidationIssue> issues = domainResult.getIssues().stream()
                .map(issue -> ValidationIssue.error("VALIDATION_FAILED", issue))
                .toList();

        ValidationDecision decision = domainResult.isApproved()
                ? ValidationDecision.APPROVED
                : ValidationDecision.PARTIAL;

        return new ValidateStackableDiscountResult(
                command.idempotencyKey(),
                decision,
                validatedDiscounts,
                rejectedDiscounts,
                domainResult.getTotalDiscount(),
                command.orderInfo().orderValue(),
                domainResult.getFinalAmount(),
                issues,
                buildExplanation(domainResult, decision),
                Instant.now(),
                processingTime
        );
    }

    /**
     * Builds rejected validation result.
     */
    private ValidateStackableDiscountResult buildRejectedResult(
            ValidateStackableDiscountCommand command,
            StackingResult domainResult,
            StackableRuleEvaluator.EvaluationResult ruleEvaluation,
            long processingTime
    ) {
        List<RejectedDiscount> rejectedDiscounts = command.discountRequests().stream()
                .map(d -> RejectedDiscount.of(
                        d.objectType(),
                        d.objectId(),
                        combineRejectionReasons(domainResult, ruleEvaluation)
                ))
                .toList();

        List<ValidationIssue> issues = new ArrayList<>();
        issues.addAll(domainResult.getIssues().stream()
                .map(issue -> ValidationIssue.error("VALIDATION_FAILED", issue))
                .toList());
        issues.addAll(ruleEvaluation.getFailureReasons().stream()
                .map(reason -> ValidationIssue.error("RULE_FAILED", reason))
                .toList());

        return new ValidateStackableDiscountResult(
                command.idempotencyKey(),
                ValidationDecision.REJECTED,
                new ArrayList<>(),
                rejectedDiscounts,
                BigDecimal.ZERO,
                command.orderInfo().orderValue(),
                command.orderInfo().orderValue(),
                issues,
                ValidationExplanation.rejected("Validation failed", issues),
                Instant.now(),
                processingTime
        );
    }

    /**
     * Combines rejection reasons from domain and rule evaluation.
     */
    private List<String> combineRejectionReasons(
            StackingResult domainResult,
            StackableRuleEvaluator.EvaluationResult ruleEvaluation
    ) {
        List<String> reasons = new ArrayList<>();
        reasons.addAll(domainResult.getIssues());
        reasons.addAll(ruleEvaluation.getFailureReasons());
        return reasons;
    }

    /**
     * Builds validation explanation.
     */
    private ValidationExplanation buildExplanation(
            StackingResult domainResult,
            ValidationDecision decision
    ) {
        if (decision == ValidationDecision.APPROVED) {
            return ValidationExplanation.approved(domainResult.getSummary());
        } else if (decision == ValidationDecision.PARTIAL) {
            return ValidationExplanation.partial(
                    domainResult.getSummary(),
                    domainResult.getValidatedCount(),
                    domainResult.getRejectedCount()
            );
        } else {
            return ValidationExplanation.rejected(
                    domainResult.getSummary(),
                    domainResult.getIssues().stream()
                            .map(issue -> ValidationIssue.error("VALIDATION_FAILED", issue))
                            .toList()
            );
        }
    }

    /**
     * Maps discount type string to enum.
     */
    private DiscountObjectType mapDiscountType(String type) {
        try {
            return DiscountObjectType.valueOf(type);
        } catch (Exception e) {
            return DiscountObjectType.DISCOUNT_CODE; // Default fallback
        }
    }

    /**
     * Handles invalid context scenario.
     */
    private ValidateStackableDiscountResult handleInvalidContext(
            ValidateStackableDiscountCommand command,
            StackingContext context,
            long startTime
    ) {
        long processingTime = System.currentTimeMillis() - startTime;
        return ValidateStackableDiscountResult.error(
                command.idempotencyKey(),
                "Invalid validation context",
                processingTime
        );
    }

    /**
     * Handles insufficient budget scenario.
     */
    private ValidateStackableDiscountResult handleInsufficientBudget(
            ValidateStackableDiscountCommand command,
            long startTime
    ) {
        long processingTime = System.currentTimeMillis() - startTime;
        return ValidateStackableDiscountResult.error(
                command.idempotencyKey(),
                "Insufficient budget for discounts",
                processingTime
        );
    }

    /**
     * Handles validation error scenario.
     */
    private ValidateStackableDiscountResult handleValidationError(
            ValidateStackableDiscountCommand command,
            Exception error,
            long startTime
    ) {
        long processingTime = System.currentTimeMillis() - startTime;
        return ValidateStackableDiscountResult.error(
                command.idempotencyKey(),
                "Validation error: " + error.getMessage(),
                processingTime
        );
    }

    /**
     * Audits the validation activity.
     */
    private void auditValidation(
            ValidateStackableDiscountCommand command,
            ValidateStackableDiscountResult result
    ) {
        try {
            auditService.auditValidation(command, result);
        } catch (Exception e) {
            log.warn("Failed to audit validation: {}", e.getMessage());
            // Don't fail validation if audit fails
        }
    }

    /**
     * Publishes validation result event.
     */
    private void publishValidationResult(
            ValidateStackableDiscountCommand command,
            ValidateStackableDiscountResult result
    ) {
        try {
            ValidateStackableDiscountResultEvent event =
                    eventBuilder.buildResultEvent(command, result);
            publishResultPort.publishValidationResult(event);
        } catch (Exception e) {
            log.error("Failed to publish validation result: {}", e.getMessage(), e);
            // Don't fail validation if publishing fails
        }
    }
}
