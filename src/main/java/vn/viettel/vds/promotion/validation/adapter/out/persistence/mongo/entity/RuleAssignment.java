package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "rule_assignments")
@CompoundIndexes({
        @CompoundIndex(def = "{'subject.type': 1, 'subject.key': 1, 'active': 1}"),
        @CompoundIndex(def = "{'ruleId': 1, 'assignmentVersion': -1}")
})
public class RuleAssignment {

    @Id
    private String id;

    private String ruleId;

    private Subject subject;

    private Integer assignmentVersion;

    private Boolean active;

    private Instant validFrom;

    private Instant validTo;

    private Integer trafficPercent;

    private Instant createdAt;

    private Instant updatedAt;

    public RuleAssignment() {
    }

    public RuleAssignment(String id, String ruleId, Subject subject, Integer assignmentVersion, Boolean active) {
        this.id = id;
        this.ruleId = ruleId;
        this.subject = subject;
        this.assignmentVersion = assignmentVersion;
        this.active = active;
        this.trafficPercent = 100; // default to 100%
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Integer getAssignmentVersion() {
        return assignmentVersion;
    }

    public void setAssignmentVersion(Integer assignmentVersion) {
        this.assignmentVersion = assignmentVersion;
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

    public static class Subject {
        private String type; // "voucher" | "campaign" | "tier" | "reward"
        private String key;

        public Subject() {
        }

        public Subject(String type, String key) {
            this.type = type;
            this.key = key;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}