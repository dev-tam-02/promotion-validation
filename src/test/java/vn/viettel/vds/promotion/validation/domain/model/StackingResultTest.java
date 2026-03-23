package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackingResult Tests")
class StackingResultTest {

    @Nested
    @DisplayName("success()")
    class SuccessTests {

        @Test
        @DisplayName("Should create APPROVED result with correct amounts")
        void shouldCreateApprovedResult() {
            var discount = StackingResult.ValidatedDiscountInfo.builder()
                    .discountId("disc-1")
                    .discountType("PERCENTAGE")
                    .discountAmount(BigDecimal.valueOf(10000))
                    .applicationOrder(1)
                    .build();

            var result = StackingResult.success(
                    "val-1",
                    List.of(discount),
                    BigDecimal.valueOf(10000),
                    BigDecimal.valueOf(100000),
                    50
            );

            assertThat(result.getDecision()).isEqualTo(StackingResult.StackingDecision.APPROVED);
            assertThat(result.isApproved()).isTrue();
            assertThat(result.isRejected()).isFalse();
            assertThat(result.isPartial()).isFalse();
            assertThat(result.getTotalDiscount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
            assertThat(result.getOriginalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(result.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(90000));
            assertThat(result.getValidatedDiscounts()).hasSize(1);
            assertThat(result.getRejectedDiscounts()).isEmpty();
            assertThat(result.getIssues()).isEmpty();
            assertThat(result.getValidatedAt()).isNotNull();
            assertThat(result.getProcessingTimeMs()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("failure()")
    class FailureTests {

        @Test
        @DisplayName("Should create REJECTED result with issues")
        void shouldCreateRejectedResult() {
            var rejected = StackingResult.RejectedDiscountInfo.builder()
                    .discountId("disc-1")
                    .discountType("FIXED")
                    .rejectionReasons(List.of("Expired"))
                    .build();

            var result = StackingResult.failure(
                    "val-1",
                    List.of(rejected),
                    List.of("Discount expired"),
                    30
            );

            assertThat(result.getDecision()).isEqualTo(StackingResult.StackingDecision.REJECTED);
            assertThat(result.isRejected()).isTrue();
            assertThat(result.isApproved()).isFalse();
            assertThat(result.getTotalDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getRejectedDiscounts()).hasSize(1);
            assertThat(result.getIssues()).containsExactly("Discount expired");
            assertThat(result.getSummary()).contains("1 issue(s) found");
        }
    }

    @Nested
    @DisplayName("partial()")
    class PartialTests {

        @Test
        @DisplayName("Should create PARTIAL result with both validated and rejected")
        void shouldCreatePartialResult() {
            var validated = StackingResult.ValidatedDiscountInfo.builder()
                    .discountId("disc-1")
                    .discountType("PERCENTAGE")
                    .discountAmount(BigDecimal.valueOf(5000))
                    .applicationOrder(1)
                    .build();

            var rejected = StackingResult.RejectedDiscountInfo.builder()
                    .discountId("disc-2")
                    .discountType("FIXED")
                    .rejectionReasons(List.of("Not stackable"))
                    .build();

            var result = StackingResult.partial(
                    "val-1",
                    List.of(validated),
                    List.of(rejected),
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(100000),
                    List.of("One discount rejected"),
                    40
            );

            assertThat(result.getDecision()).isEqualTo(StackingResult.StackingDecision.PARTIAL);
            assertThat(result.isPartial()).isTrue();
            assertThat(result.getValidatedDiscounts()).hasSize(1);
            assertThat(result.getRejectedDiscounts()).hasSize(1);
            assertThat(result.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(95000));
            assertThat(result.getSummary()).contains("1 approved").contains("1 rejected");
        }
    }

    @Nested
    @DisplayName("getValidatedCount()")
    class GetValidatedCountTests {

        @Test
        @DisplayName("Should return count of validated discounts")
        void shouldReturnCount() {
            var result = StackingResult.success(
                    "val-1",
                    List.of(
                            StackingResult.ValidatedDiscountInfo.builder().discountId("d1").discountAmount(BigDecimal.TEN).build(),
                            StackingResult.ValidatedDiscountInfo.builder().discountId("d2").discountAmount(BigDecimal.TEN).build()
                    ),
                    BigDecimal.valueOf(20),
                    BigDecimal.valueOf(100),
                    10
            );

            assertThat(result.getValidatedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return 0 when validatedDiscounts is null")
        void shouldReturnZero_whenNull() {
            var result = StackingResult.builder()
                    .validationId("val-1")
                    .decision(StackingResult.StackingDecision.REJECTED)
                    .build();

            assertThat(result.getValidatedCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getRejectedCount()")
    class GetRejectedCountTests {

        @Test
        @DisplayName("Should return count of rejected discounts")
        void shouldReturnCount() {
            var result = StackingResult.failure(
                    "val-1",
                    List.of(
                            StackingResult.RejectedDiscountInfo.builder().discountId("d1").build(),
                            StackingResult.RejectedDiscountInfo.builder().discountId("d2").build()
                    ),
                    List.of("issue1", "issue2"),
                    10
            );

            assertThat(result.getRejectedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return 0 when rejectedDiscounts is null")
        void shouldReturnZero_whenNull() {
            var result = StackingResult.builder()
                    .validationId("val-1")
                    .decision(StackingResult.StackingDecision.APPROVED)
                    .build();

            assertThat(result.getRejectedCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getValidatedDiscountIds()")
    class GetValidatedDiscountIdsTests {

        @Test
        @DisplayName("Should return list of validated discount IDs")
        void shouldReturnIds() {
            var result = StackingResult.success(
                    "val-1",
                    List.of(
                            StackingResult.ValidatedDiscountInfo.builder().discountId("d1").discountAmount(BigDecimal.TEN).build(),
                            StackingResult.ValidatedDiscountInfo.builder().discountId("d2").discountAmount(BigDecimal.TEN).build()
                    ),
                    BigDecimal.valueOf(20),
                    BigDecimal.valueOf(100),
                    10
            );

            assertThat(result.getValidatedDiscountIds()).containsExactly("d1", "d2");
        }

        @Test
        @DisplayName("Should return empty list when validatedDiscounts is null")
        void shouldReturnEmptyList_whenNull() {
            var result = StackingResult.builder()
                    .validationId("val-1")
                    .decision(StackingResult.StackingDecision.REJECTED)
                    .build();

            assertThat(result.getValidatedDiscountIds()).isEmpty();
        }
    }
}
