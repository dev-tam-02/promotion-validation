package vn.viettel.vds.promotion.validation.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable domain value object representing a single version snapshot in the
 * {@code validation_rules_history} audit trail.
 *
 * <p>One entry is recorded:
 * <ul>
 *   <li>When a rule is first created ({@code changeType = CREATE}).</li>
 *   <li>Before each update — capturing the state BEFORE the change
 *       ({@code changeType = UPDATE}).</li>
 *   <li>Before a restore — capturing the pre-restore state
 *       ({@code changeType = RESTORE}).</li>
 *   <li>When a rule is archived ({@code changeType = ARCHIVE}).</li>
 * </ul>
 *
 * <p>These entries enable the {@code GET /v1/rules/{id}/history} listing and
 * the {@code POST /v1/rules/{id}/restore/{version}} rollback flow.
 */
public class RuleHistoryEntry {

    private final String id;
    private final String ruleId;
    private final long ruleVersion;
    private final ChangeType changeType;
    private final String changedBy;
    private final Instant changedAt;
    private final Map<String, Object> dslSnapshot;
    private final String bundleHash;
    private final String state;
    private final String changeReason;
    @SuppressWarnings("java:S107")
    public RuleHistoryEntry(String id,
                            String ruleId,
                            long ruleVersion,
                            ChangeType changeType,
                            String changedBy,
                            Instant changedAt,
                            Map<String, Object> dslSnapshot,
                            String bundleHash,
                            String state,
                            String changeReason) {
        this.id = id;
        this.ruleId = ruleId;
        this.ruleVersion = ruleVersion;
        this.changeType = changeType;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.dslSnapshot = dslSnapshot;
        this.bundleHash = bundleHash;
        this.state = state;
        this.changeReason = changeReason;
    }

    public String getId() {
        return id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public long getRuleVersion() {
        return ruleVersion;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public Map<String, Object> getDslSnapshot() {
        return dslSnapshot;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public String getState() {
        return state;
    }

    public String getChangeReason() {
        return changeReason;
    }

    /**
     * Type of change that produced this history entry.
     */
    public enum ChangeType {
        /**
         * Rule was created for the first time.
         */
        CREATE,
        /**
         * Rule was updated; this entry captures the state BEFORE the update.
         */
        UPDATE,
        /**
         * Rule was restored to a prior version; this entry captures the pre-restore state.
         */
        RESTORE,
        /**
         * Rule was archived.
         */
        ARCHIVE
    }
}
