package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscountFact(
    String discountId,
    String type,
    String code,
    BigDecimal amount,
    BigDecimal percentage,
    String scope,
    String status,
    Map<String, Object> metadata
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String discountId;
        private String type;
        private String code;
        private BigDecimal amount;
        private BigDecimal percentage;
        private String scope;
        private String status;
        private Map<String, Object> metadata;

        public Builder discountId(String discountId) {
            this.discountId = discountId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder percentage(BigDecimal percentage) {
            this.percentage = percentage;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public DiscountFact build() {
            return new DiscountFact(discountId, type, code, amount, percentage, scope, status, metadata);
        }
    }
}