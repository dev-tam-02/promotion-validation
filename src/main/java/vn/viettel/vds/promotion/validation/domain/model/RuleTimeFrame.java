package vn.viettel.vds.promotion.validation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model representing a legacy rule time frame.
 * Links a validation rule to a time frame for backward compatibility.
 * <p>
 * NOTE: This is legacy code kept for backward compatibility.
 * New implementations should use RuleTemporalLink with TemporalPolicy instead.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RuleTimeFrame {
    private String id;
    private String timeFrameId;
    private String mode; // "ALLOW" | "DENY" (blackout)
    private String validationRuleId;

    // Audit fields
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version;
}
