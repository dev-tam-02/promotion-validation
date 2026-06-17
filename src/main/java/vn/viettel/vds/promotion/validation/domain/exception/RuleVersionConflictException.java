package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when a concurrent update is detected via optimistic locking.
 * The client's version is stale — another request has already saved a newer version.
 *
 * <p>HTTP Status: 409 Conflict (CONFLICTED)
 */
public class RuleVersionConflictException extends ConflictException {

    // SRS VRUL003/VRUL005 (chốt với BA): mọi xung đột phiên bản (sửa/xóa) trả về
    // một mã lỗi chung duy nhất CONFLICTED (HTTP 409).
    private static final String ERROR_CODE = "CONFLICTED";

    public RuleVersionConflictException(String ruleId) {
        super(ERROR_CODE,
                "Concurrent update detected: rule has been modified by another request. Re-fetch and retry. ruleId=" + ruleId,
                "ValidationRule", "id", ruleId);
    }
}
