package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Full rule response DTO — used for single-rule detail endpoint ({@code GET /v1/rules/{id}}).
 *
 * <p>For the list endpoint ({@code GET /v1/rules}) use {@link RuleListItemResponse} instead,
 * which omits heavy fields (description, fallbackErrorMessage, nodes, limits, logic)
 * that are unnecessary when rendering table rows.</p>
 */
@Schema(description = "Full rule response (detail view). For list views use RuleListItemResponse.")
public class RuleResponse {

    @Schema(description = "Rule ID", example = "rul_t1_WEEKEND_VIP_500K")
    @JsonProperty("ruleId")
    private String ruleId;

    @Schema(description = "Rule code", example = "WEEKEND_VIP_500K")
    @JsonProperty("code")
    private String code;

    @Schema(description = "Rule name", example = "Weekend VIP ≥500k promotion")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Rule context", example = "ORDER")
    @JsonProperty("context")
    private String context;

    @Schema(description = "Rule description")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Generic fallback error message shown to users when the rule fails",
            example = "Đơn hàng không đáp ứng điều kiện khuyến mãi VIP")
    @JsonProperty("fallbackErrorMessage")
    private String fallbackErrorMessage;

    @Schema(description = "Number of condition nodes in the rule")
    @JsonProperty("nodeCount")
    private Integer nodeCount;

    @Schema(description = "Number of campaigns assigned to this rule")
    @JsonProperty("assignmentCount")
    private Long assignmentCount;

    @Schema(description = "Optimistic locking version")
    @JsonProperty("version")
    private Long version;

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

    @Schema(description = "Lint analysis report (null if no issues found)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("lint")
    private LintReportDto lint;

    @Schema(description = "Creation timestamp")
    @JsonProperty("createdAt")
    private Instant createdAt;

    @Schema(description = "Creator user ID")
    @JsonProperty("createdBy")
    private String createdBy;

    @Schema(description = "Creator display name")
    @JsonProperty("createdByName")
    private String createdByName;

    @Schema(description = "Last update timestamp")
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @Schema(description = "Last updater user ID")
    @JsonProperty("updatedBy")
    private String updatedBy;

    @Schema(description = "Last updater display name")
    @JsonProperty("updatedByName")
    private String updatedByName;

    // Getters and setters
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

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Long getAssignmentCount() {
        return assignmentCount;
    }

    public void setAssignmentCount(Long assignmentCount) {
        this.assignmentCount = assignmentCount;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

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


    public Integer getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(Integer latestVersion) {
        this.latestVersion = latestVersion;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LintReportDto getLint() {
        return lint;
    }

    public void setLint(LintReportDto lint) {
        this.lint = lint;
    }
}