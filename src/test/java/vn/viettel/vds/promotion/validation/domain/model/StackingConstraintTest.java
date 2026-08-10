package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackingConstraint Tests")
class StackingConstraintTest {

    @Nested
    @DisplayName("defaults()")
    class DefaultsTests {
        @Test
        @DisplayName("Should create default constraints")
        void shouldCreateDefaults() {
            StackingConstraint constraint = StackingConstraint.defaults();
            assertThat(constraint.getMaxStackSize()).isEqualTo(5);
            assertThat(constraint.getMaxTotalDiscountPercentage()).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(constraint.isPercentageBeforeFixed()).isTrue();
        }
    }

    @Nested
    @DisplayName("lenient()")
    class LenientTests {
        @Test
        @DisplayName("Should create lenient constraints")
        void shouldCreateLenient() {
            StackingConstraint constraint = StackingConstraint.lenient();
            assertThat(constraint.getMaxStackSize()).isEqualTo(10);
            assertThat(constraint.isPercentageBeforeFixed()).isFalse();
        }
    }

    @Nested
    @DisplayName("strict()")
    class StrictTests {
        @Test
        @DisplayName("Should create strict constraints")
        void shouldCreateStrict() {
            StackingConstraint constraint = StackingConstraint.strict();
            assertThat(constraint.getMaxStackSize()).isEqualTo(3);
            assertThat(constraint.getMaxTotalDiscountPercentage()).isEqualByComparingTo(new BigDecimal("80"));
            assertThat(constraint.getMinOrderAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        }
    }

    @Nested
    @DisplayName("IncompatiblePair")
    class IncompatiblePairTests {
        @Test
        @DisplayName("Should match types in both directions")
        void shouldMatchBothDirections() {
            var pair = StackingConstraint.IncompatiblePair.of("PERCENTAGE", "FIXED", "Cannot combine");
            assertThat(pair.matches("PERCENTAGE", "FIXED")).isTrue();
            assertThat(pair.matches("FIXED", "PERCENTAGE")).isTrue();
        }

        @Test
        @DisplayName("Should not match different types")
        void shouldNotMatchDifferentTypes() {
            var pair = StackingConstraint.IncompatiblePair.of("PERCENTAGE", "FIXED", "reason");
            assertThat(pair.matches("PERCENTAGE", "VOUCHER")).isFalse();
        }
    }
}
