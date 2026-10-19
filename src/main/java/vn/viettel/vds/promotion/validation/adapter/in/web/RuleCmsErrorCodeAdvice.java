package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.template.ErrorTemplate;
import com.promix.platform.web.template.ResponseTemplate;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import java.util.Set;

/**
 * Mã lỗi theo TKCT cho các API CMS của quy tắc kiểm tra hợp lệ ({@code /v1/rules}, {@code /v1/rule-builder}).
 *
 * <ul>
 *   <li>400 tham số / định dạng (Bean Validation, JSON hỏng, sai kiểu, thiếu tham số) → {@code INVALID_PARAMS}.</li>
 *   <li>Tạo quy tắc ({@code POST /v1/rules}, TKCT 3.1.4 bước xác nhận lưu): vi phạm ràng buộc có mã theo trường
 *       ({@code FIELD_REQUIRED}, {@code RULE_*_LENGTH_EXCEEDED}, {@code VALIDATION_RULE_CONTEXT_REQUIRED}) → đưa mã đó
 *       lên cấp trên cùng.</li>
 *   <li>Sửa quy tắc ({@code PUT /v1/rules/{ruleId}}, TKCT 3.2.2): vi phạm ràng buộc dữ liệu → {@code VALIDATION_ERROR}.</li>
 *   <li>500 → {@code INTERNAL_SYSTEM_ERROR} (504 {@code CONNECTION_TIMEOUT} do {@link ValidationGlobalExceptionHandler}).</li>
 * </ul>
 * Mã nghiệp vụ khác (409, 404, {@code VALIDATION_ERROR} của cây điều kiện...) giữ nguyên.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class RuleCmsErrorCodeAdvice implements ResponseBodyAdvice<Object> {

    static final String INVALID_PARAMS = "INVALID_PARAMS";
    static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    static final String INTERNAL_SYSTEM_ERROR = "INTERNAL_SYSTEM_ERROR";
    static final String INTERNAL_SYSTEM_ERROR_MESSAGE = "Hệ thống có lỗi xảy ra. Vui lòng thử lại sau.";

    /** Mã do thư viện sinh cho lỗi tham số / định dạng request. */
    static final Set<String> PARAM_ERROR_CODES = Set.of("VALIDATION_FAILED", "JSON_PARSE_ERROR", "JSON_MALFORMED",
            "MESSAGE_CONVERSION_ERROR", "BINDING_FAILED", "MISSING_PARAMETER", "ARGUMENT_TYPE_MISMATCH",
            "ILLEGAL_ARGUMENT");

    /** Mã theo trường của API tạo quy tắc (TKCT 3.1.4), theo thứ tự ưu tiên khi có nhiều vi phạm. */
    static final List<String> CREATE_FIELD_CODES = List.of("FIELD_REQUIRED", "RULE_NAME_LENGTH_EXCEEDED",
            "RULE_CODE_LENGTH_EXCEEDED", "RULE_DESCRIPTION_LENGTH_EXCEEDED", "RULE_FALLBACK_MESSAGE_LENGTH_EXCEEDED",
            "VALIDATION_RULE_CONTEXT_REQUIRED");

    /** Thông điệp TKCT 3.1.4 của từng mã theo trường. */
    static final java.util.Map<String, String> FIELD_CODE_MESSAGES = java.util.Map.of(
            "FIELD_REQUIRED", "Tên quy tắc kiểm tra hợp lệ không được để trống",
            "RULE_NAME_LENGTH_EXCEEDED", "Tên quy tắc vượt quá độ dài cho phép",
            "RULE_CODE_LENGTH_EXCEEDED", "Mã quy tắc vượt quá độ dài cho phép",
            "RULE_DESCRIPTION_LENGTH_EXCEEDED", "Mô tả quy tắc vượt quá độ dài cho phép",
            "RULE_FALLBACK_MESSAGE_LENGTH_EXCEEDED", "Thông báo lỗi mặc định vượt quá độ dài cho phép",
            "VALIDATION_RULE_CONTEXT_REQUIRED", "Ngữ cảnh quy tắc không được để trống");
    static final String VALIDATION_ERROR_MESSAGE = "Tham số vi phạm ràng buộc dữ liệu. Vui lòng kiểm tra lại yêu cầu.";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof ResponseTemplate<?> template) || !appliesTo(request.getURI().getPath())) {
            return body;
        }
        if (template.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            template.setCode(INTERNAL_SYSTEM_ERROR);
            template.setMessage(INTERNAL_SYSTEM_ERROR_MESSAGE);
        } else if (template.getStatus() == HttpStatus.BAD_REQUEST.value()
                && PARAM_ERROR_CODES.contains(template.getCode())) {
            String code = codeFor(request, template);
            template.setCode(code);
            if (FIELD_CODE_MESSAGES.containsKey(code)) {
                template.setMessage(FIELD_CODE_MESSAGES.get(code));
            } else if (VALIDATION_ERROR.equals(code)) {
                template.setMessage(VALIDATION_ERROR_MESSAGE);
            }
        }
        return body;
    }

    private static String codeFor(ServerHttpRequest request, ResponseTemplate<?> template) {
        String path = normalize(request.getURI().getPath());
        boolean constraintViolation = "VALIDATION_FAILED".equals(template.getCode());
        if (constraintViolation && HttpMethod.POST.equals(request.getMethod()) && path.endsWith("/v1/rules")) {
            String fieldCode = firstFieldCode(template.getErrors());
            if (fieldCode != null) {
                return fieldCode;
            }
        }
        if (constraintViolation && HttpMethod.PUT.equals(request.getMethod()) && path.matches(".*/v1/rules/[^/:]+")) {
            return VALIDATION_ERROR;
        }
        return INVALID_PARAMS;
    }

    private static String firstFieldCode(List<ErrorTemplate> errors) {
        if (errors == null) {
            return null;
        }
        for (String code : CREATE_FIELD_CODES) {
            if (errors.stream().anyMatch(e -> code.equals(e.getCode()))) {
                return code;
            }
        }
        return null;
    }

    static boolean appliesTo(String path) {
        if (path == null) {
            return false;
        }
        String p = normalize(path);
        return p.contains("/v1/rules") || p.contains("/v1/rule-builder");
    }

    private static String normalize(String path) {
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
