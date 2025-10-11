package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

@Schema(description = "Rule node (condition or group)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleNodeDto {

    @Schema(description = "Unique node identifier", example = "n1", required = true)
    @NotBlank(message = "Node ID is required")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Node type", example = "COND", required = true, allowableValues = {"GROUP", "COND"})
    @NotBlank(message = "Node type is required")
    @JsonProperty("type")
    private String type;

    @Schema(description = "Group logic (required for GROUP nodes)", example = "ALL", allowableValues = {"ALL", "ANY", "NONE"})
    @JsonProperty("groupLogic")
    private String groupLogic;

    @Schema(description = "Operator name (required for COND nodes)", example = "order.total.gte")
    @JsonProperty("operatorName")
    private String operatorName;

    @Schema(description = "Operator version (optional, defaults to latest)", example = "1")
    @JsonProperty("operatorVersion")
    private Integer operatorVersion;

    @Schema(description = "Operator parameters (required for COND nodes)",
            example = "{\"amount\": 500000, \"currency\": \"VND\"}")
    @JsonProperty("params")
    private Map<String, Object> params;

    @Schema(description = "Reason code (required for COND nodes)", example = "ORDER_TOTAL_MIN")
    @JsonProperty("reasonCode")
    private String reasonCode;

    @Schema(description = "Child node IDs (for GROUP nodes)", example = "[\"n2\", \"n3\"]")
    @JsonProperty("children")
    private List<String> children;

    @Schema(description = "Display order", example = "1")
    @JsonProperty("order")
    private Integer order;

    // Constructors
    public RuleNodeDto() {
        // Default constructor for JSON deserialization
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGroupLogic() {
        return groupLogic;
    }

    public void setGroupLogic(String groupLogic) {
        this.groupLogic = groupLogic;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Integer getOperatorVersion() {
        return operatorVersion;
    }

    public void setOperatorVersion(Integer operatorVersion) {
        this.operatorVersion = operatorVersion;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
