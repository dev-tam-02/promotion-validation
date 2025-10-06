package vn.viettel.vds.promotion.validation.adapter.out.persistence.mongo.entity;

import java.util.List;

/**
 * MongoDB subdocument for Rule nodes
 */
public class RuleNodeDocument {
    private String nodeId;
    private String field;
    private String operator;
    private Object value;
    private String logicType;
    private String description;
    private List<RuleNodeDocument> children;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getLogicType() {
        return logicType;
    }

    public void setLogicType(String logicType) {
        this.logicType = logicType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RuleNodeDocument> getChildren() {
        return children;
    }

    public void setChildren(List<RuleNodeDocument> children) {
        this.children = children;
    }
}
