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
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.ValidityHoursPerDay;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.ValidityTimeframe;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Timeframe section of an update command is authoritative (replace, not merge).
 * Regression for the staging bug where unticking "Giữ hiệu lực trong một khoảng
 * thời gian cụ thể sau khi phát hành" or deleting khung giới hạn thời gian could
 * never clear the old binding values: set-if-present on top of existing.toBuilder()
 * kept duration/activityDurationAfterPublishing/rrule/timeWindows alive, and the
 * edit screen (reading the live binding) kept showing the pre-edit values.
 */
@ExtendWith(MockitoExtension.class)
class UpdateValidationRuleCommandHandlerTimeframeReplaceTest {

    private static final String CAMPAIGN_ID = "019eaf94-6ccf-74e9-8182-de203eff6c96";
    private static final String OBJECT_TYPE = "CAMPAIGN";
    private static final Instant START = Instant.parse("2026-06-11T03:28:00Z");

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
    @DisplayName("untick 'hiệu lực sau phát hành' + xóa khung giờ: absent temporal fields clear old binding values")
    void absentTemporalFields_clearExistingBindingValues() {
        // FE bỏ tick + xóa khung giờ → saga gửi validityTimeframe chỉ còn startDate.
        TimeFrame timeframe = TimeFrame.builder()
                .validityTimeframe(ValidityTimeframe.builder()
                        .startDate(START)
                        .build())
                .timezone("Asia/Ho_Chi_Minh")
                .build();

        RuleBinding saved = runUpdate(timeframe);

        assertThat(saved.getValidFrom()).isEqualTo(START);
        assertThat(saved.getValidTo()).isNull();
        assertThat(saved.getRrule()).isNull();
        assertThat(saved.getDuration()).isNull();
        assertThat(saved.getActivityDurationAfterPublishing()).isNull();
        assertThat(saved.getTimeWindows()).isNull();
        assertThat(saved.getScopeTimeWindows()).isNull();
    }

    @Test
    @DisplayName("temporal fields present in payload still overwrite binding (set path unchanged)")
    void presentTemporalFields_overwriteBinding() {
        TimeFrame timeframe = TimeFrame.builder()
                .validityTimeframe(ValidityTimeframe.builder()
                        .startDate(START)
                        .interval("P100D")
                        .duration("PT100H")
                        .activityDurationAfterPublishing("P126W")
                        .build())
                .validityHoursPerDay(List.of(ValidityHoursPerDay.builder()
                        .dayOfWeek(1)
                        .startTime("08:00")
                        .expirationTime("12:00")
                        .build()))
                .timezone("Asia/Ho_Chi_Minh")
                .build();

        RuleBinding saved = runUpdate(timeframe);

        assertThat(saved.getRrule()).contains("INTERVAL=100");
        assertThat(saved.getDuration()).isEqualTo("PT100H");
        assertThat(saved.getActivityDurationAfterPublishing()).isEqualTo("P126W");
        assertThat(saved.getTimeWindows()).hasSize(1);
        assertThat(saved.getScopeTimeWindows()).containsEntry("duration", "PT100H");
    }

    @Test
    @DisplayName("timeframe section absent entirely → binding temporal state untouched")
    void nullTimeframe_keepsExistingBindingValues() {
        RuleBinding saved = runUpdate(null);

        assertThat(saved.getRrule()).isEqualTo("FREQ=DAILY;INTERVAL=1");
        assertThat(saved.getDuration()).isEqualTo("PT1H");
        assertThat(saved.getActivityDurationAfterPublishing()).isEqualTo("P126W");
        assertThat(saved.getTimeWindows()).hasSize(1);
    }

    /**
     * Drives handleCommand against an existing binding that carries the full
     * pre-edit temporal state, and returns the binding passed to save.
     */
    private RuleBinding runUpdate(TimeFrame timeframe) {
        UpdateValidationRuleCommand command = UpdateValidationRuleCommand.builder()
                .id("cmd-timeframe-replace")
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
        when(idempotencyService.isProcessed("cmd-timeframe-replace")).thenReturn(false);

        RuleBinding existing = RuleBinding.builder()
                .id("binding-1")
                .objectType(OBJECT_TYPE)
                .objectId(CAMPAIGN_ID)
                .active(true)
                .validFrom(START)
                .rrule("FREQ=DAILY;INTERVAL=1")
                .duration("PT1H")
                .activityDurationAfterPublishing("P126W")
                .timeWindows(List.of(RuleBinding.TimeWindow.builder()
                        .start("08:00")
                        .end("12:00")
                        .build()))
                .scopeTimeWindows(Map.of("duration", "PT1H", "timezone", "Asia/Ho_Chi_Minh"))
                .version(3L)
                .build();
        when(ruleBindingPort.findByObject(OBJECT_TYPE, CAMPAIGN_ID))
                .thenReturn(List.of(existing));
        when(ruleBindingPort.save(any(RuleBinding.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        boolean result = handler.handleCommand(command);
        assertThat(result).isTrue();

        ArgumentCaptor<RuleBinding> captor = ArgumentCaptor.forClass(RuleBinding.class);
        verify(ruleBindingPort).save(captor.capture());
        return captor.getValue();
    }
}
