package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Result of stackable discount validation.
 * <p>
 * Contains the validation decision, validated discounts with their calculated amounts,
 * any validation issues found, and explanations for the decision.
 * </p>
 *
 * @param validationId unique identifier for this validation
 * @param decision validation decision (APPROVED, REJECTED, PARTIAL)
 * @param validatedDiscounts list of discounts that passed validation
 * @param rejectedDiscounts list of discounts that failed validation
 * @param totalDiscountAmount total discount amount from all valid discounts
 * @param originalOrderAmount original order amount before discounts
 * @param finalOrderAmount final order amount after discounts applied
 * @param issues list of validation issues found
 * @param explanation detailed explanation of validation decision
 * @param validatedAt timestamp when validation completed
 * @param processingTimeMs time taken to process validation in milliseconds
 *
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidateStackableDiscountResult(
        String validationId,
        ValidationDecision decision,
        List<ValidatedDiscount> validatedDiscounts,
        List<RejectedDiscount> rejectedDiscounts,
        BigDecimal totalDiscountAmount,
        BigDecimal originalOrderAmount,
        BigDecimal finalOrderAmount,
        List<ValidationIssue> issues,
        ValidationExplanation explanation,
        Instant validatedAt,
        long processingTimeMs
) {

    /**
     * Creates a successful validation result.
     *
     * @param validationId validation identifier
     * @param validatedDiscounts list of validated discounts
     * @param totalDiscount total discount amount
     * @param originalAmount original order amount
     * @param processingTime processing time in ms
     * @return approved validation result
     */
    public static ValidateStackableDiscountResult approved(
            String validationId,
            List<ValidatedDiscount> validatedDiscounts,
            BigDecimal totalDiscount,
            BigDecimal originalAmount,
            long processingTime
    ) {
        BigDecimal finalAmount = originalAmount.subtract(totalDiscount);

        return new ValidateStackableDiscountResult(
                validationId,
                ValidationDecision.APPROVED,
                validatedDiscounts,
                Collections.emptyList(),
                totalDiscount,
                originalAmount,
                finalAmount,
                Collections.emptyList(),
                ValidationExplanation.approved("All discounts validated successfully"),
                Instant.now(),
                processingTime
        );
    }

    /**
     * Creates a rejected validation result.
     *
     * @param validationId validation identifier
     * @param rejectedDiscounts list of rejected discounts
     * @param issues validation issues
     * @param processingTime processing time in ms
     * @return rejected validation result
     */
    public static ValidateStackableDiscountResult rejected(
            String validationId,
            List<RejectedDiscount> rejectedDiscounts,
            List<ValidationIssue> issues,
            long processingTime
    ) {
        return new ValidateStackableDiscountResult(
                validationId,
                ValidationDecision.REJECTED,
                Collections.emptyList(),
                rejectedDiscounts,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                issues,
                ValidationExplanation.rejected("Validation failed", issues),
                Instant.now(),
                processingTime
        );
    }

    /**
     * Creates a partial validation result (some discounts approved, some rejected).
     *
     * @param validationId validation identifier
     * @param validatedDiscounts list of validated discounts
     * @param rejectedDiscounts list of rejected discounts
     * @param totalDiscount total discount from validated discounts
     * @param originalAmount original order amount
     * @param issues validation issues
     * @param processingTime processing time in ms
     * @return partial validation result
     */
    public static ValidateStackableDiscountResult partial(
            String validationId,
            List<ValidatedDiscount> validatedDiscounts,
            List<RejectedDiscount> rejectedDiscounts,
            BigDecimal totalDiscount,
            BigDecimal originalAmount,
            List<ValidationIssue> issues,
            long processingTime
    ) {
        BigDecimal finalAmount = originalAmount.subtract(totalDiscount);

        return new ValidateStackableDiscountResult(
                validationId,
                ValidationDecision.PARTIAL,
                validatedDiscounts,
                rejectedDiscounts,
                totalDiscount,
                originalAmount,
                finalAmount,
                issues,
                ValidationExplanation.partial("Some discounts validated", validatedDiscounts.size(), rejectedDiscounts.size()),
                Instant.now(),
                processingTime
        );
    }

    /**
     * Creates an error validation result for system failures.
     *
     * @param validationId validation identifier
     * @param errorMessage error message
     * @param processingTime processing time in ms
     * @return error validation result
     */
    public static ValidateStackableDiscountResult error(
            String validationId,
            String errorMessage,
            long processingTime
    ) {
        ValidationIssue issue = new ValidationIssue(
                "SYSTEM_ERROR",
                "Validation Error",
                errorMessage,
                ValidationIssue.Severity.ERROR
        );

        return new ValidateStackableDiscountResult(
                validationId,
                ValidationDecision.ERROR,
                Collections.emptyList(),
                Collections.emptyList(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(issue),
                ValidationExplanation.error(errorMessage),
                Instant.now(),
                processingTime
        );
    }

    /**
     * Checks if validation was successful.
     *
     * @return true if decision is APPROVED
     */
    public boolean isApproved() {
        return decision == ValidationDecision.APPROVED;
    }

    /**
     * Checks if validation was rejected.
     *
     * @return true if decision is REJECTED
     */
    public boolean isRejected() {
        return decision == ValidationDecision.REJECTED;
    }

    /**
     * Checks if validation was partial.
     *
     * @return true if decision is PARTIAL
     */
    public boolean isPartial() {
        return decision == ValidationDecision.PARTIAL;
    }

    /**
     * Checks if validation encountered an error.
     *
     * @return true if decision is ERROR
     */
    public boolean isError() {
        return decision == ValidationDecision.ERROR;
    }

    /**
     * Returns the number of validated discounts.
     *
     * @return count of validated discounts
     */
    public int getValidatedCount() {
        return validatedDiscounts != null ? validatedDiscounts.size() : 0;
    }

    /**
     * Returns the number of rejected discounts.
     *
     * @return count of rejected discounts
     */
    public int getRejectedCount() {
        return rejectedDiscounts != null ? rejectedDiscounts.size() : 0;
    }
}
