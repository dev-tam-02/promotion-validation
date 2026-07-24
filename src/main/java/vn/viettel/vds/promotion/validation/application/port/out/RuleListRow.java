package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.Rule;

/**
 * A rules-list row: the rule plus its display counts, all resolved in a single
 * database query so the list endpoint needs no per-row count lookups.
 *
 * @param rule            the rule
 * @param nodeCount       number of condition nodes (rule_nodes)
 * @param assignmentCount number of assigned objects (distinct rule_bindings.object_id,
 *                        regardless of active — a binding deactivated when its campaign
 *                        finished is still an assignment; PROM-1368)
 */
public record RuleListRow(Rule rule, int nodeCount, long assignmentCount) {
}
