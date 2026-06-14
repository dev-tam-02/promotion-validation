package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparatorSuffixTest {

    @ParameterizedTest
    @CsvSource({
            "is_more_than, gt",
            "is_more_than_or_equal_to, gte",
            "is_exactly, equals",
            "is_less_than, lt",
            "is_less_than_or_equal_to, lte",
            "is_between, between"
    })
    void of_returnsExpectedSuffix(String comparator, String suffix) {
        assertThat(ComparatorSuffix.of(comparator)).contains(suffix);
    }

    @Test
    void of_unknownComparator_returnsEmpty() {
        assertThat(ComparatorSuffix.of("in")).isEmpty();
        assertThat(ComparatorSuffix.of("contains")).isEmpty();
    }

    @Test
    void of_nullComparator_returnsEmpty() {
        assertThat(ComparatorSuffix.of(null)).isEmpty();
    }

    @Test
    void resolve_composesCanonicalWithSuffix() {
        assertThat(ComparatorSuffix.resolve("order.total", "is_more_than"))
                .isEqualTo("order.total.gt");
        assertThat(ComparatorSuffix.resolve("order.initial.amount", "is_exactly"))
                .isEqualTo("order.initial.amount.equals");
        assertThat(ComparatorSuffix.resolve("order.total", "is_between"))
                .isEqualTo("order.total.between");
    }

    @Test
    void resolve_blankCanonical_throws() {
        assertThatThrownBy(() -> ComparatorSuffix.resolve("", "is_more_than"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canonical");
        assertThatThrownBy(() -> ComparatorSuffix.resolve(null, "is_more_than"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolve_unknownComparator_throws() {
        assertThatThrownBy(() -> ComparatorSuffix.resolve("order.total", "in"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown comparator");
    }

    @Test
    void isSupported_reflectsKnownComparators() {
        assertThat(ComparatorSuffix.isSupported("is_more_than")).isTrue();
        assertThat(ComparatorSuffix.isSupported("is_exactly")).isTrue();
        assertThat(ComparatorSuffix.isSupported("in")).isFalse();
        assertThat(ComparatorSuffix.isSupported(null)).isFalse();
    }
}
