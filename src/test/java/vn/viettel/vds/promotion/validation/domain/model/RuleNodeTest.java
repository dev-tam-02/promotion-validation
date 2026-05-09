package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleStructureException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleEvaluationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RuleNode Tests")
class RuleNodeTest {

    @Nested
    @DisplayName("Builder validation")
    class BuilderValidationTests {

        @Test
        @DisplayName("Should build placeholder node when nodeId is null")
        void shouldBuildPlaceholderNode_whenNodeIdNull() {
            var node = RuleNode.builder()
                    .field("segment")
                    .operator("EQUALS")
                    .value("VIP")
                    .build();

            assertThat(node.getNodeId()).isNull();
            assertThat(node.getField()).isEqualTo("segment");
        }

        @Test
        @DisplayName("Should build placeholder node when type is null")
        void shouldBuildPlaceholderNode_whenTypeNull() {
            var node = RuleNode.builder()
                    .nodeId("node-1")
                    .field("segment")
                    .operator("EQUALS")
                    .value("VIP")
                    .build();

            assertThat(node.getNodeId()).isEqualTo("node-1");
            assertThat(node.getType()).isNull();
        }

        @Test
        @DisplayName("Should throw when GROUP node has no groupLogic")
        void shouldThrow_whenGroupNodeHasNoGroupLogic() {
            var child = RuleNode.builder().nodeId("child-1").build();
            var builder = RuleNode.builder()
                    .nodeId("group-1")
                    .type(RuleNode.NodeType.GROUP)
                    .children(List.of(child));

            assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when GROUP node has no children")
        void shouldThrow_whenGroupNodeHasNoChildren() {
            var builder = RuleNode.builder()
                    .nodeId("group-1")
                    .type(RuleNode.NodeType.GROUP)
                    .groupLogic(Rule.LogicType.ALL);

            assertThatThrownBy(builder::build).isInstanceOf(InvalidRuleStructureException.class);
        }

        @Test
        @DisplayName("Should throw when COND node has no operatorName")
        void shouldThrow_whenCondNodeHasNoOperatorName() {
            var builder = RuleNode.builder()
                    .nodeId("cond-1")
                    .type(RuleNode.NodeType.COND);

            assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should build valid GROUP node")
        void shouldBuildValidGroupNode() {
            var child = RuleNode.builder().nodeId("child-1").build();

            var node = RuleNode.builder()
                    .nodeId("group-1")
                    .type(RuleNode.NodeType.GROUP)
                    .groupLogic(Rule.LogicType.ALL)
                    .children(List.of(child))
                    .build();

            assertThat(node.getType()).isEqualTo(RuleNode.NodeType.GROUP);
            assertThat(node.getChildren()).hasSize(1);
        }

        @Test
        @DisplayName("Should build valid COND node")
        void shouldBuildValidCondNode() {
            var node = RuleNode.builder()
                    .nodeId("cond-1")
                    .type(RuleNode.NodeType.COND)
                    .operatorName("EQUALS")
                    .reasonCode("SEGMENT_MISMATCH")
                    .build();

            assertThat(node.getType()).isEqualTo(RuleNode.NodeType.COND);
            assertThat(node.getOperatorName()).isEqualTo("EQUALS");
        }
    }

    @Nested
    @DisplayName("isLeafNode()")
    class IsLeafNodeTests {

        @Test
        @DisplayName("Should return true when no children")
        void shouldReturnTrue_whenNoChildren() {
            var node = TestFixtures.leafNode("segment", "EQUALS", "VIP");
            assertThat(node.isLeafNode()).isTrue();
        }

        @Test
        @DisplayName("Should return false when has children")
        void shouldReturnFalse_whenHasChildren() {
            var child = TestFixtures.leafNode("segment", "EQUALS", "VIP");
            var parent = TestFixtures.parentNode(LogicType.AND, List.of(child));
            assertThat(parent.isLeafNode()).isFalse();
        }
    }

    @Nested
    @DisplayName("evaluate() - leaf node")
    class EvaluateLeafNodeTests {

        @Test
        @DisplayName("Should evaluate leaf node with EQUALS operator")
        void shouldEvaluateLeafNode_equals() {
            var node = TestFixtures.leafNode("segment", "EQUALS", "VIP");
            var context = TestFixtures.validContext();

            assertThat(node.evaluate(context)).isTrue();
        }

        @Test
        @DisplayName("Should return false when field value does not match")
        void shouldReturnFalse_whenFieldValueNotMatch() {
            var node = TestFixtures.leafNode("segment", "EQUALS", "SILVER");
            var context = TestFixtures.validContext();

            assertThat(node.evaluate(context)).isFalse();
        }

        @Test
        @DisplayName("Should return false when field value is null for non-null-check operator")
        void shouldReturnFalse_whenFieldNull_nonNullCheckOperator() {
            var node = TestFixtures.leafNode("nonexistent", "EQUALS", "value");
            var context = TestFixtures.validContext();

            assertThat(node.evaluate(context)).isFalse();
        }

        @Test
        @DisplayName("Should throw when field is null in leaf node")
        void shouldThrow_whenFieldNull() {
            var node = RuleNode.builder()
                    .nodeId("node-1")
                    .operator("EQUALS")
                    .value("VIP")
                    .build();
            var context = TestFixtures.validContext();

            assertThatThrownBy(() -> node.evaluate(context))
                    .isInstanceOf(RuleEvaluationException.class);
        }

        @Test
        @DisplayName("Should throw when operator is null in leaf node")
        void shouldThrow_whenOperatorNull() {
            var node = RuleNode.builder()
                    .nodeId("node-1")
                    .field("segment")
                    .value("VIP")
                    .build();
            var context = TestFixtures.validContext();

            assertThatThrownBy(() -> node.evaluate(context))
                    .isInstanceOf(RuleEvaluationException.class);
        }

        @Test
        @DisplayName("Should throw when expected value is null for non-null-check operator")
        void shouldThrow_whenExpectedValueNull_nonNullCheck() {
            var node = RuleNode.builder()
                    .nodeId("node-1")
                    .field("segment")
                    .operator("EQUALS")
                    .build();
            var context = TestFixtures.validContext();

            assertThatThrownBy(() -> node.evaluate(context))
                    .isInstanceOf(RuleEvaluationException.class);
        }

        @Test
        @DisplayName("Should evaluate IS_NULL operator with null field value")
        void shouldEvaluate_isNull_withNullField() {
            var node = TestFixtures.leafNode("nonexistent", "IS_NULL", null);
            var context = TestFixtures.validContext();

            assertThat(node.evaluate(context)).isTrue();
        }

        @Test
        @DisplayName("Should evaluate IS_NOT_NULL operator with non-null field value")
        void shouldEvaluate_isNotNull_withNonNullField() {
            var node = TestFixtures.leafNode("segment", "IS_NOT_NULL", null);
            var context = TestFixtures.validContext();

            assertThat(node.evaluate(context)).isTrue();
        }
    }

    @Nested
    @DisplayName("evaluate() - parent node AND")
    class EvaluateAndTests {

        @Test
        @DisplayName("Should return true when all children pass")
        void shouldReturnTrue_whenAllChildrenPass() {
            var child1 = TestFixtures.leafNode("segment", "EQUALS", "VIP");
            var child2 = TestFixtures.leafNode("tier", "EQUALS", "GOLD");
            var parent = TestFixtures.parentNode(LogicType.AND, List.of(child1, child2));

            assertThat(parent.evaluate(TestFixtures.validContext())).isTrue();
        }

        @Test
        @DisplayName("Should return false when one child fails")
        void shouldReturnFalse_whenOneChildFails() {
            var child1 = TestFixtures.leafNode("segment", "EQUALS", "VIP");
            var child2 = TestFixtures.leafNode("tier", "EQUALS", "SILVER");
            var parent = TestFixtures.parentNode(LogicType.AND, List.of(child1, child2));

            assertThat(parent.evaluate(TestFixtures.validContext())).isFalse();
        }
    }

    @Nested
    @DisplayName("evaluate() - parent node OR")
    class EvaluateOrTests {

        @Test
        @DisplayName("Should return true when at least one child passes")
        void shouldReturnTrue_whenOneChildPasses() {
            var child1 = TestFixtures.leafNode("segment", "EQUALS", "SILVER");
            var child2 = TestFixtures.leafNode("tier", "EQUALS", "GOLD");
            var parent = TestFixtures.parentNode(LogicType.OR, List.of(child1, child2));

            assertThat(parent.evaluate(TestFixtures.validContext())).isTrue();
        }

        @Test
        @DisplayName("Should return false when all children fail")
        void shouldReturnFalse_whenAllChildrenFail() {
            var child1 = TestFixtures.leafNode("segment", "EQUALS", "SILVER");
            var child2 = TestFixtures.leafNode("tier", "EQUALS", "SILVER");
            var parent = TestFixtures.parentNode(LogicType.OR, List.of(child1, child2));

            assertThat(parent.evaluate(TestFixtures.validContext())).isFalse();
        }
    }

    @Nested
    @DisplayName("evaluate() - parent node NOT")
    class EvaluateNotTests {

        @Test
        @DisplayName("Should negate single child result")
        void shouldNegateChildResult() {
            var child = TestFixtures.leafNode("segment", "EQUALS", "SILVER");
            var parent = TestFixtures.parentNode(LogicType.NOT, List.of(child));

            assertThat(parent.evaluate(TestFixtures.validContext())).isTrue();
        }

        @Test
        @DisplayName("Should throw when NOT has more than one child")
        void shouldThrow_whenNotHasMultipleChildren() {
            var child1 = TestFixtures.leafNode("segment", "EQUALS", "VIP");
            var child2 = TestFixtures.leafNode("tier", "EQUALS", "GOLD");
            var parent = TestFixtures.parentNode(LogicType.NOT, List.of(child1, child2));
            var context = TestFixtures.validContext();

            assertThatThrownBy(() -> parent.evaluate(context))
                    .isInstanceOf(InvalidRuleStructureException.class);
        }
    }

    @Nested
    @DisplayName("evaluate() - parent node XOR")
    class EvaluateXorTests {

        @Test
        @DisplayName("Should return true when exactly one child passes")
        void shouldReturnTrue_whenExactlyOneChildPasses() {
            var child1 = TestFixtures.leafNode("segment", "EQUALS", "VIP");
            var child2 = TestFixtures.leafNode("tier", "EQUALS", "SILVER");
            var parent = TestFixtures.parentNode(LogicType.XOR, List.of(child1, child2));

            assertThat(parent.evaluate(TestFixtures.validContext())).isTrue();
        }

        @Test
        @DisplayName("Should return false when more than one child passes")
        void shouldReturnFalse_whenMultipleChildrenPass() {
            var child1 = TestFixtures.leafNode("segment", "EQUALS", "VIP");
            var child2 = TestFixtures.leafNode("tier", "EQUALS", "GOLD");
            var parent = TestFixtures.parentNode(LogicType.XOR, List.of(child1, child2));

            assertThat(parent.evaluate(TestFixtures.validContext())).isFalse();
        }
    }

    @Nested
    @DisplayName("getChildren()")
    class GetChildrenTests {

        @Test
        @DisplayName("Should return unmodifiable list")
        void shouldReturnUnmodifiableList() {
            var child = TestFixtures.leafNode("segment", "EQUALS", "VIP");
            var parent = TestFixtures.parentNode(LogicType.AND, List.of(child));
            var children = parent.getChildren();

            assertThatThrownBy(() -> children.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Builder.isValid()")
    class BuilderIsValidTests {

        @Test
        @DisplayName("Should return false when nodeId is null")
        void shouldReturnFalse_whenNodeIdNull() {
            var builder = RuleNode.builder().type(RuleNode.NodeType.COND);
            assertThat(builder.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should return false when type is null")
        void shouldReturnFalse_whenTypeNull() {
            var builder = RuleNode.builder().nodeId("node-1");
            assertThat(builder.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should return true for valid COND builder")
        void shouldReturnTrue_forValidCondBuilder() {
            var builder = RuleNode.builder()
                    .nodeId("cond-1")
                    .type(RuleNode.NodeType.COND)
                    .operatorName("EQUALS")
                    .reasonCode("MISMATCH");
            assertThat(builder.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should return true for valid GROUP builder")
        void shouldReturnTrue_forValidGroupBuilder() {
            var child = RuleNode.builder().build();
            var builder = RuleNode.builder()
                    .nodeId("group-1")
                    .type(RuleNode.NodeType.GROUP)
                    .groupLogic(Rule.LogicType.ALL)
                    .addChild(child);
            assertThat(builder.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Getters")
    class GetterTests {

        @Test
        @DisplayName("Should return all builder-set values")
        void shouldReturnAllValues() {
            var node = RuleNode.builder()
                    .nodeId("node-1")
                    .field("segment")
                    .operator("EQUALS")
                    .value("VIP")
                    .description("Check segment")
                    .build();

            assertThat(node.getNodeId()).isEqualTo("node-1");
            assertThat(node.getId()).isEqualTo("node-1");
            assertThat(node.getField()).isEqualTo("segment");
            assertThat(node.getOperator()).isEqualTo("EQUALS");
            assertThat(node.getValue()).isEqualTo("VIP");
            assertThat(node.getDescription()).isEqualTo("Check segment");
        }
    }
}
