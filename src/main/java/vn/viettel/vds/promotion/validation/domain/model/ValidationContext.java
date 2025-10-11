package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Value object representing the context of a validation request
 */
@Getter
@Builder
@ToString
public class ValidationContext {

    private final CustomerContext customer;
    private final OrderContext order;
    private final Map<String, Object> metadata;
    private final Instant requestTime;

    /**
     * Factory method to create ValidationContext from a map
     */
    public static ValidationContext of(Map<String, Object> contextData) {
        if (contextData == null || contextData.isEmpty()) {
            return ValidationContext.builder()
                    .requestTime(Instant.now())
                    .build();
        }

        return ValidationContext.builder()
                .customer(extractCustomerContext(contextData))
                .order(extractOrderContext(contextData))
                .metadata(contextData)
                .requestTime(Instant.now())
                .build();
    }

    private static CustomerContext extractCustomerContext(Map<String, Object> contextData) {
        if (contextData.containsKey("customer")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> customerData = (Map<String, Object>) contextData.get("customer");
            return CustomerContext.builder()
                    .customerId((String) customerData.get("customerId"))
                    .segment((String) customerData.get("segment"))
                    .tier((String) customerData.get("tier"))
                    .totalPurchaseAmount(convertToBigDecimal(customerData.get("totalPurchaseAmount")))
                    .transactionCount((Integer) customerData.get("transactionCount"))
                    .build();
        }
        return null;
    }

    private static OrderContext extractOrderContext(Map<String, Object> contextData) {
        if (contextData.containsKey("order")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> orderData = (Map<String, Object>) contextData.get("order");
            return OrderContext.builder()
                    .orderId((String) orderData.get("orderId"))
                    .orderValue(convertToBigDecimal(orderData.get("orderValue")))
                    .itemCount((Integer) orderData.get("itemCount"))
                    .channel((String) orderData.get("channel"))
                    .build();
        }
        return null;
    }

    private static BigDecimal convertToBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public boolean isValid() {
        return customer != null && customer.isValid();
    }

    public boolean hasOrder() {
        return order != null && order.isValid();
    }

    /**
     * Get value from context by field name
     */
    public Object getValue(String fieldName) {
        if (fieldName == null) {
            return null;
        }

        // Handle customer fields
        if (customer != null) {
            switch (fieldName) {
                case "customerId":
                    return customer.getCustomerId();
                case "segment":
                    return customer.getSegment();
                case "tier":
                    return customer.getTier();
                case "totalPurchaseAmount":
                    return customer.getTotalPurchaseAmount();
                case "transactionCount":
                    return customer.getTransactionCount();
                default:
                    // Field not found in customer context, continue searching
                    break;
            }
        }

        // Handle order fields
        if (order != null) {
            switch (fieldName) {
                case "orderId":
                    return order.getOrderId();
                case "orderValue":
                    return order.getOrderValue();
                case "itemCount":
                    return order.getItemCount();
                case "channel":
                    return order.getChannel();
                default:
                    // Field not found in order context, continue searching
                    break;
            }
        }

        // Handle metadata
        if (metadata != null) {
            return metadata.get(fieldName);
        }

        return null;
    }

    /**
     * Customer context for validation
     */
    @Getter
    @Builder
    @ToString
    public static class CustomerContext {
        private final String customerId;
        private final String segment;
        private final String tier;
        private final BigDecimal totalPurchaseAmount;
        private final Integer transactionCount;

        public boolean isValid() {
            return customerId != null && !customerId.isEmpty();
        }

        public boolean isHighValue() {
            return totalPurchaseAmount != null &&
                    totalPurchaseAmount.compareTo(new BigDecimal("1000000")) > 0;
        }
    }

    /**
     * Order context for validation
     */
    @Getter
    @Builder
    @ToString
    public static class OrderContext {
        private final String orderId;
        private final BigDecimal orderValue;
        private final Integer itemCount;
        private final String channel;
        private final Map<String, BigDecimal> productCategories;

        public boolean isValid() {
            return orderId != null && orderValue != null &&
                    orderValue.compareTo(BigDecimal.ZERO) > 0;
        }

        public boolean isLargeOrder() {
            return itemCount != null && itemCount > 10;
        }

        public boolean isOnlineOrder() {
            return "ONLINE".equalsIgnoreCase(channel);
        }
    }
}