package vn.viettel.vds.promotion.validation.application.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO representing the complete state of a validation rule aggregate.
 *
 * This is used for JSON serialization/deserialization of snapshots.
 * Contains all related data:
 * - Rule basic info
 * - Usage limits
 * - Nodes (rule tree structure)
 * - Time frames
 *
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
}
