package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Domain model representing the result of stackable discount validation.
 * <p>
 * This is a rich domain model that contains the validation decision,
 * calculated discount amounts, and detailed explanations.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Getter
@Builder
public class StackingResult {

    private final String validationId;
    private final StackingDecision decision;
    private final List<ValidatedDiscountInfo> validatedDiscounts;
    private final List<RejectedDiscountInfo> rejectedDiscounts;
    private final BigDecimal totalDiscount;
    private final BigDecimal originalAmount;
    private final BigDecimal finalAmount;
    private final List<String> issues;
    private final List<String> warnings;
    private final String summary;
    private final Instant validatedAt;
    private final long processingTimeMs;

    /**
     * Creates a successful stacking result.
     *
     * @param validationId       validation identifier
     * @param validatedDiscounts list of validated discounts
     * @param totalDiscount      total discount amount
     * @param originalAmount     original order amount
     * @param processingTime     processing time in ms
     * @return successful stacking result
     */
    public static StackingResult success(
            String validationId,
            List<ValidatedDiscountInfo> validatedDiscounts,
            BigDecimal totalDiscount,
            BigDecimal originalAmount,
            long processingTime
    ) {
        return StackingResult.builder()
                .validationId(validationId)
                .decision(StackingDecision.APPROVED)
                .validatedDiscounts(validatedDiscounts)
                .rejectedDiscounts(Collections.emptyList())
                .totalDiscount(totalDiscount)
                .originalAmount(originalAmount)
                .finalAmount(originalAmount.subtract(totalDiscount))
                .issues(Collections.emptyList())
                .warnings(Collections.emptyList())
                .summary("All discounts validated successfully")
                .validatedAt(Instant.now())
                .processingTimeMs(processingTime)
                .build();
    }

    /**
     * Creates a failed stacking result.
     *
     * @param validationId      validation identifier
     * @param rejectedDiscounts list of rejected discounts
     * @param issues            validation issues
     * @param processingTime    processing time in ms
     * @return failed stacking result
     */
    public static StackingResult failure(
            String validationId,
            List<RejectedDiscountInfo> rejectedDiscounts,
            List<String> issues,
            long processingTime
    ) {
        return StackingResult.builder()
                .validationId(validationId)
                .decision(StackingDecision.REJECTED)
                .validatedDiscounts(Collections.emptyList())
                .rejectedDiscounts(rejectedDiscounts)
                .totalDiscount(BigDecimal.ZERO)
                .originalAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .issues(issues)
                .warnings(Collections.emptyList())
                .summary("Validation failed: " + issues.size() + " issue(s) found")
                .validatedAt(Instant.now())
                .processingTimeMs(processingTime)
                .build();
    }

    /**
     * Creates a partial stacking result.
     *
     * @param validationId       validation identifier
     * @param validatedDiscounts list of validated discounts
     * @param rejectedDiscounts  list of rejected discounts
     * @param totalDiscount      total discount from validated discounts
     * @param originalAmount     original order amount
     * @param issues             validation issues
     * @param processingTime     processing time in ms
     * @return partial stacking result
     */
    public static StackingResult partial(
            String validationId,
            List<ValidatedDiscountInfo> validatedDiscounts,
            List<RejectedDiscountInfo> rejectedDiscounts,
            BigDecimal totalDiscount,
            BigDecimal originalAmount,
            List<String> issues,
            long processingTime
    ) {
        return StackingResult.builder()
                .validationId(validationId)
                .decision(StackingDecision.PARTIAL)
                .validatedDiscounts(validatedDiscounts)
                .rejectedDiscounts(rejectedDiscounts)
                .totalDiscount(totalDiscount)
                .originalAmount(originalAmount)
                .finalAmount(originalAmount.subtract(totalDiscount))
                .issues(issues)
                .warnings(Collections.emptyList())
                .summary(String.format("Partial validation: %d approved, %d rejected",
                        validatedDiscounts.size(), rejectedDiscounts.size()))
                .validatedAt(Instant.now())
                .processingTimeMs(processingTime)
                .build();
    }

    /**
     * Checks if stacking was approved.
     *
     * @return true if approved
     */
    public boolean isApproved() {
        return decision == StackingDecision.APPROVED;
    }

    /**
     * Checks if stacking was rejected.
     *
     * @return true if rejected
     */
    public boolean isRejected() {
        return decision == StackingDecision.REJECTED;
    }

    /**
     * Checks if stacking was partial.
     *
     * @return true if partial
     */
    public boolean isPartial() {
        return decision == StackingDecision.PARTIAL;
    }

    /**
     * Gets the count of validated discounts.
     *
     * @return validated count
     */
    public int getValidatedCount() {
        return validatedDiscounts != null ? validatedDiscounts.size() : 0;
    }

    /**
     * Gets the count of rejected discounts.
     *
     * @return rejected count
     */
    public int getRejectedCount() {
        return rejectedDiscounts != null ? rejectedDiscounts.size() : 0;
    }

    /**
     * Gets all discount IDs that were validated successfully.
     *
     * @return list of validated discount IDs
     */
    public List<String> getValidatedDiscountIds() {
        if (validatedDiscounts == null) {
            return Collections.emptyList();
        }
        return validatedDiscounts.stream()
                .map(ValidatedDiscountInfo::getDiscountId)
                .toList();
    }

    /**
     * Stacking decision enum.
     */
    public enum StackingDecision {
        APPROVED,
        REJECTED,
        PARTIAL
    }

    /**
     * Information about a validated discount.
     */
    @Getter
    @Builder
    public static class ValidatedDiscountInfo {
        private final String discountId;
        private final String discountType;
        private final BigDecimal discountAmount;
        private final int applicationOrder;
        private final String calculationMethod;
    }

    /**
     * Information about a rejected discount.
     */
    @Getter
    @Builder
    public static class RejectedDiscountInfo {
        private final String discountId;
        private final String discountType;
        private final List<String> rejectionReasons;
    }
}
