package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Context option for validation rules")
public record ContextOptionResponse(
        @Schema(description = "Context value", example = "ORDER")
        @JsonProperty("value")
        String value,

        @Schema(description = "Display label", example = "Đơn hàng")
        @JsonProperty("label")
        String label
) {
}
