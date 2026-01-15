package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleBindingEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleBindingMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleBindingJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
        log.debug("Saving rule binding: id={}, targetType={}, targetId={}",
                binding.getId(), binding.getTargetType(), binding.getTargetId());

        RuleBindingEntity entity = mapper.toEntity(binding);
        RuleBindingEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<RuleBinding> saveAll(List<RuleBinding> bindings) {
        log.debug("Saving {} rule bindings", bindings.size());

        List<RuleBindingEntity> entities = mapper.toEntityList(bindings);
        List<RuleBindingEntity> saved = repository.saveAll(entities);
        return mapper.toDomainList(saved);
    }

    // ========== Find by ID ==========

    @Override
    @Transactional(readOnly = true)
    public Optional<RuleBinding> findById(String id) {
        log.debug("Finding rule binding by id: {}", id);
        return repository.findById(id).map(mapper::toDomain);
    }

    // ========== Find by Target ==========

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findByTarget(String targetType, String targetId) {
        log.debug("Finding rule bindings by target: type={}, id={}", targetType, targetId);
        return mapper.toDomainList(repository.findByTargetTypeAndTargetId(targetType, targetId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findActiveByTarget(String targetType, String targetId) {
        log.debug("Finding active rule bindings by target: type={}, id={}", targetType, targetId);
        return mapper.toDomainList(repository.findActiveByTarget(targetType, targetId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> findByTargetTypeAndTargetIdIn(String targetType, List<String> targetIds) {
        log.debug("Finding rule bindings by target type and IDs: type={}, count={}", targetType, targetIds.size());
        return mapper.toDomainList(repository.findByTargetTypeAndTargetIdIn(targetType, targetIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RuleBinding> findTopActiveByTarget(String targetType, String targetId) {
        log.debug("Finding top active rule binding by target: type={}, id={}", targetType, targetId);
        return repository.findTopActiveByTarget(targetType, targetId).map(mapper::toDomain);
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
    public List<RuleBinding> findEffectiveAtTime(String targetType, Instant timestamp) {
        log.debug("Finding effective rule bindings at time: type={}, timestamp={}", targetType, timestamp);
        return mapper.toDomainList(repository.findEffectiveAtTime(targetType, timestamp));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findTargetIdsByTypeAndTimeRange(String targetType, Instant startTs, Instant endTs) {
        log.debug("Finding target IDs by type and time range: type={}, start={}, end={}", targetType, startTs, endTs);
        return repository.findTargetIdsByTypeAndTimeRange(targetType, startTs, endTs);
    }

    // ========== Search with Pagination ==========

    @Override
    @Transactional(readOnly = true)
    public Page<RuleBinding> search(String targetType, String targetId, String ruleId, Boolean active, Pageable pageable) {
        log.debug("Searching rule bindings: targetType={}, targetId={}, ruleId={}, active={}",
                targetType, targetId, ruleId, active);
        return repository.searchBindings(targetType, targetId, ruleId, active, pageable)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RuleBinding> findByTargetType(String targetType, Pageable pageable) {
        log.debug("Finding rule bindings by target type: {}", targetType);
        return repository.findByTargetType(targetType, pageable).map(mapper::toDomain);
    }

    // ========== Existence Checks ==========

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTargetAndRule(String targetType, String targetId, String ruleId) {
        return repository.existsByTargetTypeAndTargetIdAndRuleId(targetType, targetId, ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveByTarget(String targetType, String targetId) {
        return repository.existsByTargetTypeAndTargetIdAndActive(targetType, targetId, true);
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
    public long countByTarget(String targetType, String targetId) {
        return repository.countByTargetTypeAndTargetId(targetType, targetId);
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
    public int deleteByTarget(String targetType, String targetId) {
        log.info("Deleting rule bindings by target: type={}, id={}", targetType, targetId);
        return repository.deleteByTarget(targetType, targetId);
    }

    @Override
    public int deleteByTargetAndRule(String targetType, String targetId, String ruleId) {
        log.info("Deleting rule binding by target and rule: type={}, id={}, ruleId={}", targetType, targetId, ruleId);
        return repository.deleteByTargetAndRule(targetType, targetId, ruleId);
    }

    @Override
    public int deactivate(String id, String updatedBy) {
        log.info("Deactivating rule binding: id={}, updatedBy={}", id, updatedBy);
        return repository.deactivate(id, updatedBy);
    }

    // ========== Find Single ==========

    @Override
    @Transactional(readOnly = true)
    public Optional<RuleBinding> findByTargetAndRule(String targetType, String targetId, String ruleId) {
        log.debug("Finding rule binding by target and rule: type={}, id={}, ruleId={}", targetType, targetId, ruleId);
        return repository.findByTargetTypeAndTargetIdAndRuleId(targetType, targetId, ruleId)
                .map(mapper::toDomain);
    }
}
