package vn.viettel.vds.promotion.validation.application.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.ValidationCompileRequest;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies Task 12 (eager compile) of the timeframe/validation bundle split
 * plan: {@link RuleService#createRule} and {@link RuleService#updateRule} call
 * pp-rule-engine's {@code POST /v1/compile/validation} (via
 * {@link ValidationEngineClient#compileValidation}) right after the rule is
 * saved, so the shared standalone VALIDATION bundle stays always-latest by
 * {@code ruleId} instead of only refreshing on the next campaign bind.
 * <p>
 * The call is best-effort: a rule-engine outage (Feign exception) must not
 * fail rule create/update — the combined compile path still recompiles it on
 * the next bind.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleService — eager validation-bundle compile")
class RuleServiceEagerCompileTest {

    private static final String EVENT_TOPIC = "promotion_validation_event";

    @Mock
    private RulePersistencePort rulePersistencePort;

    @Mock
    private RuleBindingPersistencePort ruleBindingPort;

    @Mock
    private com.promix.platform.outbox.spi.OutboxService outboxService;

    @Mock
    private ValidationEngineClient validationEngineClient;

    private RuleService newServiceUnderTest() {
        RuleService withoutSelf = new RuleService(rulePersistencePort, ruleBindingPort, outboxService,
                null, null, validationEngineClient, EVENT_TOPIC);
        // RuleService uses @Lazy self-injection for transactional proxying; in a unit
        // test without a Spring context, the service itself stands in as its own proxy.
        return new RuleService(rulePersistencePort, ruleBindingPort, outboxService,
                withoutSelf, null, validationEngineClient, EVENT_TOPIC);
    }

    private static RuleNode condNode(String id, String operatorName, String reasonCode) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .reasonCode(reasonCode)
                .build();
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

    /**
     * Construct a real {@code FeignException.ServiceUnavailable} (a concrete
     * subclass of FeignException) so Mockito doesn't need to mock a final/sealed
     * exception hierarchy — mirrors the pattern already used in
     * {@code SegmentLookupAdapterTest}.
     */
    private static FeignException feignServiceUnavailable(String message) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "/promotion/promotion-rule-engine/v1/compile/validation",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        return new FeignException.ServiceUnavailable(message, request, null, Collections.emptyMap());
    }

    @Test
    @DisplayName("createRule() triggers eager compileValidation with ruleId and non-empty nodes")
    void createRule_triggersEagerValidationCompile() {
        // given
        when(rulePersistencePort.existsByCode("RULE_001")).thenReturn(false);
        when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        RuleService service = newServiceUnderTest();

        // when
        service.createRule("RULE_001", "Weekend VIP Rule", Rule.LogicType.ALL,
                List.of(condNode("n1", "op-vip", "NOT_VIP")), "admin");

        // then
        ArgumentCaptor<ValidationCompileRequest> captor = ArgumentCaptor.forClass(ValidationCompileRequest.class);
        verify(validationEngineClient).compileValidation(captor.capture());
        ValidationCompileRequest request = captor.getValue();
        assertThat(request.getRuleId()).isNotNull();
        assertThat(request.getNodes()).isNotEmpty();
        assertThat(request.getNodes().get(0))
                .containsEntry("id", "n1")
                .containsEntry("operatorName", "op-vip")
                .containsEntry("reasonCode", "NOT_VIP");
    }

    @Test
    @DisplayName("updateRule() triggers eager compileValidation with ruleId and non-empty nodes")
    void updateRule_triggersEagerValidationCompile() {
        // given
        Rule existing = sampleRule("r1", "CODE_1", "Original Name");
        when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
        when(rulePersistencePort.existsByName(anyString(), eq("r1"))).thenReturn(false);
        when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        RuleService service = newServiceUnderTest();

        // when
        service.updateRule("r1", "Renamed Rule", null, null, "editor");

        // then
        ArgumentCaptor<ValidationCompileRequest> captor = ArgumentCaptor.forClass(ValidationCompileRequest.class);
        verify(validationEngineClient).compileValidation(captor.capture());
        ValidationCompileRequest request = captor.getValue();
        assertThat(request.getRuleId()).isEqualTo("r1");
        assertThat(request.getNodes()).isNotEmpty();
    }

    @Test
    @DisplayName("createRule() still succeeds when the rule-engine client throws")
    void createRule_stillSucceeds_whenEngineClientThrows() {
        // given
        when(rulePersistencePort.existsByCode("RULE_002")).thenReturn(false);
        when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(validationEngineClient.compileValidation(any()))
                .thenThrow(feignServiceUnavailable("rule-engine unavailable"));
        RuleService service = newServiceUnderTest();

        // when / then — createRule must not propagate the Feign failure
        assertThatCode(() -> service.createRule("RULE_002", "Some Rule", Rule.LogicType.ALL,
                List.of(condNode("n1", "op-vip", "NOT_VIP")), "admin"))
                .doesNotThrowAnyException();

        // and the outbox event was still published (create is not rolled back).
        // createRule uses the 7-arg overload carrying payloadType=ValidationEvent.class (see
        // outbox double-encode fix) — the trailing any()/any() cover the nullable key + payloadType.
        verify(outboxService).createEvent(eq("ValidationRule"), anyString(),
                eq("VALIDATION_RULE_CREATED"), any(), eq(EVENT_TOPIC), any(), any());
    }

    @Test
    @DisplayName("createRule() with a null engine client (unit-test wiring) skips eager compile without failing")
    void createRule_withNullEngineClient_skipsEagerCompileGracefully() {
        // given — RuleService constructed without a validationEngineClient (mirrors
        // other RuleService tests that pass null for optional collaborators).
        when(rulePersistencePort.existsByCode("RULE_003")).thenReturn(false);
        when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        RuleService withoutSelf = new RuleService(rulePersistencePort, ruleBindingPort, outboxService,
                null, null, null, EVENT_TOPIC);
        RuleService service = new RuleService(rulePersistencePort, ruleBindingPort, outboxService,
                withoutSelf, null, null, EVENT_TOPIC);

        // when / then
        assertThatCode(() -> service.createRule("RULE_003", "Some Rule", Rule.LogicType.ALL,
                List.of(condNode("n1", "op-vip", "NOT_VIP")), "admin"))
                .doesNotThrowAnyException();

        verifyNoInteractions(validationEngineClient);
    }
}
