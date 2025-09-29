package vn.viettel.vds.promotion.validation.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "outbox_events")
@CompoundIndex(name = "dispatch_queue", def = "{'tenantId': 1, 'status': 1, 'createdAt': 1}")
public class OutboxEvent {

    @Id
    private String id;

    @Field("tenantId")
    private String tenantId;

    @Field("type")
    private EventType type;

    @Field("payload")
    private Map<String, Object> payload;

    @Field("status")
    private EventStatus status;

    @Field("attempts")
    private Integer attempts;

    @Field("createdAt")
    private Instant createdAt;

    @Field("lastTriedAt")
    private Instant lastTriedAt;

    public enum EventType {
        RULE_PUBLISHED, ASSIGNMENT_CHANGED
    }

    public enum EventStatus {
        PENDING, PROCESSING, SENT, FAILED
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastTriedAt() { return lastTriedAt; }
    public void setLastTriedAt(Instant lastTriedAt) { this.lastTriedAt = lastTriedAt; }
}