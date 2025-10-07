package vn.viettel.vds.promotion.validation.domain.factory;

import vn.viettel.vds.promotion.validation.domain.model.LogicType;
import vn.viettel.vds.promotion.validation.domain.model.RuleAggregate;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleCode;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleName;
import vn.viettel.vds.promotion.validation.domain.valueobject.TenantId;

import java.util.List;
import java.util.Objects;

/**
 * Factory for creating Rule domain entities
 */
public class RuleFactory {

    /**
     * Create a new rule with basic information
     */
    public RuleAggregate createRule(
            String tenantId,
            String code,
            String name,
            LogicType logicType,
            String createdBy
    ) {
        Objects.requireNonNull(tenantId, "TenantId cannot be null");
        Objects.requireNonNull(code, "Code cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(logicType, "LogicType cannot be null");
        Objects.requireNonNull(createdBy, "CreatedBy cannot be null");

        return new RuleAggregate(
                TenantId.of(tenantId),
                RuleCode.of(code),
                RuleName.of(name),
                logicType,
                createdBy
        );
    }

    /**
     * Create a rule with nodes
     */
    public RuleAggregate createRuleWithNodes(
            String tenantId,
            String code,
            String name,
            String description,
            LogicType logicType,
            List<RuleNode> nodes,
            String createdBy
    ) {
        RuleAggregate rule = createRule(tenantId, code, name, logicType, createdBy);

        if (description != null) {
            rule.update(rule.getName(), description, logicType, nodes, createdBy);
        } else if (nodes != null && !nodes.isEmpty()) {
            rule.update(rule.getName(), rule.getDescription(), logicType, nodes, createdBy);
        }

        return rule;
    }

    /**
     * Create a simple condition rule (single node)
     */
    public RuleAggregate createSimpleConditionRule(
            String tenantId,
            String code,
            String name,
            String field,
            String operator,
            Object value,
            String createdBy
    ) {
        RuleNode node = RuleNode.builder()
                .nodeId("node-1")
                .field(field)
                .operator(operator)
                .value(value)
                .build();

        return createRuleWithNodes(
                tenantId,
                code,
                name,
                "Simple condition rule: " + field + " " + operator + " " + value,
                LogicType.AND,
                List.of(node),
                createdBy
        );
    }

    /**
     * Create a composite rule with multiple conditions
     */
    public RuleAggregate createCompositeRule(
            String tenantId,
            String code,
            String name,
            LogicType logicType,
            List<ConditionDefinition> conditions,
            String createdBy
    ) {
        List<RuleNode> nodes = conditions.stream()
                .map(this::createNodeFromCondition)
                .toList();

        return createRuleWithNodes(
                tenantId,
                code,
                name,
                "Composite rule with " + conditions.size() + " conditions",
                logicType,
                nodes,
                createdBy
        );
    }

    /**
     * Create a rule from template
     */
    public RuleAggregate createFromTemplate(
            RuleTemplate template,
            String tenantId,
            String code,
            String name,
            String createdBy
    ) {
        RuleAggregate rule = createRule(
                tenantId,
                code,
                name,
                template.getLogicType(),
                createdBy
        );

        // Apply template nodes
        if (template.getNodeTemplates() != null) {
            List<RuleNode> nodes = template.getNodeTemplates().stream()
                    .map(nodeTemplate -> createNodeFromTemplate(nodeTemplate, template.getParameters()))
                    .toList();

            rule.update(rule.getName(), template.getDescription(), template.getLogicType(), nodes, createdBy);
        }

        return rule;
    }

    private RuleNode createNodeFromCondition(ConditionDefinition condition) {
        return RuleNode.builder()
                .nodeId(condition.getId())
                .field(condition.getField())
                .operator(condition.getOperator())
                .value(condition.getValue())
                .description(condition.getDescription())
                .build();
    }

    private RuleNode createNodeFromTemplate(NodeTemplate template, java.util.Map<String, Object> parameters) {
        // Replace placeholders with actual values
        Object value = template.getValue();
        if (value instanceof String) {
            String strValue = (String) value;
            for (java.util.Map.Entry<String, Object> param : parameters.entrySet()) {
                strValue = strValue.replace("${" + param.getKey() + "}", param.getValue().toString());
            }
            value = strValue;
        }

        return RuleNode.builder()
                .nodeId(template.getNodeId())
                .field(template.getField())
                .operator(template.getOperator())
                .value(value)
                .description(template.getDescription())
                .build();
    }

    /**
     * Simple condition definition
     */
    public static class ConditionDefinition {
        private final String id;
        private final String field;
        private final String operator;
        private final Object value;
        private final String description;

        public ConditionDefinition(String id, String field, String operator, Object value, String description) {
            this.id = id;
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getField() {
            return field;
        }

        public String getOperator() {
            return operator;
        }

        public Object getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Rule template definition
     */
    public static class RuleTemplate {
        private final String templateId;
        private final String templateName;
        private final String description;
        private final LogicType logicType;
        private final List<NodeTemplate> nodeTemplates;
        private final java.util.Map<String, Object> parameters;

        public RuleTemplate(
                String templateId,
                String templateName,
                String description,
                LogicType logicType,
                List<NodeTemplate> nodeTemplates,
                java.util.Map<String, Object> parameters
        ) {
            this.templateId = templateId;
            this.templateName = templateName;
            this.description = description;
            this.logicType = logicType;
            this.nodeTemplates = nodeTemplates;
            this.parameters = parameters;
        }

        public String getTemplateId() {
            return templateId;
        }

        public String getTemplateName() {
            return templateName;
        }

        public String getDescription() {
            return description;
        }

        public LogicType getLogicType() {
            return logicType;
        }

        public List<NodeTemplate> getNodeTemplates() {
            return nodeTemplates;
        }

        public java.util.Map<String, Object> getParameters() {
            return parameters;
        }
    }

    /**
     * Node template for rule templates
     */
    public static class NodeTemplate {
        private final String nodeId;
        private final String field;
        private final String operator;
        private final Object value;
        private final String description;

        public NodeTemplate(String nodeId, String field, String operator, Object value, String description) {
            this.nodeId = nodeId;
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.description = description;
        }

        public String getNodeId() {
            return nodeId;
        }

        public String getField() {
            return field;
        }

        public String getOperator() {
            return operator;
        }

        public Object getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }
}