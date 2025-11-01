package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * JPA entity cho validation_rules_assignment table.
 * Bảng này lưu trữ thông tin về việc gán validation rule cho các đối tượng (campaign, voucher, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "validation_rules_assignment", indexes = {
        @Index(name = "idx_vra_validation_rule", columnList = "validation_rule_id"),
        @Index(name = "idx_vra_object", columnList = "object_id"),
        @Index(name = "idx_vra_deleted", columnList = "deleted")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_vra_validation_object", columnNames = {"validation_rule_id", "object_id", "deleted"})
})
public class ValidationRuleAssignmentEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "validation_rule_id", length = 36, nullable = false)
    private String validationRuleId;

    @Column(name = "object_id", length = 36, nullable = false)
    private String objectId;

    @Column(name = "object_type", length = 50, nullable = false)
    private String objectType;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 100, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "last_modified_at")
    private OffsetDateTime lastModifiedAt;

    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    @PrePersist
    public void prePersist() {
        if (deleted == null) {
            deleted = false;
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        lastModifiedAt = OffsetDateTime.now();
    }
}
