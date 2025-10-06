package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.MapStringObjectConverter;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "operators", indexes = {
        @Index(name = "idx_operators_name_version", columnList = "tenant_id, name, operator_version", unique = true),
        @Index(name = "idx_operators_context_status", columnList = "tenant_id, context, status")
})
public class OperatorEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum OperatorStatus {
        ACTIVE, DEPRECATED
    }
}
