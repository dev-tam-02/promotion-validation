package vn.viettel.vds.promotion.validation.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "rule_bundles")
@CompoundIndexes({
    @CompoundIndex(def = "{'subject.type': 1, 'subject.key': 1, 'ruleVersion': -1, 'assignmentVersion': -1}"),
    @CompoundIndex(def = "{'bundleHash': 1}", unique = true)
})
public class RuleBundle {

    @Id
    private String id;

    @Indexed(unique = true)
    private String bundleHash;

    private RuleAssignment.Subject subject;

    private Integer ruleVersion;

    private Integer assignmentVersion;

    private byte[] kieModuleBytes;

    private String kieModuleFileId; // GridFS file ID (alternative to storing bytes directly)

    private Instant createdAt;

    public RuleBundle() {
    }

    public RuleBundle(String id, String bundleHash, RuleAssignment.Subject subject,
                      Integer ruleVersion, Integer assignmentVersion) {
        this.id = id;
        this.bundleHash = bundleHash;
        this.subject = subject;
        this.ruleVersion = ruleVersion;
        this.assignmentVersion = assignmentVersion;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public RuleAssignment.Subject getSubject() {
        return subject;
    }

    public void setSubject(RuleAssignment.Subject subject) {
        this.subject = subject;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public Integer getAssignmentVersion() {
        return assignmentVersion;
    }

    public void setAssignmentVersion(Integer assignmentVersion) {
        this.assignmentVersion = assignmentVersion;
    }

    public byte[] getKieModuleBytes() {
        return kieModuleBytes;
    }

    public void setKieModuleBytes(byte[] kieModuleBytes) {
        this.kieModuleBytes = kieModuleBytes;
    }

    public String getKieModuleFileId() {
        return kieModuleFileId;
    }

    public void setKieModuleFileId(String kieModuleFileId) {
        this.kieModuleFileId = kieModuleFileId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}