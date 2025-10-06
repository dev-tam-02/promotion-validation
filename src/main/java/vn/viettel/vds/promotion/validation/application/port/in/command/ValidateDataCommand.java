package vn.viettel.vds.promotion.validation.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Command for validating data against rules
 */
public class ValidateDataCommand {

    @NotBlank(message = "Tenant ID is required")
    private final String tenantId;

    @NotNull(message = "Validation context is required")
    private final Map<String, Object> validationContext;

    private final String ruleSetId;
    private final boolean failFast;
    private final String requestedBy;

    public ValidateDataCommand(
            String tenantId,
            Map<String, Object> validationContext,
            String ruleSetId,
            boolean failFast,
            String requestedBy
    ) {
        this.tenantId = tenantId;
        this.validationContext = validationContext;
        this.ruleSetId = ruleSetId;
        this.failFast = failFast;
        this.requestedBy = requestedBy;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public void validate() {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (validationContext == null || validationContext.isEmpty()) {
            throw new IllegalArgumentException("Validation context cannot be empty");
        }
    }

    // Getters
    public String getTenantId() {
        return tenantId;
    }

    public Map<String, Object> getValidationContext() {
        return validationContext;
    }

    public String getRuleSetId() {
        return ruleSetId;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public static class Builder {
        private String tenantId;
        private Map<String, Object> validationContext;
        private String ruleSetId;
        private boolean failFast = true;
        private String requestedBy;

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder validationContext(Map<String, Object> validationContext) {
            this.validationContext = validationContext;
            return this;
        }

        public Builder ruleSetId(String ruleSetId) {
            this.ruleSetId = ruleSetId;
            return this;
        }

        public Builder failFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        public Builder requestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
            return this;
        }

        public ValidateDataCommand build() {
            return new ValidateDataCommand(tenantId, validationContext, ruleSetId, failFast, requestedBy);
        }
    }
}