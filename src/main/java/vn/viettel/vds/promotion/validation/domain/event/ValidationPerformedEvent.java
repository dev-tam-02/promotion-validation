package vn.viettel.vds.promotion.validation.domain.event;

import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

/**
 * Event raised when a validation is performed
 */
public class ValidationPerformedEvent extends DomainEvent {

    private final String validationId;
    private final String ruleId;
    private final String tenantId;
    private final ValidationResult.Decision decision;
    private final String primaryReasonCode;
    private final long processingTimeMs;

    public ValidationPerformedEvent(String tenantId, ValidationResult result) {
        super("VALIDATION_PERFORMED");
        this.validationId = result.getValidationId();
        this.ruleId = result.getRuleId();
        this.tenantId = tenantId;
        this.decision = result.getDecision();
        this.primaryReasonCode = result.getPrimaryReasonCode();
        this.processingTimeMs = result.getProcessingTimeMs();
    }

    @Override
    public String getAggregateId() {
        return validationId;
    }

    @Override
    public String getAggregateTenantId() {
        return tenantId;
    }

    public String getValidationId() {
        return validationId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public ValidationResult.Decision getDecision() {
        return decision;
    }

    public String getPrimaryReasonCode() {
        return primaryReasonCode;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
}