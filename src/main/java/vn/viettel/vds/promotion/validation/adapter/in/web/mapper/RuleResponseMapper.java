package vn.viettel.vds.promotion.validation.adapter.in.web.mapper;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleNodeDto;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleResponse;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;

/**
 * Manual mapper for converting between Rule domain model and RuleResponse DTOs.
 *
 * <p><strong>Why Manual Mapping Instead of MapStruct?</strong></p>
 * <p>This mapper is intentionally implemented as a manual Spring component rather than using MapStruct because:</p>
 * <ul>
 *   <li><strong>Complex Tree Structure</strong>: RuleNode contains recursive tree structures with children
 *       that require custom traversal logic. MapStruct struggles with deep recursive mappings.</li>
 *   <li><strong>Bidirectional Conversion Complexity</strong>: The toRuleNode and toRuleResponse methods
 *       have asymmetric logic - IDs vs full objects, placeholders vs complete nodes.</li>
 *   <li><strong>Custom Placeholder Logic</strong>: The idsToRuleNodes method creates placeholder nodes
 *       with only IDs set, which is a business logic decision that MapStruct can't handle declaratively.</li>
 *   <li><strong>Enum Case Transformations</strong>: Custom case transformations (toLowerCase for state,
 *       toUpperCase for types) with null safety require custom methods anyway.</li>
 * </ul>
 *
 * <p><strong>Null Safety Strategy:</strong></p>
 * <ul>
 *   <li>All public methods perform null checks on input parameters</li>
 *   <li>Collection methods return null (not empty lists) when input is null, maintaining consistency</li>
 *   <li>Enum conversion methods return null for null inputs</li>
 *   <li>Methods are annotated with @Nullable and @NonNull for clarity</li>
 * </ul>
 */
@Component
public class RuleResponseMapper {

    /**
     * Converts a Rule domain model to RuleResponse DTO.
     *
     * @param rule the domain model to convert
     * @return the corresponding DTO, or null if input is null
     */
    @Nullable
    public RuleResponse toRuleResponse(@Nullable Rule rule) {
        if (rule == null) return null;

        RuleResponse response = new RuleResponse();
        response.setRuleId(rule.getId());
        response.setCode(rule.getCode());
        response.setName(rule.getName());
        response.setState(ruleStateToString(rule.getState()));
        response.setLatestVersion(rule.getLatestVersion());
        response.setLogic(logicTypeToString(rule.getLogic()));
        response.setLimits(usageLimitsToMap(rule.getLimits()));
        response.setNodes(ruleNodesToDto(rule.getNodes()));
        response.setNotes(rule.getNotes());
        response.setContext(rule.getContext());
        response.setDescription(rule.getDescription());
        response.setVersion(rule.getVersion());
        response.setNodeCount(rule.getNodes() != null ? countAllNodes(rule.getNodes()) : 0);
        response.setCreatedAt(rule.getCreatedAt());
        response.setCreatedBy(rule.getCreatedBy());
        response.setCreatedByName(rule.getCreatedBy()); // Fallback to userId until Keycloak integration
        response.setUpdatedAt(rule.getUpdatedAt());
        response.setUpdatedBy(rule.getUpdatedBy());
        response.setUpdatedByName(rule.getUpdatedBy()); // Fallback to userId until Keycloak integration

        return response;
    }

    /**
     * Converts a RuleNode domain model to RuleNodeDto.
     *
     * <p>Note: Some fields (operatorVersion, order) are not available in the RuleNode
     * domain model and are set to null in the DTO.</p>
     *
     * @param ruleNode the domain model to convert
     * @return the corresponding DTO, or null if input is null
     */
    @Nullable
    public RuleNodeDto toRuleNodeDto(@Nullable RuleNode ruleNode) {
        if (ruleNode == null) return null;

        return new RuleNodeDto(
                ruleNode.getId(),
                nodeTypeToString(ruleNode.getType()),
                logicTypeToString(ruleNode.getGroupLogic()),
                ruleNode.getOperatorName(),
                null, // operatorVersion - not available in RuleNode
                ruleNode.getParams(),
                ruleNode.getReasonCode(),
                ruleNodesToIds(ruleNode.getChildren()),
                null  // order - not available in RuleNode
        );
    }

    /**
     * Converts a RuleNodeDto to RuleNode domain model.
     *
     * <p>Note: This creates a RuleNode with child references as placeholder nodes
     * containing only IDs. The repository layer should resolve these to full nodes.</p>
     *
     * @param dto the DTO to convert
     * @return the corresponding domain model, or null if input is null
     */
    @Nullable
    public RuleNode toRuleNode(@Nullable RuleNodeDto dto) {
        if (dto == null) return null;

        return RuleNode.builder()
                .nodeId(dto.id())
                .type(stringToNodeType(dto.type()))
                .groupLogic(stringToLogicType(dto.groupLogic()))
                .operatorName(dto.operatorName())
                .params(dto.params())
                .reasonCode(dto.reasonCode())
                .children(idsToRuleNodes(dto.children()))
                .build();
    }

    /**
     * Converts a list of RuleNodeDto to a list of RuleNode domain models.
     *
     * @param dtos the list of DTOs to convert
     * @return the list of domain models, or null if input is null
     */
    @Nullable
    public List<RuleNode> toRuleNodes(@Nullable List<RuleNodeDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream()
                .map(this::toRuleNode)
                .toList();
    }

    /**
     * Converts UsageLimits object to Map for API response.
     */
    @Nullable
    private java.util.Map<String, Object> usageLimitsToMap(@Nullable Rule.UsageLimits limits) {
        if (limits == null) return null;

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (limits.getPerCodeTotal() != null) {
            map.put("perCodeTotal", limits.getPerCodeTotal());
        }
        if (limits.getPerCustomer() != null) {
            map.put("perCustomer", limits.getPerCustomer());
        }
        if (limits.getPerDay() != null) {
            map.put("perDay", limits.getPerDay());
        }
        if (limits.getPerTransaction() != null) {
            map.put("perTransaction", limits.getPerTransaction());
        }
        if (limits.getRemaining() != null) {
            map.put("remaining", limits.getRemaining());
        }
        return map;
    }

    /**
     * Converts RuleState enum to lowercase string representation.
     */
    @Nullable
    private String ruleStateToString(@Nullable Rule.RuleState state) {
        return state != null ? state.name().toLowerCase() : null;
    }

    /**
     * Converts LogicType enum to string representation.
     */
    @Nullable
    private String logicTypeToString(@Nullable Rule.LogicType logic) {
        return logic != null ? logic.name() : null;
    }

    /**
     * Converts NodeType enum to string representation.
     */
    @Nullable
    private String nodeTypeToString(@Nullable RuleNode.NodeType type) {
        return type != null ? type.name() : null;
    }

    /**
     * Converts string to NodeType enum with case normalization.
     */
    @Nullable
    private RuleNode.NodeType stringToNodeType(@Nullable String type) {
        if (type == null) return null;
        try {
            return RuleNode.NodeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // Return null for invalid values
        }
    }

    /**
     * Converts string to LogicType enum with case normalization.
     */
    @Nullable
    private Rule.LogicType stringToLogicType(@Nullable String logic) {
        if (logic == null) return null;
        try {
            return Rule.LogicType.valueOf(logic.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // Return null for invalid values
        }
    }

    /**
     * Counts total nodes (including nested children) in a node list.
     */
    private int countAllNodes(List<RuleNode> nodes) {
        int count = 0;
        for (RuleNode node : nodes) {
            count++;
            if (node.getChildren() != null) {
                count += countAllNodes(node.getChildren());
            }
        }
        return count;
    }

    /**
     * Converts a list of RuleNode domain models to a list of RuleNodeDto.
     * <p>This method flattens the tree structure into a flat list containing all nodes.</p>
     */
    @Nullable
    private List<RuleNodeDto> ruleNodesToDto(@Nullable List<RuleNode> nodes) {
        if (nodes == null) return null;

        // Flatten tree to list using recursive helper
        List<RuleNodeDto> result = new java.util.ArrayList<>();
        for (RuleNode node : nodes) {
            flattenNode(node, result);
        }
        return result;
    }

    /**
     * Recursively flattens a node and its children into a flat list.
     */
    private void flattenNode(RuleNode node, List<RuleNodeDto> result) {
        // Add current node
        result.add(toRuleNodeDto(node));

        // Recursively add children
        if (node.getChildren() != null) {
            for (RuleNode child : node.getChildren()) {
                flattenNode(child, result);
            }
        }
    }

    /**
     * Extracts IDs from a list of RuleNode objects.
     */
    @Nullable
    private List<String> ruleNodesToIds(@Nullable List<RuleNode> nodes) {
        if (nodes == null) return null;
        return nodes.stream()
                .map(RuleNode::getId)
                .toList();
    }

    /**
     * Creates placeholder RuleNode objects from a list of IDs.
     *
     * <p><strong>Important:</strong> This creates placeholder nodes with only IDs set.
     * In a real scenario, the repository layer should fetch and populate the full nodes
     * from the database or cache.</p>
     *
     * @param ids the list of node IDs
     * @return list of placeholder RuleNode objects, or null if input is null
     */
    @Nullable
    private List<RuleNode> idsToRuleNodes(@Nullable List<String> ids) {
        if (ids == null) return null;
        return ids.stream()
                .map(id -> RuleNode.builder()
                        .nodeId(id)
                        .build())
                .toList();
    }
}