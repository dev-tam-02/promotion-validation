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
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackingContext Tests")
class StackingContextTest {

    private StackingContext validContext() {
        return StackingContext.builder()
                .validationId("val-1")
                .customer(CustomerFact.builder().customerId("c1").tier("GOLD").build())
                .order(OrderFact.builder().orderId("o1").totalAmount(BigDecimal.valueOf(100000)).build())
                .discounts(List.of(DiscountFact.builder().discountId("d1").type("PERCENTAGE").build()))
                .requestedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("getDiscountCount()")
    class GetDiscountCountTests {
        @Test
        @DisplayName("Should return count of discounts")
        void shouldReturnCount() {
            assertThat(validContext().getDiscountCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 0 when discounts is null")
        void shouldReturnZeroWhenNull() {
            StackingContext ctx = StackingContext.builder().build();
            assertThat(ctx.getDiscountCount()).isZero();
        }
    }

    @Nested
    @DisplayName("hasCustomer()")
    class HasCustomerTests {
        @Test
        @DisplayName("Should return true when customer present")
        void shouldReturnTrue() {
            assertThat(validContext().hasCustomer()).isTrue();
        }

        @Test
        @DisplayName("Should return false when customer is null")
        void shouldReturnFalseWhenNull() {
            StackingContext ctx = StackingContext.builder().build();
            assertThat(ctx.hasCustomer()).isFalse();
        }

        @Test
        @DisplayName("Should return false when customerId is null")
        void shouldReturnFalseWhenIdNull() {
            StackingContext ctx = StackingContext.builder()
                    .customer(CustomerFact.builder().build())
                    .build();
            assertThat(ctx.hasCustomer()).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid()")
    class IsValidTests {
        @Test
        @DisplayName("Should return true for valid context")
        void shouldReturnTrue() {
            assertThat(validContext().isValid()).isTrue();
        }

        @Test
        @DisplayName("Should return false when validationId is null")
        void shouldReturnFalseWithoutValidationId() {
            StackingContext ctx = StackingContext.builder()
                    .customer(CustomerFact.builder().customerId("c1").build())
                    .order(OrderFact.builder().orderId("o1").build())
                    .discounts(List.of(DiscountFact.builder().discountId("d1").build()))
                    .build();
            assertThat(ctx.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCustomerSegment()")
    class GetCustomerSegmentTests {
        @Test
        @DisplayName("Should return tier as segment")
        void shouldReturnTier() {
            assertThat(validContext().getCustomerSegment()).isEqualTo("GOLD");
        }

        @Test
        @DisplayName("Should return segment from attributes when no tier")
        void shouldReturnFromAttributes() {
            StackingContext ctx = StackingContext.builder()
                    .customer(CustomerFact.builder()
                            .customerId("c1")
                            .attributes(Map.of("segment", "VIP"))
                            .build())
                    .build();
            assertThat(ctx.getCustomerSegment()).isEqualTo("VIP");
        }

        @Test
        @DisplayName("Should return null when no customer")
        void shouldReturnNullWhenNoCustomer() {
            StackingContext ctx = StackingContext.builder().build();
            assertThat(ctx.getCustomerSegment()).isNull();
        }
    }

    @Nested
    @DisplayName("createInvalidContextError()")
    class CreateInvalidContextErrorTests {
        @Test
        @DisplayName("Should create deny result with missing fields")
        void shouldCreateDenyResult() {
            StackingContext ctx = StackingContext.builder()
                    .validationId("val-1")
                    .build();
            ValidationResult result = ctx.createInvalidContextError();
            assertThat(result.isDenied()).isTrue();
            assertThat(result.getValidationId()).isEqualTo("val-1");
        }
    }
}
