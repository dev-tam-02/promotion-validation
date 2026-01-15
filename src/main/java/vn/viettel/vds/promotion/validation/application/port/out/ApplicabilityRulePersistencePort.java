package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.ApplicabilityRule;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for ApplicabilityRule persistence operations.
 */
public interface ApplicabilityRulePersistencePort {

    ApplicabilityRule save(ApplicabilityRule applicabilityRule);

    List<ApplicabilityRule> saveAll(List<ApplicabilityRule> applicabilityRules);

    Optional<ApplicabilityRule> findById(String id);

    List<ApplicabilityRule> findByAssignmentId(String assignmentId);

    List<ApplicabilityRule> findByAssignmentIdAndRuleType(String assignmentId, ApplicabilityRule.RuleType ruleType);

    void deleteByAssignmentId(String assignmentId);

    void delete(ApplicabilityRule applicabilityRule);

    void deleteById(String id);

    long countByAssignmentId(String assignmentId);
}
