package vn.viettel.vds.promotion.validation.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TKCT Validation 3.1.4 / 3.2.2: trùng tên quy tắc → 409 FIELD_DUPLICATE_NAME. */
@DisplayName("RuleNameAlreadyExistsException")
class RuleNameAlreadyExistsExceptionTest {

    @Test
    void codeAndMessageFollowTkct() {
        RuleNameAlreadyExistsException e = new RuleNameAlreadyExistsException("Quy tắc VIP");

        assertThat(e.getErrorCode()).isEqualTo("FIELD_DUPLICATE_NAME");
        assertThat(e.getMessage()).isEqualTo("Tên quy tắc kiểm tra hợp lệ đã tồn tại. Vui lòng nhập tên khác");
    }
}
