package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * JPA entity cho validation_rules_assignment_deleted table.
 * Archive table để lưu trữ lịch sử các validation rule assignment đã bị xóa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "validation_rules_assignment_deleted", indexes = {
        @Index(name = "idx_vrad_assignment_id", columnList = "assignment_id"),
        @Index(name = "idx_vrad_validation_rule", columnList = "validation_rule_id"),
        @Index(name = "idx_vrad_object", columnList = "object_id"),
        @Index(name = "idx_vrad_deleted_at", columnList = "deleted_at")
})
public class ValidationRuleAssignmentDeletedEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "assignment_id", length = 36, nullable = false)
    private String assignmentId;

    @Column(name = "validation_rule_id", length = 36, nullable = false)
    private String validationRuleId;

    @Column(name = "object_id", length = 36, nullable = false)
    private String objectId;

    @Column(name = "object_type", length = 50, nullable = false)
    private String objectType;

    @Column(name = "version")
    private Long version;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "deleted_at", nullable = false)
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by", length = 100, nullable = false)
    private String deletedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @PrePersist
    public void prePersist() {
        if (deletedAt == null) {
            deletedAt = OffsetDateTime.now();
        }
    }
}
