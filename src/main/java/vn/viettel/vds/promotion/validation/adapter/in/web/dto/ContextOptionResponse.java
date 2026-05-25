package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Context option for validation rules")
public record ContextOptionResponse(
        @Schema(description = "Context value", example = "GENERAL_USAGE")
        @JsonProperty("value")
        String value,

        @Schema(description = "English display label", example = "General usage")
        @JsonProperty("label")
        String label,

        @Schema(description = "Vietnamese display label", example = "Chung")
        @JsonProperty("labelVi")
        String labelVi
) {
}
