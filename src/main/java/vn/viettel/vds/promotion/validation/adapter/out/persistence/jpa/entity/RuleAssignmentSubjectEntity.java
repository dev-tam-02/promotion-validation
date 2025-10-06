package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "rule_assignment_subjects", indexes = {
        @Index(name = "idx_rule_assignment_subjects_type_key", columnList = "type, subject_key")
})
public class RuleAssignmentSubjectEntity extends BaseEntity {

    @Column(name = "type", nullable = false, length = 50)
    private String type; // "voucher" | "campaign" | "tier" | "reward"

    @Column(name = "subject_key", nullable = false, length = 100)
    private String key;

    // One-to-many relationship with rule assignments
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleAssignmentEntity> assignments = new ArrayList<>();

    // One-to-many relationship with rule bundles
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleBundleEntity> bundles = new ArrayList<>();

    public RuleAssignmentSubjectEntity() {
        super();
    }

    public RuleAssignmentSubjectEntity(String type, String key) {
        super();
        this.type = type;
        this.key = key;
    }
}