package vn.viettel.vds.promotion.validation.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;

/**
 * Use case for managing the lifecycle of validation rules.
 */
public interface RuleManagementUseCase {

    /**
     * Create a new rule in DRAFT state.
     *
     * @param name        human-readable name
     * @param description optional description
     * @param logic       root logic (ALL/ANY/NONE/XOR)
     * @param nodes       flat list of rule nodes
     * @param createdBy   user id
     * @return persisted rule with all nodes
     */
    Rule createRule(String name, String description, Rule.LogicType logic,
                    List<RuleNode> nodes, String createdBy);

    /**
     * Update a DRAFT rule's name/logic/nodes.
     *
     * @param ruleId    rule id
     * @param name      new name (null = unchanged)
     * @param logic     new logic (null = unchanged)
     * @param nodes     new flat node list (null = unchanged)
     * @param updatedBy user id
     * @return updated rule
     */
    Rule updateRule(String ruleId, String name, Rule.LogicType logic,
                    List<RuleNode> nodes, String updatedBy);

    /**
     * Retrieve a rule and reconstruct its node tree.
     *
     * @param ruleId rule id
     * @return rule with tree-structured nodes (GroupNode/CondNode hierarchy)
     */
    Rule getRuleTree(String ruleId);

    /**
     * Archive a rule (ACTIVE → ARCHIVED). Archived rules cannot be edited.
     *
     * @param ruleId     rule id
     * @param archivedBy user id
     * @return archived rule
     */
    Rule archiveRule(String ruleId, String archivedBy);

    /**
     * List rules with optional filters.
     *
     * @param state    filter by state (null = all)
     * @param name     filter by name pattern (null = all)
     * @param pageable pagination
     * @return page of rules
     */
    Page<Rule> listRules(Rule.RuleState state, String name, Pageable pageable);
}
