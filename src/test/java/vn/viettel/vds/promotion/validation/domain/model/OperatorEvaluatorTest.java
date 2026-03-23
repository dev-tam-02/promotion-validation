package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidOperatorException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OperatorEvaluator Tests")
class OperatorEvaluatorTest {

    @Nested
    @DisplayName("EQUALS operator")
    class EqualsTests {

        @ParameterizedTest
        @ValueSource(strings = {"EQUALS", "EQ", "=="})
        @DisplayName("Should return true for equal string values")
        void shouldReturnTrue_forEqualStrings(String op) {
            assertThat(OperatorEvaluator.evaluate(op, "hello", "hello")).isTrue();
        }

        @Test
        @DisplayName("Should return true for equal numeric values")
        void shouldReturnTrue_forEqualNumbers() {
            assertThat(OperatorEvaluator.evaluate("EQUALS", 100, BigDecimal.valueOf(100))).isTrue();
        }

        @Test
        @DisplayName("Should return true when both null")
        void shouldReturnTrue_whenBothNull() {
            assertThat(OperatorEvaluator.evaluate("EQUALS", null, null)).isTrue();
        }

        @Test
        @DisplayName("Should return false when one is null")
        void shouldReturnFalse_whenOneNull() {
            assertThat(OperatorEvaluator.evaluate("EQUALS", "value", null)).isFalse();
            assertThat(OperatorEvaluator.evaluate("EQUALS", null, "value")).isFalse();
        }

        @Test
        @DisplayName("Should return false for different values")
        void shouldReturnFalse_forDifferentValues() {
            assertThat(OperatorEvaluator.evaluate("EQUALS", "abc", "xyz")).isFalse();
        }
    }

    @Nested
    @DisplayName("NOT_EQUALS operator")
    class NotEqualsTests {

        @ParameterizedTest
        @ValueSource(strings = {"NOT_EQUALS", "NEQ", "!="})
        @DisplayName("Should return true for different values")
        void shouldReturnTrue_forDifferentValues(String op) {
            assertThat(OperatorEvaluator.evaluate(op, "abc", "xyz")).isTrue();
        }

        @Test
        @DisplayName("Should return false for equal values")
        void shouldReturnFalse_forEqualValues() {
            assertThat(OperatorEvaluator.evaluate("NOT_EQUALS", "same", "same")).isFalse();
        }
    }

    @Nested
    @DisplayName("GREATER_THAN operator")
    class GreaterThanTests {

        @ParameterizedTest
        @ValueSource(strings = {"GREATER_THAN", "GT", ">"})
        @DisplayName("Should return true when field is greater")
        void shouldReturnTrue_whenGreater(String op) {
            assertThat(OperatorEvaluator.evaluate(op, 200, 100)).isTrue();
        }

        @Test
        @DisplayName("Should return false when equal")
        void shouldReturnFalse_whenEqual() {
            assertThat(OperatorEvaluator.evaluate("GT", 100, 100)).isFalse();
        }

        @Test
        @DisplayName("Should return false when less")
        void shouldReturnFalse_whenLess() {
            assertThat(OperatorEvaluator.evaluate("GT", 50, 100)).isFalse();
        }

        @Test
        @DisplayName("Should compare numeric strings correctly")
        void shouldCompareNumericStrings() {
            assertThat(OperatorEvaluator.evaluate("GT", "200", "100")).isTrue();
        }
    }

    @Nested
    @DisplayName("GREATER_THAN_OR_EQUALS operator")
    class GteTests {

        @Test
        @DisplayName("Should return true when greater")
        void shouldReturnTrue_whenGreater() {
            assertThat(OperatorEvaluator.evaluate("GTE", 200, 100)).isTrue();
        }

        @Test
        @DisplayName("Should return true when equal")
        void shouldReturnTrue_whenEqual() {
            assertThat(OperatorEvaluator.evaluate("GTE", 100, 100)).isTrue();
        }

        @Test
        @DisplayName("Should return false when less")
        void shouldReturnFalse_whenLess() {
            assertThat(OperatorEvaluator.evaluate("GTE", 50, 100)).isFalse();
        }
    }

    @Nested
    @DisplayName("LESS_THAN operator")
    class LessThanTests {

        @Test
        @DisplayName("Should return true when less")
        void shouldReturnTrue_whenLess() {
            assertThat(OperatorEvaluator.evaluate("LT", 50, 100)).isTrue();
        }

        @Test
        @DisplayName("Should return false when greater or equal")
        void shouldReturnFalse_whenGreaterOrEqual() {
            assertThat(OperatorEvaluator.evaluate("LT", 100, 100)).isFalse();
            assertThat(OperatorEvaluator.evaluate("LT", 200, 100)).isFalse();
        }
    }

    @Nested
    @DisplayName("LESS_THAN_OR_EQUALS operator")
    class LteTests {

        @Test
        @DisplayName("Should return true when less or equal")
        void shouldReturnTrue_whenLessOrEqual() {
            assertThat(OperatorEvaluator.evaluate("LTE", 50, 100)).isTrue();
            assertThat(OperatorEvaluator.evaluate("LTE", 100, 100)).isTrue();
        }
    }

    @Nested
    @DisplayName("IN operator")
    class InTests {

        @Test
        @DisplayName("Should return true when value is in collection")
        void shouldReturnTrue_whenInCollection() {
            assertThat(OperatorEvaluator.evaluate("IN", "VIP", List.of("VIP", "GOLD", "SILVER"))).isTrue();
        }

        @Test
        @DisplayName("Should return false when value not in collection")
        void shouldReturnFalse_whenNotInCollection() {
            assertThat(OperatorEvaluator.evaluate("IN", "BRONZE", List.of("VIP", "GOLD"))).isFalse();
        }

        @Test
        @DisplayName("Should return true when value in array")
        void shouldReturnTrue_whenInArray() {
            assertThat(OperatorEvaluator.evaluate("IN", "A", new Object[]{"A", "B", "C"})).isTrue();
        }
    }

    @Nested
    @DisplayName("NOT_IN operator")
    class NotInTests {

        @Test
        @DisplayName("Should return true when value not in collection")
        void shouldReturnTrue_whenNotInCollection() {
            assertThat(OperatorEvaluator.evaluate("NOT_IN", "BRONZE", List.of("VIP", "GOLD"))).isTrue();
        }

        @Test
        @DisplayName("Should return false when value is in collection")
        void shouldReturnFalse_whenInCollection() {
            assertThat(OperatorEvaluator.evaluate("NOT_IN", "VIP", List.of("VIP", "GOLD"))).isFalse();
        }
    }

    @Nested
    @DisplayName("CONTAINS operator")
    class ContainsTests {

        @Test
        @DisplayName("Should return true when field contains expected")
        void shouldReturnTrue_whenContains() {
            assertThat(OperatorEvaluator.evaluate("CONTAINS", "Hello World", "World")).isTrue();
        }

        @Test
        @DisplayName("Should return false when field does not contain expected")
        void shouldReturnFalse_whenNotContains() {
            assertThat(OperatorEvaluator.evaluate("CONTAINS", "Hello", "World")).isFalse();
        }
    }

    @Nested
    @DisplayName("STARTS_WITH operator")
    class StartsWithTests {

        @Test
        @DisplayName("Should return true when field starts with expected")
        void shouldReturnTrue() {
            assertThat(OperatorEvaluator.evaluate("STARTS_WITH", "Hello World", "Hello")).isTrue();
        }

        @Test
        @DisplayName("Should return false when field does not start with expected")
        void shouldReturnFalse() {
            assertThat(OperatorEvaluator.evaluate("STARTS_WITH", "Hello World", "World")).isFalse();
        }
    }

    @Nested
    @DisplayName("ENDS_WITH operator")
    class EndsWithTests {

        @Test
        @DisplayName("Should return true when field ends with expected")
        void shouldReturnTrue() {
            assertThat(OperatorEvaluator.evaluate("ENDS_WITH", "Hello World", "World")).isTrue();
        }
    }

    @Nested
    @DisplayName("MATCHES / REGEX operator")
    class RegexTests {

        @Test
        @DisplayName("Should return true when field matches pattern")
        void shouldReturnTrue_whenMatches() {
            assertThat(OperatorEvaluator.evaluate("MATCHES", "abc123", "[a-z]+[0-9]+")).isTrue();
        }

        @Test
        @DisplayName("Should return false when field does not match")
        void shouldReturnFalse_whenNotMatches() {
            assertThat(OperatorEvaluator.evaluate("REGEX", "abc", "[0-9]+")).isFalse();
        }
    }

    @Nested
    @DisplayName("IS_NULL / IS_NOT_NULL operators")
    class NullCheckTests {

        @Test
        @DisplayName("Should return true for IS_NULL with null value")
        void shouldReturnTrue_isNull_withNull() {
            assertThat(OperatorEvaluator.evaluate("IS_NULL", null, null)).isTrue();
        }

        @Test
        @DisplayName("Should return false for IS_NULL with non-null value")
        void shouldReturnFalse_isNull_withNonNull() {
            assertThat(OperatorEvaluator.evaluate("IS_NULL", "value", null)).isFalse();
        }

        @Test
        @DisplayName("Should return true for IS_NOT_NULL with non-null value")
        void shouldReturnTrue_isNotNull_withNonNull() {
            assertThat(OperatorEvaluator.evaluate("IS_NOT_NULL", "value", null)).isTrue();
        }

        @Test
        @DisplayName("Should return false for IS_NOT_NULL with null value")
        void shouldReturnFalse_isNotNull_withNull() {
            assertThat(OperatorEvaluator.evaluate("IS_NOT_NULL", null, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("IS_EMPTY / IS_NOT_EMPTY operators")
    class EmptyCheckTests {

        @Test
        @DisplayName("Should return true for IS_EMPTY with null")
        void shouldReturnTrue_isEmpty_withNull() {
            assertThat(OperatorEvaluator.evaluate("IS_EMPTY", null, null)).isTrue();
        }

        @Test
        @DisplayName("Should return true for IS_EMPTY with empty string")
        void shouldReturnTrue_isEmpty_withEmptyString() {
            assertThat(OperatorEvaluator.evaluate("IS_EMPTY", "", null)).isTrue();
        }

        @Test
        @DisplayName("Should return true for IS_EMPTY with empty collection")
        void shouldReturnTrue_isEmpty_withEmptyCollection() {
            assertThat(OperatorEvaluator.evaluate("IS_EMPTY", List.of(), null)).isTrue();
        }

        @Test
        @DisplayName("Should return false for IS_NOT_EMPTY with empty string")
        void shouldReturnFalse_isNotEmpty_withEmpty() {
            assertThat(OperatorEvaluator.evaluate("IS_NOT_EMPTY", "", null)).isFalse();
        }
    }

    @Nested
    @DisplayName("BETWEEN operator")
    class BetweenTests {

        @Test
        @DisplayName("Should return true when value is within range")
        void shouldReturnTrue_withinRange() {
            assertThat(OperatorEvaluator.evaluate("BETWEEN", 50, new Object[]{10, 100})).isTrue();
        }

        @Test
        @DisplayName("Should return true when value equals boundary")
        void shouldReturnTrue_atBoundary() {
            assertThat(OperatorEvaluator.evaluate("BETWEEN", 10, new Object[]{10, 100})).isTrue();
            assertThat(OperatorEvaluator.evaluate("BETWEEN", 100, new Object[]{10, 100})).isTrue();
        }

        @Test
        @DisplayName("Should return false when value outside range")
        void shouldReturnFalse_outsideRange() {
            assertThat(OperatorEvaluator.evaluate("BETWEEN", 200, new Object[]{10, 100})).isFalse();
        }

        @Test
        @DisplayName("Should throw InvalidOperatorException when array not size 2")
        void shouldThrow_whenNotSize2() {
            assertThatThrownBy(() -> OperatorEvaluator.evaluate("BETWEEN", 50, new Object[]{10}))
                    .isInstanceOf(InvalidOperatorException.class);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw InvalidOperatorException when operator is null")
        void shouldThrow_whenOperatorNull() {
            assertThatThrownBy(() -> OperatorEvaluator.evaluate(null, "a", "b"))
                    .isInstanceOf(InvalidOperatorException.class);
        }

        @Test
        @DisplayName("Should throw UnsupportedOperationException for unknown operator")
        void shouldThrow_whenUnknownOperator() {
            assertThatThrownBy(() -> OperatorEvaluator.evaluate("UNKNOWN_OP", "a", "b"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Case insensitivity")
    class CaseInsensitivityTests {

        @ParameterizedTest
        @CsvSource({"equals, abc, abc", "Equals, abc, abc", "EQUALS, abc, abc"})
        @DisplayName("Should handle operators case-insensitively")
        void shouldHandleCaseInsensitive(String op, String field, String expected) {
            assertThat(OperatorEvaluator.evaluate(op, field, expected)).isTrue();
        }
    }
}
