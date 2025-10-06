package vn.viettel.vds.promotion.validation.domain.model;

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

    private RuleNode(Builder builder) {
        this.nodeId = builder.nodeId;
        this.field = builder.field;
        this.operator = builder.operator;
        this.value = builder.value;
        this.logicType = builder.logicType;
        this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
        this.description = builder.description;
        validate();
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        Objects.requireNonNull(nodeId, "NodeId cannot be null");

        if (isLeafNode()) {
            Objects.requireNonNull(field, "Field cannot be null for leaf node");
            Objects.requireNonNull(operator, "Operator cannot be null for leaf node");
            // Value can be null for some operators like IS_NULL
        } else {
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
        Object fieldValue = context.getValue(field);
        return OperatorEvaluator.evaluate(operator, fieldValue, value);
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

    public static class Builder {
        private String nodeId;
        private String field;
        private String operator;
        private Object value;
        private LogicType logicType;
        private List<RuleNode> children = new ArrayList<>();
        private String description;

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

        public RuleNode build() {
            return new RuleNode(this);
        }
    }
}