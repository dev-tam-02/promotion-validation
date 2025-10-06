package vn.viettel.vds.promotion.validation.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "rule_temporal_links")
@CompoundIndexes({
        @CompoundIndex(name = "by_rule", def = "{'tenantId': 1, 'ruleId': 1}"),
        @CompoundIndex(name = "by_policy", def = "{'tenantId': 1, 'policyId': 1}")
})
public class RuleTemporalLink {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("ruleId")
    private String ruleId;

    @Field("policyId")
    private String policyId;

    @Field("mode")
    private TemporalLinkMode mode;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public TemporalLinkMode getMode() {
        return mode;
    }

    public void setMode(TemporalLinkMode mode) {
        this.mode = mode;
    }

    public enum TemporalLinkMode {
        ALLOW, DENY
    }
}