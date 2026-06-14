package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleStructureException;
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

    private static final Logger logger = LoggerFactory.getLogger(RuleNodeEntityMapper.class);

    private static final String GROUP_TYPE = "GROUP";


    private static final String NODE_TYPE_GROUP = GROUP_TYPE;

    /**
     * Convert entity to domain model
     */
    public RuleNode toDomain(RuleNodeEntity entity) {
        if (entity == null) {
            logger.trace("[NODE_MAP] toDomain called with null entity");
            return null;
        }

        logger.debug("[NODE_MAP] Converting entity to domain: nodeId={}, type={}",
                entity.getNodeId(), entity.getType());

        RuleNode.Builder builder = RuleNode.builder()
                .nodeId(entity.getNodeId())
                .type(parseNodeType(entity.getType()))
                .params(entity.getParams());

        // Map based on node type
        if (NODE_TYPE_GROUP.equalsIgnoreCase(entity.getType())) {
            logger.trace("[NODE_MAP] Processing GROUP node: nodeId={}", entity.getNodeId());
            // Group nodes don't have operator-specific fields
        } else if ("COND".equalsIgnoreCase(entity.getType())) {
            logger.trace("[NODE_MAP] Processing COND node: nodeId={}, operator={}, reasonCode={}",
                    entity.getNodeId(), entity.getOperatorName(), entity.getReasonCode());
            builder.operatorName(entity.getOperatorName())
                    .reasonCode(entity.getReasonCode())
                    .violationDisplayMode(entity.getViolationDisplayMode())
                    .errorMessage(entity.getErrorMessage());
        }

        RuleNode node = builder.build();
        logger.debug("[NODE_MAP] Entity converted to domain: nodeId={}, type={}",
                node.getNodeId(), node.getType());
        return node;
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
        logger.debug("[NODE_MAP_TREE] === Starting toDomainList conversion ===");
        logger.debug("[NODE_MAP_TREE] Input: {} entities", entities != null ? entities.size() : 0);

        if (entities == null || entities.isEmpty()) {
            logger.debug("[NODE_MAP_TREE] Empty or null entities, returning empty list");
            return new ArrayList<>();
        }

        // Phase 1: Create builders for all nodes
        Map<String, RuleNode.Builder> builderMap = createBuilderMap(entities);

        // Phase 2: Connect children to GROUP nodes
        initializeGroupNodeChildren(entities, builderMap);

        // Phase 3: Build nodes bottom-up using recursive helper
        Map<String, RuleNode> builtNodes = new java.util.HashMap<>();
        Map<String, RuleNodeEntity> entityMap = entities.stream()
                .collect(Collectors.toMap(RuleNodeEntity::getNodeId, e -> e));

        // Phase 4: Build and return root nodes
        List<RuleNode> result = buildRootNodes(entities, builderMap, builtNodes, entityMap);

        logger.info("[NODE_MAP_TREE] === toDomainList complete: {} root nodes, {} total nodes built ===",
                result.size(), builtNodes.size());

        logTreeStructure(result, 0);
        return result;
    }

    /**
     * Phase 1: Create builders for all nodes.
     */
    private Map<String, RuleNode.Builder> createBuilderMap(List<RuleNodeEntity> entities) {
        logger.debug("[NODE_MAP_TREE] Phase 1: Creating builders for all nodes");
        Map<String, RuleNode.Builder> builderMap = entities.stream()
                .collect(Collectors.toMap(RuleNodeEntity::getNodeId, this::toBuilder));
        logger.debug("[NODE_MAP_TREE] Phase 1 complete: Created {} builders", builderMap.size());
        return builderMap;
    }

    /**
     * Phase 2: Initialize children lists for GROUP nodes.
     */
    private void initializeGroupNodeChildren(List<RuleNodeEntity> entities, Map<String, RuleNode.Builder> builderMap) {
        logger.debug("[NODE_MAP_TREE] Phase 2: Connecting children to GROUP nodes");
        int groupNodeCount = 0;
        for (RuleNodeEntity entity : entities) {
            if (isGroupNodeWithChildren(entity)) {
                RuleNode.Builder parentBuilder = builderMap.get(entity.getNodeId());
                parentBuilder.children(new ArrayList<>());
                groupNodeCount++;
                logGroupNodeChildren(entity);
            }
        }
        logger.debug("[NODE_MAP_TREE] Phase 2 complete: Found {} GROUP nodes with children", groupNodeCount);
    }

    /**
     * Check if entity is a GROUP node.
     * Always returns true for GROUP nodes regardless of childrenIds column state,
     * because children may be stored via parent FK instead of childrenIds column (legacy data format).
     */
    private boolean isGroupNodeWithChildren(RuleNodeEntity entity) {
        return NODE_TYPE_GROUP.equalsIgnoreCase(entity.getType());
    }

    /**
     * Log GROUP node children for tracing.
     */
    private void logGroupNodeChildren(RuleNodeEntity entity) {
        if (logger.isTraceEnabled()) {
            List<String> childIds = entity.getChildrenIds();
            logger.trace("[NODE_MAP_TREE] GROUP node '{}' has {} children IDs: [{}]",
                    entity.getNodeId(),
                    childIds != null ? childIds.size() : 0,
                    childIds != null ? String.join(", ", childIds) : "none");
        }
    }

    /**
     * Phase 4: Build and return root nodes.
     */
    private List<RuleNode> buildRootNodes(List<RuleNodeEntity> entities,
                                          Map<String, RuleNode.Builder> builderMap,
                                          Map<String, RuleNode> builtNodes,
                                          Map<String, RuleNodeEntity> entityMap) {
        logger.debug("[NODE_MAP_TREE] Phase 4: Building root nodes (parent=null)");
        List<RuleNodeEntity> rootEntities = entities.stream()
                .filter(e -> e.getParent() == null)
                .toList();
        logger.debug("[NODE_MAP_TREE] Found {} root entities", rootEntities.size());

        return rootEntities.stream()
                .map(e -> {
                    logger.debug("[NODE_MAP_TREE] Building root node: nodeId={}, type={}", e.getNodeId(), e.getType());
                    return buildNodeRecursively(e.getNodeId(), builderMap, builtNodes, entityMap);
                })
                .filter(n -> n != null)
                .toList();
    }

    /**
     * Build node and its children recursively.
     */
    private RuleNode buildNodeRecursively(String nodeId,
                                          Map<String, RuleNode.Builder> builderMap,
                                          Map<String, RuleNode> builtNodes,
                                          Map<String, RuleNodeEntity> entityMap) {
        if (builtNodes.containsKey(nodeId)) {
            logger.trace("[NODE_MAP_TREE] Node '{}' already built, returning cached", nodeId);
            return builtNodes.get(nodeId);
        }

        RuleNode.Builder builder = builderMap.get(nodeId);
        if (builder == null) {
            logger.warn("[NODE_MAP_TREE] Builder not found for nodeId: {}", nodeId);
            return null;
        }

        RuleNodeEntity entity = entityMap.get(nodeId);
        if (isGroupNodeWithChildren(entity)) {
            buildGroupNodeChildren(entity, builder, builderMap, builtNodes, entityMap);
        } else {
            logger.trace("[NODE_MAP_TREE] Building leaf COND node: {}", nodeId);
        }

        try {
            RuleNode node = builder.build();
            builtNodes.put(nodeId, node);
            logger.trace("[NODE_MAP_TREE] Node '{}' built and cached (type={})", nodeId, node.getType());
            return node;
        } catch (InvalidRuleStructureException e) {
            logger.warn("[NODE_MAP_TREE] Skipping node '{}' with invalid structure (missing/corrupt children): {}",
                    nodeId, e.getMessage());
            return null;
        }
    }

    /**
     * Build children for a GROUP node.
     * Uses childrenIds column first; falls back to parent FK lookup for legacy data
     * where childrenIds was not populated but children exist via parent_id FK.
     */
    private void buildGroupNodeChildren(RuleNodeEntity entity,
                                        RuleNode.Builder builder,
                                        Map<String, RuleNode.Builder> builderMap,
                                        Map<String, RuleNode> builtNodes,
                                        Map<String, RuleNodeEntity> entityMap) {
        List<String> childIds = entity.getChildrenIds();

        // Fallback: resolve children from parent FK when childrenIds is null/empty (legacy data format)
        if (childIds == null || childIds.isEmpty()) {
            childIds = entityMap.values().stream()
                    .filter(e -> e.getParent() != null
                            && entity.getNodeId().equals(e.getParent().getNodeId()))
                    .map(RuleNodeEntity::getNodeId)
                    .toList();
            if (!childIds.isEmpty()) {
                logger.debug("[NODE_MAP_TREE] GROUP node '{}' childrenIds empty, resolved {} children via parent FK (legacy format)",
                        entity.getNodeId(), childIds.size());
            }
        }

        logger.debug("[NODE_MAP_TREE] Building GROUP node '{}' with {} children",
                entity.getNodeId(), childIds.size());
        List<RuleNode> builtChildren = childIds.stream()
                .map(childId -> buildNodeRecursively(childId, builderMap, builtNodes, entityMap))
                .filter(n -> n != null)
                .toList();
        builder.children(builtChildren);
        logger.debug("[NODE_MAP_TREE] GROUP node '{}' built with {} children nodes",
                entity.getNodeId(), builtChildren.size());
    }

    /**
     * Log the tree structure for debugging purposes.
     */
    private void logTreeStructure(List<RuleNode> nodes, int depth) {
        if (!logger.isDebugEnabled() || nodes == null) {
            return;
        }

        String indent = "  ".repeat(depth);
        for (RuleNode node : nodes) {
            if (node.getType() == RuleNode.NodeType.GROUP) {
                logger.debug("[NODE_MAP_TREE] {}|- GROUP: nodeId={}, logic={}, children={}",
                        indent,
                        node.getNodeId(),
                        node.getGroupLogic(),
                        node.getChildren() != null ? node.getChildren().size() : 0);
                if (node.getChildren() != null) {
                    logTreeStructure(node.getChildren(), depth + 1);
                }
            } else {
                logger.debug("[NODE_MAP_TREE] {}|- COND: nodeId={}, operator={}, reasonCode={}",
                        indent,
                        node.getNodeId(),
                        node.getOperatorName(),
                        node.getReasonCode());
            }
        }
    }

    /**
     * Convert entity to builder (for tree construction)
     */
    private RuleNode.Builder toBuilder(RuleNodeEntity entity) {
        logger.trace("[NODE_MAP_TREE] Creating builder for node: nodeId={}, type={}",
                entity.getNodeId(), entity.getType());

        RuleNode.Builder builder = RuleNode.builder()
                .nodeId(entity.getNodeId())
                .type(parseNodeType(entity.getType()))
                .params(entity.getParams());

        // Map based on node type
        if (NODE_TYPE_GROUP.equalsIgnoreCase(entity.getType())) {
            builder.groupLogic(parseLogicType(entity.getGroupLogic()));
            logger.trace("[NODE_MAP_TREE] GROUP builder created: nodeId={}, groupLogic={}",
                    entity.getNodeId(), entity.getGroupLogic());
        } else if ("COND".equalsIgnoreCase(entity.getType())) {
            builder.operatorName(entity.getOperatorName())
                    .reasonCode(entity.getReasonCode())
                    .violationDisplayMode(entity.getViolationDisplayMode())
                    .errorMessage(entity.getErrorMessage());
            logger.trace("[NODE_MAP_TREE] COND builder created: nodeId={}, operator={}, reasonCode={}",
                    entity.getNodeId(), entity.getOperatorName(), entity.getReasonCode());
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
            entity.setViolationDisplayMode(node.getViolationDisplayMode());
            entity.setErrorMessage(node.getErrorMessage());
        }

        return entity;
    }
}
