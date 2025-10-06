package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Domain model representing a link between a validation rule and a temporal policy.
 * This is a pure domain object without persistence concerns.
 */
@Value
@Builder(toBuilder = true)
public class RuleTemporalLink {
    String id;
    String mode; // "ALLOW" | "DENY"

    // Relationships
    String validationRuleId;
    String temporalPolicyId;

    // Audit fields
    Instant createdAt;
    String createdBy;
    Instant updatedAt;
    String updatedBy;
    Long version;
}
