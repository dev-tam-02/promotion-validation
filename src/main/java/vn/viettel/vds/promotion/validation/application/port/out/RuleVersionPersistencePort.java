package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;
import java.util.List;
import java.util.Optional;

public interface RuleVersionPersistencePort {
    RuleVersion save(RuleVersion ruleVersion);
    Optional<RuleVersion> findById(String id);
    Optional<RuleVersion> findFirstByRuleIdOrderByVersionDesc(String ruleId);
    Optional<RuleVersion> findByRuleIdAndVersion(String ruleId, Integer version);
    List<RuleVersion> findByRuleIdOrderByVersionDesc(String ruleId);
    void delete(RuleVersion ruleVersion);
}
