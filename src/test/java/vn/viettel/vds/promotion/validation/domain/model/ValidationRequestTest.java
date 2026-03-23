package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationRequest Tests")
class ValidationRequestTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should build request with all fields")
        void shouldBuildWithAllFields() {
            var request = TestFixtures.validRequest();

            assertThat(request.getTransactionId()).isEqualTo("txn-1");
            assertThat(request.getPromotionId()).isEqualTo("promo-1");
            assertThat(request.getCustomerId()).isEqualTo("cust-1");
            assertThat(request.getTimestamp()).isNotNull();
            assertThat(request.getOrderValue()).isEqualTo(BigDecimal.valueOf(50000));
            assertThat(request.getValidationContext()).isNotNull();
        }

        @Test
        @DisplayName("Should return transactionId as requestId alias")
        void shouldReturnRequestIdAlias() {
            var request = TestFixtures.validRequest();
            assertThat(request.getRequestId()).isEqualTo(request.getTransactionId());
        }
    }

    @Nested
    @DisplayName("isFastCheckEligible()")
    class IsFastCheckEligibleTests {

        @Test
        @DisplayName("Should return true when low value and customer has purchase amount")
        void shouldReturnTrue_whenEligible() {
            var request = TestFixtures.validRequest();
            assertThat(request.isFastCheckEligible()).isTrue();
        }

        @Test
        @DisplayName("Should return false when validationContext is null")
        void shouldReturnFalse_whenContextNull() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .orderValue(BigDecimal.valueOf(50000))
                    .build();

            assertThat(request.isFastCheckEligible()).isFalse();
        }

        @Test
        @DisplayName("Should return false when customer is null")
        void shouldReturnFalse_whenCustomerNull() {
            var context = ValidationContext.builder()
                    .requestTime(Instant.now())
                    .build();
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .orderValue(BigDecimal.valueOf(50000))
                    .validationContext(context)
                    .build();

            assertThat(request.isFastCheckEligible()).isFalse();
        }

        @Test
        @DisplayName("Should return false when high value transaction")
        void shouldReturnFalse_whenHighValue() {
            var request = TestFixtures.highValueRequest();
            assertThat(request.isFastCheckEligible()).isFalse();
        }

        @Test
        @DisplayName("Should return false when customer totalPurchaseAmount is null")
        void shouldReturnFalse_whenPurchaseAmountNull() {
            var context = ValidationContext.builder()
                    .customer(ValidationContext.CustomerContext.builder()
                            .customerId("cust-1")
                            .build())
                    .requestTime(Instant.now())
                    .build();
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .orderValue(BigDecimal.valueOf(50000))
                    .validationContext(context)
                    .build();

            assertThat(request.isFastCheckEligible()).isFalse();
        }
    }

    @Nested
    @DisplayName("isHighValueTransaction()")
    class IsHighValueTransactionTests {

        @Test
        @DisplayName("Should return true when order value exceeds 1,000,000")
        void shouldReturnTrue_whenHighValue() {
            var request = TestFixtures.highValueRequest();
            assertThat(request.isHighValueTransaction()).isTrue();
        }

        @Test
        @DisplayName("Should return false when order value is below threshold")
        void shouldReturnFalse_whenBelowThreshold() {
            var request = TestFixtures.validRequest();
            assertThat(request.isHighValueTransaction()).isFalse();
        }

        @Test
        @DisplayName("Should return false when order value is null")
        void shouldReturnFalse_whenOrderValueNull() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .build();

            assertThat(request.isHighValueTransaction()).isFalse();
        }

        @Test
        @DisplayName("Should return false when order value equals threshold")
        void shouldReturnFalse_whenEqualsThreshold() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .orderValue(new BigDecimal("1000000"))
                    .build();

            assertThat(request.isHighValueTransaction()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCustomerSegment()")
    class GetCustomerSegmentTests {

        @Test
        @DisplayName("Should return segment from validation context")
        void shouldReturnSegment() {
            var request = TestFixtures.validRequest();
            assertThat(request.getCustomerSegment()).isEqualTo("VIP");
        }

        @Test
        @DisplayName("Should return STANDARD when context is null")
        void shouldReturnStandard_whenContextNull() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .build();

            assertThat(request.getCustomerSegment()).isEqualTo("STANDARD");
        }

        @Test
        @DisplayName("Should return STANDARD when customer is null")
        void shouldReturnStandard_whenCustomerNull() {
            var context = ValidationContext.builder()
                    .requestTime(Instant.now())
                    .build();
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .validationContext(context)
                    .build();

            assertThat(request.getCustomerSegment()).isEqualTo("STANDARD");
        }
    }

    @Nested
    @DisplayName("isWithinTimeWindow()")
    class IsWithinTimeWindowTests {

        @Test
        @DisplayName("Should return true when timestamp is within window")
        void shouldReturnTrue_withinWindow() {
            var request = TestFixtures.validRequest();
            assertThat(request.isWithinTimeWindow(5)).isTrue();
        }

        @Test
        @DisplayName("Should return false when timestamp is outside window")
        void shouldReturnFalse_outsideWindow() {
            var request = TestFixtures.expiredRequest();
            assertThat(request.isWithinTimeWindow(5)).isFalse();
        }

        @Test
        @DisplayName("Should return false when timestamp is null")
        void shouldReturnFalse_whenTimestampNull() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .build();

            assertThat(request.isWithinTimeWindow(5)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasValidContext()")
    class HasValidContextTests {

        @Test
        @DisplayName("Should return true when context is valid")
        void shouldReturnTrue_whenValid() {
            var request = TestFixtures.validRequest();
            assertThat(request.hasValidContext()).isTrue();
        }

        @Test
        @DisplayName("Should return false when validationContext is null")
        void shouldReturnFalse_whenContextNull() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .build();

            assertThat(request.hasValidContext()).isFalse();
        }

        @Test
        @DisplayName("Should return false when context has no customer")
        void shouldReturnFalse_whenNoCustomer() {
            var context = ValidationContext.builder()
                    .requestTime(Instant.now())
                    .build();
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .validationContext(context)
                    .build();

            assertThat(request.hasValidContext()).isFalse();
        }
    }
}
