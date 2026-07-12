package vn.viettel.vds.promotion.validation.application.service;

import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.RuleNodeDto;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared conversion from the domain {@link RuleNode} tree to the flat node
 * shapes the pp-rule-engine compile endpoints expect.
 * <p>
 * Extracted so both the combined compile path ({@link RulePublishingService},
 * {@code POST /v1/compiler/compile}) and the eager standalone-validation compile
 * path ({@link RuleService}, {@code POST /v1/compile/validation}) build nodes
 * with the IDENTICAL keys — pp-rule-engine's {@code RuleTranslationService} reads
 * nodes by exact map key (id/type/groupLogic/operatorName/operatorVersion/params/
 * reasonCode/children), so any drift between the two paths would silently break
 * DRL generation for one of them.
 */
final class RuleNodeDtoMapper {

    private RuleNodeDtoMapper() {
    }

    /**
     * Flatten the {@link RuleNode} tree (recursively) into a list of
     * {@link RuleNodeDto} — the shape sent to {@code /v1/compiler/compile}.
     */
    static List<RuleNodeDto> flatten(List<RuleNode> nodes) {
        List<RuleNodeDto> dtos = new ArrayList<>();

        for (RuleNode node : nodes) {
            List<String> childIds = convertChildrenToIds(node);
            dtos.add(toDto(node, childIds));

            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                dtos.addAll(flatten(node.getChildren()));
            }
        }

        return dtos;
    }

    /**
     * Flatten the {@link RuleNode} tree straight into the
     * {@code List<Map<String, Object>>} shape the rule-engine
     * {@code /v1/compile/validation} endpoint (and {@code RuleTranslationService})
     * expects.
     */
    static List<Map<String, Object>> toNodeMaps(List<RuleNode> nodes) {
        return flatten(nodes).stream().map(RuleNodeDtoMapper::toMap).toList();
    }

    private static List<String> convertChildrenToIds(RuleNode node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return List.of();
        }
        List<String> childIds = new ArrayList<>();
        for (RuleNode child : node.getChildren()) {
            childIds.add(child.getId());
        }
        return childIds;
    }

    private static RuleNodeDto toDto(RuleNode node, List<String> childIds) {
        RuleNodeDto dto = new RuleNodeDto();
        dto.setId(node.getId());
        dto.setType(node.getType() != null ? node.getType().name() : null);
        dto.setGroupLogic(node.getGroupLogic() != null ? node.getGroupLogic().name() : null);
        dto.setOperatorName(node.getOperatorName());

        // operatorVersion = 1 for COND nodes (required by the rule-engine)
        if (node.getType() == RuleNode.NodeType.COND && node.getOperatorName() != null) {
            dto.setOperatorVersion(1);
        }

        // Only set params if not null AND not empty (avoid empty {} for GROUP nodes)
        if (node.getParams() != null && !node.getParams().isEmpty()) {
            dto.setParams(node.getParams());
        }

        dto.setReasonCode(node.getReasonCode());
        // Preserve prior behavior: leaf/childless nodes serialize with children unset (null),
        // not an empty "children":[] — the rule-engine COND shape depends on it.
        dto.setChildren(childIds.isEmpty() ? null : childIds);
        return dto;
    }

    private static Map<String, Object> toMap(RuleNodeDto dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotNull(map, "id", dto.getId());
        putIfNotNull(map, "type", dto.getType());
        putIfNotNull(map, "groupLogic", dto.getGroupLogic());
        putIfNotNull(map, "operatorName", dto.getOperatorName());
        putIfNotNull(map, "operatorVersion", dto.getOperatorVersion());
        putIfNotNull(map, "params", dto.getParams());
        putIfNotNull(map, "reasonCode", dto.getReasonCode());
        putIfNotNull(map, "children", dto.getChildren());
        putIfNotNull(map, "order", dto.getOrder());
        return map;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
