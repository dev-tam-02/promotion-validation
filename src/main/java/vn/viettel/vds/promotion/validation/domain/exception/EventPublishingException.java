package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ExternalServiceException;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when event publishing operations fail.
 * Used for HTTP-based event publishing to external services.
 * HTTP Status: 502 Bad Gateway
 */
public class EventPublishingException extends ExternalServiceException {

    private static final String ERROR_CODE = "EVENT_PUBLISHING_FAILED";
    private static final String SERVICE_NAME = "event-publisher";

    private final String eventId;
    private final String eventType;

    public EventPublishingException(String message) {
        super(ERROR_CODE, message);
        this.eventId = null;
        this.eventType = null;
    }

    public EventPublishingException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
        this.eventId = null;
        this.eventType = null;
    }

    public EventPublishingException(String message, String eventId, String eventType) {
        super(ERROR_CODE, message, buildParams(eventId, eventType));
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public EventPublishingException(String message, Throwable cause, String eventId, String eventType) {
        super(ERROR_CODE, SERVICE_NAME, message, cause);
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    private static Map<String, Object> buildParams(String eventId, String eventType) {
        Map<String, Object> params = new HashMap<>();
        params.put("serviceName", SERVICE_NAME);
        if (eventId != null) {
            params.put("eventId", eventId);
        }
        if (eventType != null) {
            params.put("eventType", eventType);
        }
        return params;
    }
}
