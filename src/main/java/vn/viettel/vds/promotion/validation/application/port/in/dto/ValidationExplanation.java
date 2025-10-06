package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed explanation of validation decision.
 *
 * @param summary brief summary of the validation result
 * @param details detailed explanation steps
 * @param recommendations recommendations for improving validation
 *
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationExplanation(
        String summary,
        List<String> details,
        List<String> recommendations
) {

    /**
     * Creates an approved explanation.
     *
     * @param summary summary message
     * @return validation explanation
     */
    public static ValidationExplanation approved(String summary) {
        return new ValidationExplanation(
                summary,
                List.of("All validation rules passed", "Budget availability confirmed", "Stacking rules satisfied"),
                null
        );
    }

    /**
     * Creates a rejected explanation.
     *
     * @param summary summary message
     * @param issues validation issues that caused rejection
     * @return validation explanation
     */
    public static ValidationExplanation rejected(String summary, List<ValidationIssue> issues) {
        List<String> details = new ArrayList<>();
        details.add("Validation failed due to the following issues:");
        issues.forEach(issue -> details.add("- " + issue.message()));

        return new ValidationExplanation(summary, details, null);
    }

    /**
     * Creates a partial explanation.
     *
     * @param summary summary message
     * @param approvedCount number of approved discounts
     * @param rejectedCount number of rejected discounts
     * @return validation explanation
     */
    public static ValidationExplanation partial(
            String summary,
            int approvedCount,
            int rejectedCount
    ) {
        List<String> details = List.of(
                "Partial validation completed",
                String.format("%d discount(s) approved", approvedCount),
                String.format("%d discount(s) rejected", rejectedCount)
        );

        return new ValidationExplanation(summary, details, null);
    }

    /**
     * Creates an error explanation.
     *
     * @param errorMessage error message
     * @return validation explanation
     */
    public static ValidationExplanation error(String errorMessage) {
        return new ValidationExplanation(
                "Validation failed due to system error",
                List.of(errorMessage),
                List.of("Please retry the validation", "Contact support if the problem persists")
        );
    }
}
