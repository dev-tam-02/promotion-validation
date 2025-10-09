package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleNodeEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleNodeRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConditionalOnPromixJpa
public class RuleJpaAdapter implements RulePersistencePort {

    private final RuleJpaRepository repository;
    private final RuleEntityMapper mapper;
    private final RuleNodeRepository nodeRepository;
    private final RuleNodeEntityMapper nodeMapper;

    public RuleJpaAdapter(RuleJpaRepository repository, RuleEntityMapper mapper,
                          RuleNodeRepository nodeRepository, RuleNodeEntityMapper nodeMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.nodeRepository = nodeRepository;
        this.nodeMapper = nodeMapper;
    }

    @Override
    public Rule save(Rule rule) {
        RuleJpaEntity entity = mapper.toEntity(rule);
        RuleJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Rule> findById(String id) {
        return repository.findById(id).map(entity -> {
            Rule rule = mapper.toDomain(entity);
            // Load nodes from rule_nodes table
            List<RuleNodeEntity> nodeEntities = nodeRepository.findByValidationRuleIdOrderByOrder(id);
            if (nodeEntities != null && !nodeEntities.isEmpty()) {
                List<RuleNode> nodes = nodeMapper.toDomainList(nodeEntities);
                rule.setNodes(nodes);
            }
            return rule;
        });
    }

    @Override
    public Optional<Rule> findByCode(String code) {
        return repository.findByCode(code).map(entity -> {
            Rule rule = mapper.toDomain(entity);
            // Load nodes from rule_nodes table
            List<RuleNodeEntity> nodeEntities = nodeRepository.findByValidationRuleIdOrderByOrder(rule.getId());
            if (nodeEntities != null && !nodeEntities.isEmpty()) {
                List<RuleNode> nodes = nodeMapper.toDomainList(nodeEntities);
                rule.setNodes(nodes);
            }
            return rule;
        });
    }

    @Override
    public Page<Rule> findByState(Rule.RuleState state, Pageable pageable) {
        List<RuleJpaEntity> all = repository.findByState(state.name());
        return convertToPage(all, pageable);
    }

    @Override
    public Page<Rule> findWithFilters(Rule.RuleState state,
                                      String codePattern, String namePattern, Pageable pageable) {
        List<RuleJpaEntity> all = repository.findAll().stream()
                .filter(e -> state == null || state.name().equals(e.getState()))
                .filter(e -> codePattern == null ||
                        (e.getCode() != null && e.getCode().toLowerCase().contains(codePattern.toLowerCase())))
                .filter(e -> namePattern == null ||
                        (e.getName() != null && e.getName().toLowerCase().contains(namePattern.toLowerCase())))
                .collect(Collectors.toList());
        return convertToPage(all, pageable);
    }

    @Override
    public List<Rule> findAllOrderByUpdatedAtDesc() {
        return repository.findAll().stream()
                .sorted((e1, e2) -> e2.getUpdatedAt().compareTo(e1.getUpdatedAt()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public long countByState(Rule.RuleState state) {
        return repository.findByState(state.name()).size();
    }

    @Override
    public List<Rule> findByStateNot(Rule.RuleState state) {
        return repository.findAll().stream()
                .filter(e -> !state.name().equals(e.getState()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Rule> findByType(String type) {
        // Note: type field no longer exists in validation_rules table
        // This method will return empty list - consider removing from port interface
        return List.of();
    }

    @Override
    public List<Rule> findByRuleSetId(String ruleSetId) {
        // Note: ruleSetId field no longer exists in validation_rules table
        // This method will return empty list - consider removing from port interface
        return List.of();
    }

    @Override
    public List<Rule> findByCampaignId(String campaignId) {
        // Note: campaignId field no longer exists in validation_rules table
        // This method will return empty list - consider removing from port interface
        return List.of();
    }

    @Override
    public List<Rule> findByPriorityBetween(int minPriority, int maxPriority) {
        // Note: priority field no longer exists in validation_rules table
        // This method will return empty list - consider removing from port interface
        return List.of();
    }

    @Override
    public List<Rule> findByTargetSegmentsContaining(String segment) {
        // JPA doesn't have this query - filter manually
        return repository.findAll().stream()
                .filter(e -> e.getTargetSegments() != null && e.getTargetSegments().contains(segment))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Rule> findAll(Pageable pageable) {
        List<RuleJpaEntity> all = repository.findAll();
        return convertToPage(all, pageable);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public void delete(Rule rule) {
        repository.deleteById(rule.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private Page<Rule> convertToPage(List<RuleJpaEntity> entities, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<Rule> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
