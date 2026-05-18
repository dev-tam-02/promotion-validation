package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity for rule_bindings table.
 * <p>
 * This is a unified entity that replaces multiple legacy entities:
 * - AssignmentEntity
 * - TemporalPolicyEntity
 * - RuleTemporalLinkEntity
 * - AssignmentApplicabilityRuleEntity
 * <p>
 * Schema defined in: 001-create-validation-rule-engine-schema.yaml
 */
@Getter
@Setter
@Entity
@Table(name = "rule_bindings", indexes = {
        @Index(name = "idx_rb_object", columnList = "object_type, object_id"),
        @Index(name = "idx_rb_rule", columnList = "rule_id"),
        @Index(name = "idx_rb_active_time", columnList = "active, valid_from, valid_to"),
        @Index(name = "idx_rb_object_active", columnList = "object_type, object_id, active")
})
@EntityListeners(IdGenerationListener.class)
@EqualsAndHashCode(callSuper = true)
public class RuleBindingEntity extends BaseEntity {

    // ========== Rule Reference ==========
    @Column(name = "rule_id", length = 36, nullable = false)
    private String ruleId;

    @Column(name = "rule_version_pinned")
    private Integer ruleVersionPinned;

    // ========== Object Reference ==========
    @Column(name = "object_type", length = 50, nullable = false)
    private String objectType;

    @Column(name = "object_id", length = 100, nullable = false)
    private String objectId;

    // ========== Priority & State ==========
    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // ========== Time Constraints ==========
    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "timezone", length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "rrule", length = 1000)
    private String rrule;

    /**
     * JSON array of time windows: [{"start":"09:00","end":"17:00"}]
     */
    @Column(name = "time_windows", columnDefinition = "TEXT")
    private String timeWindows;

    @Column(name = "duration", length = 50)
    private String duration;

    @Column(name = "activity_duration_after_publishing", length = 50)
    private String activityDurationAfterPublishing;

    /**
     * JSON array of excluded dates: ["2024-01-01","2024-12-25"]
     */
    @Column(name = "excluded_dates", columnDefinition = "TEXT")
    private String excludedDates;

    // ========== Product Scope ==========
    @Column(name = "included_all")
    private Boolean includedAll = false;

    @Column(name = "included_products", columnDefinition = "TEXT")
    private String includedProducts;

    @Column(name = "excluded_products", columnDefinition = "TEXT")
    private String excludedProducts;

    @Column(name = "included_categories", columnDefinition = "TEXT")
    private String includedCategories;

    @Column(name = "excluded_categories", columnDefinition = "TEXT")
    private String excludedCategories;

    @Column(name = "included_brands", columnDefinition = "TEXT")
    private String includedBrands;

    @Column(name = "excluded_brands", columnDefinition = "TEXT")
    private String excludedBrands;

    // ========== Traffic Control ==========
    @Column(name = "traffic_percent")
    private Integer trafficPercent = 100;

    @Column(name = "sticky_key_strategy", length = 20)
    private String stickyKeyStrategy;

    // ========== Compiled ==========
    @Column(name = "bundle_hash", length = 255)
    private String bundleHash;

    // ========== Lifecycle Callbacks ==========

    @PrePersist
    public void prePersist() {
        if (active == null) {
            active = true;
        }
        if (priority == null) {
            priority = 0;
        }
        if (includedAll == null) {
            includedAll = false;
        }
        if (trafficPercent == null) {
            trafficPercent = 100;
        }
        if (timezone == null) {
            timezone = "Asia/Ho_Chi_Minh";
        }
    }
}
