package vn.viettel.vds.promotion.validation.domain.event;

public class RulePublishedEvent extends DomainEvent {
    private final String ruleId;
    private final String ruleCode;
    private final Integer version;
    private final String publishedBy;

    public RulePublishedEvent(String ruleId, String ruleCode, Integer version, String publishedBy) {
        super("RULE_PUBLISHED");
        this.ruleId = ruleId;
        this.ruleCode = ruleCode;
        this.version = version;
        this.publishedBy = publishedBy;
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

    public Integer getVersion() {
        return version;
    }

    public String getPublishedBy() {
        return publishedBy;
    }
}
