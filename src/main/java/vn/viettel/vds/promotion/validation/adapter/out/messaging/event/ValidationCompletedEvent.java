package vn.viettel.vds.promotion.validation.adapter.out.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Event published when validation is completed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class ValidationCompletedEvent {
    private String eventId;
    private String validationId;
    private String decision;
    private boolean isAllowed;
    private List<String> reasonCodes;
    private List<String> explanations;
    private Long processingTimeMs;
    private Instant timestamp;
    private Instant eventTimestamp;
    @Builder.Default
    private String eventType = "VALIDATION_COMPLETED";
}