package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rule_assignment_subjects", indexes = {
        @Index(name = "idx_assignment_subjects_assignment", columnList = "rule_assignment_id")
})
public class RuleAssignmentSubjectEntity extends BaseEntity {

    @Column(name = "rule_assignment_id", nullable = false, length = 36)
    private String ruleAssignmentId;

    @Column(name = "subject_type", nullable = false, length = 50)
    private String subjectType; // "voucher" | "campaign" | "tier" | "reward"

    @Column(name = "subject_key", nullable = false, length = 255)
    private String subjectKey;

    public RuleAssignmentSubjectEntity() {
        super();
    }

    public RuleAssignmentSubjectEntity(String ruleAssignmentId, String subjectType, String subjectKey) {
        super();
        this.ruleAssignmentId = ruleAssignmentId;
        this.subjectType = subjectType;
        this.subjectKey = subjectKey;
    }
}