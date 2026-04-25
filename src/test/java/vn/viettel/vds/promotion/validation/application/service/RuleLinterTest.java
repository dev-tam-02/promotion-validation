package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.model.LintCode;
import vn.viettel.vds.promotion.validation.domain.model.LintReport;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RuleLinter} — covers all four lint checks:
 * REDUNDANCY, TAUTOLOGY, CONTRADICTION, UNREACHABLE.
 *
 * Test structure mirrors the spec in {@code 09-pp-validation-gaps-v1-v10.md §V9}.
 */
@DisplayName("RuleLinter — static analysis checks")
class RuleLinterTest {

    private RuleLinter linter;

    @BeforeEach
    void setUp() {
        linter = new RuleLinter();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Build a GROUP node (ALL/AND logic) with the given COND children. */
    private RuleNode andGroup(String groupId, List<RuleNode> children) {
        return RuleNode.builder()
                .nodeId(groupId)
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL)
                .children(children)
                .build();
    }

    /** Build a GROUP node (ANY/OR logic) with the given COND children. */
    private RuleNode orGroup(String groupId, List<RuleNode> children) {
        return RuleNode.builder()
                .nodeId(groupId)
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ANY)
                .children(children)
                .build();
    }

    /** Build a COND node. */
    private RuleNode cond(String nodeId, String operatorName, Map<String, Object> params) {
        return RuleNode.builder()
                .nodeId(nodeId)
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .reasonCode("RC_" + nodeId)
                .params(params)
                .build();
    }

    private Rule dummyRule() {
        return Rule.builder().id("rule-test").name("test").build();
    }

    // -------------------------------------------------------------------------
    // Clean rule (baseline)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("clean rule — no nodes → empty LintReport")
    void nullNodes_returnsCleanReport() {
        LintReport report = linter.lint(dummyRule(), null);

        assertThat(report.isClean()).isTrue();
        assertThat(report.warnings()).isEmpty();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    @DisplayName("clean rule — distinct CONDs in AND group → empty LintReport")
    void distinctCondsInAndGroup_noIssues() {
        RuleNode root = andGroup("g1", List.of(
                cond("c1", "order.total.gte", Map.of("amount", 500)),
                cond("c2", "customer.is_vip", Map.of())
        ));

        LintReport report = linter.lint(dummyRule(), List.of(root));

        assertThat(report.isClean()).isTrue();
    }

    // -------------------------------------------------------------------------
    // REDUNDANCY (same COND in non-AND group)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("REDUNDANCY check")
    class RedundancyCheck {

        @Test
        @DisplayName("same COND 2× in OR group → REDUNDANCY warning")
        void sameCond_inOrGroup_redundancyWarning() {
            RuleNode dup = cond("c2", "order.total.gte", Map.of("amount", 500));
            RuleNode root = orGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 500)),
                    dup
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.warnings()).hasSize(1);
            assertThat(report.warnings().get(0).code()).isEqualTo(LintCode.REDUNDANCY.name());
            assertThat(report.warnings().get(0).nodeId()).isEqualTo("c2");
            assertThat(report.errors()).isEmpty();
        }

        @Test
        @DisplayName("same COND 2× in AND group → TAUTOLOGY warning (more specific than redundancy)")
        void sameCond_inAndGroup_tautologyWarning() {
            RuleNode dup = cond("c2", "order.total.gte", Map.of("amount", 500));
            RuleNode root = andGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 500)),
                    dup
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.warnings())
                    .extracting(i -> i.code())
                    .containsExactly(LintCode.TAUTOLOGY.name());
        }
    }

    // -------------------------------------------------------------------------
    // TAUTOLOGY (X AND X)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("TAUTOLOGY check")
    class TautologyCheck {

        @Test
        @DisplayName("(order.total.gte 500) AND (order.total.gte 500) → TAUTOLOGY warning")
        void identicalGteInAndGroup_tautologyWarning() {
            RuleNode root = andGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 500)),
                    cond("c2", "order.total.gte", Map.of("amount", 500))
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.warnings()).hasSize(1);
            assertThat(report.warnings().get(0).code()).isEqualTo(LintCode.TAUTOLOGY.name());
            assertThat(report.warnings().get(0).nodeId()).isEqualTo("c2");
            assertThat(report.errors()).isEmpty();
        }

        @Test
        @DisplayName("(X) AND (Y) where X ≠ Y → no tautology")
        void differentCondsInAndGroup_noTautology() {
            RuleNode root = andGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 500)),
                    cond("c2", "order.total.gte", Map.of("amount", 300))
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.warnings()
                    .stream().noneMatch(w -> w.code().equals(LintCode.TAUTOLOGY.name())))
                    .isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // CONTRADICTION
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CONTRADICTION check")
    class ContradictionCheck {

        @Test
        @DisplayName("(order.total.gte 500) AND (order.total.lte 100) → CONTRADICTION error")
        void gteAboveLte_isContradiction() {
            RuleNode root = andGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 500)),
                    cond("c2", "order.total.lte", Map.of("amount", 100))
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.errors()).hasSize(1);
            assertThat(report.errors().get(0).code()).isEqualTo(LintCode.CONTRADICTION.name());
            assertThat(report.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("(order.total.gte 100) AND (order.total.lte 500) → valid (no contradiction)")
        void gteBelow_lteAbove_noContradiction() {
            RuleNode root = andGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 100)),
                    cond("c2", "order.total.lte", Map.of("amount", 500))
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.errors().stream()
                    .noneMatch(e -> e.code().equals(LintCode.CONTRADICTION.name())))
                    .isTrue();
        }

        @Test
        @DisplayName("(loyalty.tier.gte 3) AND (loyalty.tier.lte 1) → CONTRADICTION error")
        void loyaltyTierContradiction() {
            RuleNode root = andGroup("g1", List.of(
                    cond("c1", "loyalty.tier.gte", Map.of("value", 3)),
                    cond("c2", "loyalty.tier.lte", Map.of("value", 1))
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.errors())
                    .extracting(i -> i.code())
                    .containsExactly(LintCode.CONTRADICTION.name());
        }

        @Test
        @DisplayName("contradiction in OR group is NOT flagged (OR can satisfy either)")
        void contradictionInOrGroup_notFlagged() {
            RuleNode root = orGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 500)),
                    cond("c2", "order.total.lte", Map.of("amount", 100))
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.errors().stream()
                    .noneMatch(e -> e.code().equals(LintCode.CONTRADICTION.name())))
                    .isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // UNREACHABLE
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("UNREACHABLE check")
    class UnreachableCheck {

        @Test
        @DisplayName("OR GROUP with exactly 1 COND child → UNREACHABLE warning")
        void orGroupSingleChild_unreachableWarning() {
            RuleNode root = orGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 500))
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.warnings()).hasSize(1);
            assertThat(report.warnings().get(0).code()).isEqualTo(LintCode.UNREACHABLE.name());
            assertThat(report.warnings().get(0).nodeId()).isEqualTo("g1");
            assertThat(report.errors()).isEmpty();
        }

        @Test
        @DisplayName("AND GROUP with exactly 1 COND child → UNREACHABLE warning")
        void andGroupSingleChild_unreachableWarning() {
            RuleNode root = andGroup("g1", List.of(
                    cond("c1", "customer.segment.in", Map.of("segments", List.of("VIP")))
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.warnings())
                    .extracting(i -> i.code())
                    .contains(LintCode.UNREACHABLE.name());
        }

        @Test
        @DisplayName("GROUP with 2 COND children → no UNREACHABLE")
        void groupWithTwoChildren_noUnreachable() {
            RuleNode root = andGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 500)),
                    cond("c2", "customer.is_vip", Map.of())
            ));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.warnings().stream()
                    .noneMatch(w -> w.code().equals(LintCode.UNREACHABLE.name())))
                    .isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Combined / nested scenarios
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Combined checks")
    class CombinedChecks {

        @Test
        @DisplayName("nested: outer AND group with inner OR group (single child) → UNREACHABLE warning")
        void nestedGroups_innerSingleChild_unreachable() {
            RuleNode inner = orGroup("inner", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 100))
            ));
            RuleNode outer = andGroup("outer", List.of(
                    inner,
                    cond("c2", "customer.is_vip", Map.of())
            ));

            LintReport report = linter.lint(dummyRule(), List.of(outer));

            assertThat(report.warnings())
                    .extracting(i -> i.code())
                    .containsExactlyInAnyOrder(LintCode.UNREACHABLE.name());
        }

        @Test
        @DisplayName("contradiction blocks save; warnings coexist in same report")
        void contradictionWithWarning_bothPresent() {
            // Single-child group (UNREACHABLE) containing a contradiction
            RuleNode innerAndGroup = andGroup("g1", List.of(
                    cond("c1", "order.total.gte", Map.of("amount", 500)),
                    cond("c2", "order.total.lte", Map.of("amount", 100))
            ));
            // outer OR with single child → UNREACHABLE
            RuleNode root = orGroup("root", List.of(innerAndGroup));

            LintReport report = linter.lint(dummyRule(), List.of(root));

            assertThat(report.hasErrors()).isTrue();
            assertThat(report.hasWarnings()).isTrue();
            assertThat(report.errors().stream().anyMatch(e -> e.code().equals(LintCode.CONTRADICTION.name()))).isTrue();
            assertThat(report.warnings().stream().anyMatch(w -> w.code().equals(LintCode.UNREACHABLE.name()))).isTrue();
        }
    }
}
