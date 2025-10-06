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
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_audit_logs_action_time", columnList = "tenant_id, action, at")
})
public class AuditLogEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;

    @Embedded
    private AuditTargetEmbeddable target;

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "diff", columnDefinition = "TEXT")
    private Map<String, Object> diff;

    @Column(name = "at", nullable = false)
    private Instant at;

    public enum AuditAction {
        RULE_CREATE, RULE_EDIT, RULE_PUBLISH, OP_CREATE, ASSIGN_UPDATE
    }

    @Embeddable
    @Getter
    @Setter
    public static class AuditTargetEmbeddable {
        @Column(name = "target_type", length = 50)
        private String type;

        @Column(name = "target_id", length = 100)
        private String id;
    }
}
