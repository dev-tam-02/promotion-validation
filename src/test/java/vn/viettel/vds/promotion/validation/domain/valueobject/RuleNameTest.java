package vn.viettel.vds.promotion.validation.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleNameException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RuleName Value Object Tests")
class RuleNameTest {

    @Nested
    @DisplayName("of()")
    class OfTests {

        @Test
        @DisplayName("Should create RuleName from valid string")
        void shouldCreateFromValidString() {
            var ruleName = RuleName.of("Valid Rule Name");
            assertThat(ruleName.getValue()).isEqualTo("Valid Rule Name");
        }

        @Test
        @DisplayName("Should trim whitespace")
        void shouldTrimWhitespace() {
            var ruleName = RuleName.of("  Rule Name  ");
            assertThat(ruleName.getValue()).isEqualTo("Rule Name");
        }

        @Test
        @DisplayName("Should accept name with exactly 3 characters (min)")
        void shouldAcceptMinLength() {
            var ruleName = RuleName.of("Abc");
            assertThat(ruleName.getValue()).isEqualTo("Abc");
        }

        @Test
        @DisplayName("Should accept name with exactly 100 characters (max)")
        void shouldAcceptMaxLength() {
            var name = "A".repeat(100);
            var ruleName = RuleName.of(name);
            assertThat(ruleName.getValue()).isEqualTo(name);
        }

        @Test
        @DisplayName("Should throw NullPointerException when null")
        void shouldThrowNpe_whenNull() {
            assertThatThrownBy(() -> RuleName.of(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"Ab", "X", ""})
        @DisplayName("Should throw InvalidRuleNameException when too short")
        void shouldThrowInvalidRuleNameException_whenTooShort(String name) {
            assertThatThrownBy(() -> RuleName.of(name))
                    .isInstanceOf(InvalidRuleNameException.class);
        }

        @Test
        @DisplayName("Should throw InvalidRuleNameException when exceeds 100 chars")
        void shouldThrowInvalidRuleNameException_whenTooLong() {
            var longName = "A".repeat(101);
            assertThatThrownBy(() -> RuleName.of(longName))
                    .isInstanceOf(InvalidRuleNameException.class);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same value")
        void shouldBeEqual_whenSameValue() {
            var name1 = RuleName.of("Same Name");
            var name2 = RuleName.of("Same Name");

            assertThat(name1).isEqualTo(name2);
            assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different value")
        void shouldNotBeEqual_whenDifferentValue() {
            var name1 = RuleName.of("Name One");
            var name2 = RuleName.of("Name Two");

            assertThat(name1).isNotEqualTo(name2);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("Should return value as string")
        void shouldReturnValue() {
            var ruleName = RuleName.of("Test Rule");
            assertThat(ruleName.toString()).isEqualTo("Test Rule");
        }
    }
}
