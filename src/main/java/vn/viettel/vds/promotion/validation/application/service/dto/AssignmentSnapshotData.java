package vn.viettel.vds.promotion.validation.application.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO representing the complete state of an assignment aggregate.
 *
 * This is used for JSON serialization/deserialization of snapshots.
 * Contains all related data:
 * - Assignment basic info
 * - Applicability rules
 * - Temporal links with policies
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentSnapshotData {

    // Assignment basic info
    private String id;
    private String entityType;
    private String entityId;
    private String ruleId;
    private Integer priority;
    private Boolean active;
    private String temporalBundleHash;
    private Boolean includedAll;

    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;

    // Related entities
    private List<ApplicabilityRuleSnapshot> applicabilityRules;
    private List<TemporalLinkSnapshot> temporalLinks;

    /**
     * Snapshot of AssignmentApplicabilityRuleEntity
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplicabilityRuleSnapshot {
        private String id;
        private String ruleType;
        private String objectType;
        private String objectId;
        private String effect;
        private String target;
        private Integer skipInitially;
        private Integer repeatCount;
        private Instant createdAt;
        private Instant updatedAt;
    }

    /**
     * Snapshot of RuleTemporalLinkEntity with embedded TemporalPolicy
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemporalLinkSnapshot {
        private String id;
        private String mode;
        private Instant createdAt;
        private Instant updatedAt;

        // Embedded temporal policy
        private TemporalPolicySnapshot temporalPolicy;
    }

    /**
     * Snapshot of TemporalPolicyEntity
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemporalPolicySnapshot {
        private String id;
        private String name;
        private String tz;
        private Instant startTs;
        private Instant endTs;
        private String rrule;
        private List<String> rdate;
        private String exrule;
        private List<String> exdate;
        private Map<String, Object> metadata;
        private Instant createdAt;
        private Instant updatedAt;

        // Time windows
        private List<TimeWindowSnapshot> timeWindows;
    }

    /**
     * Snapshot of TemporalPolicyWindowEntity
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeWindowSnapshot {
        private String id;
        private String start;
        private String end;
    }
}
