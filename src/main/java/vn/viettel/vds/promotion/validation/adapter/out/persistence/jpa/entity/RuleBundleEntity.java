package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rule_bundles", indexes = {
        @Index(name = "idx_rule_bundles_subject_versions", columnList = "subject_id, rule_version, assignment_version"),
        @Index(name = "idx_rule_bundles_hash", columnList = "bundle_hash", unique = true)
})
public class RuleBundleEntity extends BaseEntity {

    @Column(name = "bundle_hash", nullable = false, unique = true, length = 100)
    private String bundleHash;

    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion;

    @Column(name = "assignment_version", nullable = false)
    private Integer assignmentVersion;

    @Lob
    @Column(name = "kie_module_bytes")
    private byte[] kieModuleBytes;

    @Column(name = "kie_module_file_id", length = 100)
    private String kieModuleFileId; // GridFS file ID (alternative to storing bytes directly)

    // Many-to-one relationship with validation rule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_rule_id", nullable = false)
    private ValidationRuleEntity validationRule;

    // Many-to-one relationship with subject
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private RuleAssignmentSubjectEntity subject;

    public RuleBundleEntity() {
        super();
    }

    public RuleBundleEntity(String bundleHash, Integer ruleVersion, Integer assignmentVersion) {
        super();
        this.bundleHash = bundleHash;
        this.ruleVersion = ruleVersion;
        this.assignmentVersion = assignmentVersion;
    }
}