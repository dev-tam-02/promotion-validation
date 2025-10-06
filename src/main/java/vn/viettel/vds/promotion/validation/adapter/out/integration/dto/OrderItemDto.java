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
public class OrderItemDto {

    @Schema(description = "Product identifier", example = "prod123", required = true)
    @NotBlank(message = "Product ID is required")
    @JsonProperty("productId")
    private String productId;

    @Schema(description = "Item quantity", example = "2", required = true)
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @JsonProperty("quantity")
    private Integer quantity;

    @Schema(description = "Item price", example = "250000", required = true)
    @NotNull(message = "Price is required")
    @JsonProperty("price")
    private BigDecimal price;

    @Schema(description = "Product category", example = "ELECTRONICS")
    @JsonProperty("category")
    private String category;

    @Schema(description = "Product brand", example = "SAMSUNG")
    @JsonProperty("brand")
    private String brand;

    @Schema(description = "Item metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public OrderItemDto() {
    }

    public OrderItemDto(String productId, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters and setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}