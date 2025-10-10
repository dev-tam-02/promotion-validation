package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Value;
import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;

import java.time.Instant;
import java.util.Map;

/**
 * OutboxEvent domain model.
 * Represents an event in the transactional outbox pattern.
 * This is a pure domain model without any infrastructure dependencies.
 */
@Value
@Builder(toBuilder = true)
public class OutboxEvent {

    /**
     * Unique event identifier (UUIDv7)
     */
    String id;

    /**
     * Type of the aggregate that generated this event (e.g., "Rule", "Assignment")
     */
    String aggregateType;

    /**
     * Identifier of the aggregate instance
     */
    String aggregateId;

    /**
     * Type of the event (e.g., "rule.published", "assignment.changed")
     */
    String eventType;

    /**
     * Event payload as JSON-compatible map
     */
    Map<String, Object> payload;

    /**
     * Destination for the event (e.g., "http://validation-events", "kafka:topic-name")
     */
    String destination;

    /**
     * Additional metadata for the event
     */
    Map<String, Object> metadata;

    /**
     * Current status of the event
     */
    OutboxEventStatus status;

    /**
     * Number of processing attempts
     */
    Integer attempts;

    /**
     * Maximum number of retry attempts before moving to dead letter
     */
    Integer maxAttempts;

    /**
     * Last error message if processing failed
     */
    String lastError;

    /**
     * Timestamp when the event was created
     */
    Instant createdAt;

    /**
     * Timestamp of the last processing attempt
     */
    Instant lastAttemptAt;

    /**
     * Timestamp when the event was successfully published
     */
    Instant publishedAt;

    /**
     * Optimistic locking version
     */
    Long version;

    /**
     * Check if the event has exceeded maximum retry attempts
     */
    public boolean hasExceededMaxAttempts() {
        return attempts != null && maxAttempts != null && attempts >= maxAttempts;
    }

    /**
     * Check if the event can be retried
     */
    public boolean canRetry() {
        return status == OutboxEventStatus.FAILED && !hasExceededMaxAttempts();
    }

    /**
     * Check if the event is in a final state (published or dead letter)
     */
    public boolean isFinalState() {
        return status == OutboxEventStatus.PUBLISHED || status == OutboxEventStatus.DEAD_LETTER;
    }

    /**
     * Create a new event with incremented attempt count
     */
    public OutboxEvent withIncrementedAttempts() {
        return toBuilder()
                .attempts(attempts == null ? 1 : attempts + 1)
                .lastAttemptAt(Instant.now())
                .build();
    }

    /**
     * Mark event as published
     */
    public OutboxEvent markAsPublished() {
        return toBuilder()
                .status(OutboxEventStatus.PUBLISHED)
                .publishedAt(Instant.now())
                .build();
    }

    /**
     * Mark event as failed with error message
     */
    public OutboxEvent markAsFailed(String errorMessage) {
        OutboxEvent event = toBuilder()
                .status(OutboxEventStatus.FAILED)
                .lastError(errorMessage)
                .lastAttemptAt(Instant.now())
                .build();

        // Check if should move to dead letter
        if (event.hasExceededMaxAttempts()) {
            return event.toBuilder()
                    .status(OutboxEventStatus.DEAD_LETTER)
                    .build();
        }

        return event;
    }

    /**
     * Mark event as processing
     */
    public OutboxEvent markAsProcessing() {
        return toBuilder()
                .status(OutboxEventStatus.PROCESSING)
                .lastAttemptAt(Instant.now())
                .build();
    }
}
