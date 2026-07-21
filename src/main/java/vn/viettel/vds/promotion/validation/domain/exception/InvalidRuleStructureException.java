package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BadRequestException;

import java.util.Map;

/**
 * Exception thrown when rule structure is invalid.
 * HTTP Status: 400 Bad Request
 *
 * <p><b>Vì sao params chỉ có DUY NHẤT key {@code detail}:</b> catalog message
 * {@code INVALID_RULE_STRUCTURE=Invalid rule structure: {0}} được nội suy bởi
 * {@code HybridMessageTranslationService}, vốn lấy tham số bằng
 * {@code params.values().toArray()}. Map bất biến ({@code Map.of}) KHÔNG bảo toàn
 * thứ tự, nên khi params có nhiều key thì {0} nhận đúng một value ngẫu nhiên —
 * trước đây client chỉ thấy "Invalid rule structure: &lt;uuid&gt;" còn lý do thật
 * bị nuốt (PROM-1166). Giữ đúng một entry ⇒ {0} luôn là lý do đọc được; id của
 * rule/node được nhúng thẳng vào chuỗi detail thay vì tách key riêng.</p>
 */
public class InvalidRuleStructureException extends BadRequestException {

    private static final String ERROR_CODE = "INVALID_RULE_STRUCTURE";

    /** Key duy nhất trong params — xem javadoc lớp. */
    private static final String DETAIL = "detail";

    public InvalidRuleStructureException(String message) {
        super(ERROR_CODE, message, Map.of(DETAIL, safe(message)));
    }

    public InvalidRuleStructureException(String ruleId, String message) {
        super(ERROR_CODE, message, Map.of(DETAIL, withIds(message, ruleId, null)));
    }

    public InvalidRuleStructureException(String ruleId, String nodeId, String message) {
        super(ERROR_CODE, message, Map.of(DETAIL, withIds(message, ruleId, nodeId)));
    }

    private static String withIds(String message, String ruleId, String nodeId) {
        StringBuilder sb = new StringBuilder(safe(message));
        if (ruleId != null && !ruleId.isBlank()) {
            sb.append(" [id=").append(ruleId);
            if (nodeId != null && !nodeId.isBlank()) {
                sb.append(", nodeId=").append(nodeId);
            }
            sb.append(']');
        }
        return sb.toString();
    }

    private static String safe(String message) {
        return message != null ? message : "";
    }
}
