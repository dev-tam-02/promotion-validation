package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for RuleVersion persistence operations.
 */
public interface RuleVersionPersistencePort {

    RuleVersion save(RuleVersion ruleVersion);

    Optional<RuleVersion> findById(String id);

    Optional<RuleVersion> findByTenantIdAndRuleIdAndVersion(String tenantId, String ruleId, Integer version);

    List<RuleVersion> findByTenantIdAndRuleIdOrderByVersionDesc(String tenantId, String ruleId);

    Optional<RuleVersion> findFirstByTenantIdAndRuleIdOrderByVersionDesc(String tenantId, String ruleId);

    Page<RuleVersion> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable);

    Optional<RuleVersion> findByTenantIdAndBundleHash(String tenantId, String bundleHash);

    List<RuleVersion> findByTenantIdAndCodeOrderByVersionDesc(String tenantId, String code);

    Optional<RuleVersion> findTopVersionByTenantIdAndRuleId(String tenantId, String ruleId);

    List<RuleVersion> findByTenantIdAndOperatorsFingerprint(String tenantId, String operatorsFingerprint);

    long countByTenantIdAndRuleId(String tenantId, String ruleId);

    Integer findMaxVersionByRuleId(String tenantId, String ruleId);

    boolean existsByTenantIdAndRuleIdAndVersion(String tenantId, String ruleId, Integer version);

    void delete(RuleVersion ruleVersion);

    void deleteById(String id);
}
