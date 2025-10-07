package vn.viettel.vds.promotion.validation.domain.event;

import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.time.Instant;

/**
 * Event raised when a rule is published
 */
public class RulePublishedEvent extends DomainEvent {

    private final String ruleId;
    private final String tenantId;
    private final String ruleCode;
    private final String version;
    private final String publishedBy;
    private final Instant publishedAt;

    public RulePublishedEvent(Rule rule) {
        super("RULE_PUBLISHED");
        this.ruleId = rule.getId();
        this.tenantId = rule.getTenantId();
        this.ruleCode = rule.getCode();
        this.version = rule.getVersion() != null ? rule.getVersion().toString() : "0";
        this.publishedBy = rule.getPublishedBy();
        this.publishedAt = rule.getPublishedAt();
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

    public String getPublishedBy() {
        return publishedBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}