package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Order item information for validation.
 *
 * @param sku      product SKU
 * @param quantity item quantity
 * @param price    unit price in smallest currency unit
 * @param category product category
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderItemInfo(
        @NotBlank(message = "SKU is required")
        String sku,

        @Positive(message = "Quantity must be positive")
        int quantity,

        @Positive(message = "Price must be positive")
        BigDecimal price,

        String category
) {

    /**
     * Creates order item with minimum required fields.
     *
     * @param sku      product SKU
     * @param quantity item quantity
     * @param price    unit price
     * @return order item info
     */
    public static OrderItemInfo of(String sku, int quantity, BigDecimal price) {
        return new OrderItemInfo(sku, quantity, price, null);
    }

    /**
     * Calculates total amount for this item.
     *
     * @return total amount (quantity * price)
     */
    public BigDecimal getTotalAmount() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
