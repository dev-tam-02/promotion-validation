package vn.viettel.vds.promotion.validation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative domain model for Rule operations.
 * This is the primary model for validation rules in the system.
 * <p>
 * Use this model for all new development. ValidationRule is deprecated.
 * <p>
 * Features:
 * - Complete rule lifecycle management
 * - Usage limits and quotas
 * - Time-based effectiveness
 * - Segment targeting
 * - Campaign and rule set associations
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Rule {
    // Core Identity
    private String id;
    private String code;
    private String ruleCode;

    // Descriptive Information
    private String name;
    private String description;
    private String context;
    private String fallbackErrorMessage;
    private String notes;

    // State Management
    private RuleState state;
    private Boolean active;
    private Long ruleVersion;
    private Integer latestVersion;

    // Rule Logic
    private LogicType logic;
    private String type;
    private String expression;
    private Map<String, Object> dsl;
    private List<RuleNode> nodes;

    // Execution Control
    private Integer priority;
    private Map<String, String> configuration;

    // Usage Limits
    private UsageLimits limits;

    // Time Constraints
    private Instant effectiveFrom;
    private Instant effectiveTo;

    // Targeting
    @Builder.Default
    private Set<String> targetSegments = new HashSet<>();
    private String campaignId;
    private String ruleSetId;

    // Publishing
    private Instant publishedAt;
    private String publishedBy;
    private String bundleHash;

    // System-managed flag (set by changelog — cannot be deleted or updated via admin API)
    @Builder.Default
    private boolean isSystem = false;

    // Audit Trail
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;

    /**
     * Check if the rule is currently active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

    /**
     * Check if the rule is effective at the given time
     */
    public boolean isEffective(Instant checkTime) {
        Instant now = checkTime != null ? checkTime : Instant.now();

        return isActive() &&
                state == RuleState.PUBLISHED &&
                (effectiveFrom == null || !now.isBefore(effectiveFrom)) &&
                (effectiveTo == null || !now.isAfter(effectiveTo));
    }

    /**
     * Check if rule is effective now
     */
    public boolean isEffective() {
        return isEffective(Instant.now());
    }

    /**
     * Check if the rule applies to a specific segment
     */
    public boolean appliesTo(String segment) {
        // If no segments specified, applies to all
        if (targetSegments.isEmpty()) {
            return true;
        }

        return targetSegments.contains(segment);
    }

    /**
     * Publish this rule
     */
    public void publish(String publishedByUser) {
        this.state = RuleState.PUBLISHED;
        this.publishedAt = Instant.now();
        this.publishedBy = publishedByUser;
        this.active = true;
        this.updatedAt = Instant.now();
        this.updatedBy = publishedByUser;
    }

    /**
     * Archive this rule
     */
    public void archive(String archivedByUser) {
        this.state = RuleState.ARCHIVED;
        this.active = false;
        this.updatedAt = Instant.now();
        this.updatedBy = archivedByUser;
    }

    /**
     * Deprecate this rule
     */
    public void deprecate(String deprecatedByUser) {
        this.state = RuleState.DEPRECATED;
        this.active = false;
        this.updatedAt = Instant.now();
        this.updatedBy = deprecatedByUser;
    }

    /**
     * Get the status based on the state
     */
    public String getStatus() {
        return state != null ? state.name() : null;
    }

    /**
     * Get the logic type
     */
    public LogicType getLogicType() {
        return this.logic;
    }

    /**
     * Get rule type enum
     */
    public RuleType getRuleType() {
        if (type == null) {
            return RuleType.CUSTOM;
        }
        try {
            return RuleType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RuleType.CUSTOM;
        }
    }

    /**
     * Get target segments as Set
     */
    public Set<String> getTargetSegmentsSet() {
        return targetSegments;
    }

    /**
     * Evaluate this rule (placeholder for compatibility)
     */
    public Object evaluate(Object context) {
        // This is a placeholder - actual evaluation should be done by RuleEvaluationService
        throw new UnsupportedOperationException("Use RuleEvaluationService for rule evaluation");
    }

    /**
     * Rule state lifecycle enum
     */
    public enum RuleState {
        DRAFT,       // Being created/edited
        PUBLISHED,   // Active and in use
        ARCHIVED,    // Inactive but retained
        DEPRECATED   // Superseded by newer version
    }

    /**
     * Logic type for combining conditions
     */
    public enum LogicType {
        ALL,   // AND - all conditions must be true
        ANY,   // OR - at least one condition must be true
        NONE,  // NOT - all conditions must be false
        XOR    // Exactly one condition must be true
    }

    /**
     * Rule type classification
     */
    public enum RuleType {
        REQUIRED,
        FORMAT,
        RANGE,
        PATTERN,
        CUSTOM,
        BUSINESS_RULE,
        BLACKLIST,
        ELIGIBILITY,
        VALIDATION
    }

    /**
     * Usage limits for rule application
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageLimits {
        private Integer perCodeTotal;     // Total uses across all customers
        private Integer perCustomer;      // Uses per individual customer
        private Integer perDay;           // Daily usage limit
        private Integer perTransaction;   // Per transaction limit
        private Integer remaining;        // Remaining uses

        public boolean hasReachedLimit() {
            if (perCodeTotal != null && remaining != null) {
                return remaining <= 0;
            }
            return false;
        }

        public void decrementRemaining() {
            if (remaining != null && remaining > 0) {
                remaining--;
            }
        }
    }

}
