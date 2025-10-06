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
        @Index(name = "idx_rule_assignments_subject_active", columnList = "subject_id, active"),
        @Index(name = "idx_rule_assignments_rule_version", columnList = "validation_rule_id, assignment_version")
})
public class RuleAssignmentEntity extends BaseEntity {

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

    // Many-to-one relationship with subject
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private RuleAssignmentSubjectEntity subject;

    public RuleAssignmentEntity() {
        super();
    }

    public RuleAssignmentEntity(Integer assignmentVersion, Boolean active) {
        super();
        this.assignmentVersion = assignmentVersion;
        this.active = active;
        this.trafficPercent = 100;
    }
}