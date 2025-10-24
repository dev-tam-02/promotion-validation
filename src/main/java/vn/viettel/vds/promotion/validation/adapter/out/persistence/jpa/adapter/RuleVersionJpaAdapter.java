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
    public Optional<RuleVersion> findByRuleIdAndVersion(String ruleId, Integer version) {
        return repository.findByRuleIdAndRuleVersion(ruleId, version)
                .map(mapper::toDomain);
    }

    @Override
    public List<RuleVersion> findByRuleIdOrderByVersionDesc(String ruleId) {
        return repository.findByRuleIdOrderByRuleVersionDesc(ruleId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RuleVersion> findFirstByRuleIdOrderByVersionDesc(String ruleId) {
        List<RuleVersionEntity> versions = repository.findByRuleIdOrderByRuleVersionDesc(ruleId);
        return versions.stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public Page<RuleVersion> findByRuleId(String ruleId, Pageable pageable) {
        // Manual pagination
        List<RuleVersionEntity> all = repository.findByRuleIdOrderByRuleVersionDesc(ruleId);
        return convertToPage(all, pageable);
    }

    @Override
    public Optional<RuleVersion> findByBundleHash(String bundleHash) {
        return repository.findByBundleHash(bundleHash).map(mapper::toDomain);
    }

    @Override
    public List<RuleVersion> findByCodeOrderByVersionDesc(String code) {
        // JPA repo doesn't have this exact method - filter manually
        return repository.findAll().stream()
                .filter(e -> e.getCode().equals(code))
                .sorted((e1, e2) -> e2.getRuleVersion().compareTo(e1.getRuleVersion()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RuleVersion> findTopVersionByRuleId(String ruleId) {
        return findFirstByRuleIdOrderByVersionDesc(ruleId);
    }

    @Override
    public List<RuleVersion> findByOperatorsFingerprint(String operatorsFingerprint) {
        // JPA doesn't have this query - filter manually
        return repository.findAll().stream()
                .filter(e -> operatorsFingerprint.equals(e.getOperatorsFingerprint()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByRuleId(String ruleId) {
        return repository.findByRuleIdOrderByRuleVersionDesc(ruleId).size();
    }

    @Override
    public Integer findMaxVersionByRuleId(String ruleId) {
        return repository.findMaxVersionByRuleId(ruleId);
    }

    @Override
    public boolean existsByRuleIdAndVersion(String ruleId, Integer version) {
        return repository.existsByRuleIdAndRuleVersion(ruleId, version);
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
                .toList();
        return new PageImpl<>(pageContent, pageable, entities.size());
    }
}
