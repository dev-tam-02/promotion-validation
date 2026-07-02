package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.SendResult;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.event.ValidationRuleSettingAppliedEvent;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingValidationRuleEventPublisherTest {

    private com.promix.platform.messaging.autoconfigure.utils.KafkaUtils kafkaUtils;
    private CommandMappingService mappingService;
    private SettingValidationRuleEventPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaUtils = mock(com.promix.platform.messaging.autoconfigure.utils.KafkaUtils.class);
        mappingService = mock(CommandMappingService.class);
        publisher = new SettingValidationRuleEventPublisher(kafkaUtils, mappingService,
                "promotion_validation_event", "validation");
        when(mappingService.calculateApplicabilityStats(any()))
                .thenReturn(new CommandMappingService.ApplicabilityStats(0, 0, true));
        when(kafkaUtils.send(any(), any(), any()))
                .thenReturn(new CompletableFuture<SendResult<String, Object>>());
    }

    @Test
    void appliedEvent_carries_ruleCode_and_description_from_resolvedRule() {
        RuleBinding binding = RuleBinding.builder()
                .id("bind-1")
                .ruleId("rule-1")
                .objectType("CAMPAIGN")
                .objectId("cmp-1")
                .active(true)
                .build();
        Rule resolvedRule = Rule.builder()
                .id("rule-1")
                .code("MIN_ORDER")
                .description("Đơn tối thiểu 100.000đ")
                .build();

        var result = SettingValidationRuleCommandHandler.CommandProcessingResult
                .success(binding, null, null, resolvedRule);

        publisher.publishSuccessEvent("cmd-1", result);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaUtils).send(eq("promotion_validation_event"), eq("bind-1"), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ValidationRuleSettingAppliedEvent.class);
        ValidationRuleSettingAppliedEvent event = (ValidationRuleSettingAppliedEvent) captor.getValue();
        assertThat(event.getAssignmentResult().getRuleCode()).isEqualTo("MIN_ORDER");
        assertThat(event.getAssignmentResult().getDescription()).isEqualTo("Đơn tối thiểu 100.000đ");
        assertThat(event.getAssignmentResult().getRuleId()).isEqualTo("rule-1");
    }

    @Test
    void appliedEvent_nullResolvedRule_yields_null_ruleCode_and_description() {
        RuleBinding binding = RuleBinding.builder()
                .id("bind-2")
                .ruleId(null)
                .objectType("CAMPAIGN")
                .objectId("cmp-2")
                .active(true)
                .build();

        var result = SettingValidationRuleCommandHandler.CommandProcessingResult
                .success(binding, null, null, null);

        publisher.publishSuccessEvent("cmd-2", result);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaUtils).send(eq("promotion_validation_event"), eq("bind-2"), captor.capture());
        ValidationRuleSettingAppliedEvent event = (ValidationRuleSettingAppliedEvent) captor.getValue();
        assertThat(event.getAssignmentResult().getRuleCode()).isNull();
        assertThat(event.getAssignmentResult().getDescription()).isNull();
    }
}
