package vn.viettel.vds.promotion.validation.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import vn.viettel.vds.promotion.validation.domain.model.LogicType;

import java.util.List;

/**
 * Command representing a rule node in the rule tree
 */
public class RuleNodeCommand {

    @NotBlank(message = "Node ID is required")
    private final String nodeId;

    private final String field;
    private final String operator;
    private final Object value;
    private final LogicType logicType;
    private final List<RuleNodeCommand> children;
    private final String description;

    public RuleNodeCommand(
            String nodeId,
            String field,
            String operator,
            Object value,
            LogicType logicType,
            List<RuleNodeCommand> children,
            String description
    ) {
        this.nodeId = nodeId;
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.logicType = logicType;
        this.children = children;
        this.description = description;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
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

    public List<RuleNodeCommand> getChildren() {
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
        private List<RuleNodeCommand> children;
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

        public Builder children(List<RuleNodeCommand> children) {
            this.children = children;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public RuleNodeCommand build() {
            return new RuleNodeCommand(nodeId, field, operator, value, logicType, children, description);
        }
    }
}