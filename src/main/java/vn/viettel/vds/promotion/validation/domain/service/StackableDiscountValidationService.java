package vn.viettel.vds.promotion.validation.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.domain.fact.CustomerFact;
import vn.viettel.vds.promotion.validation.domain.fact.DiscountFact;
import vn.viettel.vds.promotion.validation.domain.fact.OrderFact;
import vn.viettel.vds.promotion.validation.domain.model.StackingContext;
import vn.viettel.vds.promotion.validation.domain.model.StackingResult;
import vn.viettel.vds.promotion.validation.domain.model.StackingResult.RejectedDiscountInfo;
import vn.viettel.vds.promotion.validation.domain.model.StackingResult.ValidatedDiscountInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain service for stackable discount validation.
 * <p>
 * Contains pure business logic for validating discount stacking rules,
 * optimizing discount order, and calculating discount amounts.
 * This service has no infrastructure dependencies.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StackableDiscountValidationService {

    private static final BigDecimal MAX_DISCOUNT_PERCENTAGE = new BigDecimal("100");
    private static final BigDecimal MIN_FINAL_AMOUNT = BigDecimal.ZERO;
    private static final int DEFAULT_SCALE = 2;

    /**
     * Validates stacking rules for a set of discounts.
     * <p>
     * This method checks:
     * - Discount compatibility
     * - Stacking constraints
     * - Priority and ordering
     * - Business rules compliance
     * - Total discount limits
     * </p>
     *
     * @param context stacking context with all validation facts
     * @return stacking result with decision and details
     */
    public StackingResult validateStackingRules(StackingContext context) {
        long startTime = System.currentTimeMillis();

        log.debug("Starting stackable discount validation: validationId={}, discountCount={}",
                context.getValidationId(), context.getDiscountCount());

        // Validate context
        if (!context.isValid()) {
            log.warn("Invalid stacking context: validationId={}", context.getValidationId());
            return createContextErrorResult(context, startTime);
        }

        // Check request freshness
        if (!context.isRequestFresh(300)) { // 5 minutes
            log.warn("Stale validation request: validationId={}", context.getValidationId());
            return createStaleRequestResult(context, startTime);
        }

        List<ValidatedDiscountInfo> validatedDiscounts = new ArrayList<>();
        List<RejectedDiscountInfo> rejectedDiscounts = new ArrayList<>();
        List<String> issues = new ArrayList<>();

        // Optimize discount order if requested
        List<DiscountFact> discountsToValidate = context.isOptimizeOrder()
                ? optimizeDiscountOrder(context.getDiscounts(), context.getOrder())
                : context.getDiscounts();

        log.debug("Processing {} discounts for validation", discountsToValidate.size());

        BigDecimal runningTotal = context.getOrder().totalAmount();
        int order = 1;

        // Validate each discount
        for (DiscountFact discount : discountsToValidate) {
            log.debug("Validating discount: id={}, type={}",
                    discount.discountId(), discount.type());

            DiscountValidation validation = validateSingleDiscount(
                    discount,
                    context.getCustomer(),
                    context.getOrder(),
                    runningTotal
            );

            if (validation.isValid()) {
                BigDecimal discountAmount = calculateDiscountAmount(discount, runningTotal);

                validatedDiscounts.add(ValidatedDiscountInfo.builder()
                        .discountId(discount.discountId())
                        .discountType(discount.type())
                        .discountAmount(discountAmount)
                        .applicationOrder(order++)
                        .calculationMethod(getCalculationMethod(discount))
                        .build());

                runningTotal = runningTotal.subtract(discountAmount);
                log.debug("Discount approved: id={}, amount={}, runningTotal={}",
                        discount.discountId(), discountAmount, runningTotal);
            } else {
                rejectedDiscounts.add(RejectedDiscountInfo.builder()
                        .discountId(discount.discountId())
                        .discountType(discount.type())
                        .rejectionReasons(validation.getRejectionReasons())
                        .build());

                issues.addAll(validation.getRejectionReasons());
                log.debug("Discount rejected: id={}, reasons={}",
                        discount.discountId(), validation.getRejectionReasons());
            }
        }

        // Calculate totals
        BigDecimal totalDiscount = validatedDiscounts.stream()
                .map(ValidatedDiscountInfo::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal originalAmount = context.getOrder().totalAmount();
        long processingTime = System.currentTimeMillis() - startTime;

        // Validate total discount doesn't exceed order amount
        if (totalDiscount.compareTo(originalAmount) > 0) {
            issues.add("Total discount exceeds order amount");
            log.warn("Total discount exceeds order amount: discount={}, order={}",
                    totalDiscount, originalAmount);
        }

        // Determine result based on validation outcomes
        StackingResult result = determineResult(
                context.getValidationId(),
                validatedDiscounts,
                rejectedDiscounts,
                totalDiscount,
                originalAmount,
                issues,
                processingTime
        );

        log.info("Validation completed: validationId={}, decision={}, validated={}, rejected={}, totalDiscount={}",
                context.getValidationId(), result.getDecision(),
                validatedDiscounts.size(), rejectedDiscounts.size(), totalDiscount);

        return result;
    }

    /**
     * Optimizes discount application order for maximum benefit.
     * <p>
     * Strategy: Apply percentage-based discounts before fixed-amount discounts
     * to maximize total discount amount.
     * </p>
     *
     * @param discounts list of discounts
     * @param order     order information
     * @return optimized list of discounts
     */
    public List<DiscountFact> optimizeDiscountOrder(
            List<DiscountFact> discounts,
            OrderFact order
    ) {
        log.debug("Optimizing discount order for {} discounts", discounts.size());

        return discounts.stream()
                .sorted(Comparator
                        // First by type (percentage before fixed)
                        .comparing(this::getDiscountTypePriority)
                        // Then by amount (higher discounts first)
                        .thenComparing(d -> getDiscountValue(d, order), Comparator.reverseOrder())
                )
                .collect(Collectors.toList());
    }

    /**
     * Checks budget availability for discounts.
     *
     * @param discounts      list of discounts
     * @param requiredAmount required budget amount
     * @return true if budget is available
     */
    public boolean checkBudgetAvailability(
            List<DiscountFact> discounts,
            BigDecimal requiredAmount
    ) {
        log.debug("Checking budget availability for {} discounts, required amount: {}",
                discounts.size(), requiredAmount);

        // This is a simplified check
        // In real implementation, this would call budget management service
        // For now, we assume budget is available if required amount is positive
        boolean available = requiredAmount.compareTo(BigDecimal.ZERO) >= 0;

        log.debug("Budget availability: {}", available);
        return available;
    }

    /**
     * Validates a single discount against business rules.
     */
    private DiscountValidation validateSingleDiscount(
            DiscountFact discount,
            CustomerFact customer,
            OrderFact order,
            BigDecimal currentOrderAmount
    ) {
        List<String> rejectionReasons = new ArrayList<>();

        // Check discount is active
        if (!"ACTIVE".equalsIgnoreCase(discount.status())) {
            rejectionReasons.add("Discount is not active: " + discount.status());
        }

        // Check minimum order amount if applicable
        if (discount.metadata() != null && discount.metadata().containsKey("minOrderAmount")) {
            BigDecimal minAmount = new BigDecimal(discount.metadata().get("minOrderAmount").toString());
            if (order.totalAmount().compareTo(minAmount) < 0) {
                rejectionReasons.add("Order amount below minimum: " + minAmount);
            }
        }

        // Check customer eligibility if applicable
        if (discount.metadata() != null && discount.metadata().containsKey("eligibleSegments")) {
            @SuppressWarnings("unchecked")
            List<String> eligibleSegments = (List<String>) discount.metadata().get("eligibleSegments");
            String customerSegment = getCustomerSegment(customer);
            if (customerSegment == null || !eligibleSegments.contains(customerSegment)) {
                rejectionReasons.add("Customer segment not eligible: " + customerSegment);
            }
        }

        // Check current order amount is positive
        if (currentOrderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            rejectionReasons.add("Insufficient order amount after previous discounts");
        }

        return new DiscountValidation(rejectionReasons.isEmpty(), rejectionReasons);
    }

    /**
     * Calculates discount amount for a given discount.
     */
    private BigDecimal calculateDiscountAmount(DiscountFact discount, BigDecimal currentAmount) {
        BigDecimal amount;

        if (discount.percentage() != null && discount.percentage().compareTo(BigDecimal.ZERO) > 0) {
            // Percentage-based discount
            amount = currentAmount
                    .multiply(discount.percentage())
                    .divide(new BigDecimal("100"), DEFAULT_SCALE, RoundingMode.HALF_UP);
        } else if (discount.amount() != null && discount.amount().compareTo(BigDecimal.ZERO) > 0) {
            // Fixed amount discount
            amount = discount.amount();
        } else {
            amount = BigDecimal.ZERO;
        }

        // Ensure discount doesn't exceed current amount
        if (amount.compareTo(currentAmount) > 0) {
            amount = currentAmount;
        }

        return amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Gets calculation method description.
     */
    private String getCalculationMethod(DiscountFact discount) {
        if (discount.percentage() != null && discount.percentage().compareTo(BigDecimal.ZERO) > 0) {
            return "PERCENTAGE:" + discount.percentage() + "%";
        } else if (discount.amount() != null) {
            return "FIXED:" + discount.amount();
        }
        return "UNKNOWN";
    }

    /**
     * Gets discount type priority for sorting (lower = higher priority).
     */
    private int getDiscountTypePriority(DiscountFact discount) {
        // Percentage discounts first (priority 1), then fixed amounts (priority 2)
        if (discount.percentage() != null && discount.percentage().compareTo(BigDecimal.ZERO) > 0) {
            return 1;
        }
        return 2;
    }

    /**
     * Gets discount value for comparison.
     */
    private BigDecimal getDiscountValue(DiscountFact discount, OrderFact order) {
        if (discount.percentage() != null) {
            return discount.percentage();
        } else if (discount.amount() != null) {
            return discount.amount();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Determines final result based on validation outcomes.
     */
    private StackingResult determineResult(
            String validationId,
            List<ValidatedDiscountInfo> validated,
            List<RejectedDiscountInfo> rejected,
            BigDecimal totalDiscount,
            BigDecimal originalAmount,
            List<String> issues,
            long processingTime
    ) {
        if (rejected.isEmpty() && issues.isEmpty()) {
            return StackingResult.success(validationId, validated, totalDiscount, originalAmount, processingTime);
        } else if (validated.isEmpty()) {
            return StackingResult.failure(validationId, rejected, issues, processingTime);
        } else {
            return StackingResult.partial(validationId, validated, rejected, totalDiscount, originalAmount, issues, processingTime);
        }
    }

    /**
     * Creates error result for invalid context.
     */
    private StackingResult createContextErrorResult(StackingContext context, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        return StackingResult.failure(
                context.getValidationId(),
                List.of(),
                List.of("Invalid validation context"),
                processingTime
        );
    }

    /**
     * Creates error result for stale request.
     */
    private StackingResult createStaleRequestResult(StackingContext context, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        return StackingResult.failure(
                context.getValidationId(),
                List.of(),
                List.of("Validation request expired"),
                processingTime
        );
    }

    /**
     * Gets customer segment from customer fact.
     */
    private String getCustomerSegment(CustomerFact customer) {
        if (customer == null) {
            return null;
        }
        if (customer.tier() != null) {
            return customer.tier();
        }
        if (customer.attributes() != null && customer.attributes().containsKey("segment")) {
            return customer.attributes().get("segment").toString();
        }
        return null;
    }

    /**
     * Internal validation result.
     */
    private static class DiscountValidation {
        private final boolean valid;
        private final List<String> rejectionReasons;

        public DiscountValidation(boolean valid, List<String> rejectionReasons) {
            this.valid = valid;
            this.rejectionReasons = rejectionReasons;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getRejectionReasons() {
            return rejectionReasons;
        }
    }
}
