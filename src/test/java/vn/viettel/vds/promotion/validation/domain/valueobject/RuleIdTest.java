package vn.viettel.vds.promotion.validation.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleIdException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RuleId Value Object Tests")
class RuleIdTest {

    @Nested
    @DisplayName("of()")
    class OfTests {

        @Test
        @DisplayName("Should create RuleId from valid string")
        void shouldCreateFromValidString() {
            // When
            var ruleId = RuleId.of("rule-123");

            // Then
            assertThat(ruleId.getValue()).isEqualTo("rule-123");
        }

        @Test
        @DisplayName("Should throw NullPointerException when value is null")
        void shouldThrowNpe_whenNull() {
            assertThatThrownBy(() -> RuleId.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("Should throw InvalidRuleIdException when value is empty")
        void shouldThrowInvalidRuleIdException_whenEmpty() {
            assertThatThrownBy(() -> RuleId.of(""))
                    .isInstanceOf(InvalidRuleIdException.class);
        }

        @Test
        @DisplayName("Should throw InvalidRuleIdException when value is blank")
        void shouldThrowInvalidRuleIdException_whenBlank() {
            assertThatThrownBy(() -> RuleId.of("   "))
                    .isInstanceOf(InvalidRuleIdException.class);
        }
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate non-null RuleId")
        void shouldGenerateNonNullId() {
            // When
            var ruleId = RuleId.generate();

            // Then
            assertThat(ruleId).isNotNull();
            assertThat(ruleId.getValue()).isNotBlank();
        }

        @Test
        @DisplayName("Should generate unique ids")
        void shouldGenerateUniqueIds() {
            // When
            var id1 = RuleId.generate();
            var id2 = RuleId.generate();

            // Then
            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same value")
        void shouldBeEqual_whenSameValue() {
            var id1 = RuleId.of("rule-1");
            var id2 = RuleId.of("rule-1");

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different value")
        void shouldNotBeEqual_whenDifferentValue() {
            var id1 = RuleId.of("rule-1");
            var id2 = RuleId.of("rule-2");

            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("Should return value as string")
        void shouldReturnValue() {
            var ruleId = RuleId.of("rule-abc");
            assertThat(ruleId.toString()).isEqualTo("rule-abc");
        }
    }
}
