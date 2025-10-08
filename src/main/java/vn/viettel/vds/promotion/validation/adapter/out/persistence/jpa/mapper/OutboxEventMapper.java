package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OutboxEventEntity;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

/**
 * Mapper between OutboxEvent domain model and OutboxEventEntity JPA entity.
 */
@Component
public class OutboxEventMapper {

    /**
     * Convert domain model to JPA entity
     */
    public OutboxEventEntity toEntity(OutboxEvent domain) {
        if (domain == null) {
            return null;
        }

        OutboxEventEntity entity = OutboxEventEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .aggregateType(domain.getAggregateType())
                .aggregateId(domain.getAggregateId())
                .eventType(domain.getEventType())
                .destination(domain.getDestination())
                .status(domain.getStatus())
                .attempts(domain.getAttempts())
                .maxAttempts(domain.getMaxAttempts())
                .lastError(domain.getLastError())
                .createdAt(domain.getCreatedAt())
                .lastAttemptAt(domain.getLastAttemptAt())
                .publishedAt(domain.getPublishedAt())
                .version(domain.getVersion())
                .build();

        // Set payload and metadata using helper methods
        entity.setPayloadFromMap(domain.getPayload());
        entity.setMetadataFromMap(domain.getMetadata());

        return entity;
    }

    /**
     * Convert JPA entity to domain model
     */
    public OutboxEvent toDomain(OutboxEventEntity entity) {
        if (entity == null) {
            return null;
        }

        return OutboxEvent.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .aggregateType(entity.getAggregateType())
                .aggregateId(entity.getAggregateId())
                .eventType(entity.getEventType())
                .payload(entity.getPayloadAsMap())
                .destination(entity.getDestination())
                .metadata(entity.getMetadataAsMap())
                .status(entity.getStatus())
                .attempts(entity.getAttempts())
                .maxAttempts(entity.getMaxAttempts())
                .lastError(entity.getLastError())
                .createdAt(entity.getCreatedAt())
                .lastAttemptAt(entity.getLastAttemptAt())
                .publishedAt(entity.getPublishedAt())
                .version(entity.getVersion())
                .build();
    }
}
