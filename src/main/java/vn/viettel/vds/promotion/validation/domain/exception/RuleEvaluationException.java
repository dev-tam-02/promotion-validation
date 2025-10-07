package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when rule evaluation fails due to invalid state or evaluation errors
 */
public class RuleEvaluationException extends RuntimeException {

    private final String nodeId;
    private final String field;
    private final String operator;

    public RuleEvaluationException(String message) {
        super(message);
        this.nodeId = null;
        this.field = null;
        this.operator = null;
    }

    public RuleEvaluationException(String message, Throwable cause) {
        super(message, cause);
        this.nodeId = null;
        this.field = null;
        this.operator = null;
    }

    public RuleEvaluationException(String message, String nodeId, String field, String operator) {
        super(message);
        this.nodeId = nodeId;
        this.field = field;
        this.operator = operator;
    }

    public RuleEvaluationException(String message, String nodeId, String field, String operator, Throwable cause) {
        super(message, cause);
        this.nodeId = nodeId;
        this.field = field;
        this.operator = operator;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (nodeId != null) {
            sb.append(" [nodeId=").append(nodeId);
        }
        if (field != null) {
            sb.append(", field=").append(field);
        }
        if (operator != null) {
            sb.append(", operator=").append(operator);
        }
        if (nodeId != null || field != null || operator != null) {
            sb.append("]");
        }
        return sb.toString();
    }
}
