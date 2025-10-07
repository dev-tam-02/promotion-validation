package vn.viettel.vds.promotion.validation.domain.model;

import vn.viettel.vds.promotion.validation.domain.exception.RuleEvaluationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a node in the rule tree structure
 */
public class RuleNode {
    private final String nodeId;
    private final String field;
    private final String operator;
    private final Object value;
    private final LogicType logicType;
    private final List<RuleNode> children;
    private final String description;

    // Additional fields for validation rules
    private final NodeType type;
    private final String operatorName;
    private final String reasonCode;
    private final Rule.LogicType groupLogic;
    private final java.util.Map<String, Object> params;

    public enum NodeType {
        GROUP,
        COND
    }

    private RuleNode(Builder builder) {
        this.nodeId = builder.nodeId;
        this.field = builder.field;
        this.operator = builder.operator;
        this.value = builder.value;
        this.logicType = builder.logicType;
        this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
        this.description = builder.description;
        this.type = builder.type;
        this.operatorName = builder.operatorName;
        this.reasonCode = builder.reasonCode;
        this.groupLogic = builder.groupLogic;
        this.params = builder.params;
        validate();
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        // Allow partial builds for placeholder nodes
        if (nodeId == null) {
            return;
        }

        if (isLeafNode()) {
            // Leaf nodes must have field and operator
            Objects.requireNonNull(field, "Field cannot be null for leaf node");
            Objects.requireNonNull(operator, "Operator cannot be null for leaf node");
            // Value can be null for some operators like IS_NULL, IS_NOT_NULL
        } else {
            // Parent nodes must have logic type and children
            Objects.requireNonNull(logicType, "LogicType cannot be null for parent node");
            if (children.isEmpty()) {
                throw new IllegalArgumentException("Parent node must have at least one child");
            }
        }
    }

    public boolean isLeafNode() {
        return children.isEmpty();
    }

    public boolean evaluate(ValidationContext context) {
        if (isLeafNode()) {
            return evaluateLeafNode(context);
        } else {
            return evaluateParentNode(context);
        }
    }

    private boolean evaluateLeafNode(ValidationContext context) {
        // Defensive null checks
        if (field == null) {
            throw new RuleEvaluationException("Field is null in leaf node", nodeId, field, operator);
        }
        if (operator == null) {
            throw new RuleEvaluationException("Operator is null in leaf node", nodeId, field, operator);
        }

        // Get field value from context
        Object fieldValue = context.getValue(field);

        // Allow null values only for null-checking operators
        if (fieldValue == null && !isNullCheckOperator(operator)) {
            // For non-null-check operators, treat null field value as false
            return false;
        }

        // Allow null expected value only for null-checking operators
        if (value == null && !isNullCheckOperator(operator)) {
            throw new RuleEvaluationException(
                "Expected value is null for non-null-check operator: " + operator,
                nodeId, field, operator
            );
        }

        return OperatorEvaluator.evaluate(operator, fieldValue, value);
    }

    /**
     * Check if operator is a null-checking operator that allows null values
     */
    private boolean isNullCheckOperator(String operator) {
        if (operator == null) {
            return false;
        }
        return operator.equals("IS_NULL") ||
               operator.equals("IS_NOT_NULL") ||
               operator.equals("is_null") ||
               operator.equals("is_not_null");
    }

    private boolean evaluateParentNode(ValidationContext context) {
        switch (logicType) {
            case AND:
                return children.stream().allMatch(child -> child.evaluate(context));
            case OR:
                return children.stream().anyMatch(child -> child.evaluate(context));
            case NOT:
                if (children.size() != 1) {
                    throw new IllegalStateException("NOT logic must have exactly one child");
                }
                return !children.get(0).evaluate(context);
            case XOR:
                long trueCount = children.stream()
                        .filter(child -> child.evaluate(context))
                        .count();
                return trueCount == 1;
            default:
                throw new UnsupportedOperationException("Logic type not supported: " + logicType);
        }
    }

    // Getters
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

    public LogicType getLogicType() {
        return logicType;
    }

    public List<RuleNode> getChildren() {
        return children;
    }

    public String getDescription() {
        return description;
    }

    // Getters for additional fields
    public String getId() {
        return nodeId;
    }

    public NodeType getType() {
        return type;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public Rule.LogicType getGroupLogic() {
        return groupLogic;
    }

    public java.util.Map<String, Object> getParams() {
        return params;
    }

    public static class Builder {
        private String nodeId;
        private String field;
        private String operator;
        private Object value;
        private LogicType logicType;
        private List<RuleNode> children = new ArrayList<>();
        private String description;
        private NodeType type;
        private String operatorName;
        private String reasonCode;
        private Rule.LogicType groupLogic;
        private java.util.Map<String, Object> params;

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public Builder logicType(LogicType logicType) {
            this.logicType = logicType;
            return this;
        }

        public Builder children(List<RuleNode> children) {
            this.children = children != null ? children : new ArrayList<>();
            return this;
        }

        public Builder addChild(RuleNode child) {
            this.children.add(child);
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(NodeType type) {
            this.type = type;
            return this;
        }

        public Builder operatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        public Builder reasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
            return this;
        }

        public Builder groupLogic(Rule.LogicType groupLogic) {
            this.groupLogic = groupLogic;
            return this;
        }

        public Builder params(java.util.Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public RuleNode build() {
            return new RuleNode(this);
        }

        /**
         * Build a partial node (for placeholder or incomplete nodes)
         * Does not trigger validation
         */
        public RuleNode buildPartial() {
            // Temporarily set nodeId to null to bypass validation
            String tempNodeId = this.nodeId;
            this.nodeId = null;
            RuleNode node = new RuleNode(this);
            this.nodeId = tempNodeId;
            return node;
        }

        /**
         * Check if the builder state is valid for building a complete node
         */
        public boolean isValid() {
            if (nodeId == null) {
                return false;
            }

            // Check leaf node requirements
            if (children == null || children.isEmpty()) {
                return field != null && operator != null;
            }

            // Check parent node requirements
            return logicType != null && !children.isEmpty();
        }
    }
}