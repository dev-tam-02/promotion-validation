package vn.viettel.vds.promotion.validation.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ValidationResponse {
    private String transactionId;
    private boolean valid;
    private String message;
    private String errorCode;
    private Instant timestamp;
    private Long executionTimeMs;
    private List<String> rulesFired;
    private Map<String, Object> metadata;

    public ValidationResponse() {
    }

    // Private constructor used exclusively by Builder pattern to ensure immutability
    @SuppressWarnings("java:S107") // Constructor parameters are managed via Builder pattern
    private ValidationResponse(String transactionId, boolean valid, String message, String errorCode,
                               Instant timestamp, Long executionTimeMs, List<String> rulesFired,
                               Map<String, Object> metadata) {
        this.transactionId = transactionId;
        this.valid = valid;
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
        this.executionTimeMs = executionTimeMs;
        this.rulesFired = rulesFired;
        this.metadata = metadata;
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

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public List<String> getRulesFired() {
        return rulesFired;
    }

    public void setRulesFired(List<String> rulesFired) {
        this.rulesFired = rulesFired;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class Builder {
        private String transactionId;
        private boolean valid;
        private String message;
        private String errorCode;
        private Instant timestamp;
        private Long executionTimeMs;
        private List<String> rulesFired;
        private Map<String, Object> metadata;

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder executionTimeMs(Long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder rulesFired(List<String> rulesFired) {
            this.rulesFired = rulesFired;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ValidationResponse build() {
            return new ValidationResponse(transactionId, valid, message, errorCode, timestamp,
                    executionTimeMs, rulesFired, metadata);
        }
    }
}