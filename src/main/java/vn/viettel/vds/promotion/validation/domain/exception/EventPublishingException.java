package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when event publishing operations fail.
 * Used for HTTP-based event publishing to external services.
 */
public class EventPublishingException extends RuntimeException {

    private final String eventId;
    private final String eventType;

    public EventPublishingException(String message) {
        super(message);
        this.eventId = null;
        this.eventType = null;
    }

    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
        this.eventId = null;
        this.eventType = null;
    }

    public EventPublishingException(String message, String eventId, String eventType) {
        super(message);
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public EventPublishingException(String message, Throwable cause, String eventId, String eventType) {
        super(message, cause);
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }
}
