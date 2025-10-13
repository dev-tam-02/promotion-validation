package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request to publish multiple rules in batch")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublishRuleBatchRequest {

    @Schema(description = "List of rule IDs to publish", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Rule IDs list cannot be empty")
    @Size(max = 50, message = "Batch size cannot exceed 50 rules")
    @JsonProperty("ruleIds")
    private List<String> ruleIds;

    @Schema(description = "Force republish even if already published", example = "false")
    @JsonProperty("force")
    private Boolean force = false;

    @Schema(description = "Skip verification step", example = "false")
    @JsonProperty("skipVerification")
    private Boolean skipVerification = false;

    @Schema(description = "Stop batch on first failure", example = "false")
    @JsonProperty("stopOnFailure")
    private Boolean stopOnFailure = false;

    @Schema(description = "Maximum parallel executions", example = "5")
    @JsonProperty("maxParallel")
    private Integer maxParallel = 5;

    // Constructors
    public PublishRuleBatchRequest() {
    }

    public PublishRuleBatchRequest(List<String> ruleIds) {
        this.ruleIds = ruleIds;
    }

    // Getters and setters
    public List<String> getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(List<String> ruleIds) {
        this.ruleIds = ruleIds;
    }

    public Boolean getForce() {
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }

    public Boolean getSkipVerification() {
        return skipVerification;
    }

    public void setSkipVerification(Boolean skipVerification) {
        this.skipVerification = skipVerification;
    }

    public Boolean getStopOnFailure() {
        return stopOnFailure;
    }

    public void setStopOnFailure(Boolean stopOnFailure) {
        this.stopOnFailure = stopOnFailure;
    }

    public Integer getMaxParallel() {
        return maxParallel;
    }

    public void setMaxParallel(Integer maxParallel) {
        this.maxParallel = maxParallel;
    }
}