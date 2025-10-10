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

    @Schema(description = "Tenant identifier", example = "default", required = true)
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

    @Schema(description = "Rule nodes as map objects", required = true)
    @NotNull(message = "Nodes are required")
    @JsonProperty("nodes")
    private List<Map<String, Object>> nodes;

    @Schema(description = "Usage limits")
    @Valid
    @JsonProperty("limits")
    private Limits limits;

    @Schema(description = "Time policy links")
    @JsonProperty("timeLinks")
    private List<TimeLink> timeLinks;

    @Schema(description = "Operators fingerprint for cache invalidation", example = "abc123", required = true)
    @NotBlank(message = "Operators fingerprint is required")
    @JsonProperty("operatorsFingerprint")
    private String operatorsFingerprint;

    @Schema(description = "Compiler identifier", example = "java-compiler-v1", required = true)
    @NotBlank(message = "Compiler ID is required")
    @JsonProperty("compilerId")
    private String compilerId;

    @Schema(description = "Source information")
    @Valid
    @JsonProperty("source")
    private Source source;

    // Constructors
    public CompileRequest() {
    }

    public CompileRequest(String tenantId, String ruleId, Integer version, List<Map<String, Object>> nodes,
                          Limits limits, List<TimeLink> timeLinks, String operatorsFingerprint,
                          String compilerId, Source source) {
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.version = version;
        this.nodes = nodes;
        this.limits = limits;
        this.timeLinks = timeLinks;
        this.operatorsFingerprint = operatorsFingerprint;
        this.compilerId = compilerId;
        this.source = source;
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

    public List<Map<String, Object>> getNodes() {
        return nodes;
    }

    public void setNodes(List<Map<String, Object>> nodes) {
        this.nodes = nodes;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits;
    }

    public List<TimeLink> getTimeLinks() {
        return timeLinks;
    }

    public void setTimeLinks(List<TimeLink> timeLinks) {
        this.timeLinks = timeLinks;
    }

    public String getOperatorsFingerprint() {
        return operatorsFingerprint;
    }

    public void setOperatorsFingerprint(String operatorsFingerprint) {
        this.operatorsFingerprint = operatorsFingerprint;
    }

    public String getCompilerId() {
        return compilerId;
    }

    public void setCompilerId(String compilerId) {
        this.compilerId = compilerId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    // Nested classes
    @Schema(description = "Usage limits")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Limits {
        @Schema(description = "Per customer limit")
        @JsonProperty("perCustomer")
        private Integer perCustomer;

        @Schema(description = "Per day limit")
        @JsonProperty("perDay")
        private Integer perDay;

        public Limits() {
        }

        public Limits(Integer perCustomer, Integer perDay) {
            this.perCustomer = perCustomer;
            this.perDay = perDay;
        }

        public Integer getPerCustomer() {
            return perCustomer;
        }

        public void setPerCustomer(Integer perCustomer) {
            this.perCustomer = perCustomer;
        }

        public Integer getPerDay() {
            return perDay;
        }

        public void setPerDay(Integer perDay) {
            this.perDay = perDay;
        }
    }

    @Schema(description = "Time policy link")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeLink {
        @Schema(description = "Policy identifier", required = true)
        @NotBlank
        @JsonProperty("policyId")
        private String policyId;

        @Schema(description = "Link mode", required = true)
        @NotBlank
        @JsonProperty("mode")
        private String mode;

        public TimeLink() {
        }

        public TimeLink(String policyId, String mode) {
            this.policyId = policyId;
            this.mode = mode;
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

    @Schema(description = "Source information")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Source {
        @Schema(description = "Rule version identifier", required = true)
        @NotBlank
        @JsonProperty("ruleVersionId")
        private String ruleVersionId;

        @Schema(description = "Snapshot hash", required = true)
        @NotBlank
        @JsonProperty("snapshotHash")
        private String snapshotHash;

        public Source() {
        }

        public Source(String ruleVersionId, String snapshotHash) {
            this.ruleVersionId = ruleVersionId;
            this.snapshotHash = snapshotHash;
        }

        public String getRuleVersionId() {
            return ruleVersionId;
        }

        public void setRuleVersionId(String ruleVersionId) {
            this.ruleVersionId = ruleVersionId;
        }

        public String getSnapshotHash() {
            return snapshotHash;
        }

        public void setSnapshotHash(String snapshotHash) {
            this.snapshotHash = snapshotHash;
        }
    }
}