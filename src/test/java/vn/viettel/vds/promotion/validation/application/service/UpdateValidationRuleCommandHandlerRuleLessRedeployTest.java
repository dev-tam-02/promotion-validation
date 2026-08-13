package vn.viettel.vds.promotion.validation.application.service;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.UpdateValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.UpdateValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.UpdateValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.ValidityTimeframe;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PROM-1437 — binding KHÔNG gắn rule nghiệp vụ (chỉ mang khung thời gian) phải được
 * biên dịch lại khi cập nhật.
 *
 * <p>Trước đây {@code deployRuleToEngine} thấy {@code ruleId == null} là log "Skipping
 * deployment - no ruleId" rồi thoát, nên binding giữ nguyên {@code bundleHash} sinh ra lúc
 * tạo. Lúc tạo thì binding chưa kịp lưu xuống DB nên {@code timeLinks} rơi mất khỏi
 * CompileRequest và pp-rule-engine trả về bundle {@code temporal_check_allow_24_7} —
 * chiến dịch giới hạn "Thứ 5,6,7" vẫn được API Find Eligible Campaigns trả về vào Thứ 4,
 * và sửa lại trên CMS bao nhiêu lần cũng không chữa được.
 */
@ExtendWith(MockitoExtension.class)
class UpdateValidationRuleCommandHandlerRuleLessRedeployTest {

    private static final String CAMPAIGN_ID = "019ff416-dc3c-7db5-9f9b-f781b37b44f6";
    private static final String BINDING_ID = "019ff416-dcdc-72c5-9687-38ad4d457186";
    private static final String OBJECT_TYPE = "DISCOUNT_COUPON";
    private static final String STALE_HASH = "sha256:c5792d635791bf80";
    private static final String NEW_HASH = "sha256:87c2836fb6093734";

    @Mock
    private RuleBindingPersistencePort ruleBindingPort;
    @Mock
    private ValidationRuleRepositoryPort validationRulePort;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private RulePublishingService rulePublishingService;
    @Mock
    private ValidationRuleSnapshotService snapshotService;
    @Mock
    private SettingValidationRuleCommandHandler settingHandler;
    @Mock
    private Validator validator;
    @Mock
    private UpdateValidationRuleCommandDTOMapper dtoMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UpdateValidationRuleCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateValidationRuleCommandHandler(
                ruleBindingPort, validationRulePort, idempotencyService, rulePublishingService,
                snapshotService, settingHandler, validator, dtoMapper, kafkaTemplate);
    }

    @Test
    @DisplayName("binding rule-less có khung thời gian: biên dịch lại kèm binding và ghi bundleHash mới")
    void ruleLessBindingWithTimeframe_isRedeployedWithSuppliedBinding() {
        givenExistingBinding(bindingWithDaysOfWeek());
        when(rulePublishingService.publishAssignmentBundle(
                eq(BINDING_ID), any(RuleBinding.class), any(), anyBoolean()))
                .thenReturn(RulePublishingService.RulePublishResult.success(BINDING_ID, NEW_HASH, 3936L));

        assertThat(handler.handleCommand(commandWithDaysOfWeek())).isTrue();

        // Binding phải được TRUYỀN THẲNG vào publish (không để service tự tra DB) và cờ
        // hasTemporalPolicy phải bật, nếu không timeLinks lại rỗng như lỗi gốc.
        ArgumentCaptor<RuleBinding> published = ArgumentCaptor.forClass(RuleBinding.class);
        verify(rulePublishingService).publishAssignmentBundle(
                eq(BINDING_ID), published.capture(), eq(null), eq(true));
        assertThat(published.getValue().getRrule()).isEqualTo("FREQ=WEEKLY;BYDAY=TH,FR,SA");

        // bundleHash cũ (bundle 24/7) phải bị thay bằng hash vừa biên dịch.
        ArgumentCaptor<RuleBinding> saved = ArgumentCaptor.forClass(RuleBinding.class);
        verify(ruleBindingPort, atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues())
                .extracting(RuleBinding::getBundleHash)
                .contains(NEW_HASH);
    }

    @Test
    @DisplayName("binding rule-less KHÔNG có ràng buộc thời gian lẫn phạm vi: không biên dịch")
    void ruleLessBindingWithoutAnyConstraint_isNotDeployed() {
        givenExistingBinding(RuleBinding.builder()
                .id(BINDING_ID)
                .objectType(OBJECT_TYPE)
                .objectId(CAMPAIGN_ID)
                .active(true)
                .version(1L)
                .build());

        // timeframe rỗng hoàn toàn → binding không còn ràng buộc nào để biên dịch.
        assertThat(handler.handleCommand(command(TimeFrame.builder().build()))).isTrue();

        verify(rulePublishingService, never())
                .publishAssignmentBundle(any(), any(RuleBinding.class), any(), anyBoolean());
    }

    private void givenExistingBinding(RuleBinding existing) {
        when(ruleBindingPort.findByObject(OBJECT_TYPE, CAMPAIGN_ID)).thenReturn(List.of(existing));
        when(ruleBindingPort.save(any(RuleBinding.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private RuleBinding bindingWithDaysOfWeek() {
        return RuleBinding.builder()
                .id(BINDING_ID)
                .objectType(OBJECT_TYPE)
                .objectId(CAMPAIGN_ID)
                .active(true)
                .timezone("Asia/Ho_Chi_Minh")
                .rrule("FREQ=WEEKLY;BYDAY=TH,FR,SA")
                .bundleHash(STALE_HASH)
                .version(2L)
                .build();
    }

    /** Payload saga gửi khi campaign chỉ đặt "Có hiệu lực vào Thứ 5, Thứ 6, Thứ 7". */
    private UpdateValidationRuleCommand commandWithDaysOfWeek() {
        return command(TimeFrame.builder()
                .validityDaysOfWeek(List.of(4, 5, 6))
                .validityTimeframe(ValidityTimeframe.builder().build())
                .timezone("Asia/Ho_Chi_Minh")
                .build());
    }

    private UpdateValidationRuleCommand command(TimeFrame timeframe) {
        UpdateValidationRuleCommand command = UpdateValidationRuleCommand.builder()
                .id("cmd-1437-ruleless")
                .type("UpdateValidationRuleCommand")
                .subject(CAMPAIGN_ID)
                .occurredAt(Instant.now())
                .payload(UpdateValidationRuleCommandPayload.builder()
                        .objectType(OBJECT_TYPE)
                        .objectId(CAMPAIGN_ID)
                        .timeframe(timeframe)
                        .updatedBy("system")
                        .build())
                .build();

        UpdateValidationRuleCommandDTO dto = new UpdateValidationRuleCommandDTO();
        when(dtoMapper.toDTO(command)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(idempotencyService.isProcessed("cmd-1437-ruleless")).thenReturn(false);
        return command;
    }
}
