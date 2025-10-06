package vn.viettel.vds.promotion.validation.application.port.in.query;

import jakarta.validation.constraints.NotBlank;

/**
 * Query for retrieving a rule by its ID
 */
public class GetRuleByIdQuery {

    @NotBlank(message = "Rule ID is required")
    private final String ruleId;

    @NotBlank(message = "Tenant ID is required")
    private final String tenantId;

    public GetRuleByIdQuery(String ruleId, String tenantId) {
        this.ruleId = ruleId;
        this.tenantId = tenantId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getTenantId() {
        return tenantId;
    }
}