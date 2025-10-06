package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response from parameter validation")
public record ValidationResponse(
        @Schema(description = "Whether validation passed", example = "true")
        @JsonProperty("ok")
        boolean ok,

        @Schema(description = "List of validation issues")
        @JsonProperty("issues")
        List<ValidationIssue> issues
) {
}