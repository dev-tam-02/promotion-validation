package vn.viettel.vds.promotion.validation.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidVersionFormatException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Version Value Object Tests")
class VersionTest {

    @Nested
    @DisplayName("of()")
    class OfTests {

        @Test
        @DisplayName("Should create version with valid numbers")
        void shouldCreateWithValidNumbers() {
            var version = Version.of(1, 2, 3);

            assertThat(version.getMajor()).isEqualTo(1);
            assertThat(version.getMinor()).isEqualTo(2);
            assertThat(version.getPatch()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should throw InvalidVersionFormatException when major is negative")
        void shouldThrow_whenMajorNegative() {
            assertThatThrownBy(() -> Version.of(-1, 0, 0))
                    .isInstanceOf(InvalidVersionFormatException.class);
        }

        @Test
        @DisplayName("Should throw InvalidVersionFormatException when minor is negative")
        void shouldThrow_whenMinorNegative() {
            assertThatThrownBy(() -> Version.of(1, -1, 0))
                    .isInstanceOf(InvalidVersionFormatException.class);
        }

        @Test
        @DisplayName("Should throw InvalidVersionFormatException when patch is negative")
        void shouldThrow_whenPatchNegative() {
            assertThatThrownBy(() -> Version.of(1, 0, -1))
                    .isInstanceOf(InvalidVersionFormatException.class);
        }
    }

    @Nested
    @DisplayName("initial()")
    class InitialTests {

        @Test
        @DisplayName("Should create version 1.0.0")
        void shouldCreateInitialVersion() {
            var version = Version.initial();

            assertThat(version.getMajor()).isEqualTo(1);
            assertThat(version.getMinor()).isZero();
            assertThat(version.getPatch()).isZero();
        }
    }

    @Nested
    @DisplayName("parse()")
    class ParseTests {

        @Test
        @DisplayName("Should parse valid version string")
        void shouldParseValidString() {
            var version = Version.parse("2.3.4");

            assertThat(version.getMajor()).isEqualTo(2);
            assertThat(version.getMinor()).isEqualTo(3);
            assertThat(version.getPatch()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should throw NullPointerException when string is null")
        void shouldThrow_whenNull() {
            assertThatThrownBy(() -> Version.parse(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw InvalidVersionFormatException when format is wrong")
        void shouldThrow_whenWrongFormat() {
            assertThatThrownBy(() -> Version.parse("1.2"))
                    .isInstanceOf(InvalidVersionFormatException.class);
        }

        @Test
        @DisplayName("Should throw InvalidVersionFormatException when non-numeric")
        void shouldThrow_whenNonNumeric() {
            assertThatThrownBy(() -> Version.parse("a.b.c"))
                    .isInstanceOf(InvalidVersionFormatException.class);
        }
    }

    @Nested
    @DisplayName("increment methods")
    class IncrementTests {

        @Test
        @DisplayName("Should increment major and reset minor/patch")
        void shouldIncrementMajor() {
            var version = Version.of(1, 2, 3).incrementMajor();

            assertThat(version.getMajor()).isEqualTo(2);
            assertThat(version.getMinor()).isZero();
            assertThat(version.getPatch()).isZero();
        }

        @Test
        @DisplayName("Should increment minor and reset patch")
        void shouldIncrementMinor() {
            var version = Version.of(1, 2, 3).incrementMinor();

            assertThat(version.getMajor()).isEqualTo(1);
            assertThat(version.getMinor()).isEqualTo(3);
            assertThat(version.getPatch()).isZero();
        }

        @Test
        @DisplayName("Should increment patch only")
        void shouldIncrementPatch() {
            var version = Version.of(1, 2, 3).incrementPatch();

            assertThat(version.getMajor()).isEqualTo(1);
            assertThat(version.getMinor()).isEqualTo(2);
            assertThat(version.getPatch()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("isNewerThan()")
    class IsNewerThanTests {

        @Test
        @DisplayName("Should return true when major is higher")
        void shouldReturnTrue_whenMajorHigher() {
            assertThat(Version.of(2, 0, 0).isNewerThan(Version.of(1, 9, 9))).isTrue();
        }

        @Test
        @DisplayName("Should return true when minor is higher")
        void shouldReturnTrue_whenMinorHigher() {
            assertThat(Version.of(1, 2, 0).isNewerThan(Version.of(1, 1, 9))).isTrue();
        }

        @Test
        @DisplayName("Should return true when patch is higher")
        void shouldReturnTrue_whenPatchHigher() {
            assertThat(Version.of(1, 1, 2).isNewerThan(Version.of(1, 1, 1))).isTrue();
        }

        @Test
        @DisplayName("Should return false when same version")
        void shouldReturnFalse_whenSameVersion() {
            assertThat(Version.of(1, 0, 0).isNewerThan(Version.of(1, 0, 0))).isFalse();
        }

        @Test
        @DisplayName("Should return false when older version")
        void shouldReturnFalse_whenOlderVersion() {
            assertThat(Version.of(1, 0, 0).isNewerThan(Version.of(2, 0, 0))).isFalse();
        }
    }

    @Nested
    @DisplayName("compareTo()")
    class CompareToTests {

        @Test
        @DisplayName("Should return positive when newer")
        void shouldReturnPositive_whenNewer() {
            assertThat(Version.of(2, 0, 0)).isGreaterThan(Version.of(1, 0, 0));
        }

        @Test
        @DisplayName("Should return zero when equal")
        void shouldReturnZero_whenEqual() {
            assertThat(Version.of(1, 0, 0)).isEqualByComparingTo(Version.of(1, 0, 0));
        }

        @Test
        @DisplayName("Should return negative when older")
        void shouldReturnNegative_whenOlder() {
            assertThat(Version.of(1, 0, 0)).isLessThan(Version.of(2, 0, 0));
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same components")
        void shouldBeEqual_whenSameComponents() {
            var v1 = Version.of(1, 2, 3);
            var v2 = Version.of(1, 2, 3);

            assertThat(v1)
                    .isEqualTo(v2)
                    .hasSameHashCodeAs(v2);
        }

        @Test
        @DisplayName("Should not be equal when different components")
        void shouldNotBeEqual_whenDifferent() {
            assertThat(Version.of(1, 0, 0)).isNotEqualTo(Version.of(1, 0, 1));
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("Should format as major.minor.patch")
        void shouldFormatCorrectly() {
            assertThat(Version.of(1, 2, 3)).hasToString("1.2.3");
        }
    }
}
