package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConditionalOnPromixJpa
public class RuleJpaAdapter implements RulePersistencePort {

    private final RuleJpaRepository repository;
    private final RuleEntityMapper mapper;

    public RuleJpaAdapter(RuleJpaRepository repository, RuleEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Rule save(Rule rule) {
        RuleJpaEntity entity = mapper.toEntity(rule);
        RuleJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Rule> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Rule> findByTenantIdAndCode(String tenantId, String code) {
        // JPA repo has different method - filter manually
        return repository.findAll().stream()
                .filter(e -> tenantId.equals(e.getTenantId()) && code.equals(e.getCode()))
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public Page<Rule> findByTenantIdAndState(String tenantId, Rule.RuleState state, Pageable pageable) {
        // Manual filtering by tenantId and state
        List<RuleJpaEntity> all = repository.findAll().stream()
                .filter(e -> tenantId.equals(e.getTenantId()) && state.name().equals(e.getState()))
                .collect(Collectors.toList());
        return convertToPage(all, pageable);
    }

    @Override
    public Page<Rule> findByTenantIdWithFilters(String tenantId, Rule.RuleState state,
                                                String codePattern, String namePattern, Pageable pageable) {
        // Manual filtering
        List<RuleJpaEntity> all = repository.findAll().stream()
                .filter(e -> tenantId.equals(e.getTenantId()))
                .filter(e -> state == null || state.name().equals(e.getState()))
                .filter(e -> codePattern == null ||
                        (e.getCode() != null && e.getCode().toLowerCase().contains(codePattern.toLowerCase())))
                .filter(e -> namePattern == null ||
                        (e.getName() != null && e.getName().toLowerCase().contains(namePattern.toLowerCase())))
                .collect(Collectors.toList());
        return convertToPage(all, pageable);
    }

    @Override
    public List<Rule> findByTenantIdOrderByUpdatedAtDesc(String tenantId) {
        return repository.findAll().stream()
                .filter(e -> tenantId.equals(e.getTenantId()))
                .sorted((e1, e2) -> e2.getUpdatedAt().compareTo(e1.getUpdatedAt()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTenantIdAndCode(String tenantId, String code) {
        return repository.findAll().stream()
                .anyMatch(e -> tenantId.equals(e.getTenantId()) && code.equals(e.getCode()));
    }

    @Override
    public long countByTenantIdAndState(String tenantId, Rule.RuleState state) {
        return repository.findAll().stream()
                .filter(e -> tenantId.equals(e.getTenantId()) && state.name().equals(e.getState()))
                .count();
    }

    @Override
    public List<Rule> findByTenantIdAndStateNot(String tenantId, Rule.RuleState state) {
        return repository.findAll().stream()
                .filter(e -> tenantId.equals(e.getTenantId()) && !state.name().equals(e.getState()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Rule> findByState(Rule.RuleState state) {
        return repository.findAll().stream()
                .filter(e -> state.name().equals(e.getState()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Rule> findByType(String type) {
        return repository.findByType(type).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Rule> findByRuleSetId(String ruleSetId) {
        return repository.findByRuleSetId(ruleSetId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Rule> findByCampaignId(String campaignId) {
        return repository.findByCampaignId(campaignId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Rule> findByPriorityBetween(int minPriority, int maxPriority) {
        return repository.findByPriorityBetween(minPriority, maxPriority).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
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
