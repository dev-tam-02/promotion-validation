package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "rule_assignments", indexes = {
        @Index(name = "idx_assignments_subject", columnList = "subject_type, subject_key, active"),
        @Index(name = "idx_assignments_rule_version", columnList = "validation_rule_id, assignment_version")
})
public class RuleAssignmentEntity extends BaseEntity {

    @Column(name = "subject_type", nullable = false, length = 50)
    private String subjectType;

    @Column(name = "subject_key", nullable = false, length = 255)
    private String subjectKey;

    @Column(name = "assignment_version", nullable = false)
    private Integer assignmentVersion;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "traffic_percent", nullable = false)
    private Integer trafficPercent = 100; // default to 100%

    // Many-to-one relationship with validation rule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_rule_id", nullable = false)
    private ValidationRuleEntity validationRule;

    public RuleAssignmentEntity() {
        super();
    }

    public RuleAssignmentEntity(String subjectType, String subjectKey, Integer assignmentVersion, Boolean active) {
        super();
        this.subjectType = subjectType;
        this.subjectKey = subjectKey;
        this.assignmentVersion = assignmentVersion;
        this.active = active;
        this.trafficPercent = 100;
    }
}