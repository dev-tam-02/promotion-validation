package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderFact(
    String orderId,
    String customerId,
    Instant orderDate,
    String status,
    BigDecimal totalAmount,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal shipping,
    String currency,
    String channel,
    String paymentMethod,
    List<OrderItemFact> items,
    List<DiscountFact> appliedDiscounts,
    BigDecimal cheapestItemPrice,
    BigDecimal mostExpensiveItemPrice,
    Integer totalQuantity,
    Instant createdAt,
    Instant updatedAt,
    Map<String, Object> metadata
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String orderId;
        private String customerId;
        private Instant orderDate;
        private String status;
        private BigDecimal totalAmount;
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal shipping;
        private String currency;
        private String channel;
        private String paymentMethod;
        private List<OrderItemFact> items;
        private List<DiscountFact> appliedDiscounts;
        private BigDecimal cheapestItemPrice;
        private BigDecimal mostExpensiveItemPrice;
        private Integer totalQuantity;
        private Instant createdAt;
        private Instant updatedAt;
        private Map<String, Object> metadata;


        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder orderDate(Instant orderDate) {
            this.orderDate = orderDate;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder subtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public Builder tax(BigDecimal tax) {
            this.tax = tax;
            return this;
        }

        public Builder shipping(BigDecimal shipping) {
            this.shipping = shipping;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder items(List<OrderItemFact> items) {
            this.items = items;
            return this;
        }

        public Builder appliedDiscounts(List<DiscountFact> appliedDiscounts) {
            this.appliedDiscounts = appliedDiscounts;
            return this;
        }

        public Builder cheapestItemPrice(BigDecimal cheapestItemPrice) {
            this.cheapestItemPrice = cheapestItemPrice;
            return this;
        }

        public Builder mostExpensiveItemPrice(BigDecimal mostExpensiveItemPrice) {
            this.mostExpensiveItemPrice = mostExpensiveItemPrice;
            return this;
        }

        public Builder totalQuantity(Integer totalQuantity) {
            this.totalQuantity = totalQuantity;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public OrderFact build() {
            return new OrderFact(
                orderId, customerId, orderDate, status, totalAmount, subtotal,
                tax, shipping, currency, channel, paymentMethod, items,
                appliedDiscounts, cheapestItemPrice, mostExpensiveItemPrice,
                totalQuantity, createdAt, updatedAt, metadata
            );
        }
    }
}