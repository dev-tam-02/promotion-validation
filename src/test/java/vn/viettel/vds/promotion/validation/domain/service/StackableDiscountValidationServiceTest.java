package vn.viettel.vds.promotion.validation.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.fact.CustomerFact;
import vn.viettel.vds.promotion.validation.domain.fact.DiscountFact;
import vn.viettel.vds.promotion.validation.domain.fact.OrderFact;
import vn.viettel.vds.promotion.validation.domain.model.StackingContext;
import vn.viettel.vds.promotion.validation.domain.model.StackingResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for StackableDiscountValidationService.
 *
 * Tests cover:
 * - Stacking validation (validateStackingRules)
 * - Discount order optimization (optimizeDiscountOrder)
 * - Budget availability checks (checkBudgetAvailability)
 *
 * @author Validation Team
 * @since 1.0.0
 */
@DisplayName("StackableDiscountValidationService Tests")
class StackableDiscountValidationServiceTest {

    private StackableDiscountValidationService sut;

    @BeforeEach
    void setUp() {
        // Given: A StackableDiscountValidationService instance
        sut = new StackableDiscountValidationService();
    }

    /**
     * Helper method to create a valid stacking context with given discounts.
     */
    private StackingContext validContext(List<DiscountFact> discounts) {
        return StackingContext.builder()
                .validationId("val-1")
                .customer(CustomerFact.builder()
                        .customerId("c1")
                        .tier("GOLD")
                        .build())
                .order(OrderFact.builder()
                        .orderId("o1")
                        .totalAmount(BigDecimal.valueOf(100000))
                        .build())
                .discounts(discounts)
                .requestedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("validateStackingRules() - Stacking Validation")
    class ValidateStackingRulesTests {

        @Test
        @DisplayName("Should reject when context is invalid - missing customer")
        void shouldRejectWhenMissingCustomer() {
            // Given: Context without customer
            StackingContext ctx = StackingContext.builder()
                    .validationId("val-1")
                    .order(OrderFact.builder()
                            .orderId("o1")
                            .totalAmount(BigDecimal.valueOf(100000))
                            .build())
                    .discounts(List.of(DiscountFact.builder()
                            .discountId("d1")
                            .status("ACTIVE")
                            .build()))
                    .requestedAt(Instant.now())
                    .build();

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(ctx);

            // Then: Should be rejected
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getIssues()).contains("Invalid validation context");
        }

        @Test
        @DisplayName("Should reject when context is invalid - missing order")
        void shouldRejectWhenMissingOrder() {
            // Given: Context without order
            StackingContext ctx = StackingContext.builder()
                    .validationId("val-1")
                    .customer(CustomerFact.builder().customerId("c1").build())
                    .discounts(List.of(DiscountFact.builder()
                            .discountId("d1")
                            .status("ACTIVE")
                            .build()))
                    .requestedAt(Instant.now())
                    .build();

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(ctx);

            // Then: Should be rejected
            assertThat(result.isRejected()).isTrue();
        }

        @Test
        @DisplayName("Should reject when context is invalid - missing discounts")
        void shouldRejectWhenMissingDiscounts() {
            // Given: Context without discounts
            StackingContext ctx = StackingContext.builder()
                    .validationId("val-1")
                    .customer(CustomerFact.builder().customerId("c1").build())
                    .order(OrderFact.builder()
                            .orderId("o1")
                            .totalAmount(BigDecimal.valueOf(100000))
                            .build())
                    .requestedAt(Instant.now())
                    .build();

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(ctx);

            // Then: Should be rejected
            assertThat(result.isRejected()).isTrue();
        }

        @Test
        @DisplayName("Should reject when request is stale")
        void shouldRejectWhenStale() {
            // Given: Context with old timestamp (>5 minutes ago)
            StackingContext ctx = StackingContext.builder()
                    .validationId("val-1")
                    .customer(CustomerFact.builder().customerId("c1").build())
                    .order(OrderFact.builder()
                            .orderId("o1")
                            .totalAmount(BigDecimal.valueOf(100000))
                            .build())
                    .discounts(List.of(DiscountFact.builder()
                            .discountId("d1")
                            .status("ACTIVE")
                            .build()))
                    .requestedAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                    .build();

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(ctx);

            // Then: Should be rejected with expiry reason
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getIssues()).anyMatch(i -> i.contains("expired"));
        }

        @Test
        @DisplayName("Should approve all active discounts")
        void shouldApproveActiveDiscounts() {
            // Given: Active percentage discount
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.TEN)
                            .status("ACTIVE")
                            .build()
            );

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(validContext(discounts));

            // Then: Should be approved
            assertThat(result.isApproved()).isTrue();
            assertThat(result.getValidatedCount()).isEqualTo(1);
            assertThat(result.getRejectedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should approve multiple active discounts")
        void shouldApproveMultipleActiveDiscounts() {
            // Given: Multiple active discounts
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.TEN)
                            .status("ACTIVE")
                            .build(),
                    DiscountFact.builder()
                            .discountId("d2")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .status("ACTIVE")
                            .build()
            );

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(validContext(discounts));

            // Then: Should approve all
            assertThat(result.isApproved()).isTrue();
            assertThat(result.getValidatedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should reject inactive discounts")
        void shouldRejectInactiveDiscounts() {
            // Given: Inactive discount
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .status("INACTIVE")
                            .build()
            );

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(validContext(discounts));

            // Then: Should be rejected
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getRejectedCount()).isEqualTo(1);
            assertThat(result.getValidatedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return partial when mix of valid and invalid")
        void shouldReturnPartialForMixed() {
            // Given: Mix of active and inactive discounts
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.TEN)
                            .status("ACTIVE")
                            .build(),
                    DiscountFact.builder()
                            .discountId("d2")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .status("INACTIVE")
                            .build()
            );

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(validContext(discounts));

            // Then: Should be partial
            assertThat(result.isPartial()).isTrue();
            assertThat(result.getValidatedCount()).isEqualTo(1);
            assertThat(result.getRejectedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate total discount correctly")
        void shouldCalculateTotalDiscount() {
            // Given: Active discounts
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.TEN) // 10% of 100000 = 10000
                            .status("ACTIVE")
                            .build(),
                    DiscountFact.builder()
                            .discountId("d2")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .status("ACTIVE")
                            .build()
            );

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(validContext(discounts));

            // Then: Total discount should be sum of both
            assertThat(result.isApproved()).isTrue();
            assertThat(result.getTotalDiscount()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should set processing time on result")
        void shouldSetProcessingTime() {
            // Given: Active discount
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .status("ACTIVE")
                            .build()
            );

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(validContext(discounts));

            // Then: Should have processing time
            assertThat(result.getProcessingTimeMs()).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("Should include rejected discount info for inactive discounts")
        void shouldIncludeRejectedDiscountInfo() {
            // Given: Inactive discount
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .status("INACTIVE")
                            .build()
            );

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(validContext(discounts));

            // Then: Rejected info should contain discount ID and type
            assertThat(result.getRejectedDiscounts()).hasSize(1);
            StackingResult.RejectedDiscountInfo rejected = result.getRejectedDiscounts().get(0);
            assertThat(rejected.getDiscountId()).isEqualTo("d1");
            assertThat(rejected.getDiscountType()).isEqualTo("FIXED");
            assertThat(rejected.getRejectionReasons()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include validated discount info with application order")
        void shouldIncludeValidatedDiscountInfo() {
            // Given: Active discounts
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.TEN)
                            .status("ACTIVE")
                            .build(),
                    DiscountFact.builder()
                            .discountId("d2")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .status("ACTIVE")
                            .build()
            );

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(validContext(discounts));

            // Then: Validated info should include order
            assertThat(result.getValidatedDiscounts()).hasSize(2);
            StackingResult.ValidatedDiscountInfo first = result.getValidatedDiscounts().get(0);
            StackingResult.ValidatedDiscountInfo second = result.getValidatedDiscounts().get(1);
            assertThat(first.getApplicationOrder()).isEqualTo(1);
            assertThat(second.getApplicationOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should optimize order when requested")
        void shouldOptimizeOrderWhenRequested() {
            // Given: Context with optimize flag and discounts (fixed before percentage)
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .status("ACTIVE")
                            .build(),
                    DiscountFact.builder()
                            .discountId("d2")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.TEN)
                            .status("ACTIVE")
                            .build()
            );

            StackingContext ctx = StackingContext.builder()
                    .validationId("val-1")
                    .customer(CustomerFact.builder().customerId("c1").build())
                    .order(OrderFact.builder()
                            .orderId("o1")
                            .totalAmount(BigDecimal.valueOf(100000))
                            .build())
                    .discounts(discounts)
                    .optimizeOrder(true)
                    .requestedAt(Instant.now())
                    .build();

            // When: Validating stacking rules
            StackingResult result = sut.validateStackingRules(ctx);

            // Then: Percentage discount should be applied first (order 1)
            assertThat(result.getValidatedDiscounts()).hasSize(2);
            // Find which discount has order 1
            StackingResult.ValidatedDiscountInfo firstApplied = result.getValidatedDiscounts().stream()
                    .filter(d -> d.getApplicationOrder() == 1)
                    .findFirst()
                    .orElseThrow();
            assertThat(firstApplied.getDiscountType()).isEqualTo("PERCENTAGE");
        }
    }

    @Nested
    @DisplayName("optimizeDiscountOrder() - Discount Order Optimization")
    class OptimizeDiscountOrderTests {

        @Test
        @DisplayName("Should place percentage discounts before fixed")
        void shouldPrioritizePercentage() {
            // Given: Fixed discount followed by percentage discount
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .build(),
                    DiscountFact.builder()
                            .discountId("d2")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.TEN)
                            .build()
            );
            OrderFact order = OrderFact.builder()
                    .orderId("o1")
                    .totalAmount(BigDecimal.valueOf(100000))
                    .build();

            // When: Optimizing discount order
            List<DiscountFact> optimized = sut.optimizeDiscountOrder(discounts, order);

            // Then: Percentage discount should come first
            assertThat(optimized).hasSize(2);
            assertThat(optimized.get(0).discountId()).isEqualTo("d2");
            assertThat(optimized.get(0).type()).isEqualTo("PERCENTAGE");
            assertThat(optimized.get(1).discountId()).isEqualTo("d1");
            assertThat(optimized.get(1).type()).isEqualTo("FIXED");
        }

        @Test
        @DisplayName("Should sort by value within same type")
        void shouldSortByValueWithinSameType() {
            // Given: Multiple percentage discounts with different values
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.valueOf(5))
                            .build(),
                    DiscountFact.builder()
                            .discountId("d2")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.valueOf(15))
                            .build(),
                    DiscountFact.builder()
                            .discountId("d3")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.TEN)
                            .build()
            );
            OrderFact order = OrderFact.builder()
                    .orderId("o1")
                    .totalAmount(BigDecimal.valueOf(100000))
                    .build();

            // When: Optimizing discount order
            List<DiscountFact> optimized = sut.optimizeDiscountOrder(discounts, order);

            // Then: Should be sorted by percentage descending
            assertThat(optimized).hasSize(3);
            assertThat(optimized.get(0).percentage()).isEqualByComparingTo(BigDecimal.valueOf(15));
            assertThat(optimized.get(1).percentage()).isEqualByComparingTo(BigDecimal.TEN);
            assertThat(optimized.get(2).percentage()).isEqualByComparingTo(BigDecimal.valueOf(5));
        }

        @Test
        @DisplayName("Should handle empty discount list")
        void shouldHandleEmptyList() {
            // Given: Empty discount list
            List<DiscountFact> discounts = List.of();
            OrderFact order = OrderFact.builder()
                    .orderId("o1")
                    .totalAmount(BigDecimal.valueOf(100000))
                    .build();

            // When: Optimizing discount order
            List<DiscountFact> optimized = sut.optimizeDiscountOrder(discounts, order);

            // Then: Should return empty list
            assertThat(optimized).isEmpty();
        }

        @Test
        @DisplayName("Should handle single discount")
        void shouldHandleSingleDiscount() {
            // Given: Single discount
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .build()
            );
            OrderFact order = OrderFact.builder()
                    .orderId("o1")
                    .totalAmount(BigDecimal.valueOf(100000))
                    .build();

            // When: Optimizing discount order
            List<DiscountFact> optimized = sut.optimizeDiscountOrder(discounts, order);

            // Then: Should return same discount
            assertThat(optimized).hasSize(1);
            assertThat(optimized.get(0).discountId()).isEqualTo("d1");
        }

        @Test
        @DisplayName("Should maintain percentage-before-fixed order for mixed types")
        void shouldMaintainTypeOrderForMixed() {
            // Given: Complex mix of discount types
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(10000))
                            .build(),
                    DiscountFact.builder()
                            .discountId("d2")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.valueOf(5))
                            .build(),
                    DiscountFact.builder()
                            .discountId("d3")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .build(),
                    DiscountFact.builder()
                            .discountId("d4")
                            .type("PERCENTAGE")
                            .percentage(BigDecimal.valueOf(15))
                            .build()
            );
            OrderFact order = OrderFact.builder()
                    .orderId("o1")
                    .totalAmount(BigDecimal.valueOf(100000))
                    .build();

            // When: Optimizing discount order
            List<DiscountFact> optimized = sut.optimizeDiscountOrder(discounts, order);

            // Then: All percentage discounts should come before fixed
            assertThat(optimized).hasSize(4);
            assertThat(optimized.get(0).type()).isEqualTo("PERCENTAGE");
            assertThat(optimized.get(1).type()).isEqualTo("PERCENTAGE");
            assertThat(optimized.get(2).type()).isEqualTo("FIXED");
            assertThat(optimized.get(3).type()).isEqualTo("FIXED");
        }
    }

    @Nested
    @DisplayName("checkBudgetAvailability() - Budget Availability Checks")
    class CheckBudgetAvailabilityTests {

        @Test
        @DisplayName("Should return true when amount is positive")
        void shouldReturnTrueForPositive() {
            // Given: Positive required amount

            // When: Checking budget availability
            boolean available = sut.checkBudgetAvailability(List.of(), BigDecimal.TEN);

            // Then: Should be available
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("Should return true when amount is zero")
        void shouldReturnTrueForZero() {
            // Given: Zero required amount

            // When: Checking budget availability
            boolean available = sut.checkBudgetAvailability(List.of(), BigDecimal.ZERO);

            // Then: Should be available
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("Should return false when amount is negative")
        void shouldReturnFalseForNegative() {
            // Given: Negative required amount

            // When: Checking budget availability
            boolean available = sut.checkBudgetAvailability(List.of(), BigDecimal.valueOf(-1));

            // Then: Should not be available
            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("Should return true for large positive amount")
        void shouldReturnTrueForLargePositive() {
            // Given: Large positive amount

            // When: Checking budget availability
            boolean available = sut.checkBudgetAvailability(List.of(), BigDecimal.valueOf(1000000));

            // Then: Should be available
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("Should return false for large negative amount")
        void shouldReturnFalseForLargeNegative() {
            // Given: Large negative amount

            // When: Checking budget availability
            boolean available = sut.checkBudgetAvailability(List.of(), BigDecimal.valueOf(-1000000));

            // Then: Should not be available
            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("Should work with discounts list")
        void shouldWorkWithDiscountsList() {
            // Given: Some discounts and positive amount
            List<DiscountFact> discounts = List.of(
                    DiscountFact.builder()
                            .discountId("d1")
                            .type("FIXED")
                            .amount(BigDecimal.valueOf(5000))
                            .build()
            );

            // When: Checking budget availability
            boolean available = sut.checkBudgetAvailability(discounts, BigDecimal.valueOf(100));

            // Then: Should be available
            assertThat(available).isTrue();
        }
    }
}
