package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(RuleVersionService.class);

    private final RuleVersionPersistencePort ruleVersionPersistencePort;

    public RuleVersionService(RuleVersionPersistencePort ruleVersionPersistencePort) {
        this.ruleVersionPersistencePort = ruleVersionPersistencePort;
    }

    /**
     * Get rule version by rule ID and version number
     */
    public RuleVersion getRuleVersion(String ruleId, Integer version) {
        // Extract tenant ID from rule ID (assuming format: rul_tenant_code)
        String tenantId = extractTenantIdFromRuleId(ruleId);

        return ruleVersionPersistencePort.findByTenantIdAndRuleIdAndVersion(tenantId, ruleId, version)
                .orElseThrow(() -> new ResourceNotFoundException());
    }

    /**
     * Get latest version of a rule
     */
    public Optional<RuleVersion> getLatestRuleVersion(String ruleId) {
        String tenantId = extractTenantIdFromRuleId(ruleId);
        return ruleVersionPersistencePort.findFirstByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId);
    }

    /**
     * Get all versions of a rule
     */
    public List<RuleVersion> getAllRuleVersions(String ruleId) {
        String tenantId = extractTenantIdFromRuleId(ruleId);
        return ruleVersionPersistencePort.findByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId);
    }

    /**
     * Get rule versions with pagination
     */
    public Page<RuleVersion> getRuleVersions(String ruleId, Pageable pageable) {
        String tenantId = extractTenantIdFromRuleId(ruleId);
        return ruleVersionPersistencePort.findByTenantIdAndRuleId(tenantId, ruleId, pageable);
    }

    /**
     * Get rule version by bundle hash
     */
    public Optional<RuleVersion> getRuleVersionByBundleHash(String tenantId, String bundleHash) {
        return ruleVersionPersistencePort.findByTenantIdAndBundleHash(tenantId, bundleHash);
    }

    /**
     * Get rule versions by code and tenant
     */
    public List<RuleVersion> getRuleVersionsByCode(String tenantId, String code) {
        return ruleVersionPersistencePort.findByTenantIdAndCodeOrderByVersionDesc(tenantId, code);
    }

    /**
     * Get next version number for a rule
     */
    public Integer getNextVersionNumber(String ruleId) {
        String tenantId = extractTenantIdFromRuleId(ruleId);
        Optional<RuleVersion> latest = ruleVersionPersistencePort.findFirstByTenantIdAndRuleIdOrderByVersionDesc(tenantId, ruleId);
        return latest.map(rv -> rv.getRuleVersion() != null ? rv.getRuleVersion() + 1 : 1).orElse(1);
    }

    /**
     * Check if a specific version exists
     */
    public boolean versionExists(String ruleId, Integer version) {
        String tenantId = extractTenantIdFromRuleId(ruleId);
        return ruleVersionPersistencePort.findByTenantIdAndRuleIdAndVersion(tenantId, ruleId, version).isPresent();
    }

    /**
     * Count versions for a rule
     */
    public long countVersions(String ruleId) {
        String tenantId = extractTenantIdFromRuleId(ruleId);
        return ruleVersionPersistencePort.countByTenantIdAndRuleId(tenantId, ruleId);
    }

    /**
     * Find rule versions with specific operators fingerprint
     */
    public List<RuleVersion> findVersionsWithOperatorsFingerprint(String tenantId, String operatorsFingerprint) {
        return ruleVersionPersistencePort.findByTenantIdAndOperatorsFingerprint(tenantId, operatorsFingerprint);
    }

    /**
     * Compare two rule versions and return differences
     */
    public VersionDiff compareVersions(String ruleId, Integer fromVersion, Integer toVersion) {
        RuleVersion from = getRuleVersion(ruleId, fromVersion);
        RuleVersion to = getRuleVersion(ruleId, toVersion);

        return VersionDiff.compare(from, to);
    }

    private String extractTenantIdFromRuleId(String ruleId) {
        // Assuming rule ID format: rul_tenant_code
        if (ruleId != null && ruleId.startsWith("rul_")) {
            String[] parts = ruleId.split("_");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        throw new IllegalArgumentException("Invalid rule ID format: " + ruleId);
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