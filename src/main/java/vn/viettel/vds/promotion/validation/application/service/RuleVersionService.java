package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RuleVersionService {

    private final RuleVersionPersistencePort ruleVersionPersistencePort;

    public RuleVersionService(RuleVersionPersistencePort ruleVersionPersistencePort) {
        this.ruleVersionPersistencePort = ruleVersionPersistencePort;
    }

    /**
     * Get rule version by rule ID and version number
     */
    public RuleVersion getRuleVersion(String ruleId, Integer version) {
        return ruleVersionPersistencePort.findByRuleIdAndVersion(ruleId, version)
                .orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * Get latest version of a rule
     */
    public Optional<RuleVersion> getLatestRuleVersion(String ruleId) {
        return ruleVersionPersistencePort.findFirstByRuleIdOrderByVersionDesc(ruleId);
    }

    /**
     * Get all versions of a rule
     */
    public List<RuleVersion> getAllRuleVersions(String ruleId) {
        return ruleVersionPersistencePort.findByRuleIdOrderByVersionDesc(ruleId);
    }

    /**
     * Get rule versions with pagination
     */
    public Page<RuleVersion> getRuleVersions(String ruleId, Pageable pageable) {
        return ruleVersionPersistencePort.findByRuleId(ruleId, pageable);
    }

    /**
     * Get rule version by bundle hash
     */
    public Optional<RuleVersion> getRuleVersionByBundleHash(String bundleHash) {
        return ruleVersionPersistencePort.findByBundleHash(bundleHash);
    }

    /**
     * Get rule versions by code
     */
    public List<RuleVersion> getRuleVersionsByCode(String code) {
        return ruleVersionPersistencePort.findByCodeOrderByVersionDesc(code);
    }

    /**
     * Get next version number for a rule
     */
    public Integer getNextVersionNumber(String ruleId) {
        Optional<RuleVersion> latest = ruleVersionPersistencePort.findFirstByRuleIdOrderByVersionDesc(ruleId);
        return latest.map(rv -> rv.getVersion() != null ? rv.getVersion() + 1 : 1).orElse(1);
    }

    /**
     * Check if a specific version exists
     */
    public boolean versionExists(String ruleId, Integer version) {
        return ruleVersionPersistencePort.findByRuleIdAndVersion(ruleId, version).isPresent();
    }

    /**
     * Count versions for a rule
     */
    public long countVersions(String ruleId) {
        return ruleVersionPersistencePort.countByRuleId(ruleId);
    }

    /**
     * Find rule versions with specific operators fingerprint
     */
    public List<RuleVersion> findVersionsWithOperatorsFingerprint(String operatorsFingerprint) {
        return ruleVersionPersistencePort.findByOperatorsFingerprint(operatorsFingerprint);
    }

    /**
     * Compare two rule versions and return differences
     */
    public VersionDiff compareVersions(String ruleId, Integer fromVersion, Integer toVersion) {
        RuleVersion from = getRuleVersion(ruleId, fromVersion);
        RuleVersion to = getRuleVersion(ruleId, toVersion);

        return VersionDiff.compare(from, to);
    }

    /**
     * Version comparison result
     */
    public static class VersionDiff {
        private final boolean nodesDifferent;
        private final boolean limitsDifferent;
        private final boolean timeLinksDifferent;
        private final boolean logicDifferent;
        private final List<String> changes;

        private VersionDiff(boolean nodesDifferent, boolean limitsDifferent,
                            boolean timeLinksDifferent, boolean logicDifferent, List<String> changes) {
            this.nodesDifferent = nodesDifferent;
            this.limitsDifferent = limitsDifferent;
            this.timeLinksDifferent = timeLinksDifferent;
            this.logicDifferent = logicDifferent;
            this.changes = changes;
        }

        public static VersionDiff compare(RuleVersion from, RuleVersion to) {
            List<String> changes = new java.util.ArrayList<>();

            boolean nodesDifferent = !java.util.Objects.equals(from.getNodes(), to.getNodes());
            if (nodesDifferent) {
                changes.add("Rule nodes modified");
            }

            boolean limitsDifferent = !java.util.Objects.equals(from.getLimits(), to.getLimits());
            if (limitsDifferent) {
                changes.add("Rule limits modified");
            }

            // Note: timeLinks field is not present in RuleVersion model
            boolean timeLinksDifferent = false;

            boolean logicDifferent = !java.util.Objects.equals(from.getLogic(), to.getLogic());
            if (logicDifferent) {
                changes.add("Root logic modified");
            }

            return new VersionDiff(nodesDifferent, limitsDifferent, timeLinksDifferent, logicDifferent, changes);
        }

        // Getters
        public boolean isNodesDifferent() {
            return nodesDifferent;
        }

        public boolean isLimitsDifferent() {
            return limitsDifferent;
        }

        public boolean isTimeLinksDifferent() {
            return timeLinksDifferent;
        }

        public boolean isLogicDifferent() {
            return logicDifferent;
        }

        public List<String> getChanges() {
            return changes;
        }

        public boolean hasDifferences() {
            return nodesDifferent || limitsDifferent || timeLinksDifferent || logicDifferent;
        }
    }
}