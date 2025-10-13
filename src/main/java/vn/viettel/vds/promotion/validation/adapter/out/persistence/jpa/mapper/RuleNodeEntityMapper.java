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
     * Convert entity list to domain list, building tree structure
     */
    public List<RuleNode> toDomainList(List<RuleNodeEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        // Create a map of nodeId -> RuleNode for easy lookup
        Map<String, RuleNode.Builder> builderMap = entities.stream()
                .collect(Collectors.toMap(
                        RuleNodeEntity::getNodeId,
                        this::toBuilder
                ));

        // Build tree structure by connecting children to parents
        for (RuleNodeEntity entity : entities) {
            if (NODE_TYPE_GROUP.equalsIgnoreCase(entity.getType()) && entity.getChildrenIds() != null) {
                RuleNode.Builder parentBuilder = builderMap.get(entity.getNodeId());
                List<RuleNode> children = entity.getChildrenIds().stream()
                        .map(builderMap::get)
                        .filter(b -> b != null)
                        .map(RuleNode.Builder::build)
                        .toList();
                parentBuilder.children(children);
            }
        }

        // Return only root nodes (nodes without parent or first node)
        // Usually first node is root in ordered list
        if (entities.get(0).getParent() == null) {
            String rootNodeId = entities.get(0).getNodeId();
            RuleNode.Builder rootBuilder = builderMap.get(rootNodeId);
            return List.of(rootBuilder.build());
        }

        // Fallback: return all nodes as flat list
        return builderMap.values().stream()
                .map(RuleNode.Builder::build)
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
