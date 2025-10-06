package vn.viettel.vds.promotion.validation.application.port.in.dto;

import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for validation results
 */
public class ValidationResponse {

    private final String validationId;
    private final String ruleId;
    private final String ruleCode;
    private final String decision;
    private final String message;
    private final List<String> reasonCodes;
    private final List<String> explanations;
    private final Instant timestamp;
    private final long processingTimeMs;
    private final boolean allowed;

    private ValidationResponse(Builder builder) {
        this.validationId = builder.validationId;
        this.ruleId = builder.ruleId;
        this.ruleCode = builder.ruleCode;
        this.decision = builder.decision;
        this.message = builder.message;
        this.reasonCodes = builder.reasonCodes;
        this.explanations = builder.explanations;
        this.timestamp = builder.timestamp;
        this.processingTimeMs = builder.processingTimeMs;
        this.allowed = builder.allowed;
    }

    public static ValidationResponse from(ValidationResult result) {
        return builder()
                .validationId(result.getValidationId())
                .ruleId(result.getRuleId())
                .ruleCode(result.getRuleCode())
                .decision(result.getDecision().name())
                .message(result.getMessage())
                .reasonCodes(result.getReasonCodes())
                .explanations(result.getExplanations())
                .timestamp(result.getTimestamp())
                .processingTimeMs(result.getProcessingTimeMs())
                .allowed(result.isAllowed())
                .build();
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getValidationId() {
        return validationId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getDecision() {
        return decision;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getReasonCodes() {
        return reasonCodes;
    }

    public List<String> getExplanations() {
        return explanations;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public static class Builder {
        private String validationId;
        private String ruleId;
        private String ruleCode;
        private String decision;
        private String message;
        private List<String> reasonCodes;
        private List<String> explanations;
        private Instant timestamp;
        private long processingTimeMs;
        private boolean allowed;

        public Builder validationId(String validationId) {
            this.validationId = validationId;
            return this;
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder ruleCode(String ruleCode) {
            this.ruleCode = ruleCode;
            return this;
        }

        public Builder decision(String decision) {
            this.decision = decision;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder reasonCodes(List<String> reasonCodes) {
            this.reasonCodes = reasonCodes;
            return this;
        }

        public Builder explanations(List<String> explanations) {
            this.explanations = explanations;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public Builder allowed(boolean allowed) {
            this.allowed = allowed;
            return this;
        }

        public ValidationResponse build() {
            return new ValidationResponse(this);
        }
    }
}