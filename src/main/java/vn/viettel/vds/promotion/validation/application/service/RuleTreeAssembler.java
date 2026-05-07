package vn.viettel.vds.promotion.validation.application.service;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.model.CondNode;
import vn.viettel.vds.promotion.validation.domain.model.GroupNode;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a tree of {@link RuleNode} objects (already assembled by the persistence mapper)
 * into the spec-typed {@link GroupNode} / {@link CondNode} object graph.
 *
 * <p>The persistence mapper ({@code RuleNodeEntityMapper.toDomainList}) builds the
 * {@link RuleNode} tree from JPA entities.  This assembler then casts each node
 * into its strongly-typed domain counterpart.
 *
 * <p>Usage:
 * <pre>
 *   List&lt;RuleNode&gt; roots = mapper.toDomainList(entities);
 *   Object typedRoot = assembler.assembleRoot(roots);
 * </pre>
 */
@Component
public class RuleTreeAssembler {

    /**
     * Assemble the root typed node from the already-built {@link RuleNode} tree.
     *
     * @param roots root nodes returned by the persistence mapper (usually a single root)
     * @return the root typed node (GroupNode or CondNode), null when roots is empty
     */
    public Object assembleRoot(List<RuleNode> roots) {
        if (roots == null || roots.isEmpty()) {
            return null;
        }
        // Well-formed rules have one root; take the first one
        return convert(roots.get(0));
    }

    /**
     * Convert all root-level nodes (convenience for callers with multiple roots).
     */
    public List<Object> assembleAll(List<RuleNode> roots) {
        if (roots == null) {
            return new ArrayList<>();
        }
        List<Object> result = new ArrayList<>();
        for (RuleNode root : roots) {
            Object node = convert(root);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    // ---------- private helpers ----------

    private Object convert(RuleNode node) {
        if (node == null) {
            return null;
        }
        if (node.getType() == RuleNode.NodeType.GROUP) {
            return convertGroup(node);
        }
        return convertCond(node);
    }

    private GroupNode convertGroup(RuleNode node) {
        GroupNode.Builder builder = GroupNode.builder()
                .id(node.getId())
                .groupLogic(mapGroupLogic(node.getGroupLogic()))
                .displayOrder(0);

        List<RuleNode> children = node.getChildren();
        if (children != null) {
            for (RuleNode child : children) {
                Object typedChild = convert(child);
                if (typedChild != null) {
                    builder.addChild(typedChild);
                }
            }
        }
        return builder.build();
    }

    private CondNode convertCond(RuleNode node) {
        return CondNode.builder()
                .id(node.getId())
                .operatorName(node.getOperatorName())
                .params(node.getParams())
                .reasonCode(node.getReasonCode())
                .displayOrder(0)
                .build();
    }

    private GroupNode.GroupLogic mapGroupLogic(Rule.LogicType logicType) {
        if (logicType == null) {
            return GroupNode.GroupLogic.AND;
        }
        return switch (logicType) {
            case ANY -> GroupNode.GroupLogic.OR;
            case NONE -> GroupNode.GroupLogic.NONE;
            default -> GroupNode.GroupLogic.AND;
        };
    }

    /**
     * Calculate maximum depth of a {@link RuleNode} tree using DFS.
     * A single node (no children) has depth 1.
     *
     * @param node root node
     * @return maximum depth
     */
    public int maxDepth(RuleNode node) {
        if (node == null) {
            return 0;
        }
        List<RuleNode> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return 1;
        }
        int maxChildDepth = 0;
        for (RuleNode child : children) {
            maxChildDepth = Math.max(maxChildDepth, maxDepth(child));
        }
        return 1 + maxChildDepth;
    }

    /**
     * Calculate maximum depth across all root nodes.
     */
    public int maxDepthForRoots(List<RuleNode> roots) {
        if (roots == null || roots.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (RuleNode root : roots) {
            max = Math.max(max, maxDepth(root));
        }
        return max;
    }
}
