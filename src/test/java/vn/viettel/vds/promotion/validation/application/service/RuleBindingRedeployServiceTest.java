package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.BindingNotFoundException;
import vn.viettel.vds.promotion.validation.domain.exception.BindingNotRedeployableException;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PROM-1437 — đường sửa dữ liệu cũ: binding đang pin bundle 24/7 phải biên dịch lại được
 * và hash mới phải được báo sang pp-rule-engine (nơi giữ cột quyết định
 * {@code assignments.timeframe_bundle_hash}).
 */
@ExtendWith(MockitoExtension.class)
class RuleBindingRedeployServiceTest {

    private static final String BINDING_ID = "019ff416-dcdc-72c5-9687-38ad4d457186";
    private static final String CAMPAIGN_ID = "019ff416-dc3c-7db5-9f9b-f781b37b44f6";
    private static final String STALE_HASH = "sha256:c5792d635791bf80";
    private static final String NEW_HASH = "sha256:87c2836fb6093734";

    @Mock
    private RuleBindingPersistencePort ruleBindingPort;
    @Mock
    private RulePublishingService rulePublishingService;
    @Mock
    private SettingValidationRuleEventPublisher eventPublisher;
    @Mock
    private ObjectProvider<SettingValidationRuleEventPublisher> eventPublisherProvider;

    private RuleBindingRedeployService service;

    @BeforeEach
    void setUp() {
        service = new RuleBindingRedeployService(ruleBindingPort, rulePublishingService, eventPublisherProvider);
    }

    @Test
    @DisplayName("binding rule-less có khung thời gian: biên dịch lại, ghi hash mới và báo rule-engine")
    void redeploysRuleLessBindingAndNotifiesRuleEngine() {
        when(ruleBindingPort.findById(BINDING_ID)).thenReturn(Optional.of(ruleLessBinding()));
        when(ruleBindingPort.save(any(RuleBinding.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventPublisherProvider.getIfAvailable()).thenReturn(eventPublisher);
        when(rulePublishingService.publishAssignmentBundle(eq(BINDING_ID), any(RuleBinding.class), eq(null), eq(true)))
                .thenReturn(RulePublishingService.RulePublishResult.success(BINDING_ID, NEW_HASH, 3936L));

        RuleBinding result = service.redeploy(BINDING_ID);

        assertThat(result.getBundleHash()).isEqualTo(NEW_HASH);

        // Sự kiện phải mang hash MỚI — đây là thứ pp-rule-engine dùng để repin assignment.
        ArgumentCaptor<RuleBinding> published = ArgumentCaptor.forClass(RuleBinding.class);
        verify(eventPublisher).publishEnableSuccessEvent(any(), eq(CAMPAIGN_ID), published.capture());
        assertThat(published.getValue().getBundleHash()).isEqualTo(NEW_HASH);
    }

    @Test
    @DisplayName("binding không tồn tại → BindingNotFoundException")
    void unknownBindingIsRejected() {
        when(ruleBindingPort.findById(BINDING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeploy(BINDING_ID))
                .isInstanceOf(BindingNotFoundException.class);
    }

    @Test
    @DisplayName("binding không active → không biên dịch")
    void inactiveBindingIsRejected() {
        when(ruleBindingPort.findById(BINDING_ID))
                .thenReturn(Optional.of(ruleLessBinding().toBuilder().active(false).build()));

        assertThatThrownBy(() -> service.redeploy(BINDING_ID))
                .isInstanceOf(BindingNotRedeployableException.class);

        verify(rulePublishingService, never())
                .publishAssignmentBundle(any(), any(RuleBinding.class), any(), anyBoolean());
    }

    /**
     * Không có kênh sự kiện thì hash mới chỉ nằm ở pp-validation, assignment bên
     * rule-engine vẫn pin bundle cũ — phải báo lỗi thay vì để lại ảo giác "đã sửa".
     */
    @Test
    @DisplayName("messaging tắt → từ chối, không ghi hash mới")
    void missingEventPublisherIsRejected() {
        when(ruleBindingPort.findById(BINDING_ID)).thenReturn(Optional.of(ruleLessBinding()));
        when(eventPublisherProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.redeploy(BINDING_ID))
                .isInstanceOf(BindingNotRedeployableException.class);

        verify(ruleBindingPort, never()).save(any(RuleBinding.class));
    }

    private RuleBinding ruleLessBinding() {
        return RuleBinding.builder()
                .id(BINDING_ID)
                .objectType("DISCOUNT_COUPON")
                .objectId(CAMPAIGN_ID)
                .active(true)
                .timezone("Asia/Ho_Chi_Minh")
                .rrule("FREQ=WEEKLY;BYDAY=TH,FR,SA")
                .bundleHash(STALE_HASH)
                .version(2L)
                .build();
    }
}
