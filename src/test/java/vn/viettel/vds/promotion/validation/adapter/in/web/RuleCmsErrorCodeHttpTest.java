package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.core.exception.ConflictException;
import com.promix.platform.validation.autoconfigure.advice.ValidationExceptionHandler;
import com.promix.platform.web.autoconfigure.exception.PlatformExceptionHandler;
import com.promix.platform.web.translation.MessageTranslationService;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.CreateRuleRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.UpdateRuleRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mã lỗi theo TKCT Validation cho API CMS của quy tắc: ràng buộc trường khi tạo, VALIDATION_ERROR khi sửa,
 * INVALID_PARAMS cho request sai định dạng, INTERNAL_SYSTEM_ERROR / CONNECTION_TIMEOUT cho lỗi hệ thống. Dùng controller
 * giả cùng đường dẫn và DTO thật, cùng bộ xử lý lỗi của ứng dụng.
 */
@DisplayName("RuleCmsErrorCodeAdvice — mã lỗi API CMS quy tắc")
class RuleCmsErrorCodeHttpTest {

    @RestController
    @RequestMapping("/promotion/promotion-validation/v1/rules")
    static class StubRuleController {
        @PostMapping
        public String create(@Valid @RequestBody CreateRuleRequest request) {
            return "ok";
        }

        @PutMapping("/{ruleId}")
        public String update(@PathVariable String ruleId, @Valid @RequestBody UpdateRuleRequest request) {
            return "ok";
        }

        @GetMapping("/timeout")
        public String timeout() {
            throw new QueryTimeoutException("db timeout");
        }

        @GetMapping("/boom")
        public String boom() {
            throw new IllegalStateException("secret detail");
        }

        @GetMapping("/duplicate")
        public String duplicate() {
            throw new ConflictException("RULE_ALREADY_EXISTS", "Mã quy tắc đã tồn tại. Vui lòng nhập mã khác");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MessageTranslationService translations = mock(MessageTranslationService.class);
        lenient().when(translations.getMessage(anyString())).thenAnswer(i -> i.getArgument(0));
        lenient().when(translations.getMessage(anyString(), any())).thenAnswer(i -> i.getArgument(0));
        vn.viettel.vds.promotion.validation.application.port.in.GetRuleContextsUseCase contexts =
                mock(vn.viettel.vds.promotion.validation.application.port.in.GetRuleContextsUseCase.class);
        lenient().when(contexts.isActiveContext(anyString())).thenReturn(true);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        jakarta.validation.ConstraintValidatorFactory defaults =
                jakarta.validation.Validation.buildDefaultValidatorFactory().getConstraintValidatorFactory();
        validator.setConstraintValidatorFactory(new jakarta.validation.ConstraintValidatorFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends jakarta.validation.ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                if (key == vn.viettel.vds.promotion.validation.config.validator.RuleContextValidator.class) {
                    return (T) new vn.viettel.vds.promotion.validation.config.validator.RuleContextValidator(contexts);
                }
                return defaults.getInstance(key);
            }

            @Override
            public void releaseInstance(jakarta.validation.ConstraintValidator<?, ?> instance) {
                // không giữ tài nguyên
            }
        });
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new StubRuleController())
                .setControllerAdvice(new ValidationExceptionHandler(translations), new PlatformExceptionHandler(null),
                        new ValidationGlobalExceptionHandler(null), new RuleCmsErrorCodeAdvice())
                .setValidator(validator)
                .build();
    }

    private static final String BASE = "/promotion/promotion-validation/v1/rules";

    @Test
    @DisplayName("Tạo: tên rỗng → 400 FIELD_REQUIRED")
    void createNameBlank() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \",\"context\":\"CAMPAIGN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FIELD_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Tên quy tắc kiểm tra hợp lệ không được để trống"));
    }

    @Test
    @DisplayName("Tạo: tên quá 200 ký tự → 400 RULE_NAME_LENGTH_EXCEEDED")
    void createNameTooLong() throws Exception {
        String name = "a".repeat(201);
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"context\":\"CAMPAIGN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RULE_NAME_LENGTH_EXCEEDED"));
    }

    @Test
    @DisplayName("Tạo: body không phải JSON hợp lệ → 400 INVALID_PARAMS")
    void createMalformedBody() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMS"));
    }

    @Test
    @DisplayName("Sửa: vi phạm ràng buộc dữ liệu → 400 VALIDATION_ERROR")
    void updateConstraintViolation() throws Exception {
        mvc.perform(put(BASE + "/r1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Quá thời gian chờ → 504 CONNECTION_TIMEOUT")
    void timeout() throws Exception {
        mvc.perform(get(BASE + "/timeout"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("CONNECTION_TIMEOUT"));
    }

    @Test
    @DisplayName("Lỗi hệ thống → 500 INTERNAL_SYSTEM_ERROR, không lộ nội dung ngoại lệ")
    void internalError() throws Exception {
        mvc.perform(get(BASE + "/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SYSTEM_ERROR"))
                .andExpect(jsonPath("$.message").value("Hệ thống có lỗi xảy ra. Vui lòng thử lại sau."));
    }

    @Test
    @DisplayName("Mã nghiệp vụ khác giữ nguyên")
    void businessCodeUnchanged() throws Exception {
        mvc.perform(get(BASE + "/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RULE_ALREADY_EXISTS"));
    }
}
