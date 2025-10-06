package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Rule persistence operations.
 */
public interface RulePersistencePort {

    Rule save(Rule rule);

    Optional<Rule> findById(String id);

    Optional<Rule> findByTenantIdAndCode(String tenantId, String code);

    Page<Rule> findByTenantIdAndState(String tenantId, Rule.RuleState state, Pageable pageable);

    Page<Rule> findByTenantIdWithFilters(String tenantId, Rule.RuleState state,
                                         String codePattern, String namePattern, Pageable pageable);

    List<Rule> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

    boolean existsByTenantIdAndCode(String tenantId, String code);

    long countByTenantIdAndState(String tenantId, Rule.RuleState state);

    List<Rule> findByTenantIdAndStateNot(String tenantId, Rule.RuleState state);

    List<Rule> findByState(Rule.RuleState state);

    List<Rule> findByType(String type);

    List<Rule> findByRuleSetId(String ruleSetId);

    List<Rule> findByCampaignId(String campaignId);

    List<Rule> findByPriorityBetween(int minPriority, int maxPriority);

    List<Rule> findByTargetSegmentsContaining(String segment);

    Page<Rule> findAll(Pageable pageable);

    boolean existsById(String id);

    void delete(Rule rule);

    void deleteById(String id);
}
