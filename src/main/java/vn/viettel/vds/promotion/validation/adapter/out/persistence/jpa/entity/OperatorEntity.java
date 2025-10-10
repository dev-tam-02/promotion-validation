package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.MapStringObjectConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Map;

/**
 * JPA entity for operators mapped to operators table.
 *
 * IMPORTANT: This entity does NOT extend BaseEntity because the schema does NOT have
 * created_by, updated_by, or version columns that BaseEntity provides.
 *
 * Schema columns (from 001-create-validation-rule-engine-schema.yaml:256-334):
 * - id: varchar(36) - Primary key
 * - name: varchar(100) - Operator name
 * - operator_version: int - Operator version
 * - context: varchar(100) - Operator context (order, customer, time, etc.)
 * - json_schema: text - JSON Schema for operator parameters
 * - compiler_id: varchar(100) - Compiler template ID
 * - status: varchar(20) - Operator status (ACTIVE, DEPRECATED)
 * - created_at: timestamp - Creation timestamp
 * - updated_at: timestamp - Last update timestamp
 *
 * NOTE: Schema does NOT have created_by, updated_by, or version columns
 */
@Getter
@Setter
@Entity
@Table(name = "operators", indexes = {
        @Index(name = "idx_operators_name_version", columnList = "name, operator_version", unique = true),
        @Index(name = "idx_operators_context_status", columnList = "context, status")
})
public class OperatorEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "operator_version", nullable = false)
    private Integer operatorVersion;

    @Column(name = "context", length = 100)
    private String context;

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "json_schema", columnDefinition = "TEXT")
    private Map<String, Object> jsonSchema;

    @Column(name = "compiler_id", length = 100)
    private String compilerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OperatorStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }

    public enum OperatorStatus {
        ACTIVE, DEPRECATED
    }
}
