package vn.viettel.vds.promotion.validation.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import vn.viettel.vds.promotion.schema.validation.command.*;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.SettingValidationRuleCommandConsumer;
import vn.viettel.vds.promotion.validation.application.service.SettingValidationRuleCommandHandler;

import java.time.Instant;
import java.util.Arrays;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingValidationRuleCommandConsumerTest {

    @Mock
    private SettingValidationRuleCommandHandler commandHandler;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private SettingValidationRuleCommandConsumer consumer;

    @Test
    void shouldHandleValidCommand() throws Exception {
        // Given
        SettingValidationRuleCommand command = createTestCommand();
        when(commandHandler.handleCommand(command)).thenReturn(true);

        // When
        consumer.handleSettingValidationRuleCommand(
                command, "test-topic", 0, 100L, "test-key", acknowledgment);

        // Then
        verify(commandHandler).handleCommand(command);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldNotAcknowledgeFailedCommand() throws Exception {
        // Given
        SettingValidationRuleCommand command = createTestCommand();
        when(commandHandler.handleCommand(command)).thenReturn(false);

        // When
        consumer.handleSettingValidationRuleCommand(
                command, "test-topic", 0, 100L, "test-key", acknowledgment);

        // Then
        verify(commandHandler).handleCommand(command);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void shouldAcknowledgeOnProcessingError() throws Exception {
        // Given
        SettingValidationRuleCommand command = createTestCommand();
        when(commandHandler.handleCommand(command))
                .thenThrow(new RuntimeException("Processing error"));

        // When
        consumer.handleSettingValidationRuleCommand(
                command, "test-topic", 0, 100L, "test-key", acknowledgment);

        // Then
        verify(commandHandler).handleCommand(command);
        verify(acknowledgment).acknowledge(); // Acknowledge to avoid infinite retry
    }

    private SettingValidationRuleCommand createTestCommand() {
        // Create ApplicabilityRule
        ApplicabilityRule applicabilityRule = ApplicabilityRule.newBuilder()
                .setObject(ObjectType.PRODUCT)
                .setId("prod-123")
                .setEffect(EffectType.APPLY_TO_EVERY)
                .setTarget(TargetType.ITEM)
                .build();

        // Create ApplicabilityScope
        ApplicabilityScope applicableTo = ApplicabilityScope.newBuilder()
                .setIncluded(Arrays.asList(applicabilityRule))
                .setExcluded(null)
                .setIncludedAll(false)
                .build();

        // Create RuleAssignment
        RuleAssignment assignRule = RuleAssignment.newBuilder()
                .setRuleId("rule-789")
                .setAssignmentId("assignment-123")
                .setActive(true)
                .setTrafficPercent(100)
                .build();

        // Create SettingValidationRuleCommandPayload
        SettingValidationRuleCommandPayload payload = SettingValidationRuleCommandPayload.newBuilder()
                .setAssignRule(assignRule)
                .setApplicableTo(applicableTo)
                .setTimeframe(null)
                .setPriority(5)
                .setNotes(null)
                .build();

        // Create SettingValidationRuleCommand
        return SettingValidationRuleCommand.newBuilder()
                .setId("cmd-123")
                .setType("SettingValidationRuleCommand")
                .setSource("campaign-service")
                .setSubject("campaign-456")
                .setOccurredAt(Instant.now())
                .setVersion(1)
                .setPayload(payload)
                .setMetadata(null)
                .build();
    }
}