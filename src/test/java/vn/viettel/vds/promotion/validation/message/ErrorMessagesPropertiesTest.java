package vn.viettel.vds.promotion.validation.message;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the delete-rule user-facing messages (PROM-1228/1229/1230). The keys must
 * exist in the fallback bundle so BaseExceptionHandler resolves a Vietnamese message
 * instead of leaking the exception's English text + rule UUID to the CMS.
 */
class ErrorMessagesPropertiesTest {

    private Properties load() throws Exception {
        Properties p = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("messages/error_messages.properties")) {
            assertThat(is).as("bundle messages/error_messages.properties on classpath").isNotNull();
            p.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    void ruleHasBindingsKeyIsVietnameseAndPresent() throws Exception {
        assertThat(load().getProperty("RULE_HAS_BINDINGS"))
                .isEqualTo("Quy tắc đã được gán cho chiến dịch, không thể xóa");
    }

    @Test
    void conflictedKeyIsVietnameseAndPresent() throws Exception {
        assertThat(load().getProperty("CONFLICTED"))
                .isEqualTo("Dữ liệu đã bị thay đổi bởi người dùng khác. Vui lòng tải lại trang và thử lại.");
    }

    @Test
    void ruleNotFoundWordingMatchesSrs() throws Exception {
        assertThat(load().getProperty("VALIDATION_RULE_NOT_FOUND"))
                .isEqualTo("Quy tắc kiểm tra hợp lệ không tồn tại hoặc đã bị xóa.");
    }
}
