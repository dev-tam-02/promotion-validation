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
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for rule history — listing versions and restoring a rule to a prior version.
 *
 * <p>The restore operation:
 * <ol>
 *   <li>Loads the history entry for the target version.</li>
 *   <li>Applies the snapshotted DSL back to the current rule.</li>
 *   <li>Records a new RESTORE history entry capturing the pre-restore state.</li>
 *   <li>Increments {@code ruleVersion} and saves.</li>
 *   <li>Recompiles the DRL and re-registers with pp-rule-engine, updating
 *       {@code bundleHash}. Without this step the runtime evaluation continues
 *       executing the pre-restore bundle.</li>
 * </ol>
 */
@Service
public class RuleHistoryService {

    private static final Logger log = LoggerFactory.getLogger(RuleHistoryService.class);

    private final RulePersistencePort rulePort;
    private final RuleHistoryPersistencePort historyPort;
    private final RuleManagementService ruleManagementService;

    public RuleHistoryService(RulePersistencePort rulePort,
                              RuleHistoryPersistencePort historyPort,
                              RuleManagementService ruleManagementService) {
        this.rulePort = rulePort;
        this.historyPort = historyPort;
        this.ruleManagementService = ruleManagementService;
    }

    /**
     * Return all history entries for a rule, ordered by version ascending.
     *
     * @param ruleId rule identifier
     * @return list of history entries (may be empty if no history recorded yet)
     */
    @Transactional(readOnly = true)
    public List<RuleHistoryEntry> getHistory(String ruleId) {
        if (!rulePort.existsById(ruleId)) {
            throw new RuleNotFoundException(ruleId);
        }
        return historyPort.findByRuleId(ruleId);
    }

    /**
     * Restore a rule to the DSL snapshot captured at {@code targetVersion}.
     *
     * <p>After applying the snapshot DSL, the rule is recompiled and re-registered
     * with pp-rule-engine so that the runtime evaluation immediately uses the
     * restored logic. The {@code bundleHash} on the returned rule reflects the
     * newly compiled bundle, not the stale pre-restore hash.
     *
     * @param ruleId        rule to restore
     * @param targetVersion version number to restore to
     * @param restoredBy    actor performing the restore
     * @return the updated rule after restore and recompile
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

        // Record pre-restore state as a RESTORE history entry
        long nextVersion = (current.getRuleVersion() != null ? current.getRuleVersion() : 1L) + 1L;
        RuleHistoryEntry preRestoreEntry = new RuleHistoryEntry(
                UUID.randomUUID().toString(),
                ruleId,
                current.getRuleVersion() != null ? current.getRuleVersion() : 1L,
                RuleHistoryEntry.ChangeType.RESTORE,
                restoredBy,
                Instant.now(),
                current.getDsl(),
                current.getBundleHash(),
                current.getState() != null ? current.getState().name() : null,
                "pre-restore snapshot before restoring to version " + targetVersion
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

        // Recompile DRL and re-register with pp-rule-engine so bundleHash reflects the
        // restored logic. Without this, the runtime evaluation executes the pre-restore bundle.
        List<RuleNode> nodes = saved.getNodes() != null ? saved.getNodes() : List.of();
        Rule recompiled = ruleManagementService.compilePipelineAndSave(saved, nodes, restoredBy);

        log.info("restore: rule {} restored to version {}, new version={}, bundleHash={}",
                ruleId, targetVersion, nextVersion, recompiled.getBundleHash());
        return recompiled;
    }
}
