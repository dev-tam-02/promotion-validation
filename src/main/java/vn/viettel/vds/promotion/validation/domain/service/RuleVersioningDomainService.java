package vn.viettel.vds.promotion.validation.domain.service;

import vn.viettel.vds.promotion.validation.domain.model.RuleAggregate;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.model.RuleStatus;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleCode;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleId;
import vn.viettel.vds.promotion.validation.domain.valueobject.Version;

import java.time.Instant;
import java.util.*;

/**
 * Domain service for rule versioning logic
 * Handles complex versioning scenarios and conflict resolution
 */
public class RuleVersioningDomainService {

    private static final String SYSTEM_MERGE_USER = "system-merge";

    /**
     * Create a new version of an existing rule
     */
    public RuleAggregate createNewVersion(RuleAggregate existingRule, VersionType versionType, String createdBy) {
        Objects.requireNonNull(existingRule, "Existing rule cannot be null");
        Objects.requireNonNull(versionType, "Version type cannot be null");
        Objects.requireNonNull(createdBy, "Created by cannot be null");

        // Only published or deprecated rules can have new versions
        if (existingRule.getStatus() != RuleStatus.PUBLISHED &&
                existingRule.getStatus() != RuleStatus.DEPRECATED) {
            throw new IllegalStateException(
                    "Can only create new version from published or deprecated rules"
            );
        }

        // Determine new version number
        Version newVersion = calculateNewVersion(existingRule.getVersion(), versionType);

        // Create new rule with incremented version
        RuleAggregate newRule = RuleAggregate.builder()
                .id(RuleId.generate())
                .code(existingRule.getCode()) // Same code, different version
                .name(existingRule.getName())
                .description(existingRule.getDescription())
                .logicType(existingRule.getLogicType())
                .nodes(new ArrayList<>(existingRule.getNodes()))
                .status(RuleStatus.DRAFT)
                .version(newVersion)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();

        return newRule;
    }

    /**
     * Calculate new version based on version type
     */
    private Version calculateNewVersion(Version currentVersion, VersionType versionType) {
        switch (versionType) {
            case MAJOR:
                return currentVersion.incrementMajor();
            case MINOR:
                return currentVersion.incrementMinor();
            case PATCH:
                return currentVersion.incrementPatch();
            default:
                throw new UnsupportedOperationException("Version type not supported: " + versionType);
        }
    }

    /**
     * Check if a rule can replace another rule
     */
    public boolean canReplace(RuleAggregate newRule, RuleAggregate existingRule) {
        // Must have same code
        if (!newRule.getCode().equals(existingRule.getCode())) {
            return false;
        }

        // New version must be higher
        if (!newRule.getVersion().isNewerThan(existingRule.getVersion())) {
            return false;
        }

        // Existing rule must be published or deprecated
        return existingRule.getStatus() == RuleStatus.PUBLISHED ||
                existingRule.getStatus() == RuleStatus.DEPRECATED;
    }

    /**
     * Merge changes from one rule to another
     */
    public RuleAggregate mergeChanges(RuleAggregate targetRule, RuleAggregate sourceRule, MergeStrategy strategy) {
        Objects.requireNonNull(targetRule, "Target rule cannot be null");
        Objects.requireNonNull(sourceRule, "Source rule cannot be null");
        Objects.requireNonNull(strategy, "Merge strategy cannot be null");

        if (!targetRule.canBeModified()) {
            throw new IllegalStateException("Target rule cannot be modified");
        }

        switch (strategy) {
            case OVERWRITE:
                // Completely replace target with source content
                targetRule.update(
                        sourceRule.getName(),
                        sourceRule.getDescription(),
                        sourceRule.getLogicType(),
                        sourceRule.getNodes(),
                        SYSTEM_MERGE_USER
                );
                break;

            case APPEND:
                // Append source nodes to target
                List<RuleNode> mergedNodes = new ArrayList<>(targetRule.getNodes());
                mergedNodes.addAll(sourceRule.getNodes());
                targetRule.update(
                        targetRule.getName(),
                        targetRule.getDescription(),
                        targetRule.getLogicType(),
                        mergedNodes,
                        SYSTEM_MERGE_USER
                );
                break;

            case SELECTIVE:
                // Merge only non-null/non-empty fields
                targetRule.update(
                        sourceRule.getName() != null ? sourceRule.getName() : targetRule.getName(),
                        sourceRule.getDescription() != null ? sourceRule.getDescription() : targetRule.getDescription(),
                        sourceRule.getLogicType() != null ? sourceRule.getLogicType() : targetRule.getLogicType(),
                        !sourceRule.getNodes().isEmpty() ? sourceRule.getNodes() : targetRule.getNodes(),
                        SYSTEM_MERGE_USER
                );
                break;

            default:
                throw new UnsupportedOperationException("Merge strategy not supported: " + strategy);
        }

        return targetRule;
    }

    /**
     * Compare two rules for differences
     */
    public RuleDifference compareRules(RuleAggregate rule1, RuleAggregate rule2) {
        RuleDifference diff = new RuleDifference();

        // Compare basic properties
        if (!rule1.getName().equals(rule2.getName())) {
            diff.addDifference("name", rule1.getName(), rule2.getName());
        }

        if (!Objects.equals(rule1.getDescription(), rule2.getDescription())) {
            diff.addDifference("description", rule1.getDescription(), rule2.getDescription());
        }

        if (rule1.getLogicType() != rule2.getLogicType()) {
            diff.addDifference("logicType", rule1.getLogicType(), rule2.getLogicType());
        }

        // Compare nodes
        if (!areNodesEqual(rule1.getNodes(), rule2.getNodes())) {
            diff.addDifference("nodes", rule1.getNodes(), rule2.getNodes());
        }

        // Compare version
        if (!rule1.getVersion().equals(rule2.getVersion())) {
            diff.addDifference("version", rule1.getVersion(), rule2.getVersion());
        }

        return diff;
    }

    private boolean areNodesEqual(List<RuleNode> nodes1, List<RuleNode> nodes2) {
        if (nodes1.size() != nodes2.size()) {
            return false;
        }

        // Simple comparison - could be enhanced for deep comparison
        for (int i = 0; i < nodes1.size(); i++) {
            RuleNode node1 = nodes1.get(i);
            RuleNode node2 = nodes2.get(i);

            if (!Objects.equals(node1.getNodeId(), node2.getNodeId()) ||
                    !Objects.equals(node1.getField(), node2.getField()) ||
                    !Objects.equals(node1.getOperator(), node2.getOperator()) ||
                    !Objects.equals(node1.getValue(), node2.getValue())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get version history for a rule code
     */
    public List<RuleAggregate> getVersionHistory(List<RuleAggregate> allRules, RuleCode code) {
        return allRules.stream()
                .filter(rule -> rule.getCode().equals(code))
                .sorted(Comparator.comparing(RuleAggregate::getVersion).reversed())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find the latest version of a rule
     */
    public Optional<RuleAggregate> findLatestVersion(List<RuleAggregate> rules, RuleCode code) {
        return rules.stream()
                .filter(rule -> rule.getCode().equals(code))
                .max(Comparator.comparing(RuleAggregate::getVersion));
    }

    /**
     * Version type enumeration
     */
    public enum VersionType {
        MAJOR,  // Breaking changes
        MINOR,  // New features, backward compatible
        PATCH   // Bug fixes
    }

    /**
     * Merge strategy enumeration
     */
    public enum MergeStrategy {
        OVERWRITE,  // Replace everything
        APPEND,     // Add to existing
        SELECTIVE   // Merge non-null fields
    }

    /**
     * Class to hold rule differences
     */
    public static class RuleDifference {
        private final Map<String, Difference> differences = new HashMap<>();

        public void addDifference(String field, Object oldValue, Object newValue) {
            differences.put(field, new Difference(field, oldValue, newValue));
        }

        public boolean hasDifferences() {
            return !differences.isEmpty();
        }

        public Map<String, Difference> getDifferences() {
            return Collections.unmodifiableMap(differences);
        }

        public static class Difference {
            private final String field;
            private final Object oldValue;
            private final Object newValue;

            public Difference(String field, Object oldValue, Object newValue) {
                this.field = field;
                this.oldValue = oldValue;
                this.newValue = newValue;
            }

            public String getField() {
                return field;
            }

            public Object getOldValue() {
                return oldValue;
            }

            public Object getNewValue() {
                return newValue;
            }
        }
    }
}