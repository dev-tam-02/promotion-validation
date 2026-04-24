package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DrlCompiler}.
 *
 * Covers:
 * - Template rendering for each of the 6 seeded operators
 * - GROUP AND composition: (a) && (b)
 * - GROUP OR  composition: (a) || (b)
 * - GROUP NONE negation:   !((a) || (b))
 * - String escape: quotes in params
 * - Missing template throws DrlCompileException
 */
@DisplayName("DrlCompiler unit tests")
class DrlCompilerTest {

    private DrlCompiler compiler;
    private Rule baseRule;

    @BeforeEach
    void setUp() {
        compiler = new DrlCompiler();
        baseRule = Rule.builder()
                .id("rule-test")
                .logic(Rule.LogicType.ALL)
                .build();
    }

    // ─── Template rendering per operator ──────────────────────────────────────

    @Test
    @DisplayName("tpl_order_total_gte_v1 renders amount and currency")
    void templateOrderTotalGte() {
        String snippet = compiler.renderCondTemplate(
                "tpl_order_total_gte_v1",
                Map.of("amount", 500_000, "currency", "VND"));

        assertThat(snippet).contains("OrderFact");
        assertThat(snippet).contains("500000");
        assertThat(snippet).contains("VND");
    }

    @Test
    @DisplayName("tpl_customer_in_segment_v1 renders segmentId")
    void templateCustomerInSegment() {
        String snippet = compiler.renderCondTemplate(
                "tpl_customer_in_segment_v1",
                Map.of("segmentId", "VIP_GOLD"));

        assertThat(snippet).contains("CustomerFact");
        assertThat(snippet).contains("segments");
        assertThat(snippet).contains("VIP_GOLD");
    }

    @Test
    @DisplayName("tpl_customer_loyalty_tier_gte_v1 renders tier")
    void templateCustomerLoyaltyTierGte() {
        String snippet = compiler.renderCondTemplate(
                "tpl_customer_loyalty_tier_gte_v1",
                Map.of("tier", "PLATINUM"));

        assertThat(snippet).contains("CustomerFact");
        assertThat(snippet).contains("loyaltyTier");
        assertThat(snippet).contains("PLATINUM");
    }

    @Test
    @DisplayName("tpl_product_in_category_v1 renders categoryId")
    void templateProductInCategory() {
        String snippet = compiler.renderCondTemplate(
                "tpl_product_in_category_v1",
                Map.of("categoryId", "cat-electronics"));

        assertThat(snippet).contains("CartItemFact");
        assertThat(snippet).contains("cat-electronics");
    }

    @Test
    @DisplayName("tpl_product_in_list_v1 renders productId")
    void templateProductInList() {
        String snippet = compiler.renderCondTemplate(
                "tpl_product_in_list_v1",
                Map.of("productId", "prod-001"));

        assertThat(snippet).contains("CartItemFact");
        assertThat(snippet).contains("prod-001");
    }

    @Test
    @DisplayName("tpl_cart_has_product_v1 renders productId as existence check")
    void templateCartHasProduct() {
        String snippet = compiler.renderCondTemplate(
                "tpl_cart_has_product_v1",
                Map.of("productId", "sku-abc"));

        assertThat(snippet).contains("exists");
        assertThat(snippet).contains("CartItemFact");
        assertThat(snippet).contains("sku-abc");
    }

    @Test
    @DisplayName("tpl_time_within_window_v1 renders from and to")
    void templateTimeWithinWindow() {
        String snippet = compiler.renderCondTemplate(
                "tpl_time_within_window_v1",
                Map.of("from", "2026-01-01T00:00:00Z", "to", "2026-12-31T23:59:59Z"));

        assertThat(snippet).contains("ExecutionContextFact");
        assertThat(snippet).contains("2026-01-01T00:00:00Z");
        assertThat(snippet).contains("2026-12-31T23:59:59Z");
    }

    // ─── GROUP AND composition ─────────────────────────────────────────────────

    @Test
    @DisplayName("GROUP AND with 2 COND → (a) && (b)")
    void groupAndComposition() {
        RuleNode cond1 = makeCond("c1", "order.total.gte",
                Map.of("amount", 100_000, "currency", "VND"),
                "tpl_order_total_gte_v1", "RC-01");

        RuleNode cond2 = makeCond("c2", "customer.in_segment",
                Map.of("segmentId", "SILVER"),
                "tpl_customer_in_segment_v1", "RC-02");

        RuleNode group = RuleNode.builder()
                .nodeId("g1")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(List.of(cond1, cond2))
                .build();

        Map<String, Operator> ops = buildOperatorMap(cond1, cond2);
        String drl = compiler.compile(baseRule, List.of(group), ops);

        assertThat(drl).contains("&&");
        assertThat(drl).doesNotContain("||");
        assertThat(drl).contains("OrderFact");
        assertThat(drl).contains("CustomerFact");
    }

    // ─── GROUP OR composition ──────────────────────────────────────────────────

    @Test
    @DisplayName("GROUP OR with 2 COND → (a) || (b)")
    void groupOrComposition() {
        RuleNode cond1 = makeCond("c1", "customer.in_segment",
                Map.of("segmentId", "VIP"),
                "tpl_customer_in_segment_v1", "RC-01");

        RuleNode cond2 = makeCond("c2", "customer.loyalty_tier.gte",
                Map.of("tier", "GOLD"),
                "tpl_customer_loyalty_tier_gte_v1", "RC-02");

        RuleNode group = RuleNode.builder()
                .nodeId("g1")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ANY)
                .children(List.of(cond1, cond2))
                .build();

        Map<String, Operator> ops = buildOperatorMap(cond1, cond2);
        String drl = compiler.compile(baseRule, List.of(group), ops);

        assertThat(drl).contains("||");
        assertThat(drl).doesNotContain("&&");
    }

    // ─── GROUP NONE negation ───────────────────────────────────────────────────

    @Test
    @DisplayName("GROUP NONE with 2 COND → !((a) || (b))")
    void groupNoneNegation() {
        RuleNode cond1 = makeCond("c1", "customer.in_segment",
                Map.of("segmentId", "BLOCKED"),
                "tpl_customer_in_segment_v1", "RC-01");

        RuleNode cond2 = makeCond("c2", "cart.has_product",
                Map.of("productId", "prod-blacklist"),
                "tpl_cart_has_product_v1", "RC-02");

        RuleNode group = RuleNode.builder()
                .nodeId("g1")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.NONE)
                .children(List.of(cond1, cond2))
                .build();

        Map<String, Operator> ops = buildOperatorMap(cond1, cond2);
        String drl = compiler.compile(baseRule, List.of(group), ops);

        assertThat(drl).contains("!(");
        assertThat(drl).contains("||");
    }

    // ─── String escape ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("rule id with quotes is escaped in DRL output")
    void ruleIdWithSpecialCharsEscaped() {
        Rule specialRule = baseRule.toBuilder().id("rule\"with\"quotes").build();

        RuleNode cond = makeCond("c1", "order.total.gte",
                Map.of("amount", 0, "currency", "VND"),
                "tpl_order_total_gte_v1", "RC");
        Map<String, Operator> ops = buildOperatorMap(cond);

        String drl = compiler.compile(specialRule, List.of(cond), ops);
        assertThat(drl).contains("rule\\\"with\\\"quotes");
    }

    // ─── Error: missing template ───────────────────────────────────────────────

    @Test
    @DisplayName("unknown compilerId throws DrlCompileException")
    void unknownTemplateThrows() {
        RuleNode cond = makeCond("c1", "unknown.operator",
                Map.of(),
                "tpl_nonexistent_v999", "RC");
        Map<String, Operator> ops = buildOperatorMap(cond);

        assertThatThrownBy(() -> compiler.compile(baseRule, List.of(cond), ops))
                .isInstanceOf(DrlCompiler.DrlCompileException.class)
                .hasMessageContaining("tpl_nonexistent_v999");
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private RuleNode makeCond(String id, String operatorName, Map<String, Object> params,
                               String compilerId, String reasonCode) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .params(params)
                .reasonCode(reasonCode)
                .build();
    }

    private Map<String, Operator> buildOperatorMap(RuleNode... condNodes) {
        Map<String, Operator> map = new java.util.HashMap<>();
        // We derive compilerId from the test setup — store it in operator
        // For these tests we rely on the convention that the node's operatorName
        // is unique enough that we can pre-set the compilerId
        // We use a lookup table matching our test data
        Map<String, String> nameToCompilerId = Map.of(
                "order.total.gte", "tpl_order_total_gte_v1",
                "customer.in_segment", "tpl_customer_in_segment_v1",
                "customer.loyalty_tier.gte", "tpl_customer_loyalty_tier_gte_v1",
                "product.in_category", "tpl_product_in_category_v1",
                "product.in_list", "tpl_product_in_list_v1",
                "cart.has_product", "tpl_cart_has_product_v1",
                "time.within_window", "tpl_time_within_window_v1",
                "unknown.operator", "tpl_nonexistent_v999"
        );
        for (RuleNode node : condNodes) {
            String opName = node.getOperatorName();
            String compilerId = nameToCompilerId.getOrDefault(opName, "tpl_" + opName.replace(".", "_") + "_v1");
            map.put(opName, Operator.builder()
                    .id("op-" + opName)
                    .name(opName)
                    .compilerId(compilerId)
                    .status(Operator.OperatorStatus.ACTIVE)
                    .build());
        }
        return map;
    }
}
