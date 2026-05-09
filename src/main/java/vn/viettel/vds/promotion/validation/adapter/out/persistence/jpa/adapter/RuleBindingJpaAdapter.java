package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleBindingMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleBindingJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA adapter implementing RuleBindingPersistencePort.
 * <p>
 * Bridges the domain layer to the JPA persistence layer for rule bindings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class RuleBindingJpaAdapter implements RuleBindingPersistencePort {

    private final RuleBindingJpaRepository repository;
    private final RuleBindingMapper mapper;

    // ========== Save Operations ==========

    @Override
    public RuleBinding save(RuleBinding binding) {
        log.debug("Saving rule binding: id={}, objectType={}, objectId={}",
                binding.getId(), binding.getObjectType(), binding.getObjectId());

        var entity = mapper.toEntity(binding);
        var existingOpt = repository.findById(entity.getId());

        if (existingOpt.isEmpty()) {
            // New entity: set version to null so isNew()=true → persist() instead of merge()
            entity.setVersion(null);
        } else {
            // Existing entity: use the current DB version to prevent OptimisticLockingFailure
            entity.setVersion(existingOpt.get().getVersion());
        }

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<RuleBinding> saveAll(List<RuleBinding> bindings) {
        log.debug("Saving {} rule bindings", bindings.size());

        var entities = mapper.toEntityList(bindings);
        var ids = entities.stream()
                .map(e -> e.getId())
                .toList();
        var existingMap = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(e -> e.getId(), e -> e));

        for (var entity : entities) {
            var existing = existingMap.get(entity.getId());
            if (existing != null) {
                entity.setVersion(existing.getVersion());
            } else {
                entity.setVersion(null);
            }
        }

        var saved = repository.saveAll(entities);
        return mapper.toDomainList(saved);
    }

    // ========== Find by ID ==========

    @Override
    @Transactional(readOnly = true)
    public Optional<RuleBinding> findById(String id) {
        log.debug("Finding rule binding by id: {}", id);
        return repository.findById(id).map(mapper::toDomain);
    }

    // ========== Find by Object ==========

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findByObject(String objectType, String objectId) {
        log.debug("Finding rule bindings by object: type={}, id={}", objectType, objectId);
        return mapper.toDomainList(repository.findByObjectTypeAndObjectId(objectType, objectId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findActiveByObject(String objectType, String objectId) {
        log.debug("Finding active rule bindings by object: type={}, id={}", objectType, objectId);
        return mapper.toDomainList(repository.findActiveByObject(objectType, objectId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findByObjectTypeAndObjectIdIn(String objectType, List<String> objectIds) {
        log.debug("Finding rule bindings by object type and IDs: type={}, count={}", objectType, objectIds.size());
        return mapper.toDomainList(repository.findByObjectTypeAndObjectIdIn(objectType, objectIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RuleBinding> findTopActiveByObject(String objectType, String objectId) {
        log.debug("Finding top active rule binding by object: type={}, id={}", objectType, objectId);
        return repository.findTopActiveByObject(objectType, objectId).map(mapper::toDomain);
    }

    // ========== Find by Rule ==========

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findByRuleId(String ruleId) {
        log.debug("Finding rule bindings by rule id: {}", ruleId);
        return mapper.toDomainList(repository.findByRuleId(ruleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findActiveByRuleId(String ruleId) {
        log.debug("Finding active rule bindings by rule id: {}", ruleId);
        return mapper.toDomainList(repository.findByRuleIdAndActive(ruleId, true));
    }

    // ========== Find by Time Range ==========

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findEffectiveAtTime(String objectType, Instant timestamp) {
        log.debug("Finding effective rule bindings at time: type={}, timestamp={}", objectType, timestamp);
        return mapper.toDomainList(repository.findEffectiveAtTime(objectType, timestamp));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findObjectIdsByTypeAndTimeRange(String objectType, Instant startTs, Instant endTs) {
        log.debug("Finding object IDs by type and time range: type={}, start={}, end={}", objectType, startTs, endTs);
        return repository.findObjectIdsByTypeAndTimeRange(objectType, startTs, endTs);
    }

    // ========== Search with Pagination ==========

    @Override
    @Transactional(readOnly = true)
    public Page<RuleBinding> search(String objectType, String objectId, String ruleId, Boolean active, Pageable pageable) {
        log.debug("Searching rule bindings: objectType={}, objectId={}, ruleId={}, active={}",
                objectType, objectId, ruleId, active);
        return repository.searchBindings(objectType, objectId, ruleId, active, pageable)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RuleBinding> findByObjectType(String objectType, Pageable pageable) {
        log.debug("Finding rule bindings by object type: {}", objectType);
        return repository.findByObjectType(objectType, pageable).map(mapper::toDomain);
    }

    // ========== Existence Checks ==========

    @Override
    @Transactional(readOnly = true)
    public boolean existsByObjectAndRule(String objectType, String objectId, String ruleId) {
        return repository.existsByObjectTypeAndObjectIdAndRuleId(objectType, objectId, ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveByObject(String objectType, String objectId) {
        return repository.existsByObjectTypeAndObjectIdAndActive(objectType, objectId, true);
    }

    // ========== Find by Active Status ==========

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findByActive(boolean active) {
        log.debug("Finding rule bindings by active status: {}", active);
        return mapper.toDomainList(repository.findByActive(active));
    }

    // ========== Count Operations ==========

    @Override
    @Transactional(readOnly = true)
    public long countByObject(String objectType, String objectId) {
        return repository.countByObjectTypeAndObjectId(objectType, objectId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByRuleId(String ruleId) {
        return repository.countByRuleIdAndActive(ruleId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByActive(boolean active) {
        return repository.countByActive(active);
    }

    // ========== Delete Operations ==========

    @Override
    public void deleteById(String id) {
        log.info("Deleting rule binding by id: {}", id);
        repository.deleteById(id);
    }

    @Override
    public int deleteByObject(String objectType, String objectId) {
        log.info("Deleting rule bindings by object: type={}, id={}", objectType, objectId);
        return repository.deleteByObject(objectType, objectId);
    }

    @Override
    public int deleteByObjectIgnoreCase(String objectType, String objectId) {
        log.info("Deleting rule bindings by object (case-insensitive): type={}, id={}", objectType, objectId);
        return repository.deleteByObjectIgnoreCase(objectType, objectId);
    }

    @Override
    public List<RuleBinding> findByObjectIgnoreCase(String objectType, String objectId) {
        log.debug("Finding rule bindings by object (case-insensitive): type={}, id={}", objectType, objectId);
        return mapper.toDomainList(repository.findByObjectIgnoreCase(objectType, objectId));
    }

    @Override
    public int deleteByObjectAndRule(String objectType, String objectId, String ruleId) {
        log.info("Deleting rule binding by object and rule: type={}, id={}, ruleId={}", objectType, objectId, ruleId);
        return repository.deleteByObjectAndRule(objectType, objectId, ruleId);
    }

    @Override
    public int deactivate(String id, String updatedBy) {
        log.info("Deactivating rule binding: id={}, updatedBy={}", id, updatedBy);
        return repository.deactivate(id, updatedBy);
    }

    // ========== Find Single ==========

    @Override
    @Transactional(readOnly = true)
    public Optional<RuleBinding> findByObjectAndRule(String objectType, String objectId, String ruleId) {
        log.debug("Finding rule binding by object and rule: type={}, id={}, ruleId={}", objectType, objectId, ruleId);
        return repository.findByObjectTypeAndObjectIdAndRuleId(objectType, objectId, ruleId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRuleId(String ruleId) {
        log.debug("Counting all bindings for rule: ruleId={}", ruleId);
        return repository.countByRuleId(ruleId);
    }
}
