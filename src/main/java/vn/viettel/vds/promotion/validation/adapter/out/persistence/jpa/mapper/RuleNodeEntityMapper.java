package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting RuleNodeEntity (persistence) to RuleNode (domain)
 */
@Component
public class RuleNodeEntityMapper {

    private static final String GROUP_TYPE = "GROUP";


    private static final String NODE_TYPE_GROUP = GROUP_TYPE;

    /**
     * Convert entity to domain model
     */
    public RuleNode toDomain(RuleNodeEntity entity) {
        if (entity == null) {
            return null;
        }

        RuleNode.Builder builder = RuleNode.builder()
                .nodeId(entity.getNodeId())
                .type(parseNodeType(entity.getType()))
                .params(entity.getParams());

        // Map based on node type
        if (NODE_TYPE_GROUP.equalsIgnoreCase(entity.getType())) {
            // Group nodes don't have operator-specific fields
        } else if ("COND".equalsIgnoreCase(entity.getType())) {
            builder.operatorName(entity.getOperatorName())
                    .reasonCode(entity.getReasonCode());
        }

        return builder.build();
    }

    /**
     * Convert entity list to domain list, building tree structure.
     *
     * <p>This method builds the tree in two phases:
     * <ol>
     *   <li>Connect children to parents (set children lists in builders)</li>
     *   <li>Build nodes bottom-up (leaf nodes first, then parents)</li>
     * </ol>
     */
    public List<RuleNode> toDomainList(List<RuleNodeEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        // Phase 1: Create builders for all nodes
        Map<String, RuleNode.Builder> builderMap = entities.stream()
                .collect(Collectors.toMap(
                        RuleNodeEntity::getNodeId,
                        this::toBuilder
                ));

        // Phase 2: Connect children to GROUP nodes (build relationships in builders)
        // DON'T build nodes yet, just set children lists
        for (RuleNodeEntity entity : entities) {
            if (NODE_TYPE_GROUP.equalsIgnoreCase(entity.getType()) && entity.getChildrenIds() != null) {
                RuleNode.Builder parentBuilder = builderMap.get(entity.getNodeId());
                // Just store child node IDs, will build later
                parentBuilder.children(new ArrayList<>()); // Initialize empty, will populate after building children
            }
        }

        // Phase 3: Build nodes bottom-up using recursive helper
        Map<String, RuleNode> builtNodes = new java.util.HashMap<>();

        // Helper function to build node and its children recursively
        java.util.function.Function<String, RuleNode> buildNode = new java.util.function.Function<String, RuleNode>() {
            @Override
            public RuleNode apply(String nodeId) {
                // Check if already built
                if (builtNodes.containsKey(nodeId)) {
                    return builtNodes.get(nodeId);
                }

                RuleNode.Builder builder = builderMap.get(nodeId);
                if (builder == null) {
                    return null;
                }

                // Find entity to check if it's a GROUP node
                RuleNodeEntity entity = entities.stream()
                        .filter(e -> nodeId.equals(e.getNodeId()))
                        .findFirst()
                        .orElse(null);

                if (entity != null && NODE_TYPE_GROUP.equalsIgnoreCase(entity.getType())
                        && entity.getChildrenIds() != null) {
                    // Build children first (recursively)
                    List<RuleNode> builtChildren = entity.getChildrenIds().stream()
                            .map(this::apply)  // Recursive call
                            .filter(n -> n != null)
                            .toList();
                    // Set built children to builder
                    builder.children(builtChildren);
                }

                // Now build this node
                RuleNode node = builder.build();
                builtNodes.put(nodeId, node);
                return node;
            }
        };

        // Phase 4: Build and return root nodes
        return entities.stream()
                .filter(e -> e.getParent() == null)
                .map(e -> buildNode.apply(e.getNodeId()))
                .filter(n -> n != null)
                .toList();
    }

    /**
     * Convert entity to builder (for tree construction)
     */
    private RuleNode.Builder toBuilder(RuleNodeEntity entity) {
        RuleNode.Builder builder = RuleNode.builder()
                .nodeId(entity.getNodeId())
                .type(parseNodeType(entity.getType()))
                .params(entity.getParams());

        // Map based on node type
        if (NODE_TYPE_GROUP.equalsIgnoreCase(entity.getType())) {
            builder.groupLogic(parseLogicType(entity.getGroupLogic()));
        } else if ("COND".equalsIgnoreCase(entity.getType())) {
            builder.operatorName(entity.getOperatorName())
                    .reasonCode(entity.getReasonCode());
        }

        return builder;
    }

    /**
     * Parse node type from string
     */
    private RuleNode.NodeType parseNodeType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return RuleNode.NodeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Parse logic type from string
     */
    private Rule.LogicType parseLogicType(String logic) {
        if (logic == null) {
            return null;
        }
        try {
            return Rule.LogicType.valueOf(logic.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Rule.LogicType.ALL; // Default fallback
        }
    }

    /**
     * Convert domain to entity (for saving)
     */
    public RuleNodeEntity toEntity(RuleNode node, RuleNodeEntity parent) {
        if (node == null) {
            return null;
        }

        RuleNodeEntity entity = new RuleNodeEntity();
        entity.setNodeId(node.getNodeId());
        entity.setType(node.getType() != null ? node.getType().name() : null);
        entity.setParams(node.getParams());

        // Set validation rule ID
        // Note: validationRule entity reference should be set by caller
        // Set parent if provided
        if (parent != null) {
            entity.setParent(parent);
        }

        // Map based on node type
        if (node.getType() == RuleNode.NodeType.GROUP) {
            entity.setGroupLogic(node.getGroupLogic() != null ? node.getGroupLogic().name() : null);

            // Extract children IDs
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                List<String> childrenIds = node.getChildren().stream()
                        .map(RuleNode::getNodeId)
                        .toList();
                entity.setChildrenIds(childrenIds);
            }
        } else if (node.getType() == RuleNode.NodeType.COND) {
            entity.setOperatorName(node.getOperatorName());
            entity.setReasonCode(node.getReasonCode());
        }

        return entity;
    }
}
