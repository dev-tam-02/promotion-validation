package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response for rule publishing operation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RulePublishResponse {

    @Schema(description = "Rule ID", example = "rule-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("ruleId")
    private String ruleId;

    @Schema(description = "Publishing success status", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("success")
    private Boolean success;

    @Schema(description = "Generated bundle hash", example = "sha256:abc123...")
    @JsonProperty("bundleHash")
    private String bundleHash;

    @Schema(description = "Compiled artifact size in bytes", example = "12345")
    @JsonProperty("artifactSize")
    private Long artifactSize;

    @Schema(description = "Error message if publishing failed")
    @JsonProperty("errorMessage")
    private String errorMessage;

    @Schema(description = "Publishing timestamp", example = "1640995200000")
    @JsonProperty("timestamp")
    private Long timestamp;

    @Schema(description = "Compilation logs")
    @JsonProperty("compilationLogs")
    private java.util.List<String> compilationLogs;

    // Constructors
    public RulePublishResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public RulePublishResponse(String ruleId, Boolean success) {
        this();
        this.ruleId = ruleId;
        this.success = success;
    }

    public static RulePublishResponse success(String ruleId, String bundleHash, Long artifactSize) {
        RulePublishResponse response = new RulePublishResponse(ruleId, true);
        response.setBundleHash(bundleHash);
        response.setArtifactSize(artifactSize);
        return response;
    }

    public static RulePublishResponse failed(String ruleId, String errorMessage) {
        RulePublishResponse response = new RulePublishResponse(ruleId, false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    // Getters and setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean isSuccess() {
        return success != null && success;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public Long getArtifactSize() {
        return artifactSize;
    }

    public void setArtifactSize(Long artifactSize) {
        this.artifactSize = artifactSize;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public java.util.List<String> getCompilationLogs() {
        return compilationLogs;
    }

    public void setCompilationLogs(java.util.List<String> compilationLogs) {
        this.compilationLogs = compilationLogs;
    }
}