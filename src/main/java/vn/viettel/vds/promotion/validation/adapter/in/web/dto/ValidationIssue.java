package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Validation issue")
public record ValidationIssue(
    @Schema(description = "Path to the problematic field", example = "params.amount")
    @JsonProperty("path")
    String path,

    @Schema(description = "Issue description", example = "Amount must be a positive number")
    @JsonProperty("message")
    String message,

    @Schema(description = "Related operator name", example = "order.total.gte")
    @JsonProperty("operator")
    String operator
) {
    public ValidationIssue(String path, String message) {
        this(path, message, null);
    }
}