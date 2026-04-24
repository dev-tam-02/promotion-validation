package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity for {@code validation_rules_history}.
 *
 * <p>Each row is an immutable audit snapshot recording the state of a rule
 * at a specific {@code ruleVersion}. The {@code dslSnapshot} column stores
 * the rule DSL as a JSON string (serialised externally before persisting).
 */
@Entity
@Table(name = "validation_rules_history", indexes = {
        @Index(name = "idx_vrh_rule_id", columnList = "rule_id"),
        @Index(name = "idx_vrh_rule_id_version", columnList = "rule_id, rule_version")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationRuleHistoryEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "rule_id", length = 36, nullable = false)
    private String ruleId;

    @Column(name = "rule_version", nullable = false)
    private Long ruleVersion;

    @Column(name = "change_type", length = 20, nullable = false)
    private String changeType;

    @Column(name = "changed_by", length = 255)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "dsl_snapshot", columnDefinition = "LONGTEXT")
    private String dslSnapshot;

    @Column(name = "bundle_hash", length = 255)
    private String bundleHash;

    @Column(name = "state", length = 30)
    private String state;

    @Column(name = "change_reason", length = 500)
    private String changeReason;
}
