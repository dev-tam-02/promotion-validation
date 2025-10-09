package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;
import java.util.List;
import java.util.Optional;

public interface RuleVersionPersistencePort {
    RuleVersion save(RuleVersion ruleVersion);
    Optional<RuleVersion> findById(String id);
    Optional<RuleVersion> findFirstByRuleIdOrderByVersionDesc(String ruleId);
    Optional<RuleVersion> findByRuleIdAndVersion(String ruleId, Integer version);
    List<RuleVersion> findByRuleIdOrderByVersionDesc(String ruleId);
    Page<RuleVersion> findByRuleId(String ruleId, Pageable pageable);
    Optional<RuleVersion> findByBundleHash(String bundleHash);
    List<RuleVersion> findByCodeOrderByVersionDesc(String code);
    Optional<RuleVersion> findTopVersionByRuleId(String ruleId);
    List<RuleVersion> findByOperatorsFingerprint(String operatorsFingerprint);
    long countByRuleId(String ruleId);
    Integer findMaxVersionByRuleId(String ruleId);
    boolean existsByRuleIdAndVersion(String ruleId, Integer version);
    void delete(RuleVersion ruleVersion);
    void deleteById(String id);
}
