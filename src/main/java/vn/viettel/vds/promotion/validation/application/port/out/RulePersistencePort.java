package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Outbound port for Rule persistence operations.
 */
public interface RulePersistencePort {

    Rule save(Rule rule);

    Optional<Rule> findById(String id);

    Optional<Rule> findByCode(String code);

    Page<Rule> findByState(Rule.RuleState state, Pageable pageable);

    Page<Rule> findWithFilters(Rule.RuleState state,
                               String codePattern, String namePattern, Pageable pageable);

    List<Rule> findAllOrderByUpdatedAtDesc();

    boolean existsByCode(String code);

    /**
     * Check whether a rule with the exact given name already exists (case- and
     * accent-sensitive, cs_as). Used by the create-screen duplicate-name guard.
     */
    boolean existsByName(String name);

    long countByState(Rule.RuleState state);

    List<Rule> findByStateNot(Rule.RuleState state);

    List<Rule> findByType(String type);

    List<Rule> findByRuleSetId(String ruleSetId);

    List<Rule> findByCampaignId(String campaignId);

    List<Rule> findByPriorityBetween(int minPriority, int maxPriority);

    List<Rule> findByTargetSegmentsContaining(String segment);

    Page<Rule> findAll(Pageable pageable);

    boolean existsById(String id);

    void delete(Rule rule);

    void deleteById(String id);

    void deleteNodesByRuleId(String ruleId);

    /**
     * Find PUBLISHED rules that have no compiled bundle hash yet.
     * Used by the bootstrap runner to populate bundle_hash for seeded system rules.
     */
    List<Rule> findPublishedWithNullBundleHash();

    /**
     * Bulk count nodes for a set of rule ids in a single query. Returned map only
     * contains ids that have ≥1 node; callers must default missing keys to 0.
     */
    Map<String, Integer> countNodesByRuleIds(Collection<String> ruleIds);
}
