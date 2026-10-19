package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.advice.ExceptionHandlerOrder;
import com.promix.platform.web.autoconfigure.exception.GlobalExceptionHandler;
import com.promix.platform.web.template.ResponseTemplate;
import com.promix.platform.web.translation.MessageTranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Lỗi không có mã riêng: 500 {@code INTERNAL_SYSTEM_ERROR}; quá thời gian chờ (đọc/ghi DB, gọi ra ngoài) ở các API CMS
 * của quy tắc → 504 {@code CONNECTION_TIMEOUT} (TKCT Validation, mục mã lỗi của VRUL001–VRUL005). Không lộ nội dung
 * ngoại lệ. Thay bean {@code GlobalExceptionHandler} mặc định của promix ({@code @ConditionalOnMissingBean}), giữ nguyên
 * các handler đã có mã riêng.
 */
@RestControllerAdvice
@Order(ExceptionHandlerOrder.WEB_DOMAIN + 1)
public class ValidationGlobalExceptionHandler extends GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ValidationGlobalExceptionHandler.class);

    static final String CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";
    static final String CONNECTION_TIMEOUT_MESSAGE = "Lỗi kết nối. Vui lòng thử lại.";

    public ValidationGlobalExceptionHandler(
            @Autowired(required = false) MessageTranslationService messageTranslationService) {
        super(messageTranslationService);
    }

    static boolean isTimeout(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof org.springframework.dao.QueryTimeoutException
                    || t instanceof org.springframework.transaction.TransactionTimedOutException
                    || t instanceof java.sql.SQLTimeoutException
                    || t instanceof java.net.SocketTimeoutException
                    || t instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static String currentPath() {
        org.springframework.web.context.request.RequestAttributes attrs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
            return org.springframework.web.util.UrlPathHelper.defaultInstance.getRequestUri(sra.getRequest());
        }
        return null;
    }

    @Override
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseTemplate<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = RuleCmsErrorCodeAdvice.INTERNAL_SYSTEM_ERROR;
        String message = RuleCmsErrorCodeAdvice.INTERNAL_SYSTEM_ERROR_MESSAGE;
        if (isTimeout(ex) && RuleCmsErrorCodeAdvice.appliesTo(currentPath())) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            code = CONNECTION_TIMEOUT;
            message = CONNECTION_TIMEOUT_MESSAGE;
        }
        ResponseTemplate<Object> body = ResponseTemplate.builder()
                .status(status.value())
                .code(code)
                .success(false)
                .message(message)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .retryable(false)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
