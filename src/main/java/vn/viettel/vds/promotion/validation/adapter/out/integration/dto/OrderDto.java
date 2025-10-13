package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Order data for rule execution")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderDto(
        @Schema(description = "Order identifier", example = "order123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Order ID is required")
        @JsonProperty("id")
        String id,

        @Schema(description = "Order total amount", example = "500000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Total is required")
        @JsonProperty("total")
        BigDecimal total,

        @Schema(description = "Currency code", example = "VND", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Currency is required")
        @JsonProperty("currency")
        String currency,

        @Schema(description = "Order items")
        @Valid
        @JsonProperty("items")
        List<OrderItemDto> items,

        @Schema(description = "Order metadata")
        @JsonProperty("metadata")
        Map<String, Object> metadata
) {
}
