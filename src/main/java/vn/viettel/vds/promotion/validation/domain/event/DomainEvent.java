package vn.viettel.vds.promotion.validation.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String eventType;
    private final int eventVersion;

    protected DomainEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.eventType = eventType;
        this.eventVersion = 1;
    }

    protected DomainEvent(String eventId, Instant occurredAt, String eventType, int eventVersion) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public abstract String getAggregateId();

    public abstract String getAggregateTenantId();
}