package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.RuleHistoryEntry;

import java.util.List;
import java.util.Optional;

/**
 * Outbound persistence port for the rule version history audit trail.
 *
 * <p>Implementations write to {@code validation.validation_rules_history}.
 */
public interface RuleHistoryPersistencePort {

    /**
     * Persist a new history entry (INSERT only — history is append-only).
     *
     * @param entry the history entry to save
     */
    void save(RuleHistoryEntry entry);

    /**
     * Return all history entries for a given rule, ordered by {@code ruleVersion} ascending.
     *
     * @param ruleId rule identifier
     * @return list of entries, earliest version first
     */
    List<RuleHistoryEntry> findByRuleId(String ruleId);

    /**
     * Return the history entry for a specific rule version.
     *
     * @param ruleId      rule identifier
     * @param ruleVersion exact version number
     * @return the matching entry, if any
     */
    Optional<RuleHistoryEntry> findByRuleIdAndVersion(String ruleId, long ruleVersion);
}
