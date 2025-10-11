package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

/**
 * Options controlling validation behavior.
 *
 * @param checkBudgetAvailability whether to check budget availability
 * @param optimizeOrder           whether to optimize discount stacking order
 * @param explainLevel            level of explanation detail
 * @param includeAlternatives     whether to include alternative stacking options
 * @param dryRun                  whether this is a dry run validation
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationOptions(
        boolean checkBudgetAvailability,
        boolean optimizeOrder,

        @NotNull
        ExplainLevel explainLevel,

        boolean includeAlternatives,
        boolean dryRun
) {

    /**
     * Creates default validation options.
     * <p>
     * Defaults:
     * - Budget check: enabled
     * - Optimize order: enabled
     * - Explain level: BASIC
     * - Include alternatives: disabled
     * - Dry run: enabled
     * </p>
     *
     * @return default validation options
     */
    public static ValidationOptions defaults() {
        return new ValidationOptions(
                true,  // checkBudgetAvailability
                true,  // optimizeOrder
                ExplainLevel.BASIC,
                false, // includeAlternatives
                true   // dryRun
        );
    }

    /**
     * Creates minimal validation options (fast validation).
     *
     * @return minimal validation options
     */
    public static ValidationOptions minimal() {
        return new ValidationOptions(
                false, // checkBudgetAvailability
                false, // optimizeOrder
                ExplainLevel.NONE,
                false, // includeAlternatives
                true   // dryRun
        );
    }

    /**
     * Creates comprehensive validation options.
     *
     * @return comprehensive validation options
     */
    public static ValidationOptions comprehensive() {
        return new ValidationOptions(
                true,  // checkBudgetAvailability
                true,  // optimizeOrder
                ExplainLevel.FULL,
                true,  // includeAlternatives
                true   // dryRun
        );
    }

    /**
     * Level of explanation detail in validation results.
     */
    public enum ExplainLevel {
        /**
         * No explanation
         */
        NONE,

        /**
         * Basic explanation (summary only)
         */
        BASIC,

        /**
         * Full explanation (detailed breakdown)
         */
        FULL
    }
}
