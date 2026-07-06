package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Request body for pp-rule-engine's eager standalone-validation compile endpoint
 * ({@code POST /v1/compile/validation}). Called by {@code RuleService} right
 * after a rule is created/updated so the shared VALIDATION bundle for the rule
 * is always-latest, independent of any per-binding timeframe bundle.
 */
@Schema(description = "Eager standalone validation-bundle compile request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationCompileRequest {

    @Schema(description = "Rule identifier", example = "rule123")
    @NotBlank(message = "Rule ID is required")
    @JsonProperty("ruleId")
    private String ruleId;

    @Schema(description = "Flattened rule nodes (id/type/groupLogic/operatorName/operatorVersion/params/reasonCode/children)")
    @NotNull(message = "Nodes are required")
    @JsonProperty("nodes")
    private List<Map<String, Object>> nodes;

    public ValidationCompileRequest() {
    }

    public ValidationCompileRequest(String ruleId, List<Map<String, Object>> nodes) {
        this.ruleId = ruleId;
        this.nodes = nodes;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public List<Map<String, Object>> getNodes() {
        return nodes;
    }

    public void setNodes(List<Map<String, Object>> nodes) {
        this.nodes = nodes;
    }
}
