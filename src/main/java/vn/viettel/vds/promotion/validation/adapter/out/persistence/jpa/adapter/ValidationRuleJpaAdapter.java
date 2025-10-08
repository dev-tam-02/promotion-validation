package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mapper.ValidationRuleMapper;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRule;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public Optional<ValidationRule> findById(String ruleId) {
        log.debug("Finding rule by ID: {}", ruleId);
        return jpaRepository.findById(ruleId)
                .map(mapper::jpaEntityToDomain);
    }

    @Override
    public List<ValidationRule> findActiveRules() {
        log.debug("Finding all active rules (published state)");
        // Get published rules from JPA (PUBLISHED state means active)
        // Note: priority field no longer exists, so no sorting by priority
        return jpaRepository.findByState("PUBLISHED").stream()
                .map(mapper::jpaEntityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ValidationRule> findByType(ValidationRule.RuleType type) {
        log.debug("Finding rules by type: {}", type);
        // Note: type field no longer exists in validation_rules table
        // Returning empty list - this method should be deprecated
        log.warn("findByType called but type field no longer exists in schema - returning empty list");
        return List.of();
    }

    @Override
    public List<ValidationRule> findByRuleSetId(String ruleSetId) {
        log.debug("Finding rules by rule set ID: {}", ruleSetId);
        // Note: ruleSetId field no longer exists in validation_rules table
        // Returning empty list - this method should be deprecated
        log.warn("findByRuleSetId called but ruleSetId field no longer exists in schema - returning empty list");
        return List.of();
    }

    @Override
    public List<ValidationRule> findByPromotionId(String promotionId) {
        log.debug("Finding rules by promotion ID: {}", promotionId);
        // Note: campaignId field no longer exists in validation_rules table
        // Returning empty list - this method should be deprecated
        log.warn("findByPromotionId called but campaignId field no longer exists in schema - returning empty list");
        return List.of();
    }

    @Override
    public ValidationRule save(ValidationRule rule) {
        log.debug("Saving rule: {}", rule.getRuleId());
        // Note: Converting ValidationRule to JPA entity is not fully supported
        // This would need additional mapping logic for complete conversion
        throw new UnsupportedOperationException("Save operation not fully supported for JPA adapter");
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
    public List<ValidationRule> findByPriorityRange(int minPriority, int maxPriority) {
        log.debug("Finding rules by priority range: {} - {}", minPriority, maxPriority);
        // Note: priority field no longer exists in validation_rules table
        // Returning empty list - this method should be deprecated
        log.warn("findByPriorityRange called but priority field no longer exists in schema - returning empty list");
        return List.of();
    }

    @Override
    public List<ValidationRule> findByTargetSegment(String segment) {
        log.debug("Finding rules by target segment: {}", segment);
        // JPA doesn't have this query - filter manually
        return jpaRepository.findAll().stream()
                .filter(e -> e.getTargetSegments() != null && e.getTargetSegments().contains(segment))
                .map(mapper::jpaEntityToDomain)
                .collect(Collectors.toList());
    }
}
