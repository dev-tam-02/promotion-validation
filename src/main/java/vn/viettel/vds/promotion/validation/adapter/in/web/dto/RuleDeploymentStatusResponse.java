package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rule deployment status response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleDeploymentStatusResponse {

    @Schema(description = "Rule ID", example = "rule-001", required = true)
    @JsonProperty("ruleId")
    private String ruleId;

    @Schema(description = "Deployment status",
            example = "DEPLOYED",
            allowableValues = {"NOT_DEPLOYED", "COMPILED_NOT_LOADED", "DEPLOYED", "ERROR"})
    @JsonProperty("status")
    private String status;

    @Schema(description = "Whether rule is currently deployed", example = "true")
    @JsonProperty("deployed")
    private Boolean deployed;

    @Schema(description = "Bundle hash if deployed", example = "sha256:abc123...")
    @JsonProperty("bundleHash")
    private String bundleHash;

    @Schema(description = "Last deployment timestamp", example = "1640995200000")
    @JsonProperty("lastDeployedAt")
    private Long lastDeployedAt;

    @Schema(description = "Engine health status", example = "HEALTHY")
    @JsonProperty("engineHealth")
    private String engineHealth;

    @Schema(description = "Additional status information")
    @JsonProperty("statusInfo")
    private String statusInfo;

    // Constructors
    public RuleDeploymentStatusResponse() {
    }

    public RuleDeploymentStatusResponse(String ruleId, String status, Boolean deployed, String bundleHash) {
        this.ruleId = ruleId;
        this.status = status;
        this.deployed = deployed;
        this.bundleHash = bundleHash;
    }

    // Getters and setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDeployed() {
        return deployed;
    }

    public void setDeployed(Boolean deployed) {
        this.deployed = deployed;
    }

    public Boolean isDeployed() {
        return deployed != null && deployed;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public Long getLastDeployedAt() {
        return lastDeployedAt;
    }

    public void setLastDeployedAt(Long lastDeployedAt) {
        this.lastDeployedAt = lastDeployedAt;
    }

    public String getEngineHealth() {
        return engineHealth;
    }

    public void setEngineHealth(String engineHealth) {
        this.engineHealth = engineHealth;
    }

    public String getStatusInfo() {
        return statusInfo;
    }

    public void setStatusInfo(String statusInfo) {
        this.statusInfo = statusInfo;
    }
}