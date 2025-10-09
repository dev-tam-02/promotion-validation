package vn.viettel.vds.promotion.validation.domain.event;

import java.util.HashMap;
import java.util.Map;

public class RuleUpdatedEvent extends DomainEvent {
    private final String ruleId;
    private final String ruleCode;
    private final String updatedBy;
    private final Map<String, Object> changes;

    public RuleUpdatedEvent(String ruleId, String ruleCode, String updatedBy, Map<String, Object> changes) {
        super("RULE_UPDATED");
        this.ruleId = ruleId;
        this.ruleCode = ruleCode;
        this.updatedBy = updatedBy;
        this.changes = changes;
    }

    @Override
    public String getAggregateId() {
        return ruleId;
    }

    @Override
    public String getAggregateTenantId() {
        return null; // No longer using tenantId
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Map<String, Object> getChanges() {
        return changes != null ? new HashMap<>(changes) : new HashMap<>();
    }
}
