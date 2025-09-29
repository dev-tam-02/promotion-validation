package vn.viettel.vds.promotion.validation.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rule_temporal_links")
public class RuleTemporalLink {

    @Id
    private String id;

    @Indexed
    private String ruleId;

    @Indexed
    private String policyId;

    private String mode; // "ALLOW" | "DENY"

    public RuleTemporalLink() {
    }

    public RuleTemporalLink(String id, String ruleId, String policyId, String mode) {
        this.id = id;
        this.ruleId = ruleId;
        this.policyId = policyId;
        this.mode = mode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}