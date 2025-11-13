package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Rule compilation request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompileRequest {

    @Schema(description = "Tenant identifier", example = "tenant1")
    @NotBlank(message = "Tenant ID is required")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Rule identifier", example = "rule123")
    @NotBlank(message = "Rule ID is required")
    @JsonProperty("ruleId")
    private String ruleId;

    @Schema(description = "Rule version", example = "1")
    @NotNull(message = "Version is required")
    @Min(value = 1, message = "Version must be positive")
    @JsonProperty("version")
    private Integer version;

    @Schema(description = "Root logic type", example = "ALL", allowableValues = {"ALL", "ANY", "NONE"})
    @NotBlank(message = "Logic is required")
    @JsonProperty("logic")
    private String logic;

    @Schema(description = "Rule nodes")
    @NotNull(message = "Nodes are required")
    @Valid
    @JsonProperty("nodes")
    private List<RuleNodeDto> nodes;

    @Schema(description = "Operators fingerprint for cache invalidation", example = "abc123")
    @JsonProperty("operatorsFingerprint")
    private String operatorsFingerprint;

    @Schema(description = "Temporal policy links (time-based constraints)")
    @JsonProperty("timeLinks")
    private List<TimeLink> timeLinks;

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

    public List<TimeLink> getTimeLinks() {
        return timeLinks;
    }

    public void setTimeLinks(List<TimeLink> timeLinks) {
        this.timeLinks = timeLinks;
    }

    // Nested classes for temporal policy data
    @Schema(description = "Temporal policy link")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeLink {
        @Schema(description = "Temporal policy identifier", example = "policy123")
        @JsonProperty("policyId")
        private String policyId;

        @Schema(description = "Application mode", example = "ENFORCE", allowableValues = {"ENFORCE", "MONITOR"})
        @JsonProperty("mode")
        private String mode;

        @Schema(description = "Temporal policy data")
        @JsonProperty("data")
        private TemporalPolicyData data;

        public TimeLink() {
        }

        public TimeLink(String policyId, String mode, TemporalPolicyData data) {
            this.policyId = policyId;
            this.mode = mode;
            this.data = data;
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

        public TemporalPolicyData getData() {
            return data;
        }

        public void setData(TemporalPolicyData data) {
            this.data = data;
        }
    }

    @Schema(description = "Temporal policy data for time-based validation")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TemporalPolicyData {
        @Schema(description = "Timezone", example = "Asia/Bangkok")
        @JsonProperty("timezone")
        private String timezone;

        @Schema(description = "RFC 5545 RRULE for recurring patterns", example = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")
        @JsonProperty("rrule")
        private String rrule;

        @Schema(description = "Start timestamp (ISO 8601)", example = "2024-01-01T00:00:00Z")
        @JsonProperty("startTs")
        private String startTs;

        @Schema(description = "End timestamp (ISO 8601)", example = "2024-12-31T23:59:59Z")
        @JsonProperty("endTs")
        private String endTs;

        @Schema(description = "Time-of-day windows")
        @JsonProperty("windows")
        private List<TimeWindow> windows;

        public TemporalPolicyData() {
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public String getRrule() {
            return rrule;
        }

        public void setRrule(String rrule) {
            this.rrule = rrule;
        }

        public String getStartTs() {
            return startTs;
        }

        public void setStartTs(String startTs) {
            this.startTs = startTs;
        }

        public String getEndTs() {
            return endTs;
        }

        public void setEndTs(String endTs) {
            this.endTs = endTs;
        }

        public List<TimeWindow> getWindows() {
            return windows;
        }

        public void setWindows(List<TimeWindow> windows) {
            this.windows = windows;
        }
    }

    @Schema(description = "Time window within a day")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeWindow {
        @Schema(description = "Start time (HH:mm format)", example = "09:00")
        @JsonProperty("startTime")
        private String startTime;

        @Schema(description = "End time (HH:mm format)", example = "17:00")
        @JsonProperty("endTime")
        private String endTime;

        public TimeWindow() {
        }

        public TimeWindow(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
}
