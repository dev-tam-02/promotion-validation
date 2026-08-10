package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogicType Tests")
class LogicTypeTest {

    @Test
    @DisplayName("Should have all expected values")
    void shouldHaveExpectedValues() {
        assertThat(LogicType.values()).hasSize(5);
    }

    @Test
    @DisplayName("AND should have correct description")
    void andShouldHaveCorrectDescription() {
        assertThat(LogicType.AND.getDescription()).isEqualTo("All conditions must be true");
    }

    @Test
    @DisplayName("OR should have correct description")
    void orShouldHaveCorrectDescription() {
        assertThat(LogicType.OR.getDescription()).isEqualTo("At least one condition must be true");
    }

    @Test
    @DisplayName("NOT should have correct description")
    void notShouldHaveCorrectDescription() {
        assertThat(LogicType.NOT.getDescription()).isEqualTo("Negates the condition");
    }

    @Test
    @DisplayName("XOR should have correct description")
    void xorShouldHaveCorrectDescription() {
        assertThat(LogicType.XOR.getDescription()).isEqualTo("Exactly one condition must be true");
    }

    @Test
    @DisplayName("CUSTOM should have correct description")
    void customShouldHaveCorrectDescription() {
        assertThat(LogicType.CUSTOM.getDescription()).isEqualTo("Custom logic implementation");
    }
}
