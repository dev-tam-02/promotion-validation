package vn.viettel.vds.promotion.validation.domain.event;

public class RuleCreatedEvent extends DomainEvent {
    private final String ruleId;
    private final String ruleCode;
    private final String createdBy;

    public RuleCreatedEvent(String ruleId, String ruleCode, String createdBy) {
        super("RULE_CREATED");
        this.ruleId = ruleId;
        this.ruleCode = ruleCode;
        this.createdBy = createdBy;
    }

    @Override
    public String getAggregateId() {
        return ruleId;
    }

    @Override
    public String getAggregateTenantId() {
        return null; // No longer using tenantId
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
