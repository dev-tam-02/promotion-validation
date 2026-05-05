package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mapper.ValidationRuleMapper;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for ValidationRuleRepositoryPort
 */
@Slf4j
@Component
@ConditionalOnPromixJpa
public class ValidationRuleJpaAdapter implements ValidationRuleRepositoryPort {

    private final RuleJpaRepository jpaRepository;
    private final ValidationRuleMapper mapper;

    public ValidationRuleJpaAdapter(RuleJpaRepository jpaRepository, ValidationRuleMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Rule> findById(String ruleId) {
        log.debug("Finding rule by ID: {}", ruleId);
        return jpaRepository.findById(ruleId)
                .map(mapper::jpaEntityToDomain);
    }

    @Override
    public List<Rule> findActiveRules() {
        log.debug("Finding all active rules (published state)");
        // Get published rules from JPA (PUBLISHED state means active)
        // Note: priority field no longer exists, so no sorting by priority
        return jpaRepository.findByState("PUBLISHED").stream()
                .map(mapper::jpaEntityToDomain)
                .toList();
    }

    @Override
    public List<Rule> findByType(Rule.RuleType type) {
        log.debug("Finding rules by type: {}", type);
        // Note: type field no longer exists in validation_rules table
        // Returning empty list - this method should be deprecated
        log.warn("findByType called but type field no longer exists in schema - returning empty list");
        return List.of();
    }

    @Override
    public List<Rule> findByRuleSetId(String ruleSetId) {
        log.debug("Finding rules by rule set ID: {}", ruleSetId);
        // Note: ruleSetId field no longer exists in validation_rules table
        // Returning empty list - this method should be deprecated
        log.warn("findByRuleSetId called but ruleSetId field no longer exists in schema - returning empty list");
        return List.of();
    }

    @Override
    public List<Rule> findByPromotionId(String promotionId) {
        log.debug("Finding rules by promotion ID: {}", promotionId);
        // Note: campaignId field no longer exists in validation_rules table
        // Returning empty list - this method should be deprecated
        log.warn("findByPromotionId called but campaignId field no longer exists in schema - returning empty list");
        return List.of();
    }

    @Override
    public Rule save(Rule rule) {
        log.debug("Saving rule: {}", rule.getId());

        // BUG-024: With @Version on RuleJpaEntity, Spring Data routes save() through
        // persist() vs merge() based on Persistable.isNew() = (version == null).
        //
        // Many call sites (Path B auto-gen, RuleManagementService.createRule, etc.) build
        // brand-new Rule domain objects with a non-null UUIDv7 id and a default
        // version=0L. Without normalisation, isNew() returns false → merge() → UPDATE
        // matches zero rows → StaleObjectStateException.
        //
        // Strategy: probe by id. If the row is not yet in DB, force version=null so the
        // entity is treated as new and routed through persist() (INSERT). Otherwise pass
        // the version through verbatim so merge() can run the optimistic-lock check.
        boolean alreadyPersisted = rule.getId() != null && jpaRepository.existsById(rule.getId());
        Long versionForSave = alreadyPersisted ? rule.getVersion() : null;

        RuleJpaEntity entity = RuleJpaEntity.builder()
                .id(rule.getId())
                .code(rule.getCode())
                .name(rule.getName())
                .state(rule.getState() != null ? rule.getState().name() : "PUBLISHED")
                .ruleVersion(rule.getRuleVersion() != null ? rule.getRuleVersion() : 1L)
                .logic(rule.getLogic() != null ? rule.getLogic().name() : null)
                .description(rule.getDescription())
                .publishedAt(rule.getPublishedAt())
                .publishedBy(rule.getPublishedBy())
                .bundleHash(rule.getBundleHash())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .createdBy(rule.getCreatedBy())
                .updatedBy(rule.getUpdatedBy())
                .version(versionForSave)
                .build();

        RuleJpaEntity saved = jpaRepository.save(entity);
        return mapper.jpaEntityToDomain(saved);
    }

    @Override
    public void deleteById(String ruleId) {
        log.debug("Deleting rule: {}", ruleId);
        jpaRepository.deleteById(ruleId);
    }

    @Override
    public boolean existsById(String ruleId) {
        return jpaRepository.existsById(ruleId);
    }

    @Override
    public List<Rule> findByPriorityRange(int minPriority, int maxPriority) {
        log.debug("Finding rules by priority range: {} - {}", minPriority, maxPriority);
        // Note: priority field no longer exists in validation_rules table
        // Returning empty list - this method should be deprecated
        log.warn("findByPriorityRange called but priority field no longer exists in schema - returning empty list");
        return List.of();
    }

    @Override
    public List<Rule> findByTargetSegment(String segment) {
        log.debug("Finding rules by target segment: {}", segment);
        // JPA doesn't have this query - filter manually
        return jpaRepository.findAll().stream()
                .filter(e -> e.getTargetSegments() != null && e.getTargetSegments().contains(segment))
                .map(mapper::jpaEntityToDomain)
                .toList();
    }
}
