package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.viettel.vds.promotion.validation.config.validator.ValidEnum;
import vn.viettel.vds.promotion.validation.domain.enums.RuleContext;

import java.util.List;
import java.util.Map;

@Schema(description = "Request to create a new rule")
public class CreateRuleRequest {

    @Schema(description = "Rule code (unique identifier, auto-generated if blank)", example = "WEEKEND_VIP_500K")
    @Size(max = 100, message = "Rule code must not exceed 100 characters")
    @JsonProperty("code")
    private String code;

    @Schema(description = "Rule name", example = "Weekend VIP ≥500k promotion")
    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name must not exceed 255 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Root logic operator (defaults to ALL if blank)", example = "ALL", allowableValues = {"ALL", "ANY", "NONE"})
    @JsonProperty("logic")
    private String logic;

    @Schema(description = "Rule context indicating the trigger event",
            example = "ORDER_CREATED",
            allowableValues = {"COMMON", "CUSTOMER_CREATED", "ORDER_CREATED", "PAYMENT_COMPLETED", "PROMOTION_APPLIED"})
    @NotBlank(message = "VALIDATION_RULE_CONTEXT_REQUIRED")
    @ValidEnum(value = RuleContext.class, message = "VALIDATION_RULE_CONTEXT_INVALID")
    @JsonProperty("context")
    private String context;

    @Schema(description = "Human-readable description of the rule purpose")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Rule limits configuration", example = "{\"perCodeTotal\": 1000, \"perCustomer\": 3}")
    @JsonProperty("limits")
    private Map<String, Object> limits;

    @Schema(description = "Rule nodes (conditions and groups)")
    @Valid
    @JsonProperty("nodes")
    private List<RuleNodeDto> nodes;

    @Schema(description = "Generic fallback error message shown to users when the rule fails",
            example = "Đơn hàng không đáp ứng điều kiện khuyến mãi VIP")
    @Size(max = 500, message = "Fallback error message must not exceed 500 characters")
    @JsonProperty("fallbackErrorMessage")
    private String fallbackErrorMessage;

    @Schema(description = "Additional notes or comments")
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @JsonProperty("notes")
    private String notes;

    // Getters and setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    public Map<String, Object> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, Object> limits) {
        this.limits = limits;
    }

    public List<RuleNodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<RuleNodeDto> nodes) {
        this.nodes = nodes;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFallbackErrorMessage() {
        return fallbackErrorMessage;
    }

    public void setFallbackErrorMessage(String fallbackErrorMessage) {
        this.fallbackErrorMessage = fallbackErrorMessage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Nested class for backward compatibility with tests
    @Schema(description = "Rule node request (nested class for backward compatibility)")
    public static class RuleNodeRequest {
        private String id;
        private String type;
        private String groupLogic;
        private String operatorName;
        private Integer operatorVersion;
        private Map<String, Object> params;
        private String reasonCode;
        private List<String> children;
        private Integer order;

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
}
