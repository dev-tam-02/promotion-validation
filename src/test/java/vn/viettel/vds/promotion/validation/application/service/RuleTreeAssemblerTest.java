package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.CondNode;
import vn.viettel.vds.promotion.validation.domain.model.GroupNode;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RuleTreeAssembler} — typed conversion of RuleNode trees
 * into GroupNode / CondNode graphs.
 */
@DisplayName("RuleTreeAssembler — typed tree conversion")
class RuleTreeAssemblerTest {

    private RuleTreeAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new RuleTreeAssembler();
    }

    // -----------------------------------------------------------------------
    //  assembleRoot
    // -----------------------------------------------------------------------

    private RuleNode condNode(String id, String operatorName, Map<String, Object> params) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .params(params)
                .reasonCode("RC-001")
                .build();
    }

    // -----------------------------------------------------------------------
    //  assembleAll
    // -----------------------------------------------------------------------

    private RuleNode groupNode(String id, Rule.LogicType logic, List<RuleNode> children) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(logic)
                .children(children)
                .build();
    }

    // -----------------------------------------------------------------------
    //  maxDepth
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("assembleRoot")
    class AssembleRoot {

        @Test
        @DisplayName("null roots returns null")
        void nullRoots_returnsNull() {
            assertThat(assembler.assembleRoot(null)).isNull();
        }

        @Test
        @DisplayName("empty roots returns null")
        void emptyRoots_returnsNull() {
            assertThat(assembler.assembleRoot(List.of())).isNull();
        }

        @Test
        @DisplayName("COND root converts to CondNode with correct fields")
        void condRoot_returnsCondNode() {
            RuleNode cond = condNode("c1", "order.total.gte", Map.of("amount", 100));
            Object result = assembler.assembleRoot(List.of(cond));
            assertThat(result).isInstanceOf(CondNode.class);
            CondNode condResult = (CondNode) result;
            assertThat(condResult.getId()).isEqualTo("c1");
            assertThat(condResult.getOperatorName()).isEqualTo("order.total.gte");
        }

        @Test
        @DisplayName("GROUP root converts to GroupNode with typed CondNode child")
        void groupRoot_returnsGroupNodeWithChildren() {
            RuleNode child = condNode("c1", "order.total.gte", null);
            RuleNode group = groupNode("g1", Rule.LogicType.ALL, List.of(child));

            Object result = assembler.assembleRoot(List.of(group));

            assertThat(result).isInstanceOf(GroupNode.class);
            GroupNode groupResult = (GroupNode) result;
            assertThat(groupResult.getId()).isEqualTo("g1");
            assertThat(groupResult.getGroupLogic()).isEqualTo(GroupNode.GroupLogic.AND);
            assertThat(groupResult.getChildren()).hasSize(1);
            assertThat(groupResult.getChildren().get(0)).isInstanceOf(CondNode.class);
        }

        @Test
        @DisplayName("ALL logic maps to AND")
        void allLogic_mapsToAND() {
            RuleNode child = condNode("c1", "op", null);
            RuleNode group = groupNode("g1", Rule.LogicType.ALL, List.of(child));
            GroupNode result = (GroupNode) assembler.assembleRoot(List.of(group));
            assertThat(result.getGroupLogic()).isEqualTo(GroupNode.GroupLogic.AND);
        }

        @Test
        @DisplayName("ANY logic maps to OR")
        void anyLogic_mapsToOR() {
            RuleNode child = condNode("c1", "op", null);
            RuleNode group = groupNode("g1", Rule.LogicType.ANY, List.of(child));
            GroupNode result = (GroupNode) assembler.assembleRoot(List.of(group));
            assertThat(result.getGroupLogic()).isEqualTo(GroupNode.GroupLogic.OR);
        }

        @Test
        @DisplayName("NONE logic maps to NONE")
        void noneLogic_mapsToNONE() {
            RuleNode child = condNode("c1", "op", null);
            RuleNode group = groupNode("g1", Rule.LogicType.NONE, List.of(child));
            GroupNode result = (GroupNode) assembler.assembleRoot(List.of(group));
            assertThat(result.getGroupLogic()).isEqualTo(GroupNode.GroupLogic.NONE);
        }

        @Test
        @DisplayName("multiple roots — only first root is returned")
        void multipleRoots_returnsFirstOnly() {
            RuleNode c1 = condNode("c1", "op.one", null);
            RuleNode c2 = condNode("c2", "op.two", null);
            Object result = assembler.assembleRoot(List.of(c1, c2));
            assertThat(result).isInstanceOf(CondNode.class);
            assertThat(((CondNode) result).getId()).isEqualTo("c1");
        }

        @Test
        @DisplayName("nested GROUP → GROUP → COND assembles full tree")
        void deepTree_assemblsFullNesting() {
            RuleNode leaf = condNode("c1", "op", null);
            RuleNode innerGroup = groupNode("g2", Rule.LogicType.ANY, List.of(leaf));
            RuleNode rootGroup = groupNode("g1", Rule.LogicType.ALL, List.of(innerGroup));

            GroupNode root = (GroupNode) assembler.assembleRoot(List.of(rootGroup));

            assertThat(root.getId()).isEqualTo("g1");
            assertThat(root.getChildren()).hasSize(1);
            Object child = root.getChildren().get(0);
            assertThat(child).isInstanceOf(GroupNode.class);
            GroupNode inner = (GroupNode) child;
            assertThat(inner.getId()).isEqualTo("g2");
            assertThat(inner.getGroupLogic()).isEqualTo(GroupNode.GroupLogic.OR);
            assertThat(inner.getChildren().get(0)).isInstanceOf(CondNode.class);
        }
    }

    // -----------------------------------------------------------------------
    //  maxDepthForRoots
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("assembleAll")
    class AssembleAll {

        @Test
        @DisplayName("null returns empty list")
        void null_returnsEmptyList() {
            assertThat(assembler.assembleAll(null)).isEmpty();
        }

        @Test
        @DisplayName("empty list returns empty list")
        void empty_returnsEmptyList() {
            assertThat(assembler.assembleAll(List.of())).isEmpty();
        }

        @Test
        @DisplayName("two COND roots return two CondNodes")
        void twoConds_returnsTwoCondNodes() {
            RuleNode c1 = condNode("c1", "op.one", null);
            RuleNode c2 = condNode("c2", "op.two", null);
            List<Object> result = assembler.assembleAll(List.of(c1, c2));
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isInstanceOf(CondNode.class);
            assertThat(result.get(1)).isInstanceOf(CondNode.class);
            assertThat(((CondNode) result.get(0)).getId()).isEqualTo("c1");
            assertThat(((CondNode) result.get(1)).getId()).isEqualTo("c2");
        }
    }

    // -----------------------------------------------------------------------
    //  helpers
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("maxDepth")
    class MaxDepth {

        @Test
        @DisplayName("null node returns 0")
        void nullNode_returns0() {
            assertThat(assembler.maxDepth(null)).isEqualTo(0);
        }

        @Test
        @DisplayName("single COND node has depth 1")
        void singleCond_returns1() {
            RuleNode cond = condNode("c1", "op", null);
            assertThat(assembler.maxDepth(cond)).isEqualTo(1);
        }

        @Test
        @DisplayName("GROUP → COND has depth 2")
        void groupWithOneCond_returns2() {
            RuleNode child = condNode("c1", "op", null);
            RuleNode group = groupNode("g1", Rule.LogicType.ALL, List.of(child));
            assertThat(assembler.maxDepth(group)).isEqualTo(2);
        }

        @Test
        @DisplayName("depth-3 chain returns 3")
        void depth3Chain_returns3() {
            RuleNode leaf = condNode("c1", "op", null);
            RuleNode mid = groupNode("g2", Rule.LogicType.ALL, List.of(leaf));
            RuleNode root = groupNode("g1", Rule.LogicType.ALL, List.of(mid));
            assertThat(assembler.maxDepth(root)).isEqualTo(3);
        }

        @Test
        @DisplayName("sibling branches — picks the deepest branch")
        void siblingBranches_picksDeepest() {
            RuleNode shallow = condNode("c1", "op1", null);
            RuleNode deepLeaf = condNode("c2", "op2", null);
            RuleNode deepGroup = groupNode("g2", Rule.LogicType.ALL, List.of(deepLeaf));
            RuleNode root = groupNode("g1", Rule.LogicType.ALL, List.of(shallow, deepGroup));
            // root → shallow (depth 2) vs root → g2 → c2 (depth 3)
            assertThat(assembler.maxDepth(root)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("maxDepthForRoots")
    class MaxDepthForRoots {

        @Test
        @DisplayName("null roots returns 0")
        void null_returns0() {
            assertThat(assembler.maxDepthForRoots(null)).isEqualTo(0);
        }

        @Test
        @DisplayName("empty roots returns 0")
        void empty_returns0() {
            assertThat(assembler.maxDepthForRoots(List.of())).isEqualTo(0);
        }

        @Test
        @DisplayName("picks maximum across multiple roots")
        void picksMaximum() {
            RuleNode shallow = condNode("c1", "op1", null);  // depth 1
            RuleNode deep = groupNode("g1", Rule.LogicType.ALL,
                    List.of(condNode("c2", "op2", null)));    // depth 2
            assertThat(assembler.maxDepthForRoots(List.of(shallow, deep))).isEqualTo(2);
        }
    }
}
