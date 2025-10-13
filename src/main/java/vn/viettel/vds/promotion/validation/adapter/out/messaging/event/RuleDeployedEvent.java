package vn.viettel.vds.promotion.validation.adapter.out.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when rules are deployed to engine
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class RuleDeployedEvent {
    private String eventId;
    private String ruleSetId;
    private Boolean deploymentSuccess;
    private Instant deployedAt;
    private Instant eventTimestamp;
    @Builder.Default
    private String eventType = "RULES_DEPLOYED";
}