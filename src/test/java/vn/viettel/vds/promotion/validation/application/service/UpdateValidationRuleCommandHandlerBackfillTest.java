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
                // Rule-less binding: Path B no longer auto-generates a skeleton rule.
                .objectType(OBJECT_TYPE)
                .objectId(CAMPAIGN_ID)
                .build();
        when(settingHandler.backfillRuleAndBinding(any(RuleBinding.class), isNull()))
                .thenReturn(created);

        boolean result = handler.handleCommand(command);

        assertThat(result).isTrue();
        // Backfilled the missing binding (Path B — ruleId null → rule-less binding).
        verify(settingHandler).backfillRuleAndBinding(any(RuleBinding.class), isNull());
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
        when(settingHandler.backfillRuleAndBinding(any(RuleBinding.class), isNull()))
                .thenReturn(RuleBinding.builder().id("b").objectId(CAMPAIGN_ID).build());

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

    // =========================================================================
    // Three-state ruleId semantics on update:
    //   null = keep current rule, "" = remove (detach), value = attach/replace
    // =========================================================================

    @Test
    @DisplayName("ruleId blank → rule detached, binding becomes rule-less")
    void blankRuleId_removesRuleFromBinding() {
        RuleBinding saved = runUpdateAgainstExistingBoundBinding("");

        assertThat(saved.getRuleId()).isNull();
    }

    @Test
    @DisplayName("ruleId null → existing rule kept (no-change semantics preserved)")
    void nullRuleId_keepsExistingRule() {
        RuleBinding saved = runUpdateAgainstExistingBoundBinding(null);

        assertThat(saved.getRuleId()).isEqualTo("rule-old");
    }

    @Test
    @DisplayName("ruleId value → rule replaced on binding")
    void newRuleId_replacesRule() {
        RuleBinding saved = runUpdateAgainstExistingBoundBinding("rule-new");

        assertThat(saved.getRuleId()).isEqualTo("rule-new");
    }

    /**
     * Drives handleCommand against an existing binding bound to "rule-old" with the
     * given payload ruleId, and returns the binding passed to ruleBindingPort.save.
     */
    private RuleBinding runUpdateAgainstExistingBoundBinding(String payloadRuleId) {
        UpdateValidationRuleCommand command = UpdateValidationRuleCommand.builder()
                .id("cmd-rule-semantics")
                .type("UpdateValidationRuleCommand")
                .subject(CAMPAIGN_ID)
                .occurredAt(Instant.now())
                .payload(UpdateValidationRuleCommandPayload.builder()
                        .objectType(OBJECT_TYPE)
                        .objectId(CAMPAIGN_ID)
                        .ruleId(payloadRuleId)
                        .updatedBy("system")
                        .build())
                .build();

        UpdateValidationRuleCommandDTO dto = new UpdateValidationRuleCommandDTO();
        when(dtoMapper.toDTO(command)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(idempotencyService.isProcessed("cmd-rule-semantics")).thenReturn(false);

        RuleBinding existing = RuleBinding.builder()
                .id("binding-1")
                .ruleId("rule-old")
                .objectType(OBJECT_TYPE)
                .objectId(CAMPAIGN_ID)
                .active(true)
                .version(3L)
                .build();
        when(ruleBindingPort.findByObject(OBJECT_TYPE, CAMPAIGN_ID))
                .thenReturn(java.util.List.of(existing));
        when(ruleBindingPort.save(any(RuleBinding.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        boolean result = handler.handleCommand(command);
        assertThat(result).isTrue();

        org.mockito.ArgumentCaptor<RuleBinding> captor =
                org.mockito.ArgumentCaptor.forClass(RuleBinding.class);
        verify(ruleBindingPort).save(captor.capture());
        return captor.getValue();
    }
}
