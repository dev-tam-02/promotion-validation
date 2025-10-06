package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleVersionEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleVersionEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleVersionJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConditionalOnPromixJpa
public class RuleVersionJpaAdapter implements RuleVersionPersistencePort {

    private final RuleVersionJpaRepository repository;
    private final RuleVersionEntityMapper mapper;

    public RuleVersionJpaAdapter(RuleVersionJpaRepository repository,
                                 RuleVersionEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RuleVersion save(RuleVersion ruleVersion) {
        RuleVersionEntity entity = mapper.toEntity(ruleVersion);
        RuleVersionEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RuleVersion> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<RuleVersion> findByTenantIdAndRuleIdAndVersion(String tenantId, String ruleId, Integer version) {
        return repository.findByTenantIdAndRuleIdAndRuleVersion(tenantId, ruleId, version)
                .map(mapper::toDomain);
    }

    @Override
    public List<RuleVersion> findByTenantIdAndRuleIdOrderByVersionDesc(String tenantId, String ruleId) {
        return repository.findByTenantIdAndRuleIdOrderByRuleVersionDesc(tenantId, ruleId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RuleVersion> findFirstByTenantIdAndRuleIdOrderByVersionDesc(String tenantId, String ruleId) {
        List<RuleVersionEntity> versions = repository.findByTenantIdAndRuleIdOrderByRuleVersionDesc(tenantId, ruleId);
        return versions.stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public Page<RuleVersion> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable) {
        // Manual pagination
        List<RuleVersionEntity> all = repository.findByTenantIdAndRuleIdOrderByRuleVersionDesc(tenantId, ruleId);
        return convertToPage(all, pageable);
    }

    @Override
    public Optional<RuleVersion> findByTenantIdAndBundleHash(String tenantId, String bundleHash) {
        return repository.findByBundleHash(tenantId, bundleHash).map(mapper::toDomain);
    }

    @Override
    public List<RuleVersion> findByTenantIdAndCodeOrderByVersionDesc(String tenantId, String code) {
        // JPA repo doesn't have this exact method - filter manually
        return repository.findAll().stream()
                .filter(e -> e.getTenantId().equals(tenantId) && e.getCode().equals(code))
                .sorted((e1, e2) -> e2.getRuleVersion().compareTo(e1.getRuleVersion()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RuleVersion> findTopVersionByTenantIdAndRuleId(String tenantId, String ruleId) {
        return findFirstByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId);
    }

    @Override
    public List<RuleVersion> findByTenantIdAndOperatorsFingerprint(String tenantId, String operatorsFingerprint) {
        // JPA doesn't have this query - filter manually
        return repository.findAll().stream()
                .filter(e -> e.getTenantId().equals(tenantId) &&
                        operatorsFingerprint.equals(e.getOperatorsFingerprint()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByTenantIdAndRuleId(String tenantId, String ruleId) {
        return repository.findByTenantIdAndRuleIdOrderByRuleVersionDesc(tenantId, ruleId).size();
    }

    @Override
    public Integer findMaxVersionByRuleId(String tenantId, String ruleId) {
        return repository.findMaxVersionByRuleId(tenantId, ruleId);
    }

    @Override
    public boolean existsByTenantIdAndRuleIdAndVersion(String tenantId, String ruleId, Integer version) {
        return repository.existsByTenantIdAndRuleIdAndRuleVersion(tenantId, ruleId, version);
    }

    @Override
    public void delete(RuleVersion ruleVersion) {
        repository.deleteById(ruleVersion.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private Page<RuleVersion> convertToPage(List<RuleVersionEntity> entities, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), entities.size());
        List<RuleVersion> pageContent = entities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
