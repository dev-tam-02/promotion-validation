package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.model.*;

import java.util.*;

/**
 * Static-analysis linter for rule trees.
 *
 * <p>Runs at rule save time (create / update) to detect common authoring mistakes
 * before they reach the Drools engine.  All checks are purely structural — no DB
 * lookups, no external calls.
 *
 * <h3>Checks performed</h3>
 * <ol>
 *   <li><b>REDUNDANCY</b> — same COND (operatorName + params) appears ≥ 2× as a direct
 *       child of the same OR/ANY GROUP.  The repeated node is always a no-op.</li>
 *   <li><b>TAUTOLOGY</b> — same COND (operatorName + params) appears ≥ 2× as a direct
 *       child of the same AND/ALL GROUP.  It can never increase information.</li>
 *   <li><b>CONTRADICTION</b> — two numeric-comparison nodes in the same AND group whose
 *       ranges are mutually exclusive (e.g. {@code order.total.gte 500} AND
 *       {@code order.total.lte 100}).  This is a blocking <em>error</em>.</li>
 *   <li><b>UNREACHABLE</b> — a GROUP (OR or AND) with exactly one direct COND child
 *       is logically equivalent to that child alone; the wrapper is dead code.</li>
 * </ol>
 *
 * <h3>Return contract</h3>
 * <ul>
 *   <li>Warnings → non-blocking; caller should log + surface to admin UI.</li>
 *   <li>Errors   → blocking; caller must reject the save operation.</li>
 * </ul>
 */
@Component
public class RuleLinter {

    private static final Logger log = LoggerFactory.getLogger(RuleLinter.class);

    // Operator suffixes recognised as numeric bounds
    private static final List<String> LOWER_BOUND_SUFFIXES = List.of(".gte", ".gt", "_gte", "_gt");
    private static final List<String> UPPER_BOUND_SUFFIXES = List.of(".lte", ".lt", "_lte", "_lt");

    // Common param key names that hold the primary numeric argument
    private static final List<String> NUMERIC_PARAM_KEYS = List.of(
            "amount", "value", "total", "threshold", "min", "max", "count", "tier"
    );

    /**
     * Run all lint checks over the given rule + its node tree.
     *
     * @param rule  the rule being saved (used for context / logging only)
     * @param nodes the flat or hierarchical node list (first element = root)
     * @return aggregated {@link LintReport}; never null
     */
    public LintReport lint(Rule rule, List<RuleNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return LintReport.clean();
        }

        List<LintIssue> warnings = new ArrayList<>();
        List<LintIssue> errors = new ArrayList<>();

        for (RuleNode root : nodes) {
            traverseAndLint(root, warnings, errors);
        }

        if (!warnings.isEmpty() || !errors.isEmpty()) {
            log.debug("RuleLinter: rule={} warnings={} errors={}",
                    rule != null ? rule.getId() : "?", warnings.size(), errors.size());
        }

        return new LintReport(warnings, errors);
    }

    // -------------------------------------------------------------------------
    // Recursive traversal
    // -------------------------------------------------------------------------

    private void traverseAndLint(RuleNode node, List<LintIssue> warnings, List<LintIssue> errors) {
        if (node == null) {
            return;
        }

        if (node.getType() == RuleNode.NodeType.GROUP) {
            List<RuleNode> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                Rule.LogicType groupLogic = node.getGroupLogic();

                // 1 + 2: redundancy / tautology among direct COND children
                detectDuplicateConds(node, children, groupLogic, warnings);

                // 3: contradiction — numeric bounds in AND group
                if (groupLogic == Rule.LogicType.ALL) {
                    detectContradiction(node, children, errors);
                }

                // 4: unreachable — group with single COND child
                detectUnreachable(node, children, warnings);

                // Recurse into children
                for (RuleNode child : children) {
                    traverseAndLint(child, warnings, errors);
                }
            }
        }
        // COND nodes have no children to recurse into
    }

    // -------------------------------------------------------------------------
    // Check 1 + 2: Redundancy / Tautology (duplicate COND in same group)
    // -------------------------------------------------------------------------

    private void detectDuplicateConds(RuleNode group, List<RuleNode> children,
                                      Rule.LogicType groupLogic, List<LintIssue> warnings) {
        // Build a map: condKey → first nodeId that used it
        Map<String, String> seen = new LinkedHashMap<>();

        for (RuleNode child : children) {
            if (child.getType() != RuleNode.NodeType.COND) {
                continue;
            }
            String key = condKey(child);
            if (seen.containsKey(key)) {
                String firstId = seen.get(key);
                String dupId = child.getNodeId();
                if (groupLogic == Rule.LogicType.ALL) {
                    // Tautology: X AND X reduces to X
                    warnings.add(LintIssue.warning(
                            LintCode.TAUTOLOGY,
                            "Node \"" + dupId + "\" duplicates \"" + firstId
                                    + "\" in AND group \"" + group.getNodeId()
                                    + "\" (operator=" + child.getOperatorName() + ").",
                            dupId));
                } else {
                    // Redundancy: X OR X is still X — second occurrence adds nothing
                    warnings.add(LintIssue.warning(
                            LintCode.REDUNDANCY,
                            "Node \"" + dupId + "\" duplicates \"" + firstId
                                    + "\" in group \"" + group.getNodeId()
                                    + "\" (operator=" + child.getOperatorName() + ").",
                            dupId));
                }
            } else {
                seen.put(key, child.getNodeId());
            }
        }
    }

    /**
     * Canonical key for a COND node: {@code operatorName|param1=v1,param2=v2...}
     */
    private String condKey(RuleNode node) {
        String op = node.getOperatorName() != null ? node.getOperatorName() : "";
        Map<String, Object> params = node.getParams();
        if (params == null || params.isEmpty()) {
            return op + "|";
        }
        // Stable ordering: sort by key name so param order doesn't matter
        StringBuilder sb = new StringBuilder(op).append("|");
        params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append('=').append(e.getValue()).append(','));
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Check 3: Contradiction (mutually exclusive numeric bounds in AND group)
    // -------------------------------------------------------------------------

    @SuppressWarnings("java:S3776")
    private void detectContradiction(RuleNode group, List<RuleNode> children,
                                     List<LintIssue> errors) {
        // Collect lower-bound and upper-bound COND nodes keyed by field prefix
        Map<String, List<BoundEntry>> lowerBounds = new HashMap<>();
        Map<String, List<BoundEntry>> upperBounds = new HashMap<>();

        for (RuleNode child : children) {
            collectBoundEntry(child, lowerBounds, upperBounds);
        }

        // Compare lower vs upper bounds for the same field prefix
        for (Map.Entry<String, List<BoundEntry>> lowerEntry : lowerBounds.entrySet()) {
            String field = lowerEntry.getKey();
            List<BoundEntry> upperList = upperBounds.get(field);
            if (upperList == null) {
                continue;
            }
            for (BoundEntry lower : lowerEntry.getValue()) {
                for (BoundEntry upper : upperList) {
                    if (isContradiction(lower, upper)) {
                        errors.add(LintIssue.error(
                                LintCode.CONTRADICTION,
                                "Contradiction in AND group \"" + group.getNodeId()
                                        + "\": node \"" + lower.nodeId + "\" requires "
                                        + field + " " + lower.opName + " " + lower.value
                                        + " but node \"" + upper.nodeId + "\" requires "
                                        + field + " " + upper.opName + " " + upper.value
                                        + " — no value can satisfy both conditions.",
                                lower.nodeId));
                    }
                }
            }
        }
    }

    /**
     * Returns true when a lower-bound constraint and an upper-bound constraint
     * cannot be simultaneously satisfied.
     *
     * <p>Rules:
     * <ul>
     *   <li>{@code field >= lo AND field <= hi} — contradiction if lo > hi</li>
     *   <li>{@code field >  lo AND field <  hi} — contradiction if lo >= hi</li>
     *   <li>{@code field >= lo AND field <  hi} — contradiction if lo >= hi</li>
     *   <li>{@code field >  lo AND field <= hi} — contradiction if lo >= hi</li>
     * </ul>
     */
    private void collectBoundEntry(RuleNode child,
                                   Map<String, List<BoundEntry>> lowerBounds,
                                   Map<String, List<BoundEntry>> upperBounds) {
        if (child.getType() != RuleNode.NodeType.COND) {
            return;
        }
        String opName = child.getOperatorName();
        if (opName == null) {
            return;
        }
        double numericValue = extractNumericValue(child);
        if (Double.isNaN(numericValue)) {
            return;
        }
        String fieldPrefix = fieldPrefix(opName);
        if (fieldPrefix == null) {
            return;
        }
        if (isLowerBound(opName)) {
            lowerBounds.computeIfAbsent(fieldPrefix, k -> new ArrayList<>())
                    .add(new BoundEntry(child.getNodeId(), opName, numericValue));
        } else if (isUpperBound(opName)) {
            upperBounds.computeIfAbsent(fieldPrefix, k -> new ArrayList<>())
                    .add(new BoundEntry(child.getNodeId(), opName, numericValue));
        }
    }

    private boolean isContradiction(BoundEntry lower, BoundEntry upper) {
        boolean loIsStrict = isStrictLower(lower.opName);
        boolean hiIsStrict = isStrictUpper(upper.opName);

        if (!loIsStrict && !hiIsStrict) {
            // gte + lte: contradiction if lo > hi
            return lower.value > upper.value;
        }
        // any strict boundary: contradiction if lo >= hi
        return lower.value >= upper.value;
    }

    // -------------------------------------------------------------------------
    // Check 4: Unreachable (GROUP with single COND child)
    // -------------------------------------------------------------------------

    private void detectUnreachable(RuleNode group, List<RuleNode> children,
                                   List<LintIssue> warnings) {
        if (children.size() == 1) {
            RuleNode onlyChild = children.get(0);
            warnings.add(LintIssue.warning(
                    LintCode.UNREACHABLE,
                    "GROUP \"" + group.getNodeId()
                            + "\" has only one child \"" + onlyChild.getNodeId()
                            + "\" — the group wrapper is redundant and can be removed.",
                    group.getNodeId()));
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Extract the numeric primary argument from a COND node's params.
     * Returns {@link Double#NaN} if no recognisable numeric value is found.
     */
    private double extractNumericValue(RuleNode node) {
        Map<String, Object> params = node.getParams();
        if (params == null || params.isEmpty()) {
            return Double.NaN;
        }
        // Try well-known param keys first
        for (String key : NUMERIC_PARAM_KEYS) {
            Object val = params.get(key);
            if (val != null) {
                Double d = toDouble(val);
                if (d != null) {
                    return d;
                }
            }
        }
        // Fall back: first numeric value encountered in params
        for (Object val : params.values()) {
            Double d = toDouble(val);
            if (d != null) {
                return d;
            }
        }
        return Double.NaN;
    }

    private Double toDouble(Object val) {
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        if (val instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                // not a number
            }
        }
        return null;
    }

    /**
     * Returns the field prefix of a numeric-comparison operator, or null if the
     * operator is not a recognised numeric bound.
     * Example: {@code "order.total.gte"} → {@code "order.total"}
     */
    private String fieldPrefix(String opName) {
        for (String suffix : LOWER_BOUND_SUFFIXES) {
            if (opName.endsWith(suffix)) {
                return opName.substring(0, opName.length() - suffix.length());
            }
        }
        for (String suffix : UPPER_BOUND_SUFFIXES) {
            if (opName.endsWith(suffix)) {
                return opName.substring(0, opName.length() - suffix.length());
            }
        }
        return null;
    }

    private boolean isLowerBound(String opName) {
        return LOWER_BOUND_SUFFIXES.stream().anyMatch(opName::endsWith);
    }

    private boolean isUpperBound(String opName) {
        return UPPER_BOUND_SUFFIXES.stream().anyMatch(opName::endsWith);
    }

    private boolean isStrictLower(String opName) {
        return opName.endsWith(".gt") || opName.endsWith("_gt");
    }

    private boolean isStrictUpper(String opName) {
        return opName.endsWith(".lt") || opName.endsWith("_lt");
    }

    // -------------------------------------------------------------------------
    // Internal data holder
    // -------------------------------------------------------------------------

    private record BoundEntry(String nodeId, String opName, double value) {
        // Canonical record; no extra members.
    }
}
