package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.fact.CustomerFact;
import vn.viettel.vds.promotion.validation.domain.fact.DiscountFact;
import vn.viettel.vds.promotion.validation.domain.fact.OrderFact;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackingRule Tests")
class StackingRuleTest {

    private StackingContext testContext() {
        return StackingContext.builder()
                .validationId("val-1")
                .customer(CustomerFact.builder().customerId("c1").tier("GOLD").build())
                .order(OrderFact.builder().orderId("o1").totalAmount(BigDecimal.valueOf(100000)).build())
                .discounts(List.of(DiscountFact.builder().discountId("d1").build()))
                .requestedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("evaluate()")
    class EvaluateTests {
        @Test
        @DisplayName("Should pass when rule is inactive")
        void shouldPassWhenInactive() {
            StackingRule rule = StackingRule.builder().active(false).build();
            assertThat(rule.evaluate(testContext())).isTrue();
        }

        @Test
        @DisplayName("Should pass when evaluator is null")
        void shouldPassWhenNoEvaluator() {
            StackingRule rule = StackingRule.builder().active(true).build();
            assertThat(rule.evaluate(testContext())).isTrue();
        }

        @Test
        @DisplayName("Should use evaluator when present")
        void shouldUseEvaluator() {
            StackingRule rule = StackingRule.builder()
                    .active(true)
                    .evaluator(ctx -> ctx.getDiscountCount() < 5)
                    .build();
            assertThat(rule.evaluate(testContext())).isTrue();
        }

        @Test
        @DisplayName("Should fail when evaluator returns false")
        void shouldFailWhenEvaluatorReturnsFalse() {
            StackingRule rule = StackingRule.builder()
                    .active(true)
                    .evaluator(ctx -> false)
                    .build();
            assertThat(rule.evaluate(testContext())).isFalse();
        }

        @Test
        @DisplayName("Should skip when segment does not match")
        void shouldSkipWhenSegmentNotMatch() {
            StackingRule rule = StackingRule.builder()
                    .active(true)
                    .targetSegments(List.of("PLATINUM"))
                    .evaluator(ctx -> false)
                    .build();
            // Rule doesn't apply to GOLD customer, so it passes
            assertThat(rule.evaluate(testContext())).isTrue();
        }
    }

    @Nested
    @DisplayName("appliesTo()")
    class AppliesToTests {
        @Test
        @DisplayName("Should apply to all when no target segments")
        void shouldApplyToAll() {
            StackingRule rule = StackingRule.builder().build();
            assertThat(rule.appliesTo(testContext())).isTrue();
        }

        @Test
        @DisplayName("Should apply when segment matches")
        void shouldApplyWhenSegmentMatches() {
            StackingRule rule = StackingRule.builder()
                    .targetSegments(List.of("GOLD", "VIP"))
                    .build();
            assertThat(rule.appliesTo(testContext())).isTrue();
        }
    }

    @Nested
    @DisplayName("getFailureMessage()")
    class GetFailureMessageTests {
        @Test
        @DisplayName("Should return custom error message")
        void shouldReturnCustomMessage() {
            StackingRule rule = StackingRule.builder()
                    .errorMessage("Custom error")
                    .build();
            assertThat(rule.getFailureMessage()).isEqualTo("Custom error");
        }

        @Test
        @DisplayName("Should return default message when errorMessage is null")
        void shouldReturnDefaultMessage() {
            StackingRule rule = StackingRule.builder()
                    .ruleCode("SR001")
                    .build();
            assertThat(rule.getFailureMessage()).isEqualTo("Rule SR001 failed");
        }
    }
}
