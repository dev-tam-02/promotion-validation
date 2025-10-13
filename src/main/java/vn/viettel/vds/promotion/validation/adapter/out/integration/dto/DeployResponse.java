package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Deploy response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeployResponse implements OperationResponse {

    @Schema(description = "Deployment success status", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Response message", example = "Rule set deployed successfully")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Rule set identifier", example = "ruleset123")
    @JsonProperty("ruleSetId")
    private String ruleSetId;

    @Schema(description = "Deployment version", example = "1")
    @JsonProperty("version")
    private String version;

    // Constructors
    public DeployResponse() {
    }

    public DeployResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(String ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Alias for isSuccess() - checks if deployment was successful.
     * Provided for backward compatibility and semantic clarity.
     *
     * @return true if deployment succeeded
     */
    public boolean isDeployed() {
        return isSuccess();
    }
}