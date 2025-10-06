package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Rule with assignment details response")
public class RuleWithAssignmentResponse {

    @Schema(description = "Rule details")
    @JsonProperty("rule")
    private RuleResponse rule;

    @Schema(description = "Assignment details")
    @JsonProperty("assignment")
    private AssignmentDetails assignment;

    // Getters and setters
    public RuleResponse getRule() {
        return rule;
    }

    public void setRule(RuleResponse rule) {
        this.rule = rule;
    }

    public AssignmentDetails getAssignment() {
        return assignment;
    }

    public void setAssignment(AssignmentDetails assignment) {
        this.assignment = assignment;
    }

    @Schema(description = "Assignment details for a rule")
    public static class AssignmentDetails {

        @Schema(description = "Assignment ID", example = "asg_t1_CAMPAIGN_camp123")
        @JsonProperty("assignmentId")
        private String assignmentId;

        @Schema(description = "Object type", example = "CAMPAIGN")
        @JsonProperty("objectType")
        private String objectType;

        @Schema(description = "Object ID", example = "camp123")
        @JsonProperty("objectId")
        private String objectId;

        @Schema(description = "Assignment active status", example = "true")
        @JsonProperty("active")
        private Boolean active;

        @Schema(description = "Valid from date")
        @JsonProperty("validFrom")
        private Instant validFrom;

        @Schema(description = "Valid to date")
        @JsonProperty("validTo")
        private Instant validTo;

        @Schema(description = "Traffic percentage", example = "100")
        @JsonProperty("trafficPercent")
        private Integer trafficPercent;

        @Schema(description = "Sticky key strategy", example = "CUSTOMER_ID")
        @JsonProperty("stickyKeyStrategy")
        private String stickyKeyStrategy;

        @Schema(description = "Rule version pinned", example = "2")
        @JsonProperty("ruleVersionPinned")
        private Integer ruleVersionPinned;

        @Schema(description = "Assignment version", example = "1")
        @JsonProperty("assignmentVersion")
        private Integer assignmentVersion;

        @Schema(description = "Assignment created at")
        @JsonProperty("createdAt")
        private Instant createdAt;

        @Schema(description = "Assignment updated at")
        @JsonProperty("updatedAt")
        private Instant updatedAt;

        // Getters and setters
        public String getAssignmentId() {
            return assignmentId;
        }

        public void setAssignmentId(String assignmentId) {
            this.assignmentId = assignmentId;
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

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        public Instant getValidFrom() {
            return validFrom;
        }

        public void setValidFrom(Instant validFrom) {
            this.validFrom = validFrom;
        }

        public Instant getValidTo() {
            return validTo;
        }

        public void setValidTo(Instant validTo) {
            this.validTo = validTo;
        }

        public Integer getTrafficPercent() {
            return trafficPercent;
        }

        public void setTrafficPercent(Integer trafficPercent) {
            this.trafficPercent = trafficPercent;
        }

        public String getStickyKeyStrategy() {
            return stickyKeyStrategy;
        }

        public void setStickyKeyStrategy(String stickyKeyStrategy) {
            this.stickyKeyStrategy = stickyKeyStrategy;
        }

        public Integer getRuleVersionPinned() {
            return ruleVersionPinned;
        }

        public void setRuleVersionPinned(Integer ruleVersionPinned) {
            this.ruleVersionPinned = ruleVersionPinned;
        }

        public Integer getAssignmentVersion() {
            return assignmentVersion;
        }

        public void setAssignmentVersion(Integer assignmentVersion) {
            this.assignmentVersion = assignmentVersion;
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
}