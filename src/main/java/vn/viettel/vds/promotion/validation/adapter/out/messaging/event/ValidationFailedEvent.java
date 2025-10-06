package vn.viettel.vds.promotion.validation.adapter.out.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when validation fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationFailedEvent {
    private String eventId;
    private String validationId;
    private String errorMessage;
    private Instant timestamp;
    @Builder.Default
    private String eventType = "VALIDATION_FAILED";
}