package vn.viettel.vds.promotion.validation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Domain model representing a binding between a validation rule and a target entity.
 * <p>
 * This is a unified model that replaces multiple legacy models:
 * - Assignment
 * - TemporalPolicy
 * - RuleTemporalLink
 * - ApplicabilityRule
 * <p>
 * A RuleBinding defines:
 * - WHAT rule to apply (ruleId)
 * - WHERE to apply it (targetType + targetId)
 * - WHEN it's valid (validFrom, validTo, rrule, timeWindows, excludedDates)
 * - WHICH products it applies to (includedAll, includedProducts, excludedProducts, etc.)
 * - HOW to control traffic (trafficPercent, stickyKeyStrategy)
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RuleBinding {

    // ========== Identity ==========
    private String id;

    // ========== Rule Reference ==========
    /**
     * The validation rule to apply
     */
    private String ruleId;

    /**
     * Pin to specific rule version. NULL means always use the latest version.
     */
    private Integer ruleVersionPinned;

    // ========== Target ==========
    /**
     * Target type: CAMPAIGN, DISCOUNT, VOUCHER, CASHBACK, etc.
     */
    private String targetType;

    /**
     * Target identifier (e.g., campaign ID, discount ID)
     */
    private String targetId;

    // ========== Priority & State ==========
    /**
     * Binding priority. Higher priority bindings are evaluated first.
     */
    @Builder.Default
    private Integer priority = 0;

    /**
     * Whether this binding is active
     */
    @Builder.Default
    private Boolean active = true;

    // ========== Time Constraints ==========
    /**
     * Start of validity period (inclusive)
     */
    private Instant validFrom;

    /**
     * End of validity period (inclusive)
     */
    private Instant validTo;

    /**
     * Timezone for time evaluations
     */
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    /**
     * RFC 5545 recurrence rule.
     * Example: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
     */
    private String rrule;

    /**
     * Time windows within each day when the rule applies.
     * Example: [{"start": "09:00", "end": "17:00"}]
     */
    private List<TimeWindow> timeWindows;

    /**
     * Dates when the rule does NOT apply (holidays, etc.)
     * Format: ISO date strings ["2024-01-01", "2024-12-25"]
     */
    private List<String> excludedDates;

    // ========== Product Scope ==========
    /**
     * If true, applies to all products (only check exclusions).
     * If false, must be in included lists to apply.
     */
    @Builder.Default
    private Boolean includedAll = false;

    private List<String> includedProducts;
    private List<String> excludedProducts;
    private List<String> includedCategories;
    private List<String> excludedCategories;
    private List<String> includedBrands;
    private List<String> excludedBrands;

    // ========== Traffic Control ==========
    /**
     * Percentage of traffic this binding applies to (0-100).
     * Used for A/B testing and gradual rollouts.
     */
    @Builder.Default
    private Integer trafficPercent = 100;

    /**
     * Strategy for consistent traffic assignment.
     * Ensures the same customer/order always gets the same decision.
     */
    private StickyKeyStrategy stickyKeyStrategy;

    // ========== Compiled ==========
    /**
     * Hash of the compiled DRL bundle for this binding.
     * Used for caching and invalidation.
     */
    private String bundleHash;

    // ========== Audit ==========
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;

    // ========== Embedded Types ==========

    /**
     * Time window within a day
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeWindow {
        /**
         * Start time in HH:mm format (e.g., "09:00")
         */
        private String start;

        /**
         * End time in HH:mm format (e.g., "17:00")
         */
        private String end;
    }

    /**
     * Strategy for sticky key in traffic control
     */
    public enum StickyKeyStrategy {
        /**
         * Same customer always gets the same decision
         */
        CUSTOMER_ID,

        /**
         * Same order always gets the same decision
         */
        ORDER_ID,

        /**
         * Same device always gets the same decision
         */
        DEVICE_ID
    }

    /**
     * Supported target types
     */
    public enum TargetType {
        CAMPAIGN,
        DISCOUNT,
        VOUCHER,
        CASHBACK,
        PROMOTION,
        REWARD
    }

    // ========== Business Methods ==========

    /**
     * Check if this binding is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

    /**
     * Check if this binding is effective at the given timestamp.
     * Evaluates: active status, validity period, and time windows.
     *
     * @param timestamp The time to check
     * @return true if the binding is effective at the given time
     */
    public boolean isEffectiveAt(Instant timestamp) {
        // Must be active
        if (!isActive()) {
            return false;
        }

        // Check validity period
        if (validFrom != null && timestamp.isBefore(validFrom)) {
            return false;
        }
        if (validTo != null && timestamp.isAfter(validTo)) {
            return false;
        }

        // Check excluded dates
        if (excludedDates != null && !excludedDates.isEmpty()) {
            ZonedDateTime zonedTime = timestamp.atZone(ZoneId.of(timezone != null ? timezone : "Asia/Ho_Chi_Minh"));
            String dateStr = zonedTime.toLocalDate().toString();
            if (excludedDates.contains(dateStr)) {
                return false;
            }
        }

        // Check time windows (if any)
        if (timeWindows != null && !timeWindows.isEmpty()) {
            return isWithinTimeWindows(timestamp);
        }

        return true;
    }

    /**
     * Check if the binding is effective now
     */
    public boolean isEffective() {
        return isEffectiveAt(Instant.now());
    }

    /**
     * Check if current time falls within any of the time windows
     */
    private boolean isWithinTimeWindows(Instant timestamp) {
        ZonedDateTime zonedTime = timestamp.atZone(ZoneId.of(timezone != null ? timezone : "Asia/Ho_Chi_Minh"));
        int currentMinutes = zonedTime.getHour() * 60 + zonedTime.getMinute();

        for (TimeWindow window : timeWindows) {
            int startMinutes = parseTimeToMinutes(window.getStart());
            int endMinutes = parseTimeToMinutes(window.getEnd());

            if (currentMinutes >= startMinutes && currentMinutes <= endMinutes) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse HH:mm time string to minutes since midnight
     */
    private int parseTimeToMinutes(String time) {
        if (time == null || !time.contains(":")) {
            return 0;
        }
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    /**
     * Check if this binding applies to a specific product.
     *
     * @param productId  Product identifier
     * @param categoryId Category identifier (can be null)
     * @param brandId    Brand identifier (can be null)
     * @return true if the binding applies to this product
     */
    public boolean appliesToProduct(String productId, String categoryId, String brandId) {
        // If includedAll is true, apply to all products (but check exclusions)
        if (Boolean.TRUE.equals(includedAll)) {
            // Check exclusions
            if (excludedProducts != null && excludedProducts.contains(productId)) {
                return false;
            }
            if (categoryId != null && excludedCategories != null && excludedCategories.contains(categoryId)) {
                return false;
            }
            if (brandId != null && excludedBrands != null && excludedBrands.contains(brandId)) {
                return false;
            }
            return true;
        }

        // If not includedAll, must be explicitly included
        if (includedProducts != null && includedProducts.contains(productId)) {
            // But still check exclusions
            if (excludedProducts != null && excludedProducts.contains(productId)) {
                return false;
            }
            return true;
        }

        if (categoryId != null && includedCategories != null && includedCategories.contains(categoryId)) {
            if (excludedCategories != null && excludedCategories.contains(categoryId)) {
                return false;
            }
            if (excludedProducts != null && excludedProducts.contains(productId)) {
                return false;
            }
            return true;
        }

        if (brandId != null && includedBrands != null && includedBrands.contains(brandId)) {
            if (excludedBrands != null && excludedBrands.contains(brandId)) {
                return false;
            }
            if (excludedProducts != null && excludedProducts.contains(productId)) {
                return false;
            }
            return true;
        }

        // No inclusion match found
        return false;
    }

    /**
     * Check if this binding should apply to a specific traffic key based on traffic percentage.
     * Uses consistent hashing to ensure the same key always gets the same result.
     *
     * @param trafficKey The key to check (customer ID, order ID, etc.)
     * @return true if this binding should apply to the given traffic key
     */
    public boolean shouldApplyToTraffic(String trafficKey) {
        if (trafficPercent == null || trafficPercent >= 100) {
            return true;
        }
        if (trafficPercent <= 0) {
            return false;
        }

        // Use consistent hashing: hash % 100 < trafficPercent
        int hash = Math.abs(trafficKey.hashCode() % 100);
        return hash < trafficPercent;
    }

    /**
     * Check if this binding has temporal constraints defined.
     * Returns true if any of: validFrom, validTo, rrule, or timeWindows are set.
     *
     * @return true if temporal constraints exist
     */
    public boolean hasTemporalConstraints() {
        return validFrom != null ||
                validTo != null ||
                (rrule != null && !rrule.isEmpty()) ||
                (timeWindows != null && !timeWindows.isEmpty());
    }
}
