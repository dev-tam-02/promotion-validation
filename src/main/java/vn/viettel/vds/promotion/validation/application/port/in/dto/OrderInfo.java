package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Order information for validation context.
 *
 * @param orderId    unique order identifier
 * @param orderValue total order value in smallest currency unit
 * @param currency   currency code (VND, USD, etc.)
 * @param orderDate  order creation timestamp
 * @param channel    order channel (ONLINE, OFFLINE, APP, etc.)
 * @param location   order location or store ID
 * @param items      list of order items
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderInfo(
        @NotBlank(message = "Order ID is required")
        String orderId,

        @NotNull(message = "Order value is required")
        @Positive(message = "Order value must be positive")
        BigDecimal orderValue,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotNull(message = "Order date is required")
        Instant orderDate,

        String channel,
        String location,

        @Valid
        List<OrderItemInfo> items
) {

    /**
     * Creates order info with minimum required fields.
     *
     * @param orderId    order identifier
     * @param orderValue order total value
     * @param currency   currency code
     * @return order info
     */
    public static OrderInfo of(String orderId, BigDecimal orderValue, String currency) {
        return new OrderInfo(
                orderId,
                orderValue,
                currency,
                Instant.now(),
                null,
                null,
                Collections.emptyList()
        );
    }

    /**
     * Returns the number of items in the order.
     *
     * @return item count
     */
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    /**
     * Checks if order has items.
     *
     * @return true if order has at least one item
     */
    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    /**
     * Calculates total quantity across all items.
     *
     * @return total quantity
     */
    public int getTotalQuantity() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .mapToInt(OrderItemInfo::quantity)
                .sum();
    }
}
