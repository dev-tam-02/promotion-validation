package vn.viettel.vds.promotion.validation.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ValidationRequest {
    private String transactionId;
    private String promotionId;
    private String customerId;
    private String sessionId;
    private Map<String, Object> context;
    private List<String> rules;
    private Instant timestamp;
    private BigDecimal orderValue;

    public ValidationRequest() {
    }

    public ValidationRequest(String transactionId, String promotionId, String customerId, String sessionId,
                           Map<String, Object> context, List<String> rules, Instant timestamp, BigDecimal orderValue) {
        this.transactionId = transactionId;
        this.promotionId = promotionId;
        this.customerId = customerId;
        this.sessionId = sessionId;
        this.context = context;
        this.rules = rules;
        this.timestamp = timestamp;
        this.orderValue = orderValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getOrderValue() {
        return orderValue;
    }

    public void setOrderValue(BigDecimal orderValue) {
        this.orderValue = orderValue;
    }

    public static class Builder {
        private String transactionId;
        private String promotionId;
        private String customerId;
        private String sessionId;
        private Map<String, Object> context;
        private List<String> rules;
        private Instant timestamp;
        private BigDecimal orderValue;

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder promotionId(String promotionId) {
            this.promotionId = promotionId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public Builder rules(List<String> rules) {
            this.rules = rules;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder orderValue(BigDecimal orderValue) {
            this.orderValue = orderValue;
            return this;
        }

        public ValidationRequest build() {
            return new ValidationRequest(transactionId, promotionId, customerId, sessionId, context, rules, timestamp, orderValue);
        }
    }
}