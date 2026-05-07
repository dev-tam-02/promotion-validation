package vn.viettel.vds.promotion.validation.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;
import vn.viettel.vds.promotion.validation.domain.fact.CustomerFact;
import vn.viettel.vds.promotion.validation.domain.fact.FactPack;
import vn.viettel.vds.promotion.validation.domain.fact.SegmentsFact;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackableRuleEvaluator Tests")
class StackableRuleEvaluatorTest {

    private StackableRuleEvaluator sut;

    @BeforeEach
    void setUp() {
        sut = new StackableRuleEvaluator();
    }

    @Nested
    @DisplayName("evaluateStackingRules()")
    class EvaluateStackingRulesTests {

        @Test
        @DisplayName("Should return error when factPack is null")
        void shouldReturnError_whenFactPackNull() {
            var result = sut.evaluateStackingRules(null, List.of(TestFixtures.activeRule("r-1", "RULE_1")));

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getSummary()).contains("error");
        }

        @Test
        @DisplayName("Should return pass when rules list is null")
        void shouldReturnPass_whenRulesNull() {
            var factPack = FactPack.builder().build();

            var result = sut.evaluateStackingRules(factPack, null);

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getSummary()).contains("No rules configured");
        }

        @Test
        @DisplayName("Should return pass when rules list is empty")
        void shouldReturnPass_whenRulesEmpty() {
            var factPack = FactPack.builder().build();

            var result = sut.evaluateStackingRules(factPack, Collections.emptyList());

            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("Should pass active rules that apply")
        void shouldPassActiveRules() {
            var factPack = FactPack.builder().build();
            var rule = TestFixtures.activeRule("r-1", "RULE_1");

            var result = sut.evaluateStackingRules(factPack, List.of(rule));

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getPassedRules()).contains("RULE_1");
            assertThat(result.getTotalRules()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should mark skipped inactive rules as failed in result")
        void shouldSkipInactiveRules() {
            var factPack = FactPack.builder().build();
            var rule = TestFixtures.inactiveRule("r-1", "RULE_INACTIVE");

            var result = sut.evaluateStackingRules(factPack, List.of(rule));

            // Skipped rules (isPassed=false) are added to failedRules, so result.isPassed() is false
            assertThat(result.isPassed()).isFalse();
            assertThat(result.getFailedRules()).contains("RULE_INACTIVE");
            assertThat(result.getTotalRules()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should mark non-matching segment rules as failed in result")
        void shouldSkipRules_whenSegmentNotMatch() {
            var factPack = FactPack.builder()
                    .customer(CustomerFact.builder()
                            .customerId("cust-1")
                            .tier("BRONZE")
                            .build())
                    .build();

            var rule = TestFixtures.ruleWithSegments("r-1", "RULE_SEG", Set.of("VIP", "GOLD"));

            var result = sut.evaluateStackingRules(factPack, List.of(rule));

            // Skipped rules are treated as failed in the evaluation result
            assertThat(result.isPassed()).isFalse();
            assertThat(result.getFailedRules()).contains("RULE_SEG");
        }

        @Test
        @DisplayName("Should match customer segment from tier")
        void shouldMatchSegmentFromTier() {
            var factPack = FactPack.builder()
                    .customer(CustomerFact.builder()
                            .customerId("cust-1")
                            .tier("VIP")
                            .build())
                    .build();

            var rule = TestFixtures.ruleWithSegments("r-1", "RULE_SEG", Set.of("VIP"));

            var result = sut.evaluateStackingRules(factPack, List.of(rule));

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getPassedRules()).contains("RULE_SEG");
        }

        @Test
        @DisplayName("Should match customer segment from attributes")
        void shouldMatchSegmentFromAttributes() {
            var factPack = FactPack.builder()
                    .customer(CustomerFact.builder()
                            .customerId("cust-1")
                            .attributes(Map.of("segment", "GOLD"))
                            .build())
                    .build();

            var rule = TestFixtures.ruleWithSegments("r-1", "RULE_SEG", Set.of("GOLD"));

            var result = sut.evaluateStackingRules(factPack, List.of(rule));

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getPassedRules()).contains("RULE_SEG");
        }

        @Test
        @DisplayName("Should match customer segment from segments fact")
        void shouldMatchSegmentFromSegmentsFact() {
            var factPack = FactPack.builder()
                    .segments(SegmentsFact.builder()
                            .segmentIds(List.of("VIP"))
                            .build())
                    .build();

            var rule = TestFixtures.ruleWithSegments("r-1", "RULE_SEG", Set.of("VIP"));

            var result = sut.evaluateStackingRules(factPack, List.of(rule));

            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("Should return correct summary for all passed")
        void shouldReturnCorrectSummary_allPassed() {
            var factPack = FactPack.builder().build();
            var rule1 = TestFixtures.activeRule("r-1", "RULE_1");
            var rule2 = TestFixtures.activeRule("r-2", "RULE_2");

            var result = sut.evaluateStackingRules(factPack, List.of(rule1, rule2));

            assertThat(result.getSummary()).contains("2 rule(s) passed");
        }

        @Test
        @DisplayName("Should report total rules count")
        void shouldReportTotalRulesCount() {
            var factPack = FactPack.builder().build();
            var rules = List.of(
                    TestFixtures.activeRule("r-1", "RULE_1"),
                    TestFixtures.activeRule("r-2", "RULE_2"),
                    TestFixtures.activeRule("r-3", "RULE_3")
            );

            var result = sut.evaluateStackingRules(factPack, rules);

            assertThat(result.getTotalRules()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("RuleResult")
    class RuleResultTests {

        @Test
        @DisplayName("Should create pass result")
        void shouldCreatePassResult() {
            var result = StackableRuleEvaluator.RuleResult.pass("RULE_1", "Passed");

            assertThat(result.isPassed()).isTrue();
            assertThat(result.isFailed()).isFalse();
            assertThat(result.isSkipped()).isFalse();
            assertThat(result.getRuleCode()).isEqualTo("RULE_1");
            assertThat(result.getReason()).isEqualTo("Passed");
        }

        @Test
        @DisplayName("Should create fail result")
        void shouldCreateFailResult() {
            var result = StackableRuleEvaluator.RuleResult.fail("RULE_1", "Failed");

            assertThat(result.isFailed()).isTrue();
            assertThat(result.isPassed()).isFalse();
        }

        @Test
        @DisplayName("Should create skipped result")
        void shouldCreateSkippedResult() {
            var result = StackableRuleEvaluator.RuleResult.skipped("RULE_1", "Skipped");

            assertThat(result.isSkipped()).isTrue();
            assertThat(result.getStatus()).isEqualTo(StackableRuleEvaluator.RuleStatus.SKIPPED);
        }

        @Test
        @DisplayName("Should create error result")
        void shouldCreateErrorResult() {
            var result = StackableRuleEvaluator.RuleResult.error("RULE_1", "Error");

            assertThat(result.getStatus()).isEqualTo(StackableRuleEvaluator.RuleStatus.ERROR);
        }
    }

    @Nested
    @DisplayName("EvaluationResult")
    class EvaluationResultTests {

        @Test
        @DisplayName("Should create pass evaluation result")
        void shouldCreatePassResult() {
            var result = StackableRuleEvaluator.EvaluationResult.pass("All good");

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getSummary()).isEqualTo("All good");
            assertThat(result.getRuleResults()).isEmpty();
            assertThat(result.getTotalRules()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should create error evaluation result")
        void shouldCreateErrorResult() {
            var result = StackableRuleEvaluator.EvaluationResult.error("Something went wrong");

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getSummary()).contains("Something went wrong");
        }

        @Test
        @DisplayName("Should return failure reasons")
        void shouldReturnFailureReasons() {
            var failedResult = StackableRuleEvaluator.RuleResult.fail("R1", "Reason 1");
            var passedResult = StackableRuleEvaluator.RuleResult.pass("R2", "OK");

            var result = StackableRuleEvaluator.EvaluationResult.builder()
                    .passed(false)
                    .summary("Mixed")
                    .ruleResults(List.of(failedResult, passedResult))
                    .passedRules(List.of("R2"))
                    .failedRules(List.of("R1"))
                    .totalRules(2)
                    .build();

            assertThat(result.getFailureReasons()).containsExactly("Reason 1");
        }
    }
}
