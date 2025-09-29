package vn.viettel.vds.promotion.validation.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "assignments")
@CompoundIndexes({
    @CompoundIndex(name = "by_subject", def = "{'tenantId': 1, 'subject.type': 1, 'subject.key': 1}"),
    @CompoundIndex(name = "by_rule_version", def = "{'tenantId': 1, 'ruleId': 1, 'assignmentVersion': -1}"),
    @CompoundIndex(name = "active_subject", def = "{'tenantId': 1, 'active': 1, 'subject.type': 1, 'subject.key': 1}")
})
public class Assignment {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("ruleId")
    private String ruleId;

    @Field("ruleVersionPinned")
    private Integer ruleVersionPinned;

    @Field("subject")
    private Subject subject;

    @Field("assignmentVersion")
    private Integer assignmentVersion;

    @Field("active")
    private Boolean active;

    @Field("validFrom")
    private Instant validFrom;

    @Field("validTo")
    private Instant validTo;

    @Field("trafficPercent")
    private Integer trafficPercent;

    @Field("stickyKeyStrategy")
    private StickyKeyStrategy stickyKeyStrategy;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;

    public static class Subject {
        @Field("type")
        private String type;

        @Field("key")
        private String key;

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
    }

    public enum StickyKeyStrategy {
        CUSTOMER_ID, ORDER_ID, DEVICE_ID
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public Integer getRuleVersionPinned() { return ruleVersionPinned; }
    public void setRuleVersionPinned(Integer ruleVersionPinned) { this.ruleVersionPinned = ruleVersionPinned; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Integer getAssignmentVersion() { return assignmentVersion; }
    public void setAssignmentVersion(Integer assignmentVersion) { this.assignmentVersion = assignmentVersion; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }

    public Instant getValidTo() { return validTo; }
    public void setValidTo(Instant validTo) { this.validTo = validTo; }

    public Integer getTrafficPercent() { return trafficPercent; }
    public void setTrafficPercent(Integer trafficPercent) { this.trafficPercent = trafficPercent; }

    public StickyKeyStrategy getStickyKeyStrategy() { return stickyKeyStrategy; }
    public void setStickyKeyStrategy(StickyKeyStrategy stickyKeyStrategy) { this.stickyKeyStrategy = stickyKeyStrategy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}