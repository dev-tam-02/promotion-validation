package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "rule_bundles", indexes = {
        @Index(name = "idx_bundles_subject_versions", columnList = "subject_type, subject_key, rule_version, assignment_version"),
        @Index(name = "idx_rule_bundles_hash", columnList = "bundle_hash", unique = true)
})
public class RuleBundleEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "bundle_hash", nullable = false, unique = true, length = 255)
    private String bundleHash;

    @Column(name = "subject_type", nullable = false, length = 50)
    private String subjectType;

    @Column(name = "subject_key", nullable = false, length = 255)
    private String subjectKey;

    @Column(name = "rule_version", nullable = false)
    private Long ruleVersion;

    @Column(name = "assignment_version", nullable = false)
    private Integer assignmentVersion;

    @Lob
    @Column(name = "kie_module_bytes", columnDefinition = "LONGBLOB")
    private byte[] kieModuleBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Many-to-one relationship with validation rule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_rule_id", nullable = false)
    private ValidationRuleEntity validationRule;

    public RuleBundleEntity() {
        this.id = UuidCreator.getTimeOrderedEpoch().toString();
    }

    public RuleBundleEntity(String bundleHash, String subjectType, String subjectKey, Long ruleVersion, Integer assignmentVersion) {
        this();
        this.bundleHash = bundleHash;
        this.subjectType = subjectType;
        this.subjectKey = subjectKey;
        this.ruleVersion = ruleVersion;
        this.assignmentVersion = assignmentVersion;
    }
}