package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when rule evaluation fails due to invalid state or evaluation errors.
 * HTTP Status: 422 Unprocessable Entity
 */
public class RuleEvaluationException extends BusinessRuleException {

    private static final String ERROR_CODE = "RULE_EVALUATION_FAILED";

    private final String nodeId;
    private final String field;
    private final String operator;

    public RuleEvaluationException(String message) {
        super(ERROR_CODE, message);
        this.nodeId = null;
        this.field = null;
        this.operator = null;
    }

    public RuleEvaluationException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
        this.nodeId = null;
        this.field = null;
        this.operator = null;
    }

    public RuleEvaluationException(String message, String nodeId, String field, String operator) {
        super(ERROR_CODE, message, buildParams(nodeId, field, operator));
        this.nodeId = nodeId;
        this.field = field;
        this.operator = operator;
    }

    public RuleEvaluationException(String message, String nodeId, String field, String operator, Throwable cause) {
        super(ERROR_CODE, message, buildParams(nodeId, field, operator), cause);
        this.nodeId = nodeId;
        this.field = field;
        this.operator = operator;
    }

    private static Map<String, Object> buildParams(String nodeId, String field, String operator) {
        Map<String, Object> params = new HashMap<>();
        if (nodeId != null) {
            params.put("nodeId", nodeId);
        }
        if (field != null) {
            params.put("field", field);
        }
        if (operator != null) {
            params.put("operator", operator);
        }
        return params;
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
