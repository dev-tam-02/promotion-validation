package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain model representing constraints for discount stacking.
 * <p>
 * Defines business rules and limits that govern how discounts can be
 * combined together.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Getter
@Builder
public class StackingConstraint {

    /**
     * Maximum number of discounts that can be stacked
     */
    private final Integer maxStackSize;

    /**
     * Maximum total discount percentage (e.g., 100%)
     */
    private final BigDecimal maxTotalDiscountPercentage;

    /**
     * Maximum total discount amount
     */
    private final BigDecimal maxTotalDiscountAmount;

    /**
     * Minimum order amount required for stacking
     */
    private final BigDecimal minOrderAmount;

    /**
     * Discount types that cannot be combined
     */
    private final List<IncompatiblePair> incompatibleTypes;

    /**
     * Discount types that must be applied first
     */
    private final List<String> priorityTypes;

    /**
     * Whether percentage discounts must be applied before fixed amounts
     */
    private final boolean percentageBeforeFixed;

    /**
     * Maximum discount per customer per day
     */
    private final BigDecimal maxDiscountPerCustomerPerDay;

    /**
     * Eligible customer segments
     */
    private final List<String> eligibleSegments;

    /**
     * Eligible customer tiers
     */
    private final List<String> eligibleTiers;

    /**
     * Creates default stacking constraints.
     *
     * @return default constraints
     */
    public static StackingConstraint defaults() {
        return StackingConstraint.builder()
                .maxStackSize(5)
                .maxTotalDiscountPercentage(new BigDecimal("100"))
                .percentageBeforeFixed(true)
                .build();
    }

    /**
     * Creates lenient stacking constraints (fewer restrictions).
     *
     * @return lenient constraints
     */
    public static StackingConstraint lenient() {
        return StackingConstraint.builder()
                .maxStackSize(10)
                .maxTotalDiscountPercentage(new BigDecimal("100"))
                .percentageBeforeFixed(false)
                .build();
    }

    /**
     * Creates strict stacking constraints (more restrictions).
     *
     * @return strict constraints
     */
    public static StackingConstraint strict() {
        return StackingConstraint.builder()
                .maxStackSize(3)
                .maxTotalDiscountPercentage(new BigDecimal("80"))
                .percentageBeforeFixed(true)
                .minOrderAmount(new BigDecimal("100000")) // 100k VND
                .build();
    }

    /**
     * Represents a pair of incompatible discount types.
     */
    @Getter
    @Builder
    public static class IncompatiblePair {
        private final String type1;
        private final String type2;
        private final String reason;

        /**
         * Creates an incompatible pair.
         *
         * @param type1  first discount type
         * @param type2  second discount type
         * @param reason reason for incompatibility
         * @return incompatible pair
         */
        public static IncompatiblePair of(String type1, String type2, String reason) {
            return IncompatiblePair.builder()
                    .type1(type1)
                    .type2(type2)
                    .reason(reason)
                    .build();
        }

        /**
         * Checks if this pair matches the given discount types.
         *
         * @param discountType1 first type to check
         * @param discountType2 second type to check
         * @return true if types match this incompatible pair
         */
        public boolean matches(String discountType1, String discountType2) {
            return (type1.equals(discountType1) && type2.equals(discountType2)) ||
                    (type1.equals(discountType2) && type2.equals(discountType1));
        }
    }
}
