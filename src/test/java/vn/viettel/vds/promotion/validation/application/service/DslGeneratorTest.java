package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DslGenerator}.
 * <p>
 * Covers:
 * - Tree depth 0: single COND node (no GROUP wrapper)
 * - Tree depth 1: root GROUP with 2 COND children
 * - Tree depth 2: GROUP AND with nested GROUP OR
 * - Logic type mapping (ALL→AND, ANY→OR, NONE→NONE)
 */
@DisplayName("DslGenerator unit tests")
class DslGeneratorTest {

    private DslGenerator dslGenerator;
    private Rule baseRule;

    @BeforeEach
    void setUp() {
        dslGenerator = new DslGenerator(new ObjectMapper());
        baseRule = Rule.builder()
                .id("rule-001")
                .logic(Rule.LogicType.ALL)
                .build();
    }

    // ─── Depth 0: single COND as root ─────────────────────────────────────────

    @Test
    @DisplayName("depth-0: single COND node serialized correctly")
    void singleCondNode() {
        RuleNode cond = RuleNode.builder()
                .nodeId("c1")
                .type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte")
                .params(Map.of("amount", 500_000, "currency", "VND"))
                .reasonCode("MIN_ORDER_NOT_MET")
                .build();

        Map<String, Object> dsl = dslGenerator.generate(baseRule, List.of(cond));

        assertThat(dsl).containsEntry("ruleId", "rule-001")
                .containsEntry("logic", "AND");

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) dsl.get("root");
        assertThat(root).containsEntry("type", "COND")
                .containsEntry("operator", "order.total.gte")
                .containsEntry("reasonCode", "MIN_ORDER_NOT_MET");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) root.get("params");
        assertThat(params).containsEntry("amount", 500_000)
                .containsEntry("currency", "VND");
    }

    // ─── Depth 1: root GROUP with 2 COND children ─────────────────────────────

    @Test
    @DisplayName("depth-1: GROUP AND with two COND children")
    void groupAndWithTwoCond() {
        RuleNode cond1 = RuleNode.builder()
                .nodeId("c1")
                .type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte")
                .params(Map.of("amount", 200_000))
                .reasonCode("RC-01")
                .build();

        RuleNode cond2 = RuleNode.builder()
                .nodeId("c2")
                .type(RuleNode.NodeType.COND)
                .operatorName("customer.in_segment")
                .params(Map.of("segmentId", "VIP"))
                .reasonCode("RC-02")
                .build();

        RuleNode group = RuleNode.builder()
                .nodeId("g1")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(List.of(cond1, cond2))
                .build();

        Map<String, Object> dsl = dslGenerator.generate(baseRule, List.of(group));

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) dsl.get("root");
        assertThat(root).containsEntry("type", "GROUP")
                .containsEntry("groupLogic", "AND");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) root.get("children");
        assertThat(children).hasSize(2);

        // Use recursive comparison to avoid field-order sensitivity
        assertThat(children.get(0))
                .usingRecursiveComparison()
                .ignoringFields()
                .isEqualTo(Map.of(
                        "type", "COND",
                        "operator", "order.total.gte",
                        "params", Map.of("amount", 200_000),
                        "reasonCode", "RC-01"
                ));
    }

    // ─── Depth 2: GROUP AND with nested GROUP OR ───────────────────────────────

    @Test
    @DisplayName("depth-2: GROUP AND containing nested GROUP OR")
    void nestedGroupOrInsideGroupAnd() {
        RuleNode condA = RuleNode.builder()
                .nodeId("ca")
                .type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte")
                .params(Map.of("amount", 100_000))
                .reasonCode("RC-A")
                .build();

        RuleNode condB = RuleNode.builder()
                .nodeId("cb")
                .type(RuleNode.NodeType.COND)
                .operatorName("customer.in_segment")
                .params(Map.of("segmentId", "SILVER"))
                .reasonCode("RC-B")
                .build();

        RuleNode condC = RuleNode.builder()
                .nodeId("cc")
                .type(RuleNode.NodeType.COND)
                .operatorName("customer.loyalty_tier.gte")
                .params(Map.of("tier", "GOLD"))
                .reasonCode("RC-C")
                .build();

        // GROUP OR contains condB and condC
        RuleNode orGroup = RuleNode.builder()
                .nodeId("g_or")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ANY)
                .children(List.of(condB, condC))
                .build();

        // Root GROUP AND contains condA and orGroup
        RuleNode andGroup = RuleNode.builder()
                .nodeId("g_and")
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(List.of(condA, orGroup))
                .build();

        Map<String, Object> dsl = dslGenerator.generate(baseRule, List.of(andGroup));

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) dsl.get("root");
        assertThat(root).containsEntry("groupLogic", "AND");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rootChildren = (List<Map<String, Object>>) root.get("children");
        assertThat(rootChildren).hasSize(2);

        // Second child is the nested OR group
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedGroup = rootChildren.get(1);
        assertThat(nestedGroup).containsEntry("type", "GROUP")
                .containsEntry("groupLogic", "OR");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orChildren = (List<Map<String, Object>>) nestedGroup.get("children");
        assertThat(orChildren).hasSize(2);
        assertThat(orChildren.get(0)).containsEntry("operator", "customer.in_segment");
        assertThat(orChildren.get(1)).containsEntry("operator", "customer.loyalty_tier.gte");
    }

    // ─── Logic type mappings ───────────────────────────────────────────────────

    @Test
    @DisplayName("logic type ANY maps to OR in DSL")
    void logicAnyMapsToOr() {
        Rule anyRule = baseRule.toBuilder().logic(Rule.LogicType.ANY).build();
        RuleNode cond = RuleNode.builder()
                .nodeId("c1").type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte").params(Map.of("amount", 0)).reasonCode("RC").build();

        Map<String, Object> dsl = dslGenerator.generate(anyRule, List.of(cond));
        assertThat(dsl).containsEntry("logic", "OR");
    }

    @Test
    @DisplayName("logic type NONE maps to NONE in DSL")
    void logicNoneMapsToNone() {
        Rule noneRule = baseRule.toBuilder().logic(Rule.LogicType.NONE).build();
        RuleNode cond = RuleNode.builder()
                .nodeId("c1").type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte").params(Map.of("amount", 0)).reasonCode("RC").build();

        Map<String, Object> dsl = dslGenerator.generate(noneRule, List.of(cond));
        assertThat(dsl).containsEntry("logic", "NONE");
    }

    @Test
    @DisplayName("toJson produces valid JSON string")
    void toJsonProducesValidJson() {
        RuleNode cond = RuleNode.builder()
                .nodeId("c1").type(RuleNode.NodeType.COND)
                .operatorName("order.total.gte").params(Map.of("amount", 100)).reasonCode("RC").build();

        Map<String, Object> dsl = dslGenerator.generate(baseRule, List.of(cond));
        String json = dslGenerator.toJson(dsl);

        assertThat(json)
                .contains("\"ruleId\"")
                .contains("\"logic\"")
                .contains("\"root\"");
    }
}
