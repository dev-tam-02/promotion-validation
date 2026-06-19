package vn.viettel.vds.promotion.validation.domain.model;

import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleStructureException;
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
    // UI comparator chosen for this COND node (in/equals/is_more_than_or_equal_to/
    // is_not...). Persisted so the comparator is BE-owned for every operator instead
    // of being smuggled through the operatorName suffix / params / reason_code.
    private final String comparator;
    private final Rule.LogicType groupLogic;
    private final java.util.Map<String, Object> params;
    // Per-node (per-rule, control 6/7 of VRUL002_B02) configuration:
    // violationDisplayMode = how a violation surfaces (HIDDEN default / DISABLED),
    // errorMessage = node-specific message that overrides the rule-level fallback.
    private final String violationDisplayMode;
    private final String errorMessage;

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
        this.comparator = builder.comparator;
        this.groupLogic = builder.groupLogic;
        this.params = builder.params;
        this.violationDisplayMode = builder.violationDisplayMode;
        this.errorMessage = builder.errorMessage;
        validate();
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        // Allow partial builds for placeholder nodes (nodeId null = no-op placeholder, type null = ID-reference placeholder)
        if (nodeId == null || type == null) {
            return;
        }

        if (type == NodeType.GROUP) {
            // Group nodes must have groupLogic and children
            Objects.requireNonNull(groupLogic, "GroupLogic cannot be null for GROUP nodes");
            if (children.isEmpty()) {
                throw new InvalidRuleStructureException(nodeId, "GROUP node must have at least one child");
            }
        } else if (type == NodeType.COND) {
            // COND nodes must have operatorName and reasonCode
            Objects.requireNonNull(operatorName, "OperatorName cannot be null for COND nodes");
            // params can be null or empty for operators that don't require parameters
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
                    throw new InvalidRuleStructureException(nodeId, "NOT logic must have exactly one child");
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
    // Alias for getNodeId() - provided for backward compatibility and convenience
    public String getId() {
        return getNodeId();
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

    public String getComparator() {
        return comparator;
    }

    public Rule.LogicType getGroupLogic() {
        return groupLogic;
    }

    public java.util.Map<String, Object> getParams() {
        return params;
    }

    public String getViolationDisplayMode() {
        return violationDisplayMode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public enum NodeType {
        GROUP,
        COND
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
        private String comparator;
        private Rule.LogicType groupLogic;
        private java.util.Map<String, Object> params;
        private String violationDisplayMode;
        private String errorMessage;

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

        public Builder comparator(String comparator) {
            this.comparator = comparator;
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

        public Builder violationDisplayMode(String violationDisplayMode) {
            this.violationDisplayMode = violationDisplayMode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
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
            if (nodeId == null || type == null) {
                return false;
            }

            if (type == NodeType.GROUP) {
                return groupLogic != null && children != null && !children.isEmpty();
            } else if (type == NodeType.COND) {
                return operatorName != null && reasonCode != null;
            }

            return false;
        }
    }
}