package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mapper.ValidationRuleMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.RuleRepository;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.entity.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRule;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB adapter implementation for ValidationRuleRepositoryPort
 */
@Slf4j
@Component
@ConditionalOnPromixMongo
@ConditionalOnBean(RuleRepository.class)
public class ValidationRuleMongoAdapter implements ValidationRuleRepositoryPort {

    private final RuleRepository mongoRepository;
    private final ValidationRuleMapper mapper;

    public ValidationRuleMongoAdapter(RuleRepository mongoRepository, ValidationRuleMapper mapper) {
        this.mongoRepository = mongoRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ValidationRule> findById(String ruleId) {
        log.debug("Finding rule by ID: {}", ruleId);
        return mongoRepository.findById(ruleId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ValidationRule> findActiveRules() {
        log.debug("Finding all active rules");
        // Get published rules from MongoDB (PUBLISHED state means active)
        List<Rule> activeRules = mongoRepository.findByState(Rule.RuleState.PUBLISHED);

        return activeRules.stream()
                .map(mapper::toDomain)
                .filter(ValidationRule::isActive)
                .sorted((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ValidationRule> findByType(ValidationRule.RuleType type) {
        log.debug("Finding rules by type: {}", type);
        List<Rule> rules = mongoRepository.findByType(type.toString());

        return rules.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ValidationRule> findByRuleSetId(String ruleSetId) {
        log.debug("Finding rules by rule set ID: {}", ruleSetId);
        List<Rule> rules = mongoRepository.findByRuleSetId(ruleSetId);

        return rules.stream()
                .map(mapper::toDomain)
                .sorted((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ValidationRule> findByPromotionId(String promotionId) {
        log.debug("Finding rules by promotion ID: {}", promotionId);
        List<Rule> rules = mongoRepository.findByCampaignId(promotionId);

        return rules.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public ValidationRule save(ValidationRule rule) {
        log.debug("Saving rule: {}", rule.getRuleId());
        Rule entity = mapper.toEntity(rule);
        Rule savedEntity = mongoRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(String ruleId) {
        log.debug("Deleting rule: {}", ruleId);
        mongoRepository.deleteById(ruleId);
    }

    @Override
    public boolean existsById(String ruleId) {
        return mongoRepository.existsById(ruleId);
    }

    @Override
    public List<ValidationRule> findByPriorityRange(int minPriority, int maxPriority) {
        log.debug("Finding rules by priority range: {} - {}", minPriority, maxPriority);
        List<Rule> rules = mongoRepository.findByPriorityBetween(minPriority, maxPriority);

        return rules.stream()
                .map(mapper::toDomain)
                .sorted((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ValidationRule> findByTargetSegment(String segment) {
        log.debug("Finding rules by target segment: {}", segment);
        List<Rule> rules = mongoRepository.findByTargetSegmentsContaining(segment);

        return rules.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
