package vn.viettel.vds.promotion.validation.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleCodeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RuleCode Value Object Tests")
class RuleCodeTest {

    @Nested
    @DisplayName("of()")
    class OfTests {

        @ParameterizedTest
        @ValueSource(strings = {"RULE_ABC", "RULE_123", "RULE_A_B_C", "RULE_TEST_123"})
        @DisplayName("Should create RuleCode from valid pattern")
        void shouldCreateFromValidPattern(String code) {
            var ruleCode = RuleCode.of(code);
            assertThat(ruleCode.getValue()).isEqualTo(code);
        }

        @Test
        @DisplayName("Should throw NullPointerException when null")
        void shouldThrowNpe_whenNull() {
            assertThatThrownBy(() -> RuleCode.of(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"INVALID", "rule_abc", "RULE_abc", "RULE", "RULE_", "ABC_123", "RULE-123"})
        @DisplayName("Should throw InvalidRuleCodeException when pattern invalid")
        void shouldThrowInvalidRuleCodeException_whenInvalidPattern(String code) {
            assertThatThrownBy(() -> RuleCode.of(code))
                    .isInstanceOf(InvalidRuleCodeException.class);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same value")
        void shouldBeEqual_whenSameValue() {
            var code1 = RuleCode.of("RULE_TEST");
            var code2 = RuleCode.of("RULE_TEST");

            assertThat(code1)
                    .isEqualTo(code2)
                    .hasSameHashCodeAs(code2);
        }

        @Test
        @DisplayName("Should not be equal when different value")
        void shouldNotBeEqual_whenDifferentValue() {
            var code1 = RuleCode.of("RULE_A");
            var code2 = RuleCode.of("RULE_B");

            assertThat(code1).isNotEqualTo(code2);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("Should return value as string")
        void shouldReturnValue() {
            var ruleCode = RuleCode.of("RULE_XYZ");
            assertThat(ruleCode).hasToString("RULE_XYZ");
        }
    }
}
