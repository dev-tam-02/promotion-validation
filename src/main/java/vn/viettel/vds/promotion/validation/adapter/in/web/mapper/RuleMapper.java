package vn.viettel.vds.promotion.validation.adapter.in.web.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleNodeDto;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleResponse;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RuleMapper {

    public RuleResponse toRuleResponse(Rule rule) {
        if (rule == null) return null;

        RuleResponse response = new RuleResponse();
        response.setRuleId(rule.getId());
        response.setTenantId(rule.getTenantId());
        response.setCode(rule.getCode());
        response.setName(rule.getName());
        response.setState(ruleStateToString(rule.getState()));
        response.setLatestVersion(rule.getLatestVersion());
        response.setLogic(logicTypeToString(rule.getLogic()));
        response.setLimits(rule.getLimits());
        response.setNodes(ruleNodesToDto(rule.getNodes()));
        response.setNotes(rule.getNotes());
        response.setCreatedAt(rule.getCreatedAt());
        response.setCreatedBy(rule.getCreatedBy());
        response.setUpdatedAt(rule.getUpdatedAt());
        response.setUpdatedBy(rule.getUpdatedBy());

        return response;
    }

    public RuleNodeDto toRuleNodeDto(RuleNode ruleNode) {
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

    public RuleNode toRuleNode(RuleNodeDto dto) {
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

    public List<RuleNode> toRuleNodes(List<RuleNodeDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(this::toRuleNode).collect(Collectors.toList());
    }

    private String ruleStateToString(Rule.RuleState state) {
        return state != null ? state.name().toLowerCase() : null;
    }

    private String logicTypeToString(Rule.LogicType logic) {
        return logic != null ? logic.name() : null;
    }

    private String nodeTypeToString(RuleNode.NodeType type) {
        return type != null ? type.name() : null;
    }

    private RuleNode.NodeType stringToNodeType(String type) {
        return type != null ? RuleNode.NodeType.valueOf(type.toUpperCase()) : null;
    }

    private Rule.LogicType stringToLogicType(String logic) {
        return logic != null ? Rule.LogicType.valueOf(logic.toUpperCase()) : null;
    }

    private List<RuleNodeDto> ruleNodesToDto(List<RuleNode> nodes) {
        if (nodes == null) return null;
        return nodes.stream().map(this::toRuleNodeDto).collect(Collectors.toList());
    }

    private List<String> ruleNodesToIds(List<RuleNode> nodes) {
        if (nodes == null) return null;
        return nodes.stream()
                .map(RuleNode::getId)
                .collect(Collectors.toList());
    }

    private List<RuleNode> idsToRuleNodes(List<String> ids) {
        if (ids == null) return null;
        // Note: This creates placeholder nodes with only IDs set
        // In a real scenario, you would need to fetch the full nodes from a service
        return ids.stream()
                .map(id -> RuleNode.builder()
                        .nodeId(id)
                        .build())
                .collect(Collectors.toList());
    }
}