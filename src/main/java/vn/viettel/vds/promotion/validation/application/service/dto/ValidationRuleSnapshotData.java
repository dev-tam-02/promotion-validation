package vn.viettel.vds.promotion.validation.application.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO representing the complete state of a validation rule aggregate.
 * <p>
 * This is used for JSON serialization/deserialization of snapshots.
 * Contains all related data:
 * - Rule basic info
 * - Usage limits
 * - Nodes (rule tree structure)
 * - Time frames
 * <p>
 * Design decisions:
 * - Flat structure for simplicity
 * - No circular references (parent stored as parentId)
 * - All nullable fields to handle partial data
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationRuleSnapshotData {

    // Rule basic info
    private String id;
    private String code;
    private String name;
    private String state;
    private Long ruleVersion;
    private String logic;
    private Map<String, Object> dsl;
    private Instant publishedAt;
    private String publishedBy;
    private String bundleHash;

    // Audit fields
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version; // JPA optimistic locking version

    // Related entities
    private UsageLimitsSnapshot limits;
    private List<NodeSnapshot> nodes;
    private List<TimeFrameSnapshot> timeFrames;

    /**
     * The specific {@code rule_binding} row whose state should be reverted on
     * compensation. The update flow modifies a binding row (validFrom, validTo,
     * timezone, rrule, timeWindows, included/excluded products, etc.), so the
     * snapshot must capture binding state — restoring only the rule would leave
     * all the user-edited fields at post-update values.
     * <p>Null when the snapshot was taken for an entity other than a binding
     * (e.g. node/limit edits — not currently used by coupon update saga).
     */
    private BindingSnapshot binding;

    /**
     * Snapshot of RuleUsageLimitsEntity
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsageLimitsSnapshot {
        private String id;
        private Integer perCodeTotal;
        private Integer perCustomer;
        private Integer perDay;
    }

    /**
     * Snapshot of RuleNodeEntity
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeSnapshot {
        private String id;
        private String nodeId;
        private String type;
        private String groupLogic;
        private List<String> childrenIds;
        private Integer order;
        // Alias of order (rule_nodes.display_order). Captured/restored alongside it so a
        // revert cannot leave the column behind at the DB default (PROM-1350).
        private Integer displayOrder;
        private String operatorName;
        private Map<String, Object> params;
        private String reasonCode;
        private String parentId; // Reference to parent node ID
    }

    /**
     * Snapshot of RuleTimeFrameEntity
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeFrameSnapshot {
        private String id;
        private String timeFrameId;
        private String mode;
    }

    /**
     * Snapshot of {@code RuleBindingEntity} — captures the fields the update
     * path modifies so revert can fully restore pre-update state.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BindingSnapshot {
        private String id;
        private String ruleId;
        private Integer ruleVersionPinned;
        private String objectType;
        private String objectId;

        // Lifecycle + traffic
        private Integer priority;
        private Boolean active;
        private Integer trafficPercent;

        // Time window
        private Instant validFrom;
        private Instant validTo;
        private String timezone;
        private String rrule;
        private String duration;
        private String activityDurationAfterPublishing;
        // Stored as JSON string at rest in the entity; round-trip as String.
        private String timeWindows;
        private String excludedDates;

        // Applicability
        private Boolean includedAll;
        // Stored as JSON string columns at rest.
        private String includedProducts;
        private String excludedProducts;
        private String includedCategories;
        private String excludedCategories;
        private String includedBrands;
        private String excludedBrands;

        private String bundleHash;
    }
}
