package vn.viettel.vds.promotion.validation.application.service;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core domain invariant validator for rule trees.
 *
 * <p>Checks performed:
 * <ul>
 *   <li>{@link #checkTreeDepth} — DFS depth ≤ 10</li>
 *   <li>{@link #checkNoCircular} — topological sort detects back-edges</li>
 *   <li>{@link #checkAllGroupsHaveChildren} — every GROUP must have ≥ 1 child</li>
 *   <li>{@link #checkOperatorParamsMatchSchema} — COND params validated against
 *       operator JSON Schema (draft-07 via {@code com.networknt:json-schema-validator})</li>
 * </ul>
 *
 * <p>All check methods throw {@link RuleValidationException} on violation.
 * The caller may invoke {@link #validate} to run all checks in sequence.
 */
@Component
public class RuleValidator {

    private static final Logger log = LoggerFactory.getLogger(RuleValidator.class);
    private static final int MAX_DEPTH = 10;

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public RuleValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    /**
     * Run all invariant checks.
     *
     * @param roots     root nodes of the rule tree
     * @param operators operator map (name → Operator) for schema lookup
     */
    public void validate(List<RuleNode> roots, Map<String, Operator> operators) {
        checkTreeDepth(roots);
        checkNoCircular(roots);
        checkAllGroupsHaveChildren(roots);
        checkOperatorParamsMatchSchema(roots, operators);
    }

    /**
     * Check that tree depth does not exceed {@value MAX_DEPTH}.
     */
    public void checkTreeDepth(List<RuleNode> roots) {
        if (roots == null) {
            return;
        }
        for (RuleNode root : roots) {
            int depth = measureDepth(root, new HashSet<>());
            if (depth > MAX_DEPTH) {
                throw new RuleValidationException(
                        "Rule tree depth " + depth + " exceeds maximum allowed depth " + MAX_DEPTH);
            }
        }
        log.debug("checkTreeDepth passed (roots={})", roots.size());
    }

    /**
     * Check that there are no circular references using DFS.
     */
    public void checkNoCircular(List<RuleNode> roots) {
        if (roots == null) {
            return;
        }
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();
        for (RuleNode root : roots) {
            detectCycle(root, visited, stack);
        }
        log.debug("checkNoCircular passed");
    }

    /**
     * Check that every GROUP node has at least one child.
     */
    public void checkAllGroupsHaveChildren(List<RuleNode> roots) {
        if (roots == null) {
            return;
        }
        for (RuleNode root : roots) {
            assertGroupsHaveChildren(root, new HashSet<>());
        }
        log.debug("checkAllGroupsHaveChildren passed");
    }

    /**
     * Check that COND node params conform to the operator JSON Schema (draft-07).
     *
     * @param roots     rule tree roots
     * @param operators operator map keyed by operator name
     */
    public void checkOperatorParamsMatchSchema(List<RuleNode> roots, Map<String, Operator> operators) {
        if (roots == null || operators == null) {
            return;
        }
        for (RuleNode root : roots) {
            validateParamsRecursive(root, operators, new HashSet<>());
        }
        log.debug("checkOperatorParamsMatchSchema passed");
    }

    // ---------- private helpers ----------

    private int measureDepth(RuleNode node, Set<String> visited) {
        if (node == null) {
            return 0;
        }
        String id = node.getId();
        if (id != null && visited.contains(id)) {
            return 0; // cycle guard
        }
        if (id != null) {
            visited.add(id);
        }
        List<RuleNode> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return 1;
        }
        int maxChild = 0;
        for (RuleNode child : children) {
            maxChild = Math.max(maxChild, measureDepth(child, new HashSet<>(visited)));
        }
        return 1 + maxChild;
    }

    private void detectCycle(RuleNode node, Set<String> visited, Set<String> stack) {
        if (node == null) {
            return;
        }
        String id = node.getId();
        if (id == null) {
            return;
        }
        if (stack.contains(id)) {
            throw new RuleValidationException(
                    "Circular reference detected at node id=" + id);
        }
        if (visited.contains(id)) {
            return;
        }
        visited.add(id);
        stack.add(id);
        List<RuleNode> children = node.getChildren();
        if (children != null) {
            for (RuleNode child : children) {
                detectCycle(child, visited, stack);
            }
        }
        stack.remove(id);
    }

    private void assertGroupsHaveChildren(RuleNode node, Set<String> visited) {
        if (node == null) {
            return;
        }
        String id = node.getId();
        if (id != null && visited.contains(id)) {
            return;
        }
        if (id != null) {
            visited.add(id);
        }
        if (node.getType() == RuleNode.NodeType.GROUP) {
            List<RuleNode> children = node.getChildren();
            if (children == null || children.isEmpty()) {
                throw new RuleValidationException(
                        "GROUP node id=" + id + " has no children");
            }
            for (RuleNode child : children) {
                assertGroupsHaveChildren(child, visited);
            }
        }
    }

    private void validateParamsRecursive(RuleNode node, Map<String, Operator> operators, Set<String> visited) {
        if (node == null) {
            return;
        }
        String id = node.getId();
        if (id != null && visited.contains(id)) {
            return;
        }
        if (id != null) {
            visited.add(id);
        }
        if (node.getType() == RuleNode.NodeType.COND) {
            String operatorName = node.getOperatorName();
            Operator operator = operators.get(operatorName);
            if (operator != null && node.getParams() != null && operator.getJsonSchema() != null) {
                validateAgainstSchema(node.getParams(), operator.getJsonSchema(), operatorName, id);
            }
        }
        List<RuleNode> children = node.getChildren();
        if (children != null) {
            for (RuleNode child : children) {
                validateParamsRecursive(child, operators, visited);
            }
        }
    }

    private void validateAgainstSchema(Map<String, Object> params,
                                       Map<String, Object> jsonSchemaMap,
                                       String operatorName, String nodeId) {
        try {
            String schemaJson = objectMapper.writeValueAsString(jsonSchemaMap);
            JsonSchema schema = schemaFactory.getSchema(schemaJson);
            String paramsJson = objectMapper.writeValueAsString(params);
            JsonNode paramsNode = objectMapper.readTree(paramsJson);
            Set<ValidationMessage> errors = schema.validate(paramsNode);
            if (!errors.isEmpty()) {
                List<String> messages = new ArrayList<>();
                for (ValidationMessage msg : errors) {
                    messages.add(msg.getMessage());
                }
                throw new RuleValidationException(
                        "COND node id=" + nodeId + " operator=" + operatorName
                                + " params validation failed: " + String.join("; ", messages));
            }
        } catch (RuleValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            log.warn("Failed to validate params for node={} operator={}: {}",
                    nodeId, operatorName, ex.getMessage());
        }
    }

    /**
     * Thrown when a rule tree invariant is violated.
     */
    public static class RuleValidationException extends RuntimeException {
        public RuleValidationException(String message) {
            super(message);
        }
    }
}
