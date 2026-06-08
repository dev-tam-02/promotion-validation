package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.command.EnableValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.EnableValidationRuleCommand.EnableValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for the Enable side of the issue #4 family (PA-Enable-A). A campaign created
 * WITHOUT validation criteria (PROM-972 gate → no RuleBinding) must still be enableable:
 * the absent-binding case is treated as an idempotent success instead of ENABLE_FAILED.
 * A genuine activation failure on an EXISTING binding must still fail.
 */
@ExtendWith(MockitoExtension.class)
class EnableValidationRuleCommandHandlerNoBindingTest {

    private static final String CAMPAIGN_ID = "019ea740-bb69-7fa2-b126-d0b54a2bb7ee";

    @Mock
    private RuleBindingPersistencePort ruleBindingPort;
    @Mock
    private ValidationRuleRepositoryPort validationRulePort;
    @Mock
    private SettingValidationRuleEventPublisher eventPublisher;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private RulePublishingService rulePublishingService;

    private EnableValidationRuleCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new EnableValidationRuleCommandHandler(
                ruleBindingPort, validationRulePort, eventPublisher, idempotencyService, rulePublishingService);
    }

    @Test
    @DisplayName("no binding for campaign → idempotent enable success (not ENABLE_FAILED)")
    void noBinding_enableIsIdempotentSuccess() {
        EnableValidationRuleCommand command = buildCommand("cmd-enable-1", null);
        when(idempotencyService.isProcessed("cmd-enable-1")).thenReturn(false);
        when(ruleBindingPort.findByObjectId(CAMPAIGN_ID)).thenReturn(Collections.emptyList());

        boolean result = handler.handleCommand(command);

        assertThat(result).isTrue();
        // Publishes ValidationRuleEnabledEvent with a null binding so the enable saga proceeds.
        verify(eventPublisher).publishEnableSuccessEvent(eq("cmd-enable-1"), eq(CAMPAIGN_ID), isNull());
        verify(idempotencyService).markAsProcessed(eq("cmd-enable-1"), anyString());
        // Must NOT report failure.
        verify(eventPublisher, never()).publishEnableErrorEvent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("binding exists but activation fails → still ENABLE_FAILED (real error not masked)")
    void existingBindingActivationFailure_stillFails() {
        EnableValidationRuleCommand command = buildCommand("cmd-enable-2", null);
        when(idempotencyService.isProcessed("cmd-enable-2")).thenReturn(false);

        RuleBinding existing = RuleBinding.builder()
                .id("binding-1")
                .objectId(CAMPAIGN_ID)
                .objectType("CAMPAIGN")
                .active(false)
                .build();
        when(ruleBindingPort.findByObjectId(CAMPAIGN_ID)).thenReturn(List.of(existing));
        // Activation (save) blows up → executeEnable returns null while a binding still exists.
        when(ruleBindingPort.save(any(RuleBinding.class))).thenThrow(new RuntimeException("db down"));

        // A binding exists, so the null return is a real failure — must NOT be reported as success.
        boolean result = handler.handleCommand(command);

        assertThat(result).isFalse();
        verify(eventPublisher, atLeastOnce()).publishEnableErrorEvent(eq("cmd-enable-2"), eq(CAMPAIGN_ID), anyString(), anyString());
        verify(eventPublisher, never()).publishEnableSuccessEvent(anyString(), anyString(), any());
    }

    private EnableValidationRuleCommand buildCommand(String commandId, String validationRuleId) {
        return EnableValidationRuleCommand.builder()
                .id(commandId)
                .type("EnableValidationRuleCommand")
                .subject(CAMPAIGN_ID)
                .occurredAt(Instant.now())
                .payload(EnableValidationRuleCommandPayload.builder()
                        .campaignId(CAMPAIGN_ID)
                        .validationRuleId(validationRuleId)
                        .build())
                .build();
    }
}
