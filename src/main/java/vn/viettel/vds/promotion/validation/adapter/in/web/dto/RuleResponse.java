package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Rule response")
public class RuleResponse {

    @Schema(description = "Rule ID", example = "rul_t1_WEEKEND_VIP_500K")
    @JsonProperty("ruleId")
    private String ruleId;

    @Schema(description = "Tenant ID", example = "t1")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Rule code", example = "WEEKEND_VIP_500K")
    @JsonProperty("code")
    private String code;

    @Schema(description = "Rule name", example = "Weekend VIP ≥500k promotion")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Rule state", example = "draft", allowableValues = {"draft", "published", "archived"})
    @JsonProperty("state")
    private String state;

    @Schema(description = "Latest published version", example = "3")
    @JsonProperty("latestVersion")
    private Integer latestVersion;

    @Schema(description = "Root logic operator", example = "ALL", allowableValues = {"ALL", "ANY", "NONE"})
    @JsonProperty("logic")
    private String logic;

    @Schema(description = "Rule limits configuration", example = "{\"perCodeTotal\": 1000, \"perCustomer\": 3}")
    @JsonProperty("limits")
    private Map<String, Object> limits;

    @Schema(description = "Rule nodes (conditions and groups)")
    @JsonProperty("nodes")
    private List<RuleNodeDto> nodes;

    @Schema(description = "Additional notes or comments")
    @JsonProperty("notes")
    private String notes;

    @Schema(description = "Creation timestamp")
    @JsonProperty("createdAt")
    private Instant createdAt;

    @Schema(description = "Creator")
    @JsonProperty("createdBy")
    private String createdBy;

    @Schema(description = "Last update timestamp")
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @Schema(description = "Last updater")
    @JsonProperty("updatedBy")
    private String updatedBy;

    // Getters and setters
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Integer getLatestVersion() { return latestVersion; }
    public void setLatestVersion(Integer latestVersion) { this.latestVersion = latestVersion; }

    public String getLogic() { return logic; }
    public void setLogic(String logic) { this.logic = logic; }

    public Map<String, Object> getLimits() { return limits; }
    public void setLimits(Map<String, Object> limits) { this.limits = limits; }

    public List<RuleNodeDto> getNodes() { return nodes; }
    public void setNodes(List<RuleNodeDto> nodes) { this.nodes = nodes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}