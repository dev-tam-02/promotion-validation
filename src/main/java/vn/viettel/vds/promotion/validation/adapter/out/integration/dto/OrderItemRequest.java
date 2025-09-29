package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Order item data")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderItemRequest(
    @Schema(description = "Product identifier", example = "prod123", required = true)
    @NotBlank(message = "Product ID is required")
    @JsonProperty("productId")
    String productId,

    @Schema(description = "Item quantity", example = "2", required = true)
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @JsonProperty("quantity")
    Integer quantity,

    @Schema(description = "Item price", example = "250000", required = true)
    @NotNull(message = "Price is required")
    @JsonProperty("price")
    BigDecimal price,

    @Schema(description = "Product category", example = "ELECTRONICS")
    @JsonProperty("category")
    String category,

    @Schema(description = "Product brand", example = "SAMSUNG")
    @JsonProperty("brand")
    String brand,

    @Schema(description = "Item metadata")
    @JsonProperty("metadata")
    Map<String, Object> metadata
) {
    public OrderItemRequest(String productId, Integer quantity, BigDecimal price) {
        this(productId, quantity, price, null, null, null);
    }
}