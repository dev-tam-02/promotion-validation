package vn.viettel.vds.promotion.validation.application.service;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.RollbackValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.RollbackValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand.RollbackValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RollbackValidationRuleCommandHandler}.
 * <p>
 * Covers F1 (BUG-007): CASHBACK rollback gap — objectType=CASHBACK must trigger
 * deleteByObjectIgnoreCase("CASHBACK", campaignId).
 */
@ExtendWith(MockitoExtension.class)
class RollbackValidationRuleCommandHandlerTest {

    @Mock
    private RuleBindingPersistencePort ruleBindingPort;
    @Mock
    private RulePersistencePort rulePort;
    @Mock
    private SettingValidationRuleEventPublisher eventPublisher;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private Validator validator;
    @Mock
    private RollbackValidationRuleCommandDTOMapper dtoMapper;

    private RollbackValidationRuleCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RollbackValidationRuleCommandHandler(
                ruleBindingPort, rulePort, eventPublisher, idempotencyService, validator, dtoMapper);
    }

    // ---- helpers ----

    private RollbackValidationRuleCommand buildCommand(String commandId, String campaignId, boolean rollbackAll) {
        return RollbackValidationRuleCommand.builder()
                .id(commandId)
                .type("RollbackValidationRuleCommand")
                .source("campaign-saga")
                .subject(campaignId)
                .occurredAt(Instant.now())
                .payload(RollbackValidationRuleCommandPayload.builder()
                        .campaignId(campaignId)
                        .rollbackAll(rollbackAll)
                        .build())
                .build();
    }

    private void stubValidation(RollbackValidationRuleCommand command, RollbackValidationRuleCommandDTO dto) {
        when(dtoMapper.toDTO(command)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(idempotencyService.isProcessed(command.getId())).thenReturn(false);
    }

    private RollbackValidationRuleCommandDTO makeDto(String id, String campaignId, boolean rollbackAll) {
        return RollbackValidationRuleCommandDTO.builder()
                .id(id)
                .type("RollbackValidationRuleCommand")
                .source("campaign-saga")
                .subject(campaignId)
                .campaignId(campaignId)
                .rollbackAll(rollbackAll)
                .build();
    }

    // ====== rollbackAll=true tests ======

    @Nested
    @DisplayName("rollbackAll=true")
    class RollbackAll {

        @Test
        @DisplayName("Calls deleteByObjectIgnoreCase for CAMPAIGN, DISCOUNT_COUPON, CASHBACK")
        void rollbackAll_deletesAcrossAllThreeObjectTypes() {
            String campaignId = "campaign-001";
            String commandId = "cmd-001";
            RollbackValidationRuleCommand cmd = buildCommand(commandId, campaignId, true);
            stubValidation(cmd, makeDto(commandId, campaignId, true));

            when(ruleBindingPort.deleteByObjectIgnoreCase(anyString(), eq(campaignId))).thenReturn(1);

            boolean result = handler.handleRollback(cmd);

            assertThat(result).isTrue();
            verify(ruleBindingPort).deleteByObjectIgnoreCase("CAMPAIGN", campaignId);
            verify(ruleBindingPort).deleteByObjectIgnoreCase("DISCOUNT_COUPON", campaignId);
            verify(ruleBindingPort).deleteByObjectIgnoreCase("CASHBACK", campaignId);
            verify(ruleBindingPort, never()).deleteByObject(anyString(), anyString());
        }

        @Test
        @DisplayName("CASHBACK: deleteByObjectIgnoreCase('CASHBACK') returns rows deleted")
        void rollbackAll_cashbackRowsDeleted() {
            String campaignId = "cashback-campaign-42";
            String commandId = "cmd-cashback-42";
            RollbackValidationRuleCommand cmd = buildCommand(commandId, campaignId, true);
            stubValidation(cmd, makeDto(commandId, campaignId, true));

            when(ruleBindingPort.deleteByObjectIgnoreCase("CAMPAIGN", campaignId)).thenReturn(0);
            when(ruleBindingPort.deleteByObjectIgnoreCase("DISCOUNT_COUPON", campaignId)).thenReturn(0);
            when(ruleBindingPort.deleteByObjectIgnoreCase("CASHBACK", campaignId)).thenReturn(3); // 3 orphaned rows

            boolean result = handler.handleRollback(cmd);

            assertThat(result).isTrue();
            verify(ruleBindingPort, times(1)).deleteByObjectIgnoreCase("CASHBACK", campaignId);
        }

        @Test
        @DisplayName("Zero rows deleted is still considered success (no bindings = already clean)")
        void rollbackAll_zeroDeletedIsSuccess() {
            String campaignId = "campaign-clean";
            String commandId = "cmd-clean";
            RollbackValidationRuleCommand cmd = buildCommand(commandId, campaignId, true);
            stubValidation(cmd, makeDto(commandId, campaignId, true));

            when(ruleBindingPort.deleteByObjectIgnoreCase(anyString(), eq(campaignId))).thenReturn(0);

            boolean result = handler.handleRollback(cmd);

            assertThat(result).isTrue();
        }
    }

    // ====== rollbackAll=false (specific ruleId) tests ======

    @Nested
    @DisplayName("rollbackAll=false — specific ruleId")
    class RollbackSpecific {

        @Test
        @DisplayName("Finds bindings via findByObjectIgnoreCase for all three types including CASHBACK")
        void specificRule_searchesAllThreeObjectTypes() {
            String campaignId = "campaign-007";
            String ruleId = "rule-abc";
            String commandId = "cmd-007";

            RollbackValidationRuleCommand cmd = RollbackValidationRuleCommand.builder()
                    .id(commandId)
                    .type("RollbackValidationRuleCommand")
                    .source("campaign-saga")
                    .subject(campaignId)
                    .occurredAt(Instant.now())
                    .payload(RollbackValidationRuleCommandPayload.builder()
                            .campaignId(campaignId)
                            .validationRuleId(ruleId)
                            .rollbackAll(false)
                            .build())
                    .build();

            RollbackValidationRuleCommandDTO dto = makeDto(commandId, campaignId, false);
            dto.setValidationRuleId(ruleId);
            stubValidation(cmd, dto);

            RuleBinding cashbackBinding = RuleBinding.builder()
                    .id("binding-xyz")
                    .ruleId(ruleId)
                    .objectType("CASHBACK")
                    .objectId(campaignId)
                    .build();

            when(ruleBindingPort.findByObjectIgnoreCase("CAMPAIGN", campaignId)).thenReturn(List.of());
            when(ruleBindingPort.findByObjectIgnoreCase("DISCOUNT_COUPON", campaignId)).thenReturn(List.of());
            when(ruleBindingPort.findByObjectIgnoreCase("CASHBACK", campaignId)).thenReturn(List.of(cashbackBinding));

            boolean result = handler.handleRollback(cmd);

            assertThat(result).isTrue();
            verify(ruleBindingPort).findByObjectIgnoreCase("CAMPAIGN", campaignId);
            verify(ruleBindingPort).findByObjectIgnoreCase("DISCOUNT_COUPON", campaignId);
            verify(ruleBindingPort).findByObjectIgnoreCase("CASHBACK", campaignId);
            verify(ruleBindingPort).deleteById("binding-xyz");
        }

        @Test
        @DisplayName("F4: filter by ruleId only (not binding.getId())")
        void specificRule_filtersByRuleIdOnly() {
            String campaignId = "campaign-f4";
            String ruleId = "rule-correct";
            String commandId = "cmd-f4";

            RollbackValidationRuleCommand cmd = RollbackValidationRuleCommand.builder()
                    .id(commandId)
                    .type("RollbackValidationRuleCommand")
                    .source("campaign-saga")
                    .subject(campaignId)
                    .occurredAt(Instant.now())
                    .payload(RollbackValidationRuleCommandPayload.builder()
                            .campaignId(campaignId)
                            .validationRuleId(ruleId)
                            .rollbackAll(false)
                            .build())
                    .build();

            RollbackValidationRuleCommandDTO dto = makeDto(commandId, campaignId, false);
            dto.setValidationRuleId(ruleId);
            stubValidation(cmd, dto);

            // Binding whose ID matches ruleId but ruleId field does NOT match
            RuleBinding wrongBinding = RuleBinding.builder()
                    .id(ruleId)            // id == ruleId (old OR logic would delete this)
                    .ruleId("rule-other")  // ruleId != ruleId → should NOT be deleted
                    .objectType("CAMPAIGN")
                    .objectId(campaignId)
                    .build();

            // Binding whose ruleId matches correctly
            RuleBinding correctBinding = RuleBinding.builder()
                    .id("binding-correct")
                    .ruleId(ruleId)
                    .objectType("CAMPAIGN")
                    .objectId(campaignId)
                    .build();

            when(ruleBindingPort.findByObjectIgnoreCase("CAMPAIGN", campaignId))
                    .thenReturn(List.of(wrongBinding, correctBinding));
            when(ruleBindingPort.findByObjectIgnoreCase("DISCOUNT_COUPON", campaignId)).thenReturn(List.of());
            when(ruleBindingPort.findByObjectIgnoreCase("CASHBACK", campaignId)).thenReturn(List.of());

            handler.handleRollback(cmd);

            // Only correctBinding.getId() should be deleted
            verify(ruleBindingPort).deleteById("binding-correct");
            verify(ruleBindingPort, never()).deleteById(ruleId); // wrongBinding.getId() must NOT be deleted
        }
    }
}
