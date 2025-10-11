package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RuleValidationService {

    private static final Logger logger = LoggerFactory.getLogger(RuleValidationService.class);
    private static final int MAX_NODES = 300;
    private static final int MAX_DEPTH = 8;

    private final OperatorService operatorService;
    private final ReasonCodeService reasonCodeService;

    public RuleValidationService(OperatorService operatorService, ReasonCodeService reasonCodeService) {
        this.operatorService = operatorService;
        this.reasonCodeService = reasonCodeService;
    }

    /**
     * Comprehensive rule linting/validation
     */
    public LintResult lintRule(List<RuleNode> nodes) {
        logger.debug("Linting rule: nodeCount={}", nodes.size());

        List<LintIssue> issues = new ArrayList<>();

        try {
            // Basic structure validation
            validateBasicStructure(nodes, issues);

            // Node limit validation
            validateNodeLimits(nodes, issues);

            // Tree structure validation
            validateTreeStructure(nodes, issues);

            // Operator validation
            validateOperators("default", nodes, issues);

            // Reason code validation
            validateReasonCodes("default", nodes, issues);

            // Circular reference validation
            validateNoCircularReferences(nodes, issues);

            boolean isValid = issues.isEmpty();

            logger.info("Rule linting completed: valid={}, issueCount={}",
                    isValid, issues.size());

            return new LintResult(isValid, issues);

        } catch (Exception e) {
            logger.error("Error during rule linting", e);
            issues.add(new LintIssue("", "Internal validation error: " + e.getMessage(), null));
            return new LintResult(false, issues);
        }
    }

    /**
     * Validate rule against operators and business rules
     */
    public ValidationResult validateRuleForPublishing(Rule rule) {
        logger.info("Validating rule for publishing: id={}", rule.getId());

        List<ValidationIssue> issues = new ArrayList<>();

        // Lint the rule structure
        LintResult lintResult = lintRule(rule.getNodes());
        issues.addAll(lintResult.getIssues().stream()
                .map(lint -> new ValidationIssue(lint.getPath(), lint.getMessage(), "LINT_ERROR"))
                .toList());

        // Validate that all operators exist and are active
        Set<String> operatorNames = extractOperatorNames(rule.getNodes());
        for (String operatorName : operatorNames) {
            try {
                Optional<vn.viettel.vds.promotion.validation.domain.model.Operator> operator =
                        operatorService.getLatestOperator("default", operatorName);

                if (operator.isEmpty()) {
                    issues.add(new ValidationIssue("operators",
                            "Operator not found: " + operatorName, "OPERATOR_NOT_FOUND"));
                } else if (operator.get().getStatus() != vn.viettel.vds.promotion.validation.domain.model.Operator.OperatorStatus.ACTIVE) {
                    issues.add(new ValidationIssue("operators",
                            "Operator is not active: " + operatorName, "OPERATOR_INACTIVE"));
                }
            } catch (Exception e) {
                issues.add(new ValidationIssue("operators",
                        "Error validating operator " + operatorName + ": " + e.getMessage(), "OPERATOR_ERROR"));
            }
        }

        // Validate rule state
        if (rule.getState() == Rule.RuleState.ARCHIVED) {
            issues.add(new ValidationIssue("state",
                    "Cannot publish archived rule", "INVALID_STATE"));
        }

        boolean isValid = issues.isEmpty();

        logger.info("Rule validation completed: id={}, valid={}, issueCount={}",
                rule.getId(), isValid, issues.size());

        return new ValidationResult(isValid, issues);
    }

    private void validateBasicStructure(List<RuleNode> nodes, List<LintIssue> issues) {
        if (nodes == null || nodes.isEmpty()) {
            issues.add(new LintIssue("nodes", "Rule must have at least one node", null));
            return;
        }

        Set<String> nodeIds = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            RuleNode node = nodes.get(i);
            String path = "nodes[" + i + "]";

            // Validate node ID
            if (node.getId() == null || node.getId().trim().isEmpty()) {
                issues.add(new LintIssue(path + ".id", "Node ID is required", null));
            } else if (nodeIds.contains(node.getId())) {
                issues.add(new LintIssue(path + ".id", "Duplicate node ID: " + node.getId(), null));
            } else {
                nodeIds.add(node.getId());
            }

            // Validate node type
            if (node.getType() == null) {
                issues.add(new LintIssue(path + ".type", "Node type is required", null));
                continue;
            }

            // Type-specific validation
            switch (node.getType()) {
                case GROUP:
                    validateGroupNode(node, path, issues);
                    break;
                case COND:
                    validateConditionNode(node, path, issues);
                    break;
            }
        }
    }

    private void validateGroupNode(RuleNode node, String path, List<LintIssue> issues) {
        if (node.getGroupLogic() == null) {
            issues.add(new LintIssue(path + ".groupLogic", "Group logic is required for GROUP nodes", null));
        }

        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            issues.add(new LintIssue(path + ".children", "GROUP nodes must have at least one child", null));
        }
    }

    private void validateConditionNode(RuleNode node, String path, List<LintIssue> issues) {
        if (node.getOperatorName() == null || node.getOperatorName().trim().isEmpty()) {
            issues.add(new LintIssue(path + ".operatorName", "Operator name is required for COND nodes", null));
        }

        if (node.getReasonCode() == null || node.getReasonCode().trim().isEmpty()) {
            issues.add(new LintIssue(path + ".reasonCode", "Reason code is required for COND nodes", null));
        }
    }

    private void validateNodeLimits(List<RuleNode> nodes, List<LintIssue> issues) {
        if (nodes.size() > MAX_NODES) {
            issues.add(new LintIssue("nodes",
                    "Too many nodes (" + nodes.size() + "). Maximum allowed: " + MAX_NODES, null));
        }
    }

    private void validateTreeStructure(List<RuleNode> nodes, List<LintIssue> issues) {
        Map<String, RuleNode> nodeMap = nodes.stream()
                .filter(node -> node.getId() != null)
                .collect(Collectors.toMap(RuleNode::getId, node -> node));

        // Validate references and calculate depth
        for (RuleNode node : nodes) {
            if (node.getType() == RuleNode.NodeType.GROUP && node.getChildren() != null) {
                for (RuleNode child : node.getChildren()) {
                    String childId = child.getId();
                    if (childId != null && !nodeMap.containsKey(childId)) {
                        issues.add(new LintIssue("nodes",
                                "Referenced child node not found: " + childId, null));
                    }
                }
            }
        }

        // Check tree depth
        try {
            int maxDepth = calculateMaxDepth(nodes, nodeMap);
            if (maxDepth > MAX_DEPTH) {
                issues.add(new LintIssue("nodes",
                        "Tree depth too deep (" + maxDepth + "). Maximum allowed: " + MAX_DEPTH, null));
            }
        } catch (Exception e) {
            issues.add(new LintIssue("nodes", "Error calculating tree depth: " + e.getMessage(), null));
        }
    }

    private void validateOperators(String tenantId, List<RuleNode> nodes, List<LintIssue> issues) {
        for (int i = 0; i < nodes.size(); i++) {
            RuleNode node = nodes.get(i);
            if (node.getType() == RuleNode.NodeType.COND && node.getOperatorName() != null) {
                String path = "nodes[" + i + "]";

                try {
                    // Check if operator exists
                    Optional<vn.viettel.vds.promotion.validation.domain.model.Operator> operator =
                            operatorService.getLatestOperator(tenantId, node.getOperatorName());

                    if (operator.isEmpty()) {
                        issues.add(new LintIssue(path + ".operatorName",
                                "Operator not found: " + node.getOperatorName(), node.getOperatorName()));
                        continue;
                    }

                    // Validate parameters against schema
                    if (node.getParams() != null) {
                        OperatorService.ValidationResult paramValidation =
                                operatorService.validateOperatorParams(
                                        tenantId,
                                        node.getOperatorName(),
                                        operator.get().getVersion().intValue(),
                                        node.getParams()
                                );

                        if (!paramValidation.isValid()) {
                            for (OperatorService.ValidationIssue issue : paramValidation.getIssues()) {
                                issues.add(new LintIssue(
                                        path + ".params." + issue.getPath(),
                                        issue.getMessage(),
                                        node.getOperatorName()
                                ));
                            }
                        }
                    }

                } catch (Exception e) {
                    issues.add(new LintIssue(path + ".operatorName",
                            "Error validating operator: " + e.getMessage(), node.getOperatorName()));
                }
            }
        }
    }

    private void validateReasonCodes(String tenantId, List<RuleNode> nodes, List<LintIssue> issues) {
        for (int i = 0; i < nodes.size(); i++) {
            RuleNode node = nodes.get(i);
            if (node.getType() == RuleNode.NodeType.COND && node.getReasonCode() != null) {
                String path = "nodes[" + i + "].reasonCode";

                try {
                    if (!reasonCodeService.existsReasonCode(tenantId, node.getReasonCode())) {
                        issues.add(new LintIssue(path,
                                "Reason code not found: " + node.getReasonCode(), null));
                    }
                } catch (Exception e) {
                    issues.add(new LintIssue(path,
                            "Error validating reason code: " + e.getMessage(), null));
                }
            }
        }
    }

    private void validateNoCircularReferences(List<RuleNode> nodes, List<LintIssue> issues) {
        Map<String, RuleNode> nodeMap = nodes.stream()
                .filter(node -> node.getId() != null)
                .collect(Collectors.toMap(RuleNode::getId, node -> node));

        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (RuleNode node : nodes) {
            if (node.getId() != null && !visited.contains(node.getId())) {
                if (hasCircularReference(node.getId(), nodeMap, visited, recursionStack)) {
                    issues.add(new LintIssue("nodes",
                            "Circular reference detected starting from node: " + node.getId(), null));
                    break;
                }
            }
        }
    }

    private boolean hasCircularReference(String nodeId, Map<String, RuleNode> nodeMap,
                                         Set<String> visited, Set<String> recursionStack) {
        visited.add(nodeId);
        recursionStack.add(nodeId);

        RuleNode node = nodeMap.get(nodeId);
        if (node != null && node.getChildren() != null) {
            for (RuleNode child : node.getChildren()) {
                String childId = child.getId();
                if (childId == null) continue;
                if (!visited.contains(childId)) {
                    if (hasCircularReference(childId, nodeMap, visited, recursionStack)) {
                        return true;
                    }
                } else if (recursionStack.contains(childId)) {
                    return true;
                }
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    private int calculateMaxDepth(List<RuleNode> nodes, Map<String, RuleNode> nodeMap) {
        int maxDepth = 0;

        // Find root nodes (nodes not referenced by any parent)
        Set<String> referencedNodes = nodes.stream()
                .filter(node -> node.getChildren() != null)
                .flatMap(node -> node.getChildren().stream()
                        .map(RuleNode::getId)
                        .filter(id -> id != null))
                .collect(Collectors.toSet());

        List<String> rootNodes = nodes.stream()
                .map(RuleNode::getId)
                .filter(id -> id != null && !referencedNodes.contains(id))
                .toList();

        for (String rootId : rootNodes) {
            int depth = calculateDepthRecursive(rootId, nodeMap, new HashSet<>());
            maxDepth = Math.max(maxDepth, depth);
        }

        return maxDepth;
    }

    private int calculateDepthRecursive(String nodeId, Map<String, RuleNode> nodeMap, Set<String> visited) {
        if (visited.contains(nodeId)) {
            return 0; // Avoid infinite recursion
        }

        visited.add(nodeId);
        RuleNode node = nodeMap.get(nodeId);

        if (node == null || node.getChildren() == null || node.getChildren().isEmpty()) {
            visited.remove(nodeId);
            return 1;
        }

        int maxChildDepth = 0;
        for (RuleNode child : node.getChildren()) {
            String childId = child.getId();
            if (childId == null) continue;
            int childDepth = calculateDepthRecursive(childId, nodeMap, visited);
            maxChildDepth = Math.max(maxChildDepth, childDepth);
        }

        visited.remove(nodeId);
        return 1 + maxChildDepth;
    }

    private Set<String> extractOperatorNames(List<RuleNode> nodes) {
        return nodes.stream()
                .filter(node -> node.getType() == RuleNode.NodeType.COND)
                .map(RuleNode::getOperatorName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    // Result classes
    public static class LintResult {
        private final boolean valid;
        private final List<LintIssue> issues;

        public LintResult(boolean valid, List<LintIssue> issues) {
            this.valid = valid;
            this.issues = issues;
        }

        public boolean isValid() {
            return valid;
        }

        public List<LintIssue> getIssues() {
            return issues;
        }
    }

    public static class LintIssue {
        private final String path;
        private final String message;
        private final String operator;

        public LintIssue(String path, String message, String operator) {
            this.path = path;
            this.message = message;
            this.operator = operator;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }

        public String getOperator() {
            return operator;
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<ValidationIssue> issues;

        public ValidationResult(boolean valid, List<ValidationIssue> issues) {
            this.valid = valid;
            this.issues = issues;
        }

        public boolean isValid() {
            return valid;
        }

        public List<ValidationIssue> getIssues() {
            return issues;
        }
    }

    public static class ValidationIssue {
        private final String path;
        private final String message;
        private final String type;

        public ValidationIssue(String path, String message, String type) {
            this.path = path;
            this.message = message;
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }

        public String getType() {
            return type;
        }
    }
}