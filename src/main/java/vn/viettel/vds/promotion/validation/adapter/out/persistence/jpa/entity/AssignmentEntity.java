package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "assignments", indexes = {
        @Index(name = "idx_assignments_subject", columnList = "tenant_id, subject_type, subject_key"),
        @Index(name = "idx_assignments_rule_version", columnList = "tenant_id, rule_id, assignment_version"),
        @Index(name = "idx_assignments_active_subject", columnList = "tenant_id, active, subject_type, subject_key")
})
public class AssignmentEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "rule_version_pinned")
    private Integer ruleVersionPinned;

    @Embedded
    private SubjectEmbeddable subject;

    @Column(name = "assignment_version", nullable = false)
    private Integer assignmentVersion;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "traffic_percent")
    private Integer trafficPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "sticky_key_strategy", length = 50)
    private StickyKeyStrategy stickyKeyStrategy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum StickyKeyStrategy {
        CUSTOMER_ID, ORDER_ID, DEVICE_ID
    }

    @Embeddable
    @Getter
    @Setter
    public static class SubjectEmbeddable {
        @Column(name = "subject_type", length = 50)
        private String type;

        @Column(name = "subject_key", length = 200)
        private String key;
    }
}
