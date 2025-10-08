package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OutboxEventEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.OutboxEventMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.OutboxEventJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.OutboxEventPersistencePort;
import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA adapter implementation for OutboxEventPersistencePort.
 * Provides JPA-based persistence for outbox events.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class OutboxEventJpaAdapter implements OutboxEventPersistencePort {

    private final OutboxEventJpaRepository repository;
    private final OutboxEventMapper mapper;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity entity = mapper.toEntity(event);
        OutboxEventEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> findById(String id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findByStatus(OutboxEventStatus status) {
        return repository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OutboxEvent> findByStatus(OutboxEventStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findPendingEvents(pageable).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findByTenantIdAndStatus(String tenantId, OutboxEventStatus status) {
        return repository.findByTenantIdAndStatus(tenantId, status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findEventsCreatedBefore(Instant timestamp) {
        return repository.findByCreatedAtBefore(timestamp).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPublishedEventsOlderThan(Instant timestamp) {
        return repository.findPublishedEventsOlderThan(timestamp).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(OutboxEventStatus status) {
        return repository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteAll(List<OutboxEvent> events) {
        List<OutboxEventEntity> entities = events.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        repository.deleteAll(entities);
    }

    @Override
    public int deleteEventsCreatedBefore(Instant timestamp) {
        return repository.deleteByCreatedAtBefore(timestamp);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId) {
        return repository.findByAggregateTypeAndAggregateId(aggregateType, aggregateId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findByEventType(String eventType) {
        return repository.findByEventType(eventType).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
