package vn.viettel.vds.promotion.validation.adapter.out.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when a rule is created or updated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleCreatedEvent {
    private String eventId;
    private String ruleId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private Integer priority;
    private Boolean active;
    private Instant createdAt;
    private Instant eventTimestamp;
    @Builder.Default
    private String eventType = "RULE_CREATED";
}