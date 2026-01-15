package vn.viettel.vds.promotion.validation.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for rule versioning operations.
 * <p>
 * NOTE: This service has dependencies on adapters and ports, so it should ideally
 * be located in application/service/ layer. It is kept here temporarily for
 * backward compatibility. Future refactoring should move this to application layer.
 * </p>
 * <p>
 * Spring annotations have been removed. Bean wiring is done via DomainServicesConfig.
 * Transaction management should be handled at the use case level.
 * </p>
 */
public class RuleVersioningService {

    private static final Logger logger = LoggerFactory.getLogger(RuleVersioningService.class);

    private final RulePersistencePort rulePersistencePort;
    private final RuleVersionPersistencePort ruleVersionPersistencePort;
    private final RulePublishingService rulePublishingService;

    public RuleVersioningService(RulePersistencePort rulePersistencePort,
                                 RuleVersionPersistencePort ruleVersionPersistencePort,
                                 RulePublishingService rulePublishingService) {
        this.rulePersistencePort = rulePersistencePort;
        this.ruleVersionPersistencePort = ruleVersionPersistencePort;
        this.rulePublishingService = rulePublishingService;
    }

    public RuleVersionResult createNewVersion(String ruleId) {
        logger.info("Creating new version for rule: ruleId={}", ruleId);

        try {
            Rule currentRule = rulePersistencePort.findByCode(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

            // Create new version
            Rule newVersion = createRuleCopy(currentRule);
            newVersion.setLatestVersion(getNextVersion(ruleId));
            newVersion.setState(Rule.RuleState.DRAFT);
            // Note: bundleHash and publishedAt not available in current entity
            newVersion.setCreatedAt(Instant.now());
            newVersion.setUpdatedAt(Instant.now());

            Rule savedRule = rulePersistencePort.save(newVersion);

            logger.info("New rule version created: ruleId={}, version={}", ruleId, savedRule.getLatestVersion());

            return RuleVersionResult.success(savedRule.getId(), savedRule.getLatestVersion(),
                    "New version created successfully");

        } catch (Exception e) {
            logger.error("Failed to create new version: ruleId={}", ruleId, e);
            return RuleVersionResult.failed(ruleId, null, "Failed to create new version: " + e.getMessage());
        }
    }

    public RuleVersionResult rollbackToVersion(String ruleId, Integer targetVersion) {
        logger.info("Rolling back rule to version: ruleId={}, targetVersion={}",
                ruleId, targetVersion);

        RuleVersion targetRuleVersion = null;
        try {
            // Find current active rule
            Rule currentRule = rulePersistencePort.findByCode(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

            // Find target version
            // Find target version from version repository
            Optional<RuleVersion> targetVersionOpt = ruleVersionPersistencePort.findByRuleIdAndVersion(ruleId, targetVersion);
            targetRuleVersion = targetVersionOpt.orElseThrow(() -> new IllegalArgumentException(
                    "Target version not found: " + ruleId + " v" + targetVersion));

            // Validate rollback
            validateRollback(currentRule, targetRuleVersion);

            // Unpublish current rule if it's active
            if (currentRule.getState() == Rule.RuleState.PUBLISHED) {
                logger.info("Unpublishing current rule before rollback: ruleId={}", ruleId);
                rulePublishingService.unpublishRule(ruleId);
            }

            // Create new version from target
            Rule rolledBackRule = createRuleFromVersion(targetRuleVersion);
            rolledBackRule.setLatestVersion(getNextVersion(ruleId));
            rolledBackRule.setState(Rule.RuleState.DRAFT);
            // Note: bundleHash and publishedAt not available in current entity
            rolledBackRule.setCreatedAt(Instant.now());
            rolledBackRule.setUpdatedAt(Instant.now());

            // Add rollback metadata using notes field
            String rollbackInfo = String.format("Rolled back from version %d to %d at %s",
                    currentRule.getLatestVersion(), targetRuleVersion.getEntityVersion(), Instant.now());
            rolledBackRule.setNotes(rollbackInfo);

            Rule savedRule = rulePersistencePort.save(rolledBackRule);

            logger.info("Rule rolled back successfully: ruleId={}, newVersion={}, rolledBackToVersion={}",
                    ruleId, savedRule.getLatestVersion(), targetRuleVersion.getEntityVersion().intValue());

            return RuleVersionResult.success(savedRule.getId(), savedRule.getLatestVersion(),
                    "Rolled back to version " + targetRuleVersion.getEntityVersion().intValue());

        } catch (Exception e) {
            logger.error("Failed to rollback rule: ruleId={}, targetVersion={}",
                    ruleId, targetVersion, e);
            return RuleVersionResult.failed(ruleId, targetVersion,
                    "Rollback failed: " + e.getMessage());
        }
    }

    public List<RuleVersionInfo> getRuleVersionHistory(String ruleId) {
        logger.debug("Getting version history: ruleId={}", ruleId);

        try {
            // Get all versions for this rule from version repository
            List<RuleVersion> versions = ruleVersionPersistencePort.findByRuleIdOrderByVersionDesc(ruleId);

            return versions.stream()
                    .map(this::convertToVersionInfo)
                    .sorted((v1, v2) -> Integer.compare(v2.getEntityVersion(), v1.getEntityVersion().intValue())) // Descending order
                    .toList();

        } catch (Exception e) {
            logger.error("Failed to get version history: ruleId={}", ruleId, e);
            return List.of();
        }
    }

    public Optional<RuleVersionInfo> getSpecificVersion(String ruleId, Integer version) {
        logger.debug("Getting specific version: ruleId={}, version={}", ruleId, version);

        try {
            return ruleVersionPersistencePort.findByRuleIdAndVersion(ruleId, version)
                    .map(this::convertToVersionInfo);

        } catch (Exception e) {
            logger.error("Failed to get specific version: ruleId={}, version={}",
                    ruleId, version, e);
            return Optional.empty();
        }
    }

    public RuleVersionResult deleteVersion(String ruleId, Integer version) {
        logger.info("Deleting rule version: ruleId={}, version={}", ruleId, version);

        try {
            RuleVersion ruleVersion = ruleVersionPersistencePort.findByRuleIdAndVersion(ruleId, version)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Version not found: " + ruleId + " v" + version));

            // Prevent deletion of active rule
            if (ruleVersion.getPublishedAt() != null) {
                throw new IllegalStateException("Cannot delete published rule version");
            }

            // Check if it's the only version
            List<RuleVersion> allVersions = ruleVersionPersistencePort.findByRuleIdOrderByVersionDesc(ruleId);
            if (allVersions.size() <= 1) {
                throw new IllegalStateException("Cannot delete the only version of a rule");
            }

            ruleVersionPersistencePort.delete(ruleVersion);

            logger.info("Rule version deleted: ruleId={}, version={}", ruleId, version);

            return RuleVersionResult.success(ruleId, version, "Version deleted successfully");

        } catch (Exception e) {
            logger.error("Failed to delete version: ruleId={}, version={}",
                    ruleId, version, e);
            return RuleVersionResult.failed(ruleId, version, "Deletion failed: " + e.getMessage());
        }
    }

    private Rule createRuleCopy(Rule source) {
        Rule copy = new Rule();

        copy.setCode(source.getCode());
        copy.setLogic(source.getLogic());
        copy.setNodes(source.getNodes()); // Deep copy needed for production
        copy.setLimits(copyUsageLimits(source.getLimits()));
        copy.setNotes(source.getNotes());

        return copy;
    }

    private Rule createRuleFromVersion(RuleVersion source) {
        Rule rule = new Rule();

        rule.setCode(source.getCode());
        // Convert RuleVersion.LogicType to Rule.LogicType
        if (source.getLogic() != null) {
            rule.setLogic(Rule.LogicType.valueOf(source.getLogic().name()));
        }
        // Convert List<Map<String, Object>> to List<RuleNode>
        rule.setNodes(convertNodesToRuleNodes(source.getNodes()));
        rule.setLimits(convertMapToUsageLimits(source.getLimits()));
        rule.setNotes("Restored from version " + source.getEntityVersion().intValue());

        return rule;
    }

    private Integer getNextVersion(String ruleId) {
        List<RuleVersion> versions = ruleVersionPersistencePort.findByRuleIdOrderByVersionDesc(ruleId);

        // Also check current rule version
        Optional<Rule> currentRule = rulePersistencePort.findByCode(ruleId);
        int maxVersion = versions.stream()
                .mapToInt(ruleVersion -> ruleVersion.getVersion() != null ? ruleVersion.getVersion() : 0)
                .max()
                .orElse(0);

        if (currentRule.isPresent() && currentRule.get().getLatestVersion() != null) {
            maxVersion = Math.max(maxVersion, currentRule.get().getLatestVersion());
        }

        return maxVersion + 1;
    }

    private void validateRollback(Rule currentRule, RuleVersion targetRuleVersion) {
        if (currentRule.getLatestVersion().equals(targetRuleVersion.getEntityVersion().intValue())) {
            throw new IllegalArgumentException("Cannot rollback to the same version");
        }

        if (!currentRule.getCode().equals(targetRuleVersion.getCode())) {
            throw new IllegalArgumentException("Rules do not match for rollback");
        }
    }

    private Rule.UsageLimits copyUsageLimits(Rule.UsageLimits source) {
        if (source == null) return null;

        return Rule.UsageLimits.builder()
                .perCodeTotal(source.getPerCodeTotal())
                .perCustomer(source.getPerCustomer())
                .perDay(source.getPerDay())
                .perTransaction(source.getPerTransaction())
                .remaining(source.getRemaining())
                .build();
    }

    private Rule.UsageLimits convertMapToUsageLimits(Map<String, Object> limitsMap) {
        if (limitsMap == null) return null;

        return Rule.UsageLimits.builder()
                .perCodeTotal(getIntegerFromMap(limitsMap, "perCodeTotal"))
                .perCustomer(getIntegerFromMap(limitsMap, "perCustomer"))
                .perDay(getIntegerFromMap(limitsMap, "perDay"))
                .perTransaction(getIntegerFromMap(limitsMap, "perTransaction"))
                .remaining(getIntegerFromMap(limitsMap, "remaining"))
                .build();
    }

    private Integer getIntegerFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer integer) return integer;
        if (value instanceof Number number) return number.intValue();
        return null;
    }

    private RuleVersionInfo convertToVersionInfo(RuleVersion ruleVersion) {
        return RuleVersionInfo.builder()
                .ruleId(ruleVersion.getId())
                .version(ruleVersion.getVersion()) // Use ruleVersion (Integer) not version (Long)
                .status(ruleVersion.getPublishedAt() != null ? Rule.RuleState.PUBLISHED : Rule.RuleState.DRAFT)
                .name(ruleVersion.getCode()) // Using code as name
                .description(null) // description not available in RuleVersion
                .bundleHash(ruleVersion.getCompile() != null ? ruleVersion.getCompile().getBundleHash() : null)
                .createdAt(null) // createdAt not available in RuleVersion
                .publishedAt(ruleVersion.getPublishedAt())
                .metadata(ruleVersion.getDsl())
                .build();
    }

    /**
     * Convert List<Map<String, Object>> to List<RuleNode>
     */
    private List<vn.viettel.vds.promotion.validation.domain.model.RuleNode> convertNodesToRuleNodes(List<java.util.Map<String, Object>> nodeMaps) {
        if (nodeMaps == null) {
            return new java.util.ArrayList<>();
        }

        List<vn.viettel.vds.promotion.validation.domain.model.RuleNode> nodes = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> nodeMap : nodeMaps) {
            nodes.add(convertMapToRuleNode(nodeMap));
        }
        return nodes;
    }

    /**
     * Convert a Map to RuleNode
     */
    @SuppressWarnings("unchecked")
    private vn.viettel.vds.promotion.validation.domain.model.RuleNode convertMapToRuleNode(java.util.Map<String, Object> nodeMap) {
        vn.viettel.vds.promotion.validation.domain.model.RuleNode.Builder builder = vn.viettel.vds.promotion.validation.domain.model.RuleNode.builder();

        if (nodeMap.containsKey("id")) {
            builder.nodeId((String) nodeMap.get("id"));
        }
        if (nodeMap.containsKey("field")) {
            builder.field((String) nodeMap.get("field"));
        }
        if (nodeMap.containsKey("operator")) {
            builder.operator((String) nodeMap.get("operator"));
        }
        if (nodeMap.containsKey("value")) {
            builder.value(nodeMap.get("value"));
        }
        if (nodeMap.containsKey("type")) {
            String type = (String) nodeMap.get("type");
            if ("GROUP".equals(type)) {
                builder.type(vn.viettel.vds.promotion.validation.domain.model.RuleNode.NodeType.GROUP);
            } else if ("COND".equals(type)) {
                builder.type(vn.viettel.vds.promotion.validation.domain.model.RuleNode.NodeType.COND);
            }
        }
        if (nodeMap.containsKey("operatorName")) {
            builder.operatorName((String) nodeMap.get("operatorName"));
        }
        if (nodeMap.containsKey("reasonCode")) {
            builder.reasonCode((String) nodeMap.get("reasonCode"));
        }
        if (nodeMap.containsKey("params")) {
            builder.params((java.util.Map<String, Object>) nodeMap.get("params"));
        }

        return builder.build();
    }

    // Result and info classes
    public static class RuleVersionResult {
        private final String ruleId;
        private final Integer version;
        private final boolean success;
        private final String message;

        private RuleVersionResult(String ruleId, Integer version, boolean success, String message) {
            this.ruleId = ruleId;
            this.version = version;
            this.success = success;
            this.message = message;
        }

        public static RuleVersionResult success(String ruleId, Integer version, String message) {
            return new RuleVersionResult(ruleId, version, true, message);
        }

        public static RuleVersionResult failed(String ruleId, Integer version, String message) {
            return new RuleVersionResult(ruleId, version, false, message);
        }

        // Getters
        public String getRuleId() {
            return ruleId;
        }

        public Integer getEntityVersion() {
            return version;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class RuleVersionInfo {
        private final String ruleId;
        private final Integer version;
        private final Rule.RuleState status;
        private final String name;
        private final String description;
        private final String bundleHash;
        private final Instant createdAt;
        private final Instant publishedAt;
        private final java.util.Map<String, Object> metadata;

        // Private constructor used exclusively by Builder pattern to ensure immutability
        @SuppressWarnings("java:S107") // Constructor parameters are managed via Builder pattern
        private RuleVersionInfo(String ruleId, Integer version, Rule.RuleState status, String name,
                                String description, String bundleHash, Instant createdAt,
                                Instant publishedAt, java.util.Map<String, Object> metadata) {
            this.ruleId = ruleId;
            this.version = version;
            this.status = status;
            this.name = name;
            this.description = description;
            this.bundleHash = bundleHash;
            this.createdAt = createdAt;
            this.publishedAt = publishedAt;
            this.metadata = metadata;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getRuleId() {
            return ruleId;
        }

        public Integer getEntityVersion() {
            return version;
        }

        public Rule.RuleState getStatus() {
            return status;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public Instant getPublishedAt() {
            return publishedAt;
        }

        public java.util.Map<String, Object> getMetadata() {
            return metadata;
        }

        public static class Builder {
            private String ruleId;
            private Integer version;
            private Rule.RuleState status;
            private String name;
            private String description;
            private String bundleHash;
            private Instant createdAt;
            private Instant publishedAt;
            private java.util.Map<String, Object> metadata;

            public Builder ruleId(String ruleId) {
                this.ruleId = ruleId;
                return this;
            }

            public Builder version(Integer version) {
                this.version = version;
                return this;
            }

            public Builder status(Rule.RuleState status) {
                this.status = status;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder bundleHash(String bundleHash) {
                this.bundleHash = bundleHash;
                return this;
            }

            public Builder createdAt(Instant createdAt) {
                this.createdAt = createdAt;
                return this;
            }

            public Builder publishedAt(Instant publishedAt) {
                this.publishedAt = publishedAt;
                return this;
            }

            public Builder metadata(java.util.Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public RuleVersionInfo build() {
                return new RuleVersionInfo(ruleId, version, status, name, description, bundleHash, createdAt, publishedAt, metadata);
            }
        }
    }
}