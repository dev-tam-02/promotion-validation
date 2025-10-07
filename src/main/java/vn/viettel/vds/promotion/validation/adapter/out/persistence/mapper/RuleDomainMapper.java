package vn.viettel.vds.promotion.validation.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity.RuleDocument;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity.RuleNodeDocument;
import vn.viettel.vds.promotion.validation.domain.model.LogicType;
import vn.viettel.vds.promotion.validation.domain.model.RuleAggregate;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.model.RuleStatus;
import vn.viettel.vds.promotion.validation.domain.valueobject.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between RuleAggregate domain objects and RuleDocument entities
 */
@Component
public class RuleDomainMapper {

    /**
     * Convert domain RuleAggregate to RuleDocument
     */
    public RuleDocument toDocument(RuleAggregate rule) {
        RuleDocument document = new RuleDocument();
        document.setId(rule.getId().getValue());
        document.setTenantId(rule.getTenantId().getValue());
        document.setCode(rule.getCode().getValue());
        document.setName(rule.getName().getValue());
        document.setDescription(rule.getDescription());
        document.setLogicType(rule.getLogicType().name());
        document.setStatus(rule.getStatus().name());
        document.setVersion(rule.getVersion().toString());
        document.setCreatedAt(rule.getCreatedAt());
        document.setUpdatedAt(rule.getUpdatedAt());
        document.setCreatedBy(rule.getCreatedBy());
        document.setUpdatedBy(rule.getUpdatedBy());
        document.setPublishedAt(rule.getPublishedAt());
        document.setPublishedBy(rule.getPublishedBy());

        // Map nodes
        if (rule.getNodes() != null) {
            List<RuleNodeDocument> nodeDocuments = rule.getNodes().stream()
                    .map(this::toNodeDocument)
                    .collect(Collectors.toList());
            document.setNodes(nodeDocuments);
        }

        return document;
    }

    /**
     * Convert RuleDocument to domain RuleAggregate
     */
    public RuleAggregate toDomain(RuleDocument document) {
        // Map nodes
        List<RuleNode> nodes = null;
        if (document.getNodes() != null) {
            nodes = document.getNodes().stream()
                    .map(this::toRuleNode)
                    .collect(Collectors.toList());
        }

        String versionStr = (document.getVersion() != null && !document.getVersion().isEmpty())
                ? document.getVersion()
                : "1.0.0";

        return RuleAggregate.builder()
                .id(RuleId.of(document.getId()))
                .tenantId(TenantId.of(document.getTenantId()))
                .code(RuleCode.of(document.getCode()))
                .name(RuleName.of(document.getName()))
                .description(document.getDescription())
                .logicType(LogicType.valueOf(document.getLogicType()))
                .nodes(nodes)
                .status(RuleStatus.valueOf(document.getStatus()))
                .version(Version.parse(versionStr))
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .createdBy(document.getCreatedBy())
                .updatedBy(document.getUpdatedBy())
                .publishedAt(document.getPublishedAt())
                .publishedBy(document.getPublishedBy())
                .build();
    }

    /**
     * Convert domain RuleNode to RuleNodeDocument
     */
    private RuleNodeDocument toNodeDocument(RuleNode node) {
        RuleNodeDocument document = new RuleNodeDocument();
        document.setNodeId(node.getNodeId());
        document.setField(node.getField());
        document.setOperator(node.getOperator());
        document.setValue(node.getValue());
        document.setDescription(node.getDescription());

        if (node.getLogicType() != null) {
            document.setLogicType(node.getLogicType().name());
        }

        // Map children recursively
        if (node.getChildren() != null) {
            List<RuleNodeDocument> childDocuments = node.getChildren().stream()
                    .map(this::toNodeDocument)
                    .collect(Collectors.toList());
            document.setChildren(childDocuments);
        }

        return document;
    }

    /**
     * Convert RuleNodeDocument to domain RuleNode
     */
    private RuleNode toRuleNode(RuleNodeDocument document) {
        // Map children recursively
        List<RuleNode> children = null;
        if (document.getChildren() != null) {
            children = document.getChildren().stream()
                    .map(this::toRuleNode)
                    .collect(Collectors.toList());
        }

        LogicType logicType = null;
        if (document.getLogicType() != null) {
            logicType = LogicType.valueOf(document.getLogicType());
        }

        return RuleNode.builder()
                .nodeId(document.getNodeId())
                .field(document.getField())
                .operator(document.getOperator())
                .value(document.getValue())
                .logicType(logicType)
                .children(children)
                .description(document.getDescription())
                .build();
    }
}