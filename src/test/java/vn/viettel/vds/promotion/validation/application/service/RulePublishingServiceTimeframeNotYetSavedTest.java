package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.CompileRequest;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.CompileResponse;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.WarmupRequest;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.WarmupResponse;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.config.TenantProperties;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import com.promix.platform.web.template.ResponseTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PROM-1437 — khung thời gian phải đi kèm CompileRequest ngay cả khi binding CHƯA
 * được ghi xuống DB.
 *
 * <p>Đường tạo mới ({@code SettingValidationRuleCommandHandler}) gọi deploy TRƯỚC
 * {@code ruleBindingPort.save(...)}. Trước đây {@code buildAssignmentBundleCompileRequest}
 * luôn tra lại binding bằng {@code findById} nên tra hụt → CompileRequest đi KHÔNG có
 * {@code timeLinks} → pp-rule-engine sinh bundle {@code temporal_check_allow_24_7} và
 * campaign giới hạn "Thứ 5,6,7" vẫn được API #01 trả về vào Thứ 4.
 *
 * <p>Test mô phỏng đúng tình huống đó: {@code findById} trả rỗng, binding chỉ tồn tại
 * trong bộ nhớ của caller.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RulePublishingServiceTimeframeNotYetSavedTest {

    private static final String BINDING_ID = "019ff551-2835-7066-ab65-8a76efb3c5b0";
    private static final String RRULE = "FREQ=WEEKLY;BYDAY=TH,FR,SA";

    @Mock
    private ValidationEngineClient validationEngineClient;
    @Mock
    private RulePersistencePort rulePersistencePort;
    @Mock
    private RuleBindingPersistencePort ruleBindingPersistencePort;

    private RulePublishingService newService() {
        TenantProperties tenantProperties = new TenantProperties();
        return new RulePublishingService(validationEngineClient, rulePersistencePort,
                tenantProperties, ruleBindingPersistencePort, new ObjectMapper());
    }

    private RuleBinding bindingWithDaysOfWeek() {
        return RuleBinding.builder()
                .id(BINDING_ID)
                .objectType("DISCOUNT_COUPON")
                .objectId("019ff551-244c-7a61-bded-6b0f968f3285")
                .active(true)
                .timezone("Asia/Ho_Chi_Minh")
                .rrule(RRULE)
                .build();
    }

    private void stubEngineOk() {
        CompileResponse compileResponse = new CompileResponse();
        compileResponse.setOk(true);
        compileResponse.setBundleHash("sha256:test");
        compileResponse.setArtifactBytes(new byte[]{1, 2, 3});

        ResponseTemplate<CompileResponse> compileTemplate = new ResponseTemplate<>();
        compileTemplate.setSuccess(true);
        compileTemplate.setData(compileResponse);
        when(validationEngineClient.compile(any(CompileRequest.class))).thenReturn(compileTemplate);

        WarmupResponse warmupResponse = new WarmupResponse();
        warmupResponse.setOk(true);
        ResponseTemplate<WarmupResponse> warmupTemplate = new ResponseTemplate<>();
        warmupTemplate.setSuccess(true);
        warmupTemplate.setData(warmupResponse);
        when(validationEngineClient.warmup(any(WarmupRequest.class))).thenReturn(warmupTemplate);
    }

    @Test
    @DisplayName("Binding chưa save: truyền binding vào thì timeLinks vẫn có rrule ngày trong tuần")
    void suppliedBindingCarriesTimeframeEvenWhenNotPersistedYet() {
        stubEngineOk();
        // Đúng trạng thái của đường create: hàng chưa tồn tại trong DB.
        when(ruleBindingPersistencePort.findById(BINDING_ID)).thenReturn(Optional.empty());

        newService().publishAssignmentBundle(BINDING_ID, bindingWithDaysOfWeek(), null, true);

        ArgumentCaptor<CompileRequest> captor = ArgumentCaptor.forClass(CompileRequest.class);
        verify(validationEngineClient).compile(captor.capture());

        List<CompileRequest.TimeLink> timeLinks = captor.getValue().getTimeLinks();
        assertThat(timeLinks)
                .as("timeLinks phải có mặt, nếu không rule-engine sinh bundle ALLOW 24/7")
                .isNotNull()
                .hasSize(1);
        assertThat(timeLinks.get(0).getData().getRrule()).isEqualTo(RRULE);
        // Không khai giờ trong ngày ⇒ mặc định cả ngày; thiếu window thì generator bỏ qua BYDAY.
        assertThat(timeLinks.get(0).getData().getWindows())
                .as("phải có window mặc định 00:00-23:59 thì BYDAY mới được sinh vào DRL")
                .hasSize(1);
    }

    @Test
    @DisplayName("Không truyền binding: vẫn tra DB như cũ (đường publish lại, binding đã có sẵn)")
    void fallsBackToDatabaseLookupWhenNoBindingSupplied() {
        stubEngineOk();
        when(ruleBindingPersistencePort.findById(BINDING_ID)).thenReturn(Optional.of(bindingWithDaysOfWeek()));

        newService().publishAssignmentBundle(BINDING_ID, null, true);

        ArgumentCaptor<CompileRequest> captor = ArgumentCaptor.forClass(CompileRequest.class);
        verify(validationEngineClient).compile(captor.capture());
        assertThat(captor.getValue().getTimeLinks()).hasSize(1);
        assertThat(captor.getValue().getTimeLinks().get(0).getData().getRrule()).isEqualTo(RRULE);
    }
}
