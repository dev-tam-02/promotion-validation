package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO for applicability rule.
 * Represents an included/excluded product, collection, or SKU for an assignment.
 */
@Schema(description = "Applicability rule response")
public class ApplicabilityRuleResponse {

    @Schema(description = "Applicability rule ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Assignment ID that this rule belongs to", example = "550e8400-e29b-41d4-a716-446655440001")
    @JsonProperty("assignmentId")
    private String assignmentId;

    @Schema(description = "Rule type - INCLUDED or EXCLUDED", example = "INCLUDED", allowableValues = {"INCLUDED", "EXCLUDED"})
    @JsonProperty("ruleType")
    private String ruleType;

    @Schema(description = "Object type - COLLECTION, PRODUCT, or SKU", example = "PRODUCT", allowableValues = {"COLLECTION", "PRODUCT", "SKU"})
    @JsonProperty("objectType")
    private String objectType;

    @Schema(description = "Object ID (collection ID, product ID, or SKU)", example = "PROD-001")
    @JsonProperty("objectId")
    private String objectId;

    @Schema(description = "Effect type - how the rule is applied", example = "APPLY_TO_EVERY",
            allowableValues = {"APPLY_TO_EVERY", "APPLY_TO_CHEAPEST", "APPLY_TO_MOST_EXPENSIVE"})
    @JsonProperty("effect")
    private String effect;

    @Schema(description = "Target type - what the rule applies to", example = "ITEM", allowableValues = {"ITEM", "ORDER", "CUSTOMER"})
    @JsonProperty("target")
    private String target;

    @Schema(description = "Number of items to skip initially", example = "0")
    @JsonProperty("skipInitially")
    private Integer skipInitially;

    @Schema(description = "Number of times to repeat the effect", example = "1")
    @JsonProperty("repeatCount")
    private Integer repeatCount;

    @Schema(description = "Creation timestamp")
    @JsonProperty("createdAt")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    /**
     * Default constructor required for JSON deserialization (Jackson).
     */
    public ApplicabilityRuleResponse() {
        // Required by Jackson for JSON deserialization
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Integer getSkipInitially() {
        return skipInitially;
    }

    public void setSkipInitially(Integer skipInitially) {
        this.skipInitially = skipInitially;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
