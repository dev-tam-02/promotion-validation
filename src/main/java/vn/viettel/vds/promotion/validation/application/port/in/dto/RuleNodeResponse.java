package vn.viettel.vds.promotion.validation.application.port.in.dto;

import vn.viettel.vds.promotion.validation.domain.model.LogicType;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;

/**
 * Response DTO for a rule node
 */
public class RuleNodeResponse {

    private final String nodeId;
    private final String field;
    private final String operator;
    private final Object value;
    private final LogicType logicType;
    private final List<RuleNodeResponse> children;
    private final String description;

    private RuleNodeResponse(Builder builder) {
        this.nodeId = builder.nodeId;
        this.field = builder.field;
        this.operator = builder.operator;
        this.value = builder.value;
        this.logicType = builder.logicType;
        this.children = builder.children;
        this.description = builder.description;
    }

    public static RuleNodeResponse from(RuleNode node) {
        List<RuleNodeResponse> childResponses = node.getChildren() != null ?
                node.getChildren().stream()
                        .map(RuleNodeResponse::from)
                        .toList() :
                List.of();

        return builder()
                .nodeId(node.getNodeId())
                .field(node.getField())
                .operator(node.getOperator())
                .value(node.getValue())
                .logicType(node.getLogicType())
                .children(childResponses)
                .description(node.getDescription())
                .build();
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

    public List<RuleNodeResponse> getChildren() {
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
        private List<RuleNodeResponse> children;
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

        public Builder children(List<RuleNodeResponse> children) {
            this.children = children;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public RuleNodeResponse build() {
            return new RuleNodeResponse(this);
        }
    }
}