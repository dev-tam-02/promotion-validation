package vn.viettel.vds.promotion.validation.adapter.in.web.mapper;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleListItemResponse;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleNodeDto;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleResponse;
import vn.viettel.vds.promotion.validation.domain.model.ComparatorSuffix;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        response.setLatestVersion(rule.getLatestVersion());
        response.setLogic(logicTypeToString(rule.getLogic()));
        response.setLimits(usageLimitsToMap(rule.getLimits()));
        response.setNodes(ruleNodesToDto(rule.getNodes()));
        response.setNotes(rule.getNotes());
        response.setContext(rule.getContext());
        response.setDescription(rule.getDescription());
        response.setFallbackErrorMessage(rule.getFallbackErrorMessage());
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
     * Converts a Rule domain model to a light-weight {@link RuleListItemResponse} for list views.
     *
     * <p>Omits heavy fields (description, fallbackErrorMessage, nodes, notes, limits, logic)
     * that are only needed on the detail page. Use {@link #toRuleResponse} for detail views.</p>
     *
     * @param rule the domain model to convert
     * @return the light-weight list DTO, or null if input is null
     */
    @Nullable
    public RuleListItemResponse toListItemResponse(@Nullable Rule rule) {
        return toListItemResponse(rule, null);
    }

    @Nullable
    public RuleListItemResponse toListItemResponse(@Nullable Rule rule, @Nullable Long assignmentCount) {
        return toListItemResponse(rule, assignmentCount, null);
    }

    /**
     * List-view variant that lets the caller inject {@code nodeCount} computed
     * by a bulk query. Needed because paged finders don't load the rule tree —
     * {@code rule.getNodes()} is always null in list responses, which would
     * make the inline count fall back to 0 for every row.
     */
    @Nullable
    public RuleListItemResponse toListItemResponse(@Nullable Rule rule,
                                                   @Nullable Long assignmentCount,
                                                   @Nullable Integer nodeCountOverride) {
        if (rule == null) return null;

        int nodeCount = resolveNodeCount(rule, nodeCountOverride);

        return new RuleListItemResponse(
                rule.getId(),                                                     // id
                rule.getCode(),                                                   // code
                rule.getName(),                                                   // name
                rule.getContext(),                                                 // context
                rule.getRuleVersion(),                                            // ruleVersion
                rule.getVersion(),                                                // version
                nodeCount,                                                        // nodeCount
                assignmentCount,                                                  // assignmentCount
                rule.getCreatedAt(),                                              // createdAt
                rule.getCreatedBy(),                                              // createdBy
                rule.getCreatedBy(),                                              // createdByName (fallback until Keycloak)
                rule.getUpdatedAt(),                                              // updatedAt
                rule.getUpdatedBy(),                                              // updatedBy
                rule.getUpdatedBy()                                               // updatedByName (fallback until Keycloak)
        );
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
                null, // order - not available in RuleNode
                resolveNodeComparator(ruleNode),
                ruleNode.getViolationDisplayMode(),
                ruleNode.getErrorMessage()
        );
    }

    /**
     * Resolve the UI comparator for a node so the FE can pre-fill on edit and
     * render the operator label without re-deriving it client-side.
     *
     * <p>Numeric operators carry the comparator as an operator_name suffix
     * (e.g. {@code "order.total.gte"} → {@code "is_more_than_or_equal_to"}).
     * Virtual operators like {@code metadata.access} have no suffix and store the
     * comparator in {@code params.comparator} (e.g. {@code "in"}/{@code "equals"}).
     * Returns null only when neither is present (suffix-less operators whose
     * comparator currently lives in the reason code — handled FE-side until the
     * reason-code ownership moves to BE).</p>
     */
    @Nullable
    private String resolveNodeComparator(RuleNode node) {
        // 1. Persisted comparator (rows created after the comparator column exists —
        //    covers every operator, including segment is/is_not).
        if (node.getComparator() != null && !node.getComparator().isBlank()) {
            return node.getComparator();
        }
        // 2. Numeric operators encode the comparator as the operator_name suffix.
        Optional<String> fromSuffix =
                ComparatorSuffix.comparatorFromOperatorName(node.getOperatorName());
        if (fromSuffix.isPresent()) {
            return fromSuffix.get();
        }
        // 3. metadata.access stores it in params.comparator.
        Map<String, Object> params = node.getParams();
        if (params != null && params.get("comparator") instanceof String comparator
                && !comparator.isBlank()) {
            return comparator;
        }
        // 4. Legacy rows (segment is/is_not) created before the comparator column —
        //    recover it from the reason_code suffix.
        return comparatorFromReasonCode(node.getOperatorName(), node.getReasonCode());
    }

    /**
     * Recover the comparator from a legacy reason_code that encodes it as a suffix
     * after the operator-name-derived prefix (e.g. operatorName
     * {@code "customer.in_segment"} + reasonCode {@code "CUSTOMER_IN_SEGMENT_IS"}
     * → {@code "is"}). Only used as a backward-compat fallback for rows persisted
     * before the {@code comparator} column existed.
     */
    @Nullable
    private String comparatorFromReasonCode(String operatorName, String reasonCode) {
        if (operatorName == null || reasonCode == null) {
            return null;
        }
        String prefix = operatorName.replaceAll("[^a-zA-Z0-9]", "_")
                .toUpperCase()
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
        if (!prefix.isEmpty() && reasonCode.startsWith(prefix + "_")) {
            String suffix = reasonCode.substring(prefix.length() + 1).toLowerCase();
            return suffix.isEmpty() ? null : suffix;
        }
        return null;
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
                .operatorName(resolveEffectiveOperatorName(dto.operatorName(), dto.comparator()))
                .params(dto.params())
                .reasonCode(dto.reasonCode())
                .comparator(dto.comparator())
                .violationDisplayMode(dto.violationDisplayMode())
                .errorMessage(dto.errorMessage())
                .children(idsToRuleNodes(dto.children()))
                .build();
    }

    /**
     * Compose the effective operator_name from a canonical operatorName + UI comparator
     * (e.g. {@code "order.total" + "is_more_than"} → {@code "order.total.gt"}).
     *
     * <p>Returns the operatorName unchanged when:
     * <ul>
     *   <li>comparator is null/blank,</li>
     *   <li>operatorName already carries a known comparator suffix (.gt/.gte/.equals/.lt/.lte), or</li>
     *   <li>comparator is not one of the 5 numeric comparators ({@code is_more_than},
     *       {@code is_more_than_or_equal_to}, {@code is_exactly}, {@code is_less_than},
     *       {@code is_less_than_or_equal_to}) — non-numeric operators like
     *       {@code customer.in_segment} use {@code is}/{@code is_not}/{@code in} which
     *       carry semantic in {@code operatorName} itself rather than a suffix.</li>
     * </ul>
     */
    private String resolveEffectiveOperatorName(String operatorName, String comparator) {
        if (operatorName == null || operatorName.isBlank()) {
            return operatorName;
        }
        if (comparator == null || comparator.isBlank()) {
            return operatorName;
        }
        if (ComparatorSuffix.hasComparatorSuffix(operatorName)) {
            return operatorName;
        }
        if (!ComparatorSuffix.isSupported(comparator)) {
            return operatorName;
        }
        return ComparatorSuffix.resolve(operatorName, comparator);
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

    private int resolveNodeCount(Rule rule, Integer nodeCountOverride) {
        if (nodeCountOverride != null) {
            return nodeCountOverride;
        }
        return rule.getNodes() != null ? countAllNodes(rule.getNodes()) : 0;
    }

    /**
     * Counts condition (COND) nodes in a tree, traversing into GROUP children.
     *
     * <p>Surfaced as "Số lượng điều kiện" on the list/detail screens — GROUP nodes
     * are logical containers, not conditions, so they are walked through but not
     * counted. Mirrors the {@code type = 'COND'} filter in
     * {@code RuleJpaAdapter.NODE_COUNT_SUBQUERY}.</p>
     */
    private int countAllNodes(List<RuleNode> nodes) {
        int count = 0;
        for (RuleNode node : nodes) {
            if (node.getType() == RuleNode.NodeType.COND) {
                count++;
            }
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