package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Comprehensive parameterized test for {@link DrlCompiler}.
 *
 * <p>Covers all Mustache templates shipped with pp-validation and verifies:
 * <ol>
 *   <li>Template renders without throwing an exception.</li>
 *   <li>Rendered snippet contains every expected substring.</li>
 *   <li>Snippet does NOT contain disallowed patterns (negative checks).</li>
 *   <li>{@link DrlCompiler#compile} produces a well-formed DRL rule block.</li>
 * </ol>
 *
 * <p>Note: pp-validation does not depend on Drools at test time — DRL syntax
 * correctness at the Kie level is verified by pp-rule-engine tests. Here we
 * assert on rendered text only.
 */
@DisplayName("DrlCompiler — comprehensive template rendering")
class DrlCompilerComprehensiveTest {

    private DrlCompiler compiler;

    static Stream<Arguments> templateRenderCases() {
        return Stream.of(

                // ─── tpl_order_total_gte_v1 ─────────────────────────────────────
                args("order_total_gte: standard",
                        "tpl_order_total_gte_v1",
                        Map.of("amount", 500_000, "currency", "VND"),
                        List.of("OrderFact", "totalAmount", "500000", "VND"),
                        List.of()),

                args("order_total_gte: large amount",
                        "tpl_order_total_gte_v1",
                        Map.of("amount", 10_000_000, "currency", "USD"),
                        List.of("OrderFact", "10000000", "USD"),
                        List.of()),

                // ─── tpl_customer_in_segment_v1 ──────────────────────────────────
                args("customer_in_segment: single segment",
                        "tpl_customer_in_segment_v1",
                        Map.of("segments", List.of("VIP_GOLD")),
                        List.of("CustomerFact", "segments contains \"VIP_GOLD\""),
                        List.of()),

                args("customer_in_segment: two segments joined with OR",
                        "tpl_customer_in_segment_v1",
                        Map.of("segments", List.of("VIP", "PREMIUM")),
                        List.of("CustomerFact", "segments contains \"VIP\"", "segments contains \"PREMIUM\"", " || "),
                        List.of(" && ")),

                args("customer_in_segment: three segments",
                        "tpl_customer_in_segment_v1",
                        Map.of("segments", List.of("BRONZE", "SILVER", "GOLD")),
                        List.of("CustomerFact", "BRONZE", "SILVER", "GOLD", " || "),
                        List.of()),

                // ─── tpl_customer_loyalty_tier_gte_v1 ────────────────────────────
                args("customer_loyalty_tier_gte: GOLD",
                        "tpl_customer_loyalty_tier_gte_v1",
                        Map.of("tier", "GOLD"),
                        List.of("CustomerFact", "loyaltyTier", "GOLD", "compareTo"),
                        List.of()),

                args("customer_loyalty_tier_gte: PLATINUM",
                        "tpl_customer_loyalty_tier_gte_v1",
                        Map.of("tier", "PLATINUM"),
                        List.of("CustomerFact", "loyaltyTier", "PLATINUM", ">= 0"),
                        List.of()),

                // ─── tpl_order_items_count_gte_v1 ────────────────────────────────
                args("order_items_count_gte: count 3",
                        "tpl_order_items_count_gte_v1",
                        Map.of("count", 3),
                        List.of("OrderFact", "items.size", "3"),
                        List.of()),

                args("order_items_count_gte: count 10",
                        "tpl_order_items_count_gte_v1",
                        Map.of("count", 10),
                        List.of("OrderFact", "items.size", "10"),
                        List.of()),

                // ─── tpl_product_in_category_v1 ──────────────────────────────────
                args("product_in_category: electronics",
                        "tpl_product_in_category_v1",
                        Map.of("categoryId", "cat-electronics"),
                        List.of("CartItemFact", "cat-electronics"),
                        List.of()),

                args("product_in_category: foods",
                        "tpl_product_in_category_v1",
                        Map.of("categoryId", "cat-foods-001"),
                        List.of("CartItemFact", "cat-foods-001"),
                        List.of()),

                // ─── tpl_product_in_list_v1 ──────────────────────────────────────
                args("product_in_list: single SKU",
                        "tpl_product_in_list_v1",
                        Map.of("productId", "sku-abc"),
                        List.of("CartItemFact", "sku-abc"),
                        List.of()),

                args("product_in_list: hyphenated ID",
                        "tpl_product_in_list_v1",
                        Map.of("productId", "prod-001-abc"),
                        List.of("CartItemFact", "prod-001-abc"),
                        List.of()),

                // ─── tpl_cart_has_product_v1 ─────────────────────────────────────
                args("cart_has_product: existence check",
                        "tpl_cart_has_product_v1",
                        Map.of("productId", "sku-abc"),
                        List.of("exists", "CartItemFact", "sku-abc"),
                        List.of()),

                args("cart_has_product: blacklisted product",
                        "tpl_cart_has_product_v1",
                        Map.of("productId", "prod-blacklist-999"),
                        List.of("exists", "CartItemFact", "prod-blacklist-999"),
                        List.of()),

                // ─── tpl_time_within_window_v1 ────────────────────────────────────
                args("time_within_window: date range",
                        "tpl_time_within_window_v1",
                        Map.of("from", "2026-01-01T00:00:00Z", "to", "2026-12-31T23:59:59Z"),
                        List.of("ExecutionContextFact", "2026-01-01T00:00:00Z", "2026-12-31T23:59:59Z"),
                        List.of()),

                args("time_within_window: narrow window",
                        "tpl_time_within_window_v1",
                        Map.of("from", "2026-06-01T08:00:00Z", "to", "2026-06-01T22:00:00Z"),
                        List.of("ExecutionContextFact", "2026-06-01T08:00:00Z", "2026-06-01T22:00:00Z"),
                        List.of()),

                // ─── tpl_customer_is_owner_v1 ────────────────────────────────────
                args("customer_is_owner: no params needed",
                        "tpl_customer_is_owner_v1",
                        Map.of(),
                        List.of("VoucherFact", "ownerCustomerId", "CustomerFact"),
                        List.of()),

                // ─── tpl_binding_validity_window_v1 — both bounds ────────────────
                args("binding_validity_window: both start+end",
                        "tpl_binding_validity_window_v1",
                        Map.of("startDate", "2026-04-26T00:00:00Z", "endDate", "2026-12-31T23:59:59Z"),
                        List.of("eval(", "isBefore", "isAfter", "&&",
                                "2026-04-26T00:00:00Z", "2026-12-31T23:59:59Z"),
                        List.of()),

                args("binding_validity_window: only startDate",
                        "tpl_binding_validity_window_v1",
                        Map.of("startDate", "2026-01-01T00:00:00Z"),
                        List.of("eval(", "isBefore", "2026-01-01T00:00:00Z"),
                        List.of("isAfter", "&&")),

                args("binding_validity_window: only endDate",
                        "tpl_binding_validity_window_v1",
                        Map.of("endDate", "2026-06-30T23:59:59Z"),
                        List.of("eval(", "isAfter", "2026-06-30T23:59:59Z"),
                        List.of("isBefore", "&&")),

                args("binding_validity_window: no bounds (pass-through eval)",
                        "tpl_binding_validity_window_v1",
                        null,
                        List.of("eval("),
                        List.of("isBefore", "isAfter", "&&")),

                args("binding_validity_window: offset timezone string",
                        "tpl_binding_validity_window_v1",
                        Map.of("startDate", "2026-04-26T07:00:00+07:00", "endDate", "2026-12-31T23:59:59+07:00"),
                        List.of("2026-04-26T07:00:00+07:00", "2026-12-31T23:59:59+07:00", "&&"),
                        List.of())
        );
    }

    // =========================================================================
    // A. renderCondTemplate — parameterized across all templates + variants
    // =========================================================================

    static Stream<Arguments> compileGroupCases() {
        Rule testRule = Rule.builder().id("rule-grp-test").logic(Rule.LogicType.ALL).build();

        // COND: order.total.gte + customer.in_segment
        RuleNode orderCond = cond("c1", "order.total.gte", "tpl_order_total_gte_v1",
                Map.of("amount", 100_000, "currency", "VND"));
        RuleNode segmentCond = cond("c2", "customer.in_segment", "tpl_customer_in_segment_v1",
                Map.of("segments", List.of("VIP")));
        RuleNode loyaltyCond = cond("c3", "customer.loyalty_tier.gte", "tpl_customer_loyalty_tier_gte_v1",
                Map.of("tier", "GOLD"));
        RuleNode cartCond = cond("c4", "cart.has_product", "tpl_cart_has_product_v1",
                Map.of("productId", "sku-special"));
        RuleNode timeCond = cond("c5", "time.within_window", "tpl_time_within_window_v1",
                Map.of("from", "2026-01-01T00:00:00Z", "to", "2026-12-31T23:59:59Z"));
        RuleNode validityWindowCond = cond("c6", "binding.validity_window", "tpl_binding_validity_window_v1",
                Map.of("startDate", "2026-04-26T00:00:00Z", "endDate", "2026-12-31T23:59:59Z"));

        Map<String, Operator> allOps = opMap(orderCond, segmentCond, loyaltyCond, cartCond, timeCond, validityWindowCond);

        // GROUP AND: order.total.gte && customer.in_segment
        RuleNode andGroup = group("g1", Rule.LogicType.ALL, List.of(orderCond, segmentCond));

        // GROUP OR: customer.in_segment || customer.loyalty_tier.gte
        RuleNode orGroup = group("g2", Rule.LogicType.ANY, List.of(segmentCond, loyaltyCond));

        // GROUP NONE: !(cart.has_product)
        RuleNode noneGroup = group("g3", Rule.LogicType.NONE, List.of(cartCond));

        // Nested group: inner (order+segment) combined with time condition
        RuleNode nestedInner = group("gi1", Rule.LogicType.ALL, List.of(orderCond, segmentCond));
        RuleNode nestedOuter = group("go1", Rule.LogicType.ALL, List.of(nestedInner, timeCond));

        return Stream.of(
                Arguments.of("GROUP AND: order.total.gte && customer.in_segment",
                        testRule, List.of(andGroup), allOps,
                        List.of("&&", "OrderFact", "CustomerFact", "100000", "VND", "VIP")),

                Arguments.of("GROUP OR: customer.in_segment || loyalty_tier",
                        testRule, List.of(orGroup), allOps,
                        List.of("||", "CustomerFact", "VIP", "GOLD", "loyaltyTier")),

                Arguments.of("GROUP NONE: !(cart.has_product)",
                        testRule, List.of(noneGroup), allOps,
                        List.of("!(", "CartItemFact", "sku-special")),

                Arguments.of("single COND leaf: binding.validity_window",
                        testRule, List.of(validityWindowCond), allOps,
                        List.of("eval(", "isBefore", "isAfter", "2026-04-26T00:00:00Z")),

                Arguments.of("nested GROUP: (order && segment) && time",
                        testRule, List.of(nestedOuter), allOps,
                        List.of("&&", "OrderFact", "CustomerFact", "ExecutionContextFact")),

                Arguments.of("rule id escaping with quotes",
                        Rule.builder().id("rule\"with\"quotes").logic(Rule.LogicType.ALL).build(),
                        List.of(orderCond), allOps,
                        List.of("rule\\\"with\\\"quotes"))
        );
    }

    static Stream<Arguments> allTemplateSmokeCases() {
        return Stream.of(
                Arguments.of("tpl_order_total_gte_v1", Map.of("amount", 1, "currency", "VND")),
                Arguments.of("tpl_customer_in_segment_v1", Map.of("segments", List.of("S1"))),
                Arguments.of("tpl_customer_loyalty_tier_gte_v1", Map.of("tier", "SILVER")),
                Arguments.of("tpl_order_items_count_gte_v1", Map.of("count", 2)),
                Arguments.of("tpl_product_in_category_v1", Map.of("categoryId", "cat-x")),
                Arguments.of("tpl_product_in_list_v1", Map.of("productId", "sku-y")),
                Arguments.of("tpl_cart_has_product_v1", Map.of("productId", "sku-z")),
                Arguments.of("tpl_time_within_window_v1", Map.of("from", "2026-01-01T00:00:00Z", "to", "2026-12-31T23:59:59Z")),
                Arguments.of("tpl_customer_is_owner_v1", Map.of()),
                Arguments.of("tpl_binding_validity_window_v1", Map.of("startDate", "2026-01-01T00:00:00Z", "endDate", "2026-12-31T23:59:59Z")),
                Arguments.of("tpl_binding_validity_window_v1", Map.of("startDate", "2026-01-01T00:00:00Z")),
                Arguments.of("tpl_binding_validity_window_v1", Map.of("endDate", "2026-12-31T23:59:59Z")),
                Arguments.of("tpl_binding_validity_window_v1", null)
        );
    }

    // =========================================================================
    // B. compile() — full DRL rule block (GROUP compositions)
    // =========================================================================

    private static Arguments args(String label, String compilerId,
                                  Map<String, Object> params,
                                  List<String> mustContain,
                                  List<String> mustNotContain) {
        return Arguments.of(label, compilerId, params, mustContain, mustNotContain);
    }

    @SuppressWarnings("java:S1172")
    private static RuleNode cond(String id, String operatorName, String compilerId,
                                 Map<String, Object> params) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .params(params)
                .reasonCode("RC-" + id)
                .build();
    }

    // =========================================================================
    // C. renderCondTemplate — does not throw for all templates (smoke test)
    // =========================================================================

    private static RuleNode group(String id, Rule.LogicType logic, List<RuleNode> children) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(logic)
                .children(children)
                .build();
    }

    private static Map<String, Operator> opMap(RuleNode... nodes) {
        Map<String, String> nameToCompilerId = new HashMap<>();
        for (RuleNode n : nodes) {
            if (n.getType() == RuleNode.NodeType.COND) {
                // compilerId stored in operator; we use fallback convention from DrlCompiler:
                // tpl_{operatorName.replace('.','_')}_v1
                // But for the helper, we derive it from the node directly via a lookup table
                nameToCompilerId.put(n.getOperatorName(),
                        "tpl_" + n.getOperatorName().replace(".", "_") + "_v1");
            }
        }
        Map<String, Operator> map = new HashMap<>();
        for (RuleNode n : nodes) {
            if (n.getType() == RuleNode.NodeType.COND) {
                String opName = n.getOperatorName();
                map.put(opName, Operator.builder()
                        .id("op-" + opName)
                        .name(opName)
                        .compilerId(nameToCompilerId.get(opName))
                        .status(Operator.OperatorStatus.ACTIVE)
                        .build());
            }
        }
        return map;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Recursively collect all COND nodes from a group node tree.
     */
    @SuppressWarnings("unused")
    private static void collectConds(RuleNode node, java.util.List<RuleNode> acc) {
        if (node.getType() == RuleNode.NodeType.COND) {
            acc.add(node);
        } else if (node.getChildren() != null) {
            for (RuleNode child : node.getChildren()) {
                collectConds(child, acc);
            }
        }
    }

    @BeforeEach
    void setUp() {
        compiler = new DrlCompiler();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("templateRenderCases")
    @DisplayName("renderCondTemplate produces expected output")
    void renderCondTemplate_containsExpected(
            String label,
            String compilerId,
            Map<String, Object> params,
            List<String> mustContain,
            List<String> mustNotContain) {

        String snippet = compiler.renderCondTemplate(compilerId, params);

        assertThat(snippet)
                .as("Template %s with params %s", compilerId, params)
                .isNotBlank();

        for (String expected : mustContain) {
            assertThat(snippet)
                    .as("Snippet for [%s] should contain <%s>:\n%s", label, expected, snippet)
                    .contains(expected);
        }

        for (String forbidden : mustNotContain) {
            assertThat(snippet)
                    .as("Snippet for [%s] should NOT contain <%s>:\n%s", label, forbidden, snippet)
                    .doesNotContain(forbidden);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("compileGroupCases")
    @DisplayName("compile() builds complete DRL rule block")
    void compile_producesWellFormedDrl(
            String label,
            Rule rule,
            List<RuleNode> nodes,
            Map<String, Operator> operators,
            List<String> drlMustContain) {

        String[] drlRef = new String[1];

        assertThatCode(() -> {
            drlRef[0] = compiler.compile(rule, nodes, operators);
        })
                .as("compile() for [%s] must not throw", label)
                .doesNotThrowAnyException();

        for (String expected : drlMustContain) {
            assertThat(drlRef[0])
                    .as("DRL for [%s] should contain <%s>:\n%s", label, expected, drlRef[0])
                    .contains(expected);
        }

        // All compiled DRL must start with package and contain rule keyword
        assertThat(drlRef[0]).contains("package").contains("rule \"");
    }

    @ParameterizedTest(name = "[{index}] {0} renders without exception")
    @MethodSource("allTemplateSmokeCases")
    @DisplayName("All templates render without exception")
    void renderCondTemplate_noException(String compilerId, Map<String, Object> params) {
        assertThatCode(() -> compiler.renderCondTemplate(compilerId, params))
                .as("renderCondTemplate(%s) should not throw", compilerId)
                .doesNotThrowAnyException();
    }
}
