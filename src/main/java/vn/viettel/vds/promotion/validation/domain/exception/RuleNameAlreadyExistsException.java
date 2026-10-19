package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ConflictException;

/**
 * Exception thrown when attempting to create (or rename) a rule with a name that
 * already exists. Name uniqueness is enforced system-wide, on the {@code name}
 * column alone, after trimming leading/trailing whitespace (SRS VRUL002_B03).
 * <p>
 * TKCT Validation 3.1.4 / 3.2.2: 409 {@code FIELD_DUPLICATE_NAME} "Tên quy tắc kiểm tra hợp lệ đã tồn tại. Vui lòng
 * nhập tên khác". CMS bắt mã này để báo lỗi tại ô Tên quy tắc (nhận cả mã cũ {@code VALIDATION_RULE_DUPLICATE_NAME}
 * trong giai đoạn chuyển đổi).
 * HTTP Status: 409 Conflict.
 */
public class RuleNameAlreadyExistsException extends ConflictException {

    static final String ERROR_CODE = "FIELD_DUPLICATE_NAME";
    static final String MESSAGE = "Tên quy tắc kiểm tra hợp lệ đã tồn tại. Vui lòng nhập tên khác";

    public RuleNameAlreadyExistsException(String name) {
        super(ERROR_CODE, MESSAGE, java.util.Map.of("entity", "ValidationRule", "field", "name", "value", name));
    }
}
