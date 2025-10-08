package vn.viettel.vds.promotion.validation.domain.enums;

/**
 * Outbox event status enumeration.
 * Represents the lifecycle states of an outbox event.
 */
public enum OutboxEventStatus {
    /**
     * Event is pending and waiting to be processed
     */
    PENDING,

    /**
     * Event is currently being processed
     */
    PROCESSING,

    /**
     * Event has been successfully published
     */
    PUBLISHED,

    /**
     * Event processing failed and will be retried
     */
    FAILED,

    /**
     * Event has exceeded max retry attempts and moved to dead letter
     */
    DEAD_LETTER
}
