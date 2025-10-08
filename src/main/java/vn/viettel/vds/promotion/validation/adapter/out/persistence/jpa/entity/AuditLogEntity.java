package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity for admin audit logs mapped to admin_audit_logs table.
 *
 * Schema columns (from 001-create-validation-rule-engine-schema.yaml:1003-1079):
 * - id: varchar(36) - Primary key (from BaseEntity)
 * - entity_type: varchar(100) - Entity type (Rule, Operator, Assignment, etc.)
 * - entity_id: varchar(100) - Entity identifier
 * - action: varchar(50) - Action performed
 * - actor_id: varchar(100) - User who performed action
 * - details: text - Action details as JSON
 * - snapshot_before: text - Entity state before action
 * - snapshot_after: text - Entity state after action
 * - timestamp: timestamp - When action occurred
 * - created_at: timestamp (from BaseEntity)
 * - updated_at: timestamp (from BaseEntity)
 *
 * NOTE: Schema does NOT have tenant_id or diff columns
 */
@Getter
@Setter
@Entity
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_logs_actor", columnList = "actor_id")
})
public class AuditLogEntity extends BaseEntity {

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "snapshot_before", columnDefinition = "TEXT")
    private String snapshotBefore;

    @Column(name = "snapshot_after", columnDefinition = "TEXT")
    private String snapshotAfter;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
