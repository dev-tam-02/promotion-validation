package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link RuleValidator} covering:
 * - JSON Schema validation (draft-07) via networknt
 * - Tree depth guard (≤ 10)
 * - Circular reference detection
 * - GROUP nodes must have children
 */
@DisplayName("RuleValidator — domain invariant checks")
class RuleValidatorTest {

    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RuleValidator(new ObjectMapper());
    }

    // -----------------------------------------------------------------------
    //  checkTreeDepth
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("checkTreeDepth")
    class CheckTreeDepth {

        @Test
        @DisplayName("depth 3 passes without exception")
        void depth3_passes() {
            List<RuleNode> nodes = buildTree(3);
            assertThatCode(() -> validator.checkTreeDepth(nodes))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("depth exactly 10 passes")
        void depth10_passes() {
            List<RuleNode> nodes = buildTree(10);
            assertThatCode(() -> validator.checkTreeDepth(nodes))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("depth 11 throws RuleValidationException")
        void depth11_throws() {
            List<RuleNode> nodes = buildTree(11);
            assertThatThrownBy(() -> validator.checkTreeDepth(nodes))
                    .isInstanceOf(RuleValidator.RuleValidationException.class)
                    .hasMessageContaining("depth");
        }

        @Test
        @DisplayName("null roots does not throw")
        void nullRoots_passes() {
            assertThatCode(() -> validator.checkTreeDepth(null))
                    .doesNotThrowAnyException();
        }
    }

    // -----------------------------------------------------------------------
    //  checkNoCircular
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("checkNoCircular")
    class CheckNoCircular {

        @Test
        @DisplayName("acyclic tree passes")
        void acyclicTree_passes() {
            List<RuleNode> tree = buildTree(3);
            assertThatCode(() -> validator.checkNoCircular(tree))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("flat COND node passes")
        void singleCondNode_passes() {
            RuleNode cond = condNode("c1", "order.total.gte", Map.of("amount", 100));
            assertThatCode(() -> validator.checkNoCircular(List.of(cond)))
                    .doesNotThrowAnyException();
        }
    }

    // -----------------------------------------------------------------------
    //  checkAllGroupsHaveChildren
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("checkAllGroupsHaveChildren")
    class CheckAllGroupsHaveChildren {

        @Test
        @DisplayName("GROUP with one child passes")
        void groupWithChild_passes() {
            RuleNode cond = condNode("c1", "order.total.gte", Map.of("amount", 100));
            RuleNode group = groupNode("g1", Rule.LogicType.ALL, List.of(cond));
            assertThatCode(() -> validator.checkAllGroupsHaveChildren(List.of(group)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("empty GROUP throws RuleValidationException")
        void emptyGroup_throws() {
            RuleNode group = groupNode("g1", Rule.LogicType.ALL, List.of());
            assertThatThrownBy(() -> validator.checkAllGroupsHaveChildren(List.of(group)))
                    .isInstanceOf(RuleValidator.RuleValidationException.class)
                    .hasMessageContaining("GROUP node");
        }

        @Test
        @DisplayName("COND node (leaf) always passes")
        void condNode_passes() {
            RuleNode cond = condNode("c1", "order.total.gte", null);
            assertThatCode(() -> validator.checkAllGroupsHaveChildren(List.of(cond)))
                    .doesNotThrowAnyException();
        }
    }

    // -----------------------------------------------------------------------
    //  checkOperatorParamsMatchSchema
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("checkOperatorParamsMatchSchema — JSON Schema draft-07")
    class CheckOperatorParamsMatchSchema {

        private static final String ORDER_TOTAL_SCHEMA =
                "{\"type\":\"object\",\"required\":[\"amount\",\"currency\"]," +
                "\"properties\":{\"amount\":{\"type\":\"number\",\"minimum\":0}," +
                "\"currency\":{\"type\":\"string\",\"enum\":[\"VND\",\"USD\"]}}}";

        @Test
        @DisplayName("valid params pass schema validation")
        void validParams_pass() {
            Operator op = operatorWithSchema("order.total.gte", ORDER_TOTAL_SCHEMA);
            RuleNode cond = condNode("c1", "order.total.gte",
                    Map.of("amount", 100_000, "currency", "VND"));
            assertThatCode(() -> validator.checkOperatorParamsMatchSchema(
                    List.of(cond), Map.of("order.total.gte", op)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("missing required field throws RuleValidationException")
        void missingRequiredField_throws() {
            Operator op = operatorWithSchema("order.total.gte", ORDER_TOTAL_SCHEMA);
            // missing "currency"
            RuleNode cond = condNode("c1", "order.total.gte", Map.of("amount", 100_000));
            assertThatThrownBy(() -> validator.checkOperatorParamsMatchSchema(
                    List.of(cond), Map.of("order.total.gte", op)))
                    .isInstanceOf(RuleValidator.RuleValidationException.class)
                    .hasMessageContaining("params validation failed");
        }

        @Test
        @DisplayName("invalid enum value throws RuleValidationException")
        void invalidEnumValue_throws() {
            Operator op = operatorWithSchema("order.total.gte", ORDER_TOTAL_SCHEMA);
            // "EUR" not in enum
            RuleNode cond = condNode("c1", "order.total.gte",
                    Map.of("amount", 100_000, "currency", "EUR"));
            assertThatThrownBy(() -> validator.checkOperatorParamsMatchSchema(
                    List.of(cond), Map.of("order.total.gte", op)))
                    .isInstanceOf(RuleValidator.RuleValidationException.class);
        }

        @Test
        @DisplayName("unknown operator (not in map) is skipped without error")
        void unknownOperator_skipped() {
            RuleNode cond = condNode("c1", "unknown.op", Map.of("x", 1));
            assertThatCode(() -> validator.checkOperatorParamsMatchSchema(
                    List.of(cond), Map.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null params on COND node are skipped")
        void nullParams_skipped() {
            Operator op = operatorWithSchema("order.total.gte", ORDER_TOTAL_SCHEMA);
            RuleNode cond = condNode("c1", "order.total.gte", null);
            assertThatCode(() -> validator.checkOperatorParamsMatchSchema(
                    List.of(cond), Map.of("order.total.gte", op)))
                    .doesNotThrowAnyException();
        }
    }

    // -----------------------------------------------------------------------
    //  helpers
    // -----------------------------------------------------------------------

    /** Build a chain of GROUP → GROUP → ... → COND with the given depth. */
    private List<RuleNode> buildTree(int depth) {
        if (depth <= 0) {
            return List.of();
        }
        return List.of(buildChain(depth, 1));
    }

    private RuleNode buildChain(int depth, int level) {
        if (level == depth) {
            return condNode("node-" + level, "order.total.gte", null);
        }
        RuleNode child = buildChain(depth, level + 1);
        return groupNode("node-" + level, Rule.LogicType.ALL, List.of(child));
    }

    private RuleNode condNode(String id, String operatorName, Map<String, Object> params) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .params(params)
                .reasonCode("RC-001")
                .build();
    }

    private RuleNode groupNode(String id, Rule.LogicType logic, List<RuleNode> children) {
        RuleNode.Builder builder = RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(logic)
                .children(children);
        // Use buildPartial() for empty-children groups so the domain model doesn't throw
        // before the application-level validator gets a chance to check.
        return children.isEmpty() ? builder.buildPartial() : builder.build();
    }

    @SuppressWarnings("unchecked")
    private Operator operatorWithSchema(String name, String schemaJson) {
        try {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> schemaMap = om.readValue(schemaJson, Map.class);
            return Operator.builder()
                    .id("op-" + name)
                    .name(name)
                    .jsonSchema(schemaMap)
                    .compilerId("tpl")
                    .status(Operator.OperatorStatus.ACTIVE)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
