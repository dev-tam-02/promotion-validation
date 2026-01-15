package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

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
        @Index(name = "idx_rb_target", columnList = "target_type, target_id"),
        @Index(name = "idx_rb_rule", columnList = "rule_id"),
        @Index(name = "idx_rb_active_time", columnList = "active, valid_from, valid_to"),
        @Index(name = "idx_rb_target_active", columnList = "target_type, target_id, active")
})
public class RuleBindingEntity {

    // ========== Identity ==========
    @Id
    @Column(name = "id", length = 36)
    private String id;

    // ========== Rule Reference ==========
    @Column(name = "rule_id", length = 36, nullable = false)
    private String ruleId;

    @Column(name = "rule_version_pinned")
    private Integer ruleVersionPinned;

    // ========== Target ==========
    @Column(name = "target_type", length = 50, nullable = false)
    private String targetType;

    @Column(name = "target_id", length = 100, nullable = false)
    private String targetId;

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

    // ========== Audit ==========
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    // ========== Lifecycle Callbacks ==========

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
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
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
