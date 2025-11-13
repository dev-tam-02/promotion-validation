package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Domain model representing a link between an assignment and a temporal policy.
 * This is a pure domain object without persistence concerns.
 *
 * FIXED: Changed from linking validation rules to linking assignments
 * Rationale: Temporal constraints are assignment-specific, not rule-specific
 */
@Value
@Builder(toBuilder = true)
public class RuleTemporalLink {
    String id;
    String mode; // "ALLOW" | "DENY"

    // Relationships
    String assignmentId;  // FIXED: was validationRuleId
    String temporalPolicyId;

    // Audit fields
    Instant createdAt;
    String createdBy;
    Instant updatedAt;
    String updatedBy;
    Long version;
}
