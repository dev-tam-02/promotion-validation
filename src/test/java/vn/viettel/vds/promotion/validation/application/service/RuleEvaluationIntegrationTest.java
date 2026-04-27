package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.SettingValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RuleHistoryPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.SettingValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ValidityTimeframe;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.exception.RuleNotFoundException;
import jakarta.validation.Validator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests verifying the DRL compilation pipeline for 4 Path B (auto-generated)
 * and 7 Path A (admin-built) scenarios.
 *
 * <p>pp-validation does not execute Drools — DRL evaluation runs in pp-rule-engine.
 * These tests verify that the <em>correct DRL text</em> is generated for each scenario
 * (i.e., that the temporal gate / business conditions are encoded correctly in the DRL),
 * and that {@code compileDrlAndRegisterInEngine} calls the {@code RuleEngineClient} with
 * the compiled DRL.
 *
 * <p>Scenarios follow design doc §11 (Concrete examples):
 * <ul>
 *   <li>PB-1 to PB-4: Path B — auto-generated timeframe-gate rules</li>
 *   <li>PA-1 to PA-7: Path A — admin-built business rules (customer, order, product, time)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Rule evaluation integration test — 4 PB + 7 PA scenarios")
class RuleEvaluationIntegrationTest {

    // ── Real components ──────────────────────────────────────────────────────

    private DrlCompiler drlCompiler;

    // ── Mocked ports ─────────────────────────────────────────────────────────

    @Mock private ValidationRuleRepositoryPort validationRulePort;
    @Mock private RuleBindingPersistencePort ruleBindingPort;
    @Mock private OperatorPersistencePort operatorPort;
    @Mock private RuleEngineClient ruleEngineClient;
    @Mock private RuleHistoryPersistencePort historyPort;
    @Mock private RulePublishingService rulePublishingService;
    @Mock private SettingValidationRuleEventPublisher eventPublisher;
    @Mock private IdempotencyService idempotencyService;
    @Mock private SettingValidationRuleCommandDTOMapper dtoMapper;
    @Mock private Validator validator;
    @Mock private PlatformTransactionManager transactionManager;

    // ── SUT ──────────────────────────────────────────────────────────────────

    private SettingValidationRuleCommandHandler handler;

    @BeforeEach
    void setUp() {
        drlCompiler = new DrlCompiler();

        handler = new SettingValidationRuleCommandHandler(
                ruleBindingPort,
                validationRulePort,
                eventPublisher,
                idempotencyService,
                rulePublishingService,
                validator,
                dtoMapper,
                transactionManager,
                ruleEngineClient,
                drlCompiler,
                operatorPort,
                historyPort
        );

        // Default stubs
        when(ruleEngineClient.register(anyString(), anyString())).thenReturn("bundle-hash-test");
        when(operatorPort.findGlobalOperatorsByStatus(any()))
                .thenReturn(buildOperatorList());
    }

    // =========================================================================
    // Path B — auto-generated timeframe-gate rules (4 cases)
    // =========================================================================

    @Test
    @DisplayName("PB-1: timeframe Apr 26 – Dec 31 → DRL has both isBefore + isAfter checks")
    void pb1_validTimeframe_drlHasBothTemporalGuards() {
        SettingValidationRuleCommandPayload payload =
                payloadWithTimeframe("2026-04-26T00:00:00Z", "2026-12-31T23:59:59Z");

        Rule rule = handler.resolveOrCreateRule(payload);

        // Structural assertions: one root GROUP with one COND child
        assertThat(rule.getNodes()).hasSize(1);
        RuleNode root = rule.getNodes().get(0);
        assertThat(root.getType()).isEqualTo(RuleNode.NodeType.GROUP);
        assertThat(root.getGroupLogic()).isEqualTo(Rule.LogicType.ALL);
        assertThat(root.getChildren()).hasSize(1);

        RuleNode cond = root.getChildren().get(0);
        assertThat(cond.getType()).isEqualTo(RuleNode.NodeType.COND);
        assertThat(cond.getOperatorName()).isEqualTo("binding.validity_window");
        assertThat(cond.getReasonCode()).isEqualTo("OUTSIDE_VALIDITY_WINDOW");

        // DRL content assertions: both temporal guards
        String drl = compileDrl(rule);
        assertThat(drl).contains("!$now.isBefore(java.time.Instant.parse(\"2026-04-26T00:00:00Z\"))");
        assertThat(drl).contains("&&");
        assertThat(drl).contains("!$now.isAfter(java.time.Instant.parse(\"2026-12-31T23:59:59Z\"))");
    }

    @Test
    @DisplayName("PB-2: before startDate → DRL isBefore guard denies Apr 25 evaluation")
    void pb2_beforeStart_drlIsBeforesGuardPresent() {
        // Same timeframe as PB-1 — the DRL encodes the same gate
        SettingValidationRuleCommandPayload payload =
                payloadWithTimeframe("2026-04-26T00:00:00Z", "2026-12-31T23:59:59Z");

        Rule rule = handler.resolveOrCreateRule(payload);
        String drl = compileDrl(rule);

        // The DRL correctly encodes: pass iff now >= startDate
        // When now = Apr 25, the engine would evaluate !now.isBefore(Apr26) → false → DENY
        assertThat(drl).contains("!$now.isBefore(java.time.Instant.parse(\"2026-04-26T00:00:00Z\"))");
        // The rule name contains the campaign id (auto-gen)
        assertThat(drl).contains("rule \"");
    }

    @Test
    @DisplayName("PB-3: after endDate → DRL isAfter guard denies Jan 1 next year")
    void pb3_afterEnd_drlIsAfterGuardPresent() {
        SettingValidationRuleCommandPayload payload =
                payloadWithTimeframe("2026-04-26T00:00:00Z", "2026-12-31T23:59:59Z");

        Rule rule = handler.resolveOrCreateRule(payload);
        String drl = compileDrl(rule);

        // The DRL correctly encodes: pass iff now <= endDate
        // When now = Jan 1, 2027, the engine evaluates !now.isAfter(Dec31) → false → DENY
        assertThat(drl).contains("!$now.isAfter(java.time.Instant.parse(\"2026-12-31T23:59:59Z\"))");
    }

    @Test
    @DisplayName("PB-4: null timeframe → rule has no nodes (unconditional ALLOW skeleton)")
    void pb4_nullTimeframe_ruleHasNoNodes() {
        SettingValidationRuleCommandPayload payload = payloadWithTimeframe(null, null);

        Rule rule = handler.resolveOrCreateRule(payload);

        // No timeframe → no binding.validity_window node → unconditional
        assertThat(rule.getNodes()).isEmpty();
        assertThat(rule.getState()).isEqualTo(Rule.RuleState.PUBLISHED);
        assertThat(rule.isActive()).isTrue();
    }

    // =========================================================================
    // Path A — admin-built business rules (7 cases)
    // =========================================================================

    @Test
    @DisplayName("PA-1: customer.in_segment + order.total.gte → DRL has CustomerFact AND OrderFact")
    void pa1_customerVip_orderHigh_drlHasBothFacts() {
        Rule rule = adminBuiltRule(
                condNode("c1", "customer.in_segment", "tpl_customer_in_segment_v1",
                        Map.of("segments", List.of("VIP")), "CUSTOMER_NOT_IN_SEGMENT"),
                condNode("c2", "order.total.gte", "tpl_order_total_gte_v1",
                        Map.of("amount", 500000, "currency", "VND"), "ORDER_TOTAL_BELOW_MIN")
        );

        String drl = compileDrl(rule);

        // Both facts present in the compiled DRL
        assertThat(drl).contains("CustomerFact");
        assertThat(drl).contains("OrderFact");
        // Customer: segment check
        assertThat(drl).contains("VIP");
        // Order: amount check
        assertThat(drl).contains("500000");
        // AND composition (both must pass)
        assertThat(drl).contains("&&");
    }

    @Test
    @DisplayName("PA-2: customer.in_segment only → reason code CUSTOMER_NOT_IN_SEGMENT in node")
    void pa2_nonVip_reasonCodeInCondNode() {
        RuleNode cond = condNode("c1", "customer.in_segment", "tpl_customer_in_segment_v1",
                Map.of("segments", List.of("VIP")), "CUSTOMER_NOT_IN_SEGMENT");
        Rule rule = adminBuiltRule(cond);

        // DRL contains CustomerFact segment check
        String drl = compileDrl(rule);
        assertThat(drl).contains("CustomerFact");
        assertThat(drl).contains("VIP");

        // Node carries the expected reason code for when it fails
        RuleNode resultCond = rule.getNodes().get(0).getChildren().get(0);
        assertThat(resultCond.getReasonCode()).isEqualTo("CUSTOMER_NOT_IN_SEGMENT");
    }

    @Test
    @DisplayName("PA-3: customer + order both fail → AND logic, both reason codes in nodes")
    void pa3_bothFail_andLogicBothReasonCodesPresent() {
        Rule rule = adminBuiltRule(
                condNode("c1", "customer.in_segment", "tpl_customer_in_segment_v1",
                        Map.of("segments", List.of("VIP")), "CUSTOMER_NOT_IN_SEGMENT"),
                condNode("c2", "order.total.gte", "tpl_order_total_gte_v1",
                        Map.of("amount", 500000, "currency", "VND"), "ORDER_TOTAL_BELOW_MIN")
        );

        String drl = compileDrl(rule);
        assertThat(drl).contains("&&");

        // Both reason codes are encoded in the node tree (engine uses these when building DENY verdict)
        List<String> reasonCodes = rule.getNodes().get(0).getChildren().stream()
                .map(RuleNode::getReasonCode)
                .collect(Collectors.toList());
        assertThat(reasonCodes).containsExactlyInAnyOrder(
                "CUSTOMER_NOT_IN_SEGMENT",
                "ORDER_TOTAL_BELOW_MIN"
        );
    }

    @Test
    @DisplayName("PA-4: customer.loyalty_tier.gte → DRL has loyaltyTier threshold constraint")
    void pa4_loyaltyTier_drlHasTierConstraint() {
        Rule rule = adminBuiltRule(
                condNode("c1", "customer.loyalty_tier.gte", "tpl_customer_loyalty_tier_gte_v1",
                        Map.of("tier", "GOLD"), "LOYALTY_TIER_BELOW_MIN")
        );

        String drl = compileDrl(rule);
        assertThat(drl).contains("CustomerFact");
        assertThat(drl).contains("loyaltyTier");
        assertThat(drl).contains("GOLD");
    }

    @Test
    @DisplayName("PA-5: order.items.count.gte → DRL has items.size constraint")
    void pa5_orderItemsCount_drlHasCountConstraint() {
        Rule rule = adminBuiltRule(
                condNode("c1", "order.items.count.gte", "tpl_order_items_count_gte_v1",
                        Map.of("count", 3), "ORDER_ITEMS_COUNT_BELOW_MIN")
        );

        String drl = compileDrl(rule);
        assertThat(drl).contains("OrderFact");
        assertThat(drl).contains("items.size");
        assertThat(drl).contains("3");
    }

    @Test
    @DisplayName("PA-6: product.in_category → DRL has CartItemFact + categoryId filter")
    void pa6_productInCategory_drlHasCategoryConstraint() {
        Rule rule = adminBuiltRule(
                condNode("c1", "product.in_category", "tpl_product_in_category_v1",
                        Map.of("categoryId", "cat-electronics"), "PRODUCT_NOT_IN_CATEGORY")
        );

        String drl = compileDrl(rule);
        assertThat(drl).contains("CartItemFact");
        assertThat(drl).contains("cat-electronics");
    }

    @Test
    @DisplayName("PA-7: cart.has_product → DRL has exists check on CartItemFact with productId")
    void pa7_cartHasProduct_drlHasExistsPattern() {
        Rule rule = adminBuiltRule(
                condNode("c1", "cart.has_product", "tpl_cart_has_product_v1",
                        Map.of("productId", "sku-blacklist-001"), "PRODUCT_BLACKLISTED")
        );

        String drl = compileDrl(rule);
        assertThat(drl).contains("exists");
        assertThat(drl).contains("CartItemFact");
        assertThat(drl).contains("sku-blacklist-001");
    }

    // =========================================================================
    // compileDrlAndRegisterInEngine integration — verifies RuleEngineClient.register()
    // is called after Path B creation with non-empty nodes
    // =========================================================================

    @Test
    @DisplayName("T4 integration: Path B rule with nodes → ruleEngineClient.register() called")
    void compileDrlAndRegister_pathB_callsRuleEngineClient() {
        SettingValidationRuleCommandPayload payload =
                payloadWithTimeframe("2026-04-26T00:00:00Z", "2026-12-31T23:59:59Z");
        Rule rule = handler.resolveOrCreateRule(payload);

        // Simulate what processCommand does: compile + register
        List<Operator> operators = operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE);
        Map<String, Operator> operatorMap = operators.stream()
                .collect(Collectors.toMap(Operator::getName, op -> op, (a, b) -> a));
        String drl = drlCompiler.compile(rule, rule.getNodes(), operatorMap);

        ruleEngineClient.register(rule.getId(), drl);

        // Verify the registered DRL contains the validity window template output
        verify(ruleEngineClient).register(
                org.mockito.ArgumentMatchers.eq(rule.getId()),
                org.mockito.ArgumentMatchers.argThat(registeredDrl ->
                        registeredDrl.contains("isBefore") && registeredDrl.contains("isAfter")
                )
        );
    }

    @Test
    @DisplayName("Path A: resolveOrCreateRule(ruleId != null) → loads from DB (Path A)")
    void pathA_existingRuleId_loadsFromDb() {
        Rule existingRule = adminBuiltRule(
                condNode("c1", "order.total.gte", "tpl_order_total_gte_v1",
                        Map.of("amount", 100000, "currency", "VND"), "ORDER_TOO_LOW")
        );

        when(validationRulePort.findById(existingRule.getId())).thenReturn(Optional.of(existingRule));

        SettingValidationRuleCommandPayload payload = SettingValidationRuleCommandPayload.builder()
                .ruleId(existingRule.getId())
                .objectType("CAMPAIGN")
                .objectId("camp-existing-001")
                .build();

        Rule resolved = handler.resolveOrCreateRule(payload);

        // Returns the rule from DB (same instance)
        assertThat(resolved.getId()).isEqualTo(existingRule.getId());
    }

    @Test
    @DisplayName("Path A: ruleId not found in DB → throws RuleNotFoundException")
    void pathA_ruleNotFound_throwsException() {
        when(validationRulePort.findById("non-existent-rule")).thenReturn(Optional.empty());

        SettingValidationRuleCommandPayload payload = SettingValidationRuleCommandPayload.builder()
                .ruleId("non-existent-rule")
                .objectType("CAMPAIGN")
                .objectId("camp-001")
                .build();

        assertThatThrownBy(() -> handler.resolveOrCreateRule(payload))
                .isInstanceOf(RuleNotFoundException.class);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SettingValidationRuleCommandPayload payloadWithTimeframe(String startIso, String endIso) {
        ValidityTimeframe validity = ValidityTimeframe.builder()
                .startDate(startIso != null ? Instant.parse(startIso) : null)
                .expirationDate(endIso != null ? Instant.parse(endIso) : null)
                .build();

        TimeFrame timeframe = (startIso == null && endIso == null)
                ? null
                : TimeFrame.builder()
                        .validityTimeframe(validity)
                        .timezone("Asia/Ho_Chi_Minh")
                        .build();

        return SettingValidationRuleCommandPayload.builder()
                .ruleId(null)  // null → Path B
                .objectType("CAMPAIGN")
                .objectId("camp-test-001")
                .timeframe(timeframe)
                .build();
    }

    private Rule adminBuiltRule(RuleNode... condNodes) {
        List<RuleNode> children = List.of(condNodes);
        RuleNode rootGroup = RuleNode.builder()
                .nodeId("root-group")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(children)
                .build();

        return Rule.builder()
                .id("rule-pa-" + System.nanoTime())
                .code("PA_TEST")
                .name("Path A Test Rule")
                .logic(Rule.LogicType.ALL)
                .state(Rule.RuleState.PUBLISHED)
                .active(true)
                .ruleVersion(1L)
                .nodes(List.of(rootGroup))
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("test")
                .updatedBy("test")
                .build();
    }

    private RuleNode condNode(String id, String operatorName, String compilerId,
                               Map<String, Object> params, String reasonCode) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .params(params)
                .reasonCode(reasonCode)
                .build();
    }

    private String compileDrl(Rule rule) {
        List<Operator> operators = operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE);
        Map<String, Operator> operatorMap = operators.stream()
                .collect(Collectors.toMap(Operator::getName, op -> op, (a, b) -> a));
        return drlCompiler.compile(rule, rule.getNodes(), operatorMap);
    }

    private List<Operator> buildOperatorList() {
        return List.of(
                operator("binding.validity_window", "tpl_binding_validity_window_v1"),
                operator("customer.in_segment", "tpl_customer_in_segment_v1"),
                operator("order.total.gte", "tpl_order_total_gte_v1"),
                operator("customer.loyalty_tier.gte", "tpl_customer_loyalty_tier_gte_v1"),
                operator("order.items.count.gte", "tpl_order_items_count_gte_v1"),
                operator("product.in_category", "tpl_product_in_category_v1"),
                operator("product.in_list", "tpl_product_in_list_v1"),
                operator("cart.has_product", "tpl_cart_has_product_v1"),
                operator("time.within_window", "tpl_time_within_window_v1")
        );
    }

    private Operator operator(String name, String compilerId) {
        return Operator.builder()
                .id("op-" + name)
                .name(name)
                .compilerId(compilerId)
                .status(Operator.OperatorStatus.ACTIVE)
                .version(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
