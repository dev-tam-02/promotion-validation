package vn.viettel.vds.promotion.validation.application.service;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for issue #4 — update timeframe on a campaign created WITHOUT validation
 * criteria (no RuleBinding exists). Previously {@code handleCommand} threw
 * {@code BindingNotFoundException} (assignmentId=null), causing the update saga to roll
 * back and the timeframe to be lost. The fix backfills the missing rule+binding instead.
 */
@ExtendWith(MockitoExtension.class)
class UpdateValidationRuleCommandHandlerBackfillTest {

    private static final String CAMPAIGN_ID = "019ea740-bb69-7fa2-b126-d0b54a2bb7ee";
    private static final String OBJECT_TYPE = "CAMPAIGN";
    private static final Instant START = Instant.parse("2026-06-10T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-10T00:00:00Z");

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
    @DisplayName("issue #4: first-time timeframe update with no existing binding backfills instead of throwing")
    void firstTimeTimeframeUpdate_backfillsBinding_doesNotThrow() {
        UpdateValidationRuleCommand command = buildTimeframeUpdateCommand();

        UpdateValidationRuleCommandDTO dto = new UpdateValidationRuleCommandDTO();
        when(dtoMapper.toDTO(command)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(idempotencyService.isProcessed("cmd-issue4")).thenReturn(false);
        // No binding exists for this campaign (created without validation criteria).
        when(ruleBindingPort.findByObject(OBJECT_TYPE, CAMPAIGN_ID)).thenReturn(Collections.emptyList());

        RuleBinding created = RuleBinding.builder()
                .id("binding-new")
                .ruleId("rule-new")
                .objectType(OBJECT_TYPE)
                .objectId(CAMPAIGN_ID)
                .build();
        when(settingHandler.backfillRuleAndBinding(
                any(RuleBinding.class), isNull(), eq(START), eq(END), eq("Asia/Ho_Chi_Minh")))
                .thenReturn(created);

        boolean result = handler.handleCommand(command);

        assertThat(result).isTrue();
        // Backfilled the missing rule+binding (Path B — ruleId null → auto-generate).
        verify(settingHandler).backfillRuleAndBinding(
                any(RuleBinding.class), isNull(), eq(START), eq(END), eq("Asia/Ho_Chi_Minh"));
        // Marked processed + published the success event (saga proceeds, no rollback).
        verify(idempotencyService).markAsProcessed(eq("cmd-issue4"), anyString());
        verify(kafkaTemplate).send(any(), eq(CAMPAIGN_ID), any());
        // The old throw branch must NOT touch the snapshot service.
        verify(snapshotService, never()).createSnapshot(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("issue #4: handleCommand no longer throws BindingNotFoundException when binding is absent")
    void firstTimeTimeframeUpdate_neverThrows() {
        UpdateValidationRuleCommand command = buildTimeframeUpdateCommand();
        UpdateValidationRuleCommandDTO dto = new UpdateValidationRuleCommandDTO();
        when(dtoMapper.toDTO(command)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(idempotencyService.isProcessed("cmd-issue4")).thenReturn(false);
        when(ruleBindingPort.findByObject(OBJECT_TYPE, CAMPAIGN_ID)).thenReturn(Collections.emptyList());
        when(settingHandler.backfillRuleAndBinding(any(RuleBinding.class), isNull(), any(), any(), any()))
                .thenReturn(RuleBinding.builder().id("b").ruleId("r").objectId(CAMPAIGN_ID).build());

        assertThatCode(() -> handler.handleCommand(command)).doesNotThrowAnyException();
    }

    private UpdateValidationRuleCommand buildTimeframeUpdateCommand() {
        return UpdateValidationRuleCommand.builder()
                .id("cmd-issue4")
                .type("UpdateValidationRuleCommand")
                .subject(CAMPAIGN_ID)
                .occurredAt(Instant.now())
                .payload(UpdateValidationRuleCommandPayload.builder()
                        // assignmentId + ruleId intentionally null — campaign had no rule.
                        .objectType(OBJECT_TYPE)
                        .objectId(CAMPAIGN_ID)
                        .timeframe(TimeFrame.builder()
                                .validityTimeframe(ValidityTimeframe.builder()
                                        .startDate(START)
                                        .expirationDate(END)
                                        .build())
                                .timezone("Asia/Ho_Chi_Minh")
                                .build())
                        .updatedBy("system")
                        .build())
                .build();
    }
}
