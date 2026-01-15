package vn.viettel.vds.promotion.validation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model representing an applicability rule for assignments.
 * Defines which products/entities are included or excluded from an assignment.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ApplicabilityRule {
    private String id;
    private String assignmentId;
    private RuleType ruleType;  // INCLUDED or EXCLUDED
    private ObjectType objectType;  // PRODUCT, CATEGORY, etc.
    private String objectId;
    private EffectType effect;
    private TargetType target;
    private Integer skipInitially;
    private Integer repeatCount;

    // Audit fields
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version;

    /**
     * Rule type - whether this rule includes or excludes products
     */
    public enum RuleType {
        INCLUDED,
        EXCLUDED
    }

    /**
     * Type of object this rule applies to
     */
    public enum ObjectType {
        PRODUCT,
        CATEGORY,
        BRAND,
        SUPPLIER
    }

    /**
     * Effect type for the rule
     */
    public enum EffectType {
        ALLOW,
        DENY
    }

    /**
     * Target type for applying the rule
     */
    public enum TargetType {
        ALL,
        SPECIFIC
    }
}
