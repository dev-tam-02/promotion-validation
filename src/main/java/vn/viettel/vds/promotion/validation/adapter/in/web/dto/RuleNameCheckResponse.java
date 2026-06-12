package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for the rule-name duplicate check endpoint.
 */
@Schema(description = "Result of a rule-name duplicate check")
public record RuleNameCheckResponse(

        @Schema(description = "True if a rule with the exact name already exists", example = "true")
        boolean duplicated
) {
}
