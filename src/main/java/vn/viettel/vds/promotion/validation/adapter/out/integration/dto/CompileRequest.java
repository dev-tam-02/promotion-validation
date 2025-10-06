package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@Schema(description = "Rule compilation request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompileRequest {

    @Schema(description = "Tenant identifier", example = "tenant1", required = true)
    @NotBlank(message = "Tenant ID is required")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Rule identifier", example = "rule123", required = true)
    @NotBlank(message = "Rule ID is required")
    @JsonProperty("ruleId")
    private String ruleId;

    @Schema(description = "Rule version", example = "1", required = true)
    @NotNull(message = "Version is required")
    @Min(value = 1, message = "Version must be positive")
    @JsonProperty("version")
    private Integer version;

    @Schema(description = "Root logic type", example = "ALL", required = true, allowableValues = {"ALL", "ANY", "NONE"})
    @NotBlank(message = "Logic is required")
    @JsonProperty("logic")
    private String logic;

    @Schema(description = "Rule nodes", required = true)
    @NotNull(message = "Nodes are required")
    @Valid
    @JsonProperty("nodes")
    private List<RuleNodeDto> nodes;

    @Schema(description = "Operators fingerprint for cache invalidation", example = "abc123")
    @JsonProperty("operatorsFingerprint")
    private String operatorsFingerprint;

    @Schema(description = "Rules map for compilation")
    @JsonProperty("rules")
    private Map<String, String> rules;

    // Constructors
    public CompileRequest() {
    }

    public CompileRequest(String tenantId, String ruleId, Integer version, String logic, List<RuleNodeDto> nodes) {
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.version = version;
        this.logic = logic;
        this.nodes = nodes;
    }

    // Getters and setters
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    public List<RuleNodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<RuleNodeDto> nodes) {
        this.nodes = nodes;
    }

    public String getOperatorsFingerprint() {
        return operatorsFingerprint;
    }

    public void setOperatorsFingerprint(String operatorsFingerprint) {
        this.operatorsFingerprint = operatorsFingerprint;
    }

    public Map<String, String> getRules() {
        return rules;
    }

    public void setRules(Map<String, String> rules) {
        this.rules = rules;
    }
}