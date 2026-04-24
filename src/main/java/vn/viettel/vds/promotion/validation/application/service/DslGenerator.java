package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a JSON DSL snapshot from a Rule + its node tree.
 *
 * <p>Output format (stored in validation_rules.dsl):
 * <pre>
 * {
 *   "ruleId": "...",
 *   "logic": "ALL",
 *   "root": {
 *     "type": "GROUP",
 *     "groupLogic": "AND",
 *     "children": [
 *       { "type": "COND", "operator": "order.total.gte",
 *         "params": {"amount":500000,"currency":"VND"},
 *         "reasonCode": "MIN_ORDER_NOT_MET" },
 *       { "type": "GROUP", "groupLogic": "OR", "children": [...] }
 *     ]
 *   }
 * }
 * </pre>
 *
 * <p>The returned {@code Map<String, Object>} is stored directly on
 * {@code Rule.dsl} and serialised to TEXT by {@code RuleEntityMapper}.
 */
@Component
public class DslGenerator {

    private static final Logger log = LoggerFactory.getLogger(DslGenerator.class);

    private final ObjectMapper objectMapper;

    public DslGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Generate a DSL snapshot for the given rule.
     *
     * @param rule  the rule aggregate (provides id and root logic)
     * @param nodes flat list of nodes — must already form a tree via {@link RuleNode#getChildren()}
     * @return DSL as {@code Map<String, Object>} suitable for {@link Rule#setDsl}
     */
    public Map<String, Object> generate(Rule rule, List<RuleNode> nodes) {
        log.debug("Generating DSL for ruleId={}", rule.getId());

        Map<String, Object> dsl = new LinkedHashMap<>();
        dsl.put("ruleId", rule.getId());
        dsl.put("logic", logicLabel(rule.getLogic()));

        if (nodes != null && !nodes.isEmpty()) {
            // First node is the root (tree is already assembled by caller)
            RuleNode root = nodes.get(0);
            dsl.put("root", serializeNode(root));
        }

        log.debug("DSL generated for ruleId={}", rule.getId());
        return dsl;
    }

    /**
     * Serialize DSL map to JSON string (convenience for logging / tests).
     */
    public String toJson(Map<String, Object> dsl) {
        try {
            return objectMapper.writeValueAsString(dsl);
        } catch (Exception e) {
            log.warn("Failed to serialize DSL to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    // ---------- private helpers ----------

    private Map<String, Object> serializeNode(RuleNode node) {
        if (node == null) {
            return null;
        }
        if (node.getType() == RuleNode.NodeType.GROUP) {
            return serializeGroup(node);
        }
        return serializeCond(node);
    }

    private Map<String, Object> serializeGroup(RuleNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "GROUP");
        map.put("groupLogic", logicLabel(node.getGroupLogic()));

        List<Map<String, Object>> children = new ArrayList<>();
        if (node.getChildren() != null) {
            for (RuleNode child : node.getChildren()) {
                Map<String, Object> serialized = serializeNode(child);
                if (serialized != null) {
                    children.add(serialized);
                }
            }
        }
        map.put("children", children);
        return map;
    }

    private Map<String, Object> serializeCond(RuleNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "COND");
        map.put("operator", node.getOperatorName());
        map.put("params", node.getParams() != null ? node.getParams() : Map.of());
        map.put("reasonCode", node.getReasonCode());
        return map;
    }

    /**
     * Map Rule.LogicType to DSL string label.
     * ALL=AND, ANY=OR, NONE=NONE, XOR=XOR
     */
    private String logicLabel(Rule.LogicType logic) {
        if (logic == null) {
            return "AND";
        }
        return switch (logic) {
            case ALL -> "AND";
            case ANY -> "OR";
            case NONE -> "NONE";
            case XOR -> "XOR";
        };
    }
}
