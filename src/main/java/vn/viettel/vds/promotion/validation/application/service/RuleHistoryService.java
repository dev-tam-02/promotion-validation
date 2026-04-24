package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleHistoryPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.RuleNotFoundException;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleHistoryEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Application service for rule history — listing versions and restoring a rule to a prior version.
 *
 * <p>The restore operation:
 * <ol>
 *   <li>Loads the history entry for the target version.</li>
 *   <li>Applies the snapshotted DSL back to the current rule.</li>
 *   <li>Records a new UPDATE history entry capturing the pre-restore state.</li>
 *   <li>Increments {@code ruleVersion} and saves.</li>
 * </ol>
 */
@Service
public class RuleHistoryService {

    private static final Logger log = LoggerFactory.getLogger(RuleHistoryService.class);

    private final RulePersistencePort rulePort;
    private final RuleHistoryPersistencePort historyPort;

    public RuleHistoryService(RulePersistencePort rulePort, RuleHistoryPersistencePort historyPort) {
        this.rulePort = rulePort;
        this.historyPort = historyPort;
    }

    /**
     * Return all history entries for a rule, ordered by version ascending.
     *
     * @param ruleId rule identifier
     * @return list of history entries (may be empty if no history recorded yet)
     */
    @Transactional(readOnly = true)
    public List<RuleHistoryEntry> getHistory(String ruleId) {
        rulePort.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        return historyPort.findByRuleId(ruleId);
    }

    /**
     * Restore a rule to the DSL snapshot captured at {@code targetVersion}.
     *
     * @param ruleId        rule to restore
     * @param targetVersion version number to restore to
     * @param restoredBy    actor performing the restore
     * @return the updated rule after restore
     */
    @Transactional
    public Rule restore(String ruleId, long targetVersion, String restoredBy) {
        log.info("restore: ruleId={}, targetVersion={}, by={}", ruleId, targetVersion, restoredBy);

        Rule current = rulePort.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));

        Optional<RuleHistoryEntry> targetEntry = historyPort.findByRuleIdAndVersion(ruleId, targetVersion);
        if (targetEntry.isEmpty()) {
            throw new IllegalArgumentException(
                    "No history entry found for ruleId=" + ruleId + " at version=" + targetVersion);
        }

        RuleHistoryEntry snapshot = targetEntry.get();

        // Record pre-restore state as an UPDATE history entry
        long nextVersion = (current.getRuleVersion() != null ? current.getRuleVersion() : 1L) + 1L;
        RuleHistoryEntry preRestoreEntry = new RuleHistoryEntry(
                java.util.UUID.randomUUID().toString(),
                ruleId,
                current.getRuleVersion() != null ? current.getRuleVersion() : 1L,
                RuleHistoryEntry.ChangeType.UPDATE,
                restoredBy,
                Instant.now(),
                current.getDsl(),
                current.getBundleHash(),
                current.getState() != null ? current.getState().name() : null
        );
        historyPort.save(preRestoreEntry);

        // Apply the snapshot DSL to the rule and bump version
        Rule restored = current.toBuilder()
                .dsl(snapshot.getDslSnapshot())
                .ruleVersion(nextVersion)
                .updatedAt(Instant.now())
                .updatedBy(restoredBy)
                .build();

        Rule saved = rulePort.save(restored);
        log.info("restore: rule {} restored to version {}, new version={}", ruleId, targetVersion, nextVersion);
        return saved;
    }
}
