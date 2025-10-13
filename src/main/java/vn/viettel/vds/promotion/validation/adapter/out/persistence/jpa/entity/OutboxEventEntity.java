package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationException;
import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA entity for outbox events.
 * Stores events in a transactional outbox table for reliable event publishing.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status", columnList = "status"),
        @Index(name = "idx_outbox_created_at", columnList = "created_at"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_outbox_event_type", columnList = "event_type"),
        @Index(name = "idx_outbox_status_attempts", columnList = "status, attempts")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventEntity {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "aggregate_type", length = 100, nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 100, nullable = false)
    private String aggregateId;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "destination", length = 500)
    private String destination;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OutboxEventStatus status;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Convert JSON payload string to Map
     */
    public Map<String, Object> getPayloadAsMap() {
        if (payload == null || payload.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to deserialize payload", e);
        }
    }

    /**
     * Set payload from Map
     */
    public void setPayloadFromMap(Map<String, Object> payloadMap) {
        if (payloadMap == null) {
            this.payload = null;
            return;
        }
        try {
            this.payload = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize payload", e);
        }
    }

    /**
     * Convert JSON metadata string to Map
     */
    public Map<String, Object> getMetadataAsMap() {
        if (metadata == null || metadata.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(metadata, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to deserialize metadata", e);
        }
    }

    /**
     * Set metadata from Map
     */
    public void setMetadataFromMap(Map<String, Object> metadataMap) {
        if (metadataMap == null) {
            this.metadata = null;
            return;
        }
        try {
            this.metadata = objectMapper.writeValueAsString(metadataMap);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize metadata", e);
        }
    }
}
