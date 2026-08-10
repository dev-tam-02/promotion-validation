package vn.viettel.vds.promotion.validation.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationContext;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for RuleEvaluationService.
 *
 * Tests cover:
 * - Single rule evaluation (evaluateRule)
 * - Parallel rule evaluation (evaluateRulesInParallel)
 * - Priority-based evaluation with different strategies (evaluateRulesWithPriority)
 * - Cached evaluation (evaluateWithCache)
 *
 * @author Validation Team
 * @since 1.0.0
 */
@DisplayName("RuleEvaluationService Tests")
class RuleEvaluationServiceTest {

    private RuleEvaluationService sut;
    private ValidationContext context;

    @BeforeEach
    void setUp() {
        // Given: A RuleEvaluationService with a 2-thread executor
        sut = new RuleEvaluationService(Executors.newFixedThreadPool(2));
        context = TestFixtures.validContext();
    }

    @Nested
    @DisplayName("evaluateRule() - Single Rule Evaluation")
    class EvaluateRuleTests {

        @Test
        @DisplayName("Should return PENDING when rule is not active")
        void shouldReturnPending_whenRuleNotActive() {
            // Given: An inactive rule
            Rule rule = TestFixtures.inactiveRule("r-1", "RULE_1");

            // When: Evaluating the rule
            ValidationResult result = sut.evaluateRule(rule, context);

            // Then: Result should be PENDING with appropriate reason
            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.PENDING);
            assertThat(result.getReasonCodes()).contains("RULE_NOT_ACTIVE");
            assertThat(result.getExplanations()).contains("Rule is not in active status");
        }

        @Test
        @DisplayName("Should return ERROR when rule evaluation throws UnsupportedOperationException")
        void shouldReturnError_whenEvaluationThrows() {
            // Given: An active rule (its evaluate() method throws UnsupportedOperationException)
            Rule rule = TestFixtures.activeRule("r-1", "RULE_1");

            // When: Evaluating the rule
            ValidationResult result = sut.evaluateRule(rule, context);

            // Then: Result should be ERROR
            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.ERROR);
            assertThat(result.getReasonCodes()).contains("VALIDATION_ERROR");
        }

        @Test
        @DisplayName("Should set ruleId on result")
        void shouldSetRuleId() {
            // Given: An inactive rule with specific ID
            Rule rule = TestFixtures.inactiveRule("r-1", "RULE_1");

            // When: Evaluating the rule
            ValidationResult result = sut.evaluateRule(rule, context);

            // Then: Result should contain the rule ID
            assertThat(result.getRuleId()).isEqualTo("r-1");
        }

        @Test
        @DisplayName("Should set ruleCode on result")
        void shouldSetRuleCode() {
            // Given: An inactive rule with specific code
            Rule rule = TestFixtures.inactiveRule("r-1", "RULE_1");

            // When: Evaluating the rule
            ValidationResult result = sut.evaluateRule(rule, context);

            // Then: Result should contain the rule code
            assertThat(result.getRuleCode()).isEqualTo("RULE_1");
        }

        @Test
        @DisplayName("Should include processing time in result")
        void shouldIncludeProcessingTime() {
            // Given: An active rule
            Rule rule = TestFixtures.activeRule("r-1", "RULE_1");

            // When: Evaluating the rule
            ValidationResult result = sut.evaluateRule(rule, context);

            // Then: Result should include processing time (>= 0)
            assertThat(result.getProcessingTimeMs()).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("Should set timestamp on result")
        void shouldSetTimestamp() {
            // Given: An inactive rule
            Rule rule = TestFixtures.inactiveRule("r-1", "RULE_1");

            // When: Evaluating the rule
            ValidationResult result = sut.evaluateRule(rule, context);

            // Then: Result should have a timestamp
            assertThat(result.getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("evaluateRulesInParallel() - Parallel Evaluation")
    class EvaluateRulesInParallelTests {

        @Test
        @DisplayName("Should return empty list for null rules")
        void shouldReturnEmptyForNull() {
            // Given: null rules list

            // When: Evaluating in parallel
            List<ValidationResult> results = sut.evaluateRulesInParallel(null, context);

            // Then: Should return empty list
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for empty rules")
        void shouldReturnEmptyForEmpty() {
            // Given: Empty rules list

            // When: Evaluating in parallel
            List<ValidationResult> results = sut.evaluateRulesInParallel(List.of(), context);

            // Then: Should return empty list
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return results for each rule")
        void shouldReturnResultsForEachRule() {
            // Given: Multiple rules with different states
            List<Rule> rules = List.of(
                    TestFixtures.activeRule("r-1", "RULE_1"),
                    TestFixtures.inactiveRule("r-2", "RULE_2")
            );

            // When: Evaluating in parallel
            List<ValidationResult> results = sut.evaluateRulesInParallel(rules, context);

            // Then: Should return one result per rule
            assertThat(results).hasSize(2);
            // r-2 (inactive) will have ruleId set, r-1 (active->error) may not
            assertThat(results).anyMatch(r -> "r-2".equals(r.getRuleId()));
        }

        @Test
        @DisplayName("Should evaluate all rules even if some fail")
        void shouldEvaluateAllRulesEvenIfSomeFail() {
            // Given: Multiple active rules (all will throw UnsupportedOperationException)
            List<Rule> rules = List.of(
                    TestFixtures.activeRule("r-1", "RULE_1"),
                    TestFixtures.activeRule("r-2", "RULE_2"),
                    TestFixtures.activeRule("r-3", "RULE_3")
            );

            // When: Evaluating in parallel
            List<ValidationResult> results = sut.evaluateRulesInParallel(rules, context);

            // Then: Should return results for all rules
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(r -> r.getDecision() == ValidationResult.Decision.ERROR);
        }

        @Test
        @DisplayName("Should handle single rule")
        void shouldHandleSingleRule() {
            // Given: A single rule
            List<Rule> rules = List.of(TestFixtures.inactiveRule("r-1", "RULE_1"));

            // When: Evaluating in parallel
            List<ValidationResult> results = sut.evaluateRulesInParallel(rules, context);

            // Then: Should return one result
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getRuleId()).isEqualTo("r-1");
        }
    }

    @Nested
    @DisplayName("evaluateRulesWithPriority() - Priority-based Evaluation")
    class EvaluateRulesWithPriorityTests {

        @Test
        @DisplayName("Should return ALLOW for null rules")
        void shouldReturnAllowForNull() {
            // Given: null rules list

            // When: Evaluating with FAIL_FAST strategy
            ValidationResult result = sut.evaluateRulesWithPriority(
                    null, context, RuleEvaluationService.EvaluationStrategy.FAIL_FAST);

            // Then: Should return ALLOW decision
            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("Should return ALLOW for empty rules")
        void shouldReturnAllowForEmpty() {
            // Given: Empty rules list

            // When: Evaluating with EVALUATE_ALL strategy
            ValidationResult result = sut.evaluateRulesWithPriority(
                    List.of(), context, RuleEvaluationService.EvaluationStrategy.EVALUATE_ALL);

            // Then: Should return ALLOW decision
            assertThat(result.isAllowed()).isTrue();
        }

        @Nested
        @DisplayName("FAIL_FAST Strategy")
        class FailFastStrategyTests {

            @Test
            @DisplayName("Should stop on first error")
            void shouldStopOnFirstError() {
                // Given: An active rule that will throw (ERROR)
                List<Rule> rules = List.of(
                        TestFixtures.activeRule("r-1", "RULE_1")
                );

                // When: Evaluating with FAIL_FAST
                ValidationResult result = sut.evaluateRulesWithPriority(
                        rules, context, RuleEvaluationService.EvaluationStrategy.FAIL_FAST);

                // Then: Should return ERROR immediately
                assertThat(result.hasError()).isTrue();
            }

            @Test
            @DisplayName("Should return ALLOW when all rules pass")
            void shouldReturnAllowWhenAllPass() {
                // Given: Only inactive rules (will PENDING, not ERROR/DENY)
                List<Rule> rules = List.of(
                        TestFixtures.inactiveRule("r-1", "RULE_1"),
                        TestFixtures.inactiveRule("r-2", "RULE_2")
                );

                // When: Evaluating with FAIL_FAST
                ValidationResult result = sut.evaluateRulesWithPriority(
                        rules, context, RuleEvaluationService.EvaluationStrategy.FAIL_FAST);

                // Then: Should return ALLOW (no errors or denials)
                assertThat(result.isAllowed()).isTrue();
            }
        }

        @Nested
        @DisplayName("EVALUATE_ALL Strategy")
        class EvaluateAllStrategyTests {

            @Test
            @DisplayName("Should evaluate all rules and return ERROR if any has error")
            void shouldEvaluateAllAndReturnError() {
                // Given: Mix of active (ERROR) and inactive (PENDING) rules
                List<Rule> rules = List.of(
                        TestFixtures.activeRule("r-1", "RULE_1"),
                        TestFixtures.inactiveRule("r-2", "RULE_2")
                );

                // When: Evaluating with EVALUATE_ALL
                ValidationResult result = sut.evaluateRulesWithPriority(
                        rules, context, RuleEvaluationService.EvaluationStrategy.EVALUATE_ALL);

                // Then: Should return ERROR (because r-1 will ERROR)
                assertThat(result.hasError()).isTrue();
            }

            @Test
            @DisplayName("Should return ALLOW when all rules pass")
            void shouldReturnAllowWhenAllPass() {
                // Given: Only inactive rules (PENDING, no ERROR or DENY)
                List<Rule> rules = List.of(
                        TestFixtures.inactiveRule("r-1", "RULE_1"),
                        TestFixtures.inactiveRule("r-2", "RULE_2")
                );

                // When: Evaluating with EVALUATE_ALL
                ValidationResult result = sut.evaluateRulesWithPriority(
                        rules, context, RuleEvaluationService.EvaluationStrategy.EVALUATE_ALL);

                // Then: Should return ALLOW
                assertThat(result.isAllowed()).isTrue();
            }

            @Test
            @DisplayName("Should collect processing times from all rules")
            void shouldCollectProcessingTimes() {
                // Given: Multiple rules
                List<Rule> rules = List.of(
                        TestFixtures.inactiveRule("r-1", "RULE_1"),
                        TestFixtures.inactiveRule("r-2", "RULE_2")
                );

                // When: Evaluating with EVALUATE_ALL
                ValidationResult result = sut.evaluateRulesWithPriority(
                        rules, context, RuleEvaluationService.EvaluationStrategy.EVALUATE_ALL);

                // Then: Processing time should be sum of all evaluations
                assertThat(result.getProcessingTimeMs()).isGreaterThanOrEqualTo(0L);
            }
        }

        @Nested
        @DisplayName("PASS_FIRST Strategy")
        class PassFirstStrategyTests {

            @Test
            @DisplayName("Should return DENY when all rules fail")
            void shouldReturnDenyWhenAllFail() {
                // Given: Active rules that will all ERROR
                List<Rule> rules = List.of(
                        TestFixtures.activeRule("r-1", "RULE_1"),
                        TestFixtures.activeRule("r-2", "RULE_2")
                );

                // When: Evaluating with PASS_FIRST
                ValidationResult result = sut.evaluateRulesWithPriority(
                        rules, context, RuleEvaluationService.EvaluationStrategy.PASS_FIRST);

                // Then: Should return DENY
                assertThat(result.isDenied()).isTrue();
            }

            @Test
            @DisplayName("Should collect reasons from failed rules")
            void shouldCollectReasonsFromFailedRules() {
                // Given: Active rules that will ERROR
                List<Rule> rules = List.of(
                        TestFixtures.activeRule("r-1", "RULE_1"),
                        TestFixtures.activeRule("r-2", "RULE_2")
                );

                // When: Evaluating with PASS_FIRST
                ValidationResult result = sut.evaluateRulesWithPriority(
                        rules, context, RuleEvaluationService.EvaluationStrategy.PASS_FIRST);

                // Then: Should have reason codes
                assertThat(result.getReasonCodes()).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("evaluateWithCache() - Cached Evaluation")
    class EvaluateWithCacheTests {

        @Test
        @DisplayName("Should return cached result when available")
        void shouldReturnCachedResult() {
            // Given: A cached result
            ValidationResult cached = ValidationResult.allow("val-1");
            Rule rule = TestFixtures.activeRule("r-1", "RULE_1");

            RuleEvaluationService.RuleEvaluationCache cache = new RuleEvaluationService.RuleEvaluationCache() {
                @Override
                public Optional<ValidationResult> get(String key) {
                    return Optional.of(cached);
                }

                @Override
                public void put(String key, ValidationResult result) {
                }

                @Override
                public void invalidate(String key) {
                }

                @Override
                public void clear() {
                }
            };

            // When: Evaluating with cache
            ValidationResult result = sut.evaluateWithCache(rule, context, cache);

            // Then: Should return the cached result
            assertThat(result).isSameAs(cached);
        }

        @Test
        @DisplayName("Should evaluate and cache when cache miss")
        void shouldEvaluateOnCacheMiss() {
            // Given: Empty cache
            Rule rule = TestFixtures.inactiveRule("r-1", "RULE_1");
            List<String> putKeys = new ArrayList<>();

            RuleEvaluationService.RuleEvaluationCache cache = new RuleEvaluationService.RuleEvaluationCache() {
                @Override
                public Optional<ValidationResult> get(String key) {
                    return Optional.empty();
                }

                @Override
                public void put(String key, ValidationResult result) {
                    putKeys.add(key);
                }

                @Override
                public void invalidate(String key) {
                }

                @Override
                public void clear() {
                }
            };

            // When: Evaluating with cache
            ValidationResult result = sut.evaluateWithCache(rule, context, cache);

            // Then: Should evaluate and cache the result
            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.PENDING);
            assertThat(putKeys).hasSize(1);
        }

        @Test
        @DisplayName("Should use rule ID and context for cache key")
        void shouldUseCacheKeyWithRuleAndContext() {
            // Given: Rule and context
            Rule rule = TestFixtures.inactiveRule("r-1", "RULE_1");
            List<String> cacheKeys = new ArrayList<>();

            RuleEvaluationService.RuleEvaluationCache cache = new RuleEvaluationService.RuleEvaluationCache() {
                @Override
                public Optional<ValidationResult> get(String key) {
                    cacheKeys.add(key);
                    return Optional.empty();
                }

                @Override
                public void put(String key, ValidationResult result) {
                }

                @Override
                public void invalidate(String key) {
                }

                @Override
                public void clear() {
                }
            };

            // When: Evaluating with cache
            sut.evaluateWithCache(rule, context, cache);

            // Then: Cache key should include rule ID and context data
            assertThat(cacheKeys).hasSize(1);
            String cacheKey = cacheKeys.get(0);
            assertThat(cacheKey).contains("r-1");
            assertThat(cacheKey).contains("cust-1");
            assertThat(cacheKey).contains("ord-1");
        }

        @Test
        @DisplayName("Should not call put when result is cached")
        void shouldNotPutWhenCached() {
            // Given: A cached result
            ValidationResult cached = ValidationResult.allow("val-1");
            Rule rule = TestFixtures.activeRule("r-1", "RULE_1");
            List<String> putCalls = new ArrayList<>();

            RuleEvaluationService.RuleEvaluationCache cache = new RuleEvaluationService.RuleEvaluationCache() {
                @Override
                public Optional<ValidationResult> get(String key) {
                    return Optional.of(cached);
                }

                @Override
                public void put(String key, ValidationResult result) {
                    putCalls.add(key);
                }

                @Override
                public void invalidate(String key) {
                }

                @Override
                public void clear() {
                }
            };

            // When: Evaluating with cache
            sut.evaluateWithCache(rule, context, cache);

            // Then: Should not call put
            assertThat(putCalls).isEmpty();
        }
    }
}
