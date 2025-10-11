package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for pre-flight validation rule compatibility check
 * Used by campaign service to validate rule before starting saga
 */
public record ValidateCompatibilityRequest(
        @NotBlank(message = "Rule ID is required")
        String ruleId,

        @NotBlank(message = "Campaign type is required")
        String campaignType
) {
}
