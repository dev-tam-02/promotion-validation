package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.application.service.RuleLinter;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Service-level integration test covering the end-to-end flow:
 *   POST /rules → GET /rules/{id} → POST /rules/{id}/bindings → GET /rule-bindings
 *
 * Uses Mockito to stub the persistence ports so no database is required.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Rule management E2E flow (service-level)")
class RuleManagementIntegrationTest {

    @Mock
    private RulePersistencePort rulePort;

    @Mock
    private RuleBindingPersistencePort bindingPort;

    @Mock
    private OperatorPersistencePort operatorPort;

    @Mock
    private RuleEngineClient ruleEngineClient;

    private RuleManagementService ruleManagementService;
    private RuleBindingManagementService bindingManagementService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        RuleTreeAssembler assembler = new RuleTreeAssembler();
        RuleValidator validator = new RuleValidator(objectMapper);
        DslGenerator dslGenerator = new DslGenerator(objectMapper);
        DrlCompiler drlCompiler = new DrlCompiler();

        // Stub: engine registers and returns a bundleHash
        when(ruleEngineClient.register(anyString(), anyString())).thenReturn("test-bundle-hash");
        // Stub: no operators (compilerId fallback will be used)
        when(operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE))
                .thenReturn(List.of());

        ruleManagementService = new RuleManagementService(
                rulePort, assembler, validator, new RuleLinter(), dslGenerator, drlCompiler,
                ruleEngineClient, operatorPort, Optional.empty());
        bindingManagementService = new RuleBindingManagementService(bindingPort);
    }

    @Test
    @DisplayName("full flow: create rule → fetch tree → bind → list bindings")
    void fullFlow_createFetchBindList() {
        // ─── Step 1: POST /rules ───────────────────────────────────────────
        RuleNode condNode = RuleNode.builder()
                .nodeId("c1")
                .type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte")
                .params(java.util.Map.of("amount", 500_000))
                .reasonCode("RC-001")
                .build();

        RuleNode groupNode = RuleNode.builder()
                .nodeId("g1")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(List.of(condNode))
                .build();

        Rule savedRule = Rule.builder()
                .id("rule-001")
                .name("Min order 500k")
                .description("Minimum order total 500,000 VND")
                .logic(Rule.LogicType.ALL)
                .state(Rule.RuleState.DRAFT)
                .nodes(List.of(groupNode))
                .active(false)
                .ruleVersion(1L)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        when(rulePort.save(any(Rule.class))).thenReturn(savedRule);

        Rule created = ruleManagementService.createRule(
                "Min order 500k",
                "Minimum order total 500,000 VND",
                Rule.LogicType.ALL,
                List.of(groupNode),
                "admin");

        assertThat(created.getId()).isEqualTo("rule-001");
        assertThat(created.getState()).isEqualTo(Rule.RuleState.DRAFT);
        assertThat(created.getNodes()).hasSize(1);

        // ─── Step 2: GET /rules/{id} ───────────────────────────────────────
        when(rulePort.findById("rule-001")).thenReturn(Optional.of(savedRule));

        Rule fetched = ruleManagementService.getRuleTree("rule-001");

        assertThat(fetched.getId()).isEqualTo("rule-001");
        assertThat(fetched.getNodes()).hasSize(1);
        // GROUP node has child COND node
        RuleNode fetchedGroup = fetched.getNodes().get(0);
        assertThat(fetchedGroup.getType()).isEqualTo(RuleNode.NodeType.GROUP);
        assertThat(fetchedGroup.getChildren()).hasSize(1);
        assertThat(fetchedGroup.getChildren().get(0).getType()).isEqualTo(RuleNode.NodeType.COND);

        // ─── Step 3: POST /rules/{id}/bindings ────────────────────────────
        RuleBinding savedBinding = RuleBinding.builder()
                .id("binding-001")
                .ruleId("rule-001")
                .objectType("CAMPAIGN")
                .objectId("camp-abc")
                .active(true)
                .priority(10)
                .version(0L)
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        when(bindingPort.save(any(RuleBinding.class))).thenReturn(savedBinding);

        RuleBinding binding = bindingManagementService.bindRuleToResource(
                "rule-001", "CAMPAIGN", "camp-abc",
                null, null, 10, "admin");

        assertThat(binding.getId()).isEqualTo("binding-001");
        assertThat(binding.getRuleId()).isEqualTo("rule-001");
        assertThat(binding.getObjectType()).isEqualTo("CAMPAIGN");
        assertThat(binding.getObjectId()).isEqualTo("camp-abc");
        assertThat(binding.isActive()).isTrue();

        // ─── Step 4: GET /rule-bindings?resourceType=CAMPAIGN&resourceId=camp-abc ─
        when(bindingPort.findActiveByObject("CAMPAIGN", "camp-abc"))
                .thenReturn(List.of(savedBinding));

        List<RuleBinding> bindings = bindingManagementService.listBindings("CAMPAIGN", "camp-abc");

        assertThat(bindings).hasSize(1);
        assertThat(bindings.get(0).getRuleId()).isEqualTo("rule-001");
    }

    @Test
    @DisplayName("update rule replaces nodes and keeps DRAFT state")
    void updateRule_replacesNodes() {
        RuleNode oldCond = RuleNode.builder()
                .nodeId("c1")
                .type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte")
                .reasonCode("RC-001")
                .build();
        RuleNode oldGroup = RuleNode.builder()
                .nodeId("g1")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(List.of(oldCond))
                .build();

        Rule existing = Rule.builder()
                .id("rule-001")
                .name("Old name")
                .logic(Rule.LogicType.ALL)
                .state(Rule.RuleState.DRAFT)
                .nodes(List.of(oldGroup))
                .active(false)
                .ruleVersion(1L)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        RuleNode newCond = RuleNode.builder()
                .nodeId("c2")
                .type(RuleNode.NodeType.COND)
                .operatorName("customer.loyalty_tier.gte")
                .reasonCode("RC-002")
                .build();
        RuleNode newGroup = RuleNode.builder()
                .nodeId("g2")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ANY)
                .children(List.of(newCond))
                .build();

        Rule updated = existing.toBuilder()
                .name("New name")
                .nodes(List.of(newGroup))
                .build();

        when(rulePort.findById("rule-001")).thenReturn(Optional.of(existing));
        when(rulePort.save(any(Rule.class))).thenReturn(updated);

        Rule result = ruleManagementService.updateRule(
                "rule-001", "New name", Rule.LogicType.ANY, List.of(newGroup), "admin");

        assertThat(result.getName()).isEqualTo("New name");
        assertThat(result.getNodes().get(0).getNodeId()).isEqualTo("g2");
    }

    @Test
    @DisplayName("list bindings returns empty list when none exist")
    void listBindings_emptyWhenNone() {
        when(bindingPort.findActiveByObject("COUPON", "coup-xyz"))
                .thenReturn(List.of());

        List<RuleBinding> bindings = bindingManagementService.listBindings("COUPON", "coup-xyz");

        assertThat(bindings).isEmpty();
    }
}
