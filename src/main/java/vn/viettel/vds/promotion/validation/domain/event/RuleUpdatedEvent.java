package vn.viettel.vds.promotion.validation.domain.event;

import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.time.Instant;

/**
 * Event raised when a rule is updated
 */
public class RuleUpdatedEvent extends DomainEvent {

    private final String ruleId;
    private final String tenantId;
    private final String ruleCode;
    private final String version;
    private final String updatedBy;
    private final Instant updatedAt;
    private final String updateSummary;

    public RuleUpdatedEvent(Rule rule, String updateSummary) {
        super("RULE_UPDATED");
        this.ruleId = rule.getId().getValue();
        this.tenantId = rule.getTenantId().getValue();
        this.ruleCode = rule.getCode().getValue();
        this.version = rule.getVersion().toString();
        this.updatedBy = rule.getUpdatedBy();
        this.updatedAt = rule.getUpdatedAt();
        this.updateSummary = updateSummary;
    }

    @Override
    public String getAggregateId() {
        return ruleId;
    }

    @Override
    public String getAggregateTenantId() {
        return tenantId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getVersion() {
        return version;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdateSummary() {
        return updateSummary;
    }
}