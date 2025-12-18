package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * JPA entity for assignment applicability rules.
 * Stores included/excluded products, collections, and SKUs for an assignment.
 * <p>
 * Schema columns (from 015-create-assignment-applicability-rules.yaml):
 * - id: varchar(36) - Primary key
 * - assignment_id: varchar(36) - Foreign key to assignments
 * - rule_type: varchar(20) - INCLUDED or EXCLUDED
 * - object_type: varchar(20) - COLLECTION, PRODUCT, or SKU
 * - object_id: varchar(100) - Object identifier
 * - effect: varchar(30) - APPLY_TO_EVERY, APPLY_TO_CHEAPEST, APPLY_TO_MOST_EXPENSIVE
 * - target: varchar(20) - ITEM, ORDER, CUSTOMER
 * - skip_initially: int - Items to skip initially
 * - repeat_count: int - Times to repeat
 * - created_at: timestamp - Creation timestamp
 * - updated_at: timestamp - Last update timestamp
 */
@Getter
@Setter
@Entity
@Table(name = "assignment_applicability_rules")
public class AssignmentApplicabilityRuleEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AssignmentEntity assignment;

    @Column(name = "rule_type", length = 20, nullable = false)
    private String ruleType;

    @Column(name = "object_type", length = 20, nullable = false)
    private String objectType;

    @Column(name = "object_id", length = 100, nullable = false)
    private String objectId;

    @Column(name = "effect", length = 30)
    private String effect = "APPLY_TO_EVERY";

    @Column(name = "target", length = 20)
    private String target = "ITEM";

    @Column(name = "skip_initially")
    private Integer skipInitially = 0;

    @Column(name = "repeat_count")
    private Integer repeatCount = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (effect == null) {
            effect = "APPLY_TO_EVERY";
        }
        if (target == null) {
            target = "ITEM";
        }
        if (skipInitially == null) {
            skipInitially = 0;
        }
        if (repeatCount == null) {
            repeatCount = 1;
        }
    }

    /**
     * Rule type enum for type safety
     */
    public enum RuleType {
        INCLUDED,
        EXCLUDED
    }

    /**
     * Object type enum for type safety
     */
    public enum ObjectType {
        COLLECTION,
        PRODUCT,
        SKU
    }

    /**
     * Effect type enum for type safety
     */
    public enum EffectType {
        APPLY_TO_EVERY,
        APPLY_TO_CHEAPEST,
        APPLY_TO_MOST_EXPENSIVE
    }

    /**
     * Target type enum for type safety
     */
    public enum TargetType {
        ITEM,
        ORDER,
        CUSTOMER
    }
}
