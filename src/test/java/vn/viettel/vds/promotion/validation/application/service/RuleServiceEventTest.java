package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promix.platform.outbox.spi.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.event.ValidationEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleCreatedEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleDeletedEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleUpdatedEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link RuleService} publishes the pp-schema {@code ValidationEvent}
 * envelope (with the {@code type} discriminator) for rule create/update/delete,
 * instead of the raw {@code Map} it previously sent — a bare Map is missing the
 * {@code type} property that pp-rule-engine's {@code ValidationEventConsumer}
 * requires for polymorphic deserialization, which used to fail with
 * {@code InvalidTypeIdException}. Each test round-trips the captured payload
 * through Jackson exactly as the consumer does, to prove the fix end-to-end.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleService — outbox event envelope")
class RuleServiceEventTest {

    private static final String EVENT_TOPIC = "promotion_validation_event";

    @Mock
    private RulePersistencePort rulePersistencePort;

    @Mock
    private RuleBindingPersistencePort ruleBindingPort;

    @Mock
    private OutboxService outboxService;

    private RuleService newServiceUnderTest() {
        RuleService withoutSelf =
                new RuleService(rulePersistencePort, ruleBindingPort, outboxService, null, null, null, EVENT_TOPIC);
        // RuleService uses @Lazy self-injection for transactional proxying; in a unit
        // test without a Spring context, the service itself stands in as its own proxy.
        return new RuleService(rulePersistencePort, ruleBindingPort, outboxService, withoutSelf, null, null, EVENT_TOPIC);
    }

    private static RuleNode condNode(String id, String operatorName, String reasonCode) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .reasonCode(reasonCode)
                .build();
    }

    /**
     * The real {@code ValidationEventConsumer} in pp-rule-engine deserializes with
     * a Spring-managed {@code ObjectMapper} bean, whose auto-configuration disables
     * {@code FAIL_ON_UNKNOWN_PROPERTIES} by default (unlike a bare {@code new
     * ObjectMapper()}). The pp-schema event classes expose Lombok convenience
     * getters (e.g. {@code getValidationRuleId()}) that Jackson also serializes as
     * top-level properties duplicating the nested payload field, so the round-trip
     * must tolerate them exactly as the consumer does.
     */
    private static ObjectMapper newConsumerObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static Rule sampleRule(String id, String code, String name) {
        return Rule.builder()
                .id(id)
                .code(code)
                .name(name)
                .active(false)
                .latestVersion(0)
                .version(1L)
                .logic(Rule.LogicType.ALL)
                .nodes(List.of(condNode("node-1", "op-1", "reason-1")))
                .createdAt(Instant.parse("2026-03-01T00:00:00Z"))
                .createdBy("user-1")
                .updatedAt(Instant.parse("2026-03-01T00:00:00Z"))
                .updatedBy("user-1")
                .build();
    }

    @Test
    @DisplayName("createRule() publishes a ValidationRuleCreatedEvent envelope with type discriminator")
    void createRule_publishesEnvelopeWithTypeDiscriminator() throws Exception {
        // given
        when(rulePersistencePort.existsByCode("RULE_001")).thenReturn(false);
        when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        RuleService service = newServiceUnderTest();

        // when
        service.createRule("RULE_001", "Weekend VIP Rule", Rule.LogicType.ALL,
                List.of(condNode("n1", "op-vip", "NOT_VIP")), "admin");

        // then
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).createEvent(eq("ValidationRule"), anyString(),
                eq("VALIDATION_RULE_CREATED"), payloadCaptor.capture(), eq(EVENT_TOPIC),
                isNull(), eq(ValidationEvent.class));
        Object payload = payloadCaptor.getValue();
        assertThat(payload).isInstanceOf(ValidationRuleCreatedEvent.class);

        // round-trip Jackson exactly as pp-rule-engine's ValidationEventConsumer does
        ObjectMapper om = newConsumerObjectMapper();
        String json = om.writeValueAsString(payload);
        ValidationEvent back = om.readValue(json, ValidationEvent.class);
        assertThat(back).isInstanceOf(ValidationRuleCreatedEvent.class);
        assertThat(back.getType()).isEqualTo("ValidationRuleCreatedEvent");
    }

    @Test
    @DisplayName("updateRule() publishes a ValidationRuleUpdatedEvent envelope with type discriminator")
    void updateRule_publishesEnvelopeWithTypeDiscriminator() throws Exception {
        // given
        Rule existing = sampleRule("r1", "CODE_1", "Original Name");
        when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
        when(rulePersistencePort.existsByName(anyString(), eq("r1"))).thenReturn(false);
        when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        RuleService service = newServiceUnderTest();

        // when
        service.updateRule("r1", "Renamed Rule", null, null, "editor");

        // then
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).createEvent(eq("ValidationRule"), eq("r1"),
                eq("VALIDATION_RULE_UPDATED"), payloadCaptor.capture(), eq(EVENT_TOPIC),
                isNull(), eq(ValidationEvent.class));
        Object payload = payloadCaptor.getValue();
        assertThat(payload).isInstanceOf(ValidationRuleUpdatedEvent.class);

        ObjectMapper om = newConsumerObjectMapper();
        String json = om.writeValueAsString(payload);
        ValidationEvent back = om.readValue(json, ValidationEvent.class);
        assertThat(back).isInstanceOf(ValidationRuleUpdatedEvent.class);
        assertThat(back.getType()).isEqualTo("ValidationRuleUpdatedEvent");
    }

    @Test
    @DisplayName("deleteRule() publishes a ValidationRuleDeletedEvent envelope with type discriminator")
    void deleteRule_publishesEnvelopeWithTypeDiscriminator() throws Exception {
        // given
        Rule existing = sampleRule("r1", "CODE_1", "Rule to Delete");
        when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
        when(ruleBindingPort.countByRuleId("r1")).thenReturn(0L);
        RuleService service = newServiceUnderTest();

        // when
        service.deleteRule("r1", 1L);

        // then
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).createEvent(eq("ValidationRule"), eq("r1"),
                eq("VALIDATION_RULE_DELETED"), payloadCaptor.capture(), eq(EVENT_TOPIC),
                isNull(), eq(ValidationEvent.class));
        Object payload = payloadCaptor.getValue();
        assertThat(payload).isInstanceOf(ValidationRuleDeletedEvent.class);

        ObjectMapper om = newConsumerObjectMapper();
        String json = om.writeValueAsString(payload);
        ValidationEvent back = om.readValue(json, ValidationEvent.class);
        assertThat(back).isInstanceOf(ValidationRuleDeletedEvent.class);
        assertThat(back.getType()).isEqualTo("ValidationRuleDeletedEvent");
    }
}
