package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderItemFact(
        String itemId,
        String sku,
        String canonicalSku,
        String productId,
        String name,
        String category,
        String brand,
        List<String> tags,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal totalPrice,
        BigDecimal discountAmount,
        String unit,
        Map<String, Object> attributes
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String itemId;
        private String sku;
        private String canonicalSku;
        private String productId;
        private String name;
        private String category;
        private String brand;
        private List<String> tags;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal totalPrice;
        private BigDecimal discountAmount;
        private String unit;
        private Map<String, Object> attributes;

        public Builder itemId(String itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder canonicalSku(String canonicalSku) {
            this.canonicalSku = canonicalSku;
            return this;
        }

        public Builder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder totalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        public Builder discountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
            return this;
        }

        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public OrderItemFact build() {
            return new OrderItemFact(
                    itemId, sku, canonicalSku, productId, name, category, brand,
                    tags, unitPrice, quantity, totalPrice, discountAmount, unit, attributes
            );
        }
    }
}