package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.autoconfigure.exception.PlatformExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.viettel.vds.promotion.validation.application.service.RulePublishingService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TKCT Validation 3.4.3: deploy quy tắc sang Rule Engine thất bại → 502 RULE_ENGINE_DEPLOY_FAILED. */
@DisplayName("POST /v1/rules/publishing/rules/{ruleId} — deploy thất bại")
class RulePublishingControllerDeployFailedTest {

    private MockMvc mvc(RulePublishingService service) {
        return MockMvcBuilders.standaloneSetup(new RulePublishingController(service))
                .addPlaceholderValue("spring.application.context-path", "")
                .setControllerAdvice(new PlatformExceptionHandler(null))
                .build();
    }

    @Test
    void deployFailedReturns502() throws Exception {
        RulePublishingService service = mock(RulePublishingService.class);
        when(service.publishRule("r1")).thenReturn(RulePublishingService.RulePublishResult.failed("r1", "rule-engine 500"));

        mvc(service).perform(post("/v1/rules/publishing/rules/r1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("RULE_ENGINE_DEPLOY_FAILED"));
    }

    @Test
    void deploySuccessReturns200() throws Exception {
        RulePublishingService service = mock(RulePublishingService.class);
        when(service.publishRule("r1")).thenReturn(RulePublishingService.RulePublishResult.success("r1", "sha256:x", 10L));

        mvc(service).perform(post("/v1/rules/publishing/rules/r1"))
                .andExpect(status().isOk());
    }
}
