package vn.viettel.vds.promotion.validation.domain.event;

import vn.viettel.vds.promotion.validation.domain.model.Rule;

/**
 * Event raised when a new rule is created
 */
public class RuleCreatedEvent extends DomainEvent {

    private final String ruleId;
    private final String tenantId;
    private final String ruleCode;
    private final String ruleName;
    private final String createdBy;

    public RuleCreatedEvent(Rule rule) {
        super("RULE_CREATED");
        this.ruleId = rule.getId();
        this.tenantId = rule.getTenantId();
        this.ruleCode = rule.getCode();
        this.ruleName = rule.getName();
        this.createdBy = rule.getCreatedBy();
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

    public String getRuleName() {
        return ruleName;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}