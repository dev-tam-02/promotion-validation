package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.adapter;

import com.promix.platform.autoconfigure.mongo.condition.ConditionalOnPromixMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.repository.RuleVersionRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort;
import vn.viettel.vds.promotion.validation.domain.entity.RuleVersion;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixMongo
public class RuleVersionMongoAdapter implements RuleVersionPersistencePort {

    private final RuleVersionRepository repository;

    public RuleVersionMongoAdapter(RuleVersionRepository repository) {
        this.repository = repository;
    }

    @Override
    public RuleVersion save(RuleVersion ruleVersion) {
        return repository.save(ruleVersion);
    }

    @Override
    public Optional<RuleVersion> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<RuleVersion> findByTenantIdAndRuleIdAndVersion(String tenantId, String ruleId, Integer version) {
        return repository.findByTenantIdAndRuleIdAndVersion(tenantId, ruleId, version);
    }

    @Override
    public List<RuleVersion> findByTenantIdAndRuleIdOrderByVersionDesc(String tenantId, String ruleId) {
        return repository.findByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId);
    }

    @Override
    public Optional<RuleVersion> findFirstByTenantIdAndRuleIdOrderByVersionDesc(String tenantId, String ruleId) {
        return repository.findFirstByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId);
    }

    @Override
    public Page<RuleVersion> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable) {
        return repository.findByTenantIdAndRuleId(tenantId, ruleId, pageable);
    }

    @Override
    public Optional<RuleVersion> findByTenantIdAndBundleHash(String tenantId, String bundleHash) {
        return repository.findByTenantIdAndBundleHash(tenantId, bundleHash);
    }

    @Override
    public List<RuleVersion> findByTenantIdAndCodeOrderByVersionDesc(String tenantId, String code) {
        return repository.findByTenantIdAndCodeOrderByVersionDesc(tenantId, code);
    }

    @Override
    public Optional<RuleVersion> findTopVersionByTenantIdAndRuleId(String tenantId, String ruleId) {
        return repository.findTopVersionByTenantIdAndRuleId(tenantId, ruleId);
    }

    @Override
    public List<RuleVersion> findByTenantIdAndOperatorsFingerprint(String tenantId, String operatorsFingerprint) {
        return repository.findByTenantIdAndOperatorsFingerprint(tenantId, operatorsFingerprint);
    }

    @Override
    public long countByTenantIdAndRuleId(String tenantId, String ruleId) {
        return repository.countByTenantIdAndRuleId(tenantId, ruleId);
    }

    @Override
    public Integer findMaxVersionByRuleId(String tenantId, String ruleId) {
        // MongoDB doesn't have this exact method, use findFirst instead
        return findFirstByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId)
                .map(RuleVersion::getVersion)
                .orElse(0);
    }

    @Override
    public boolean existsByTenantIdAndRuleIdAndVersion(String tenantId, String ruleId, Integer version) {
        return findByTenantIdAndRuleIdAndVersion(tenantId, ruleId, version).isPresent();
    }

    @Override
    public void delete(RuleVersion ruleVersion) {
        repository.delete(ruleVersion);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
