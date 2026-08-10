package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleBinding Tests")
class RuleBindingTest {

    @Nested
    @DisplayName("isActive()")
    class IsActiveTests {
        @Test
        @DisplayName("Should return true when active is true")
        void shouldReturnTrueWhenActive() {
            RuleBinding binding = RuleBinding.builder().active(true).build();
            assertThat(binding.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should return false when active is false")
        void shouldReturnFalseWhenInactive() {
            RuleBinding binding = RuleBinding.builder().active(false).build();
            assertThat(binding.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should return false when active is null")
        void shouldReturnFalseWhenNull() {
            RuleBinding binding = new RuleBinding();
            binding.setActive(null);
            assertThat(binding.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("isEffectiveAt()")
    class IsEffectiveAtTests {
        @Test
        @DisplayName("Should return true when within validity period")
        void shouldReturnTrueWithinPeriod() {
            Instant now = Instant.now();
            RuleBinding binding = RuleBinding.builder()
                    .active(true)
                    .validFrom(now.minus(1, ChronoUnit.HOURS))
                    .validTo(now.plus(1, ChronoUnit.HOURS))
                    .build();
            assertThat(binding.isEffectiveAt(now)).isTrue();
        }

        @Test
        @DisplayName("Should return false when before validFrom")
        void shouldReturnFalseBeforeValidFrom() {
            Instant future = Instant.now().plus(1, ChronoUnit.DAYS);
            RuleBinding binding = RuleBinding.builder()
                    .active(true)
                    .validFrom(future)
                    .build();
            assertThat(binding.isEffectiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Should return false when after validTo")
        void shouldReturnFalseAfterValidTo() {
            Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
            RuleBinding binding = RuleBinding.builder()
                    .active(true)
                    .validTo(past)
                    .build();
            assertThat(binding.isEffectiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Should return false when not active")
        void shouldReturnFalseWhenNotActive() {
            RuleBinding binding = RuleBinding.builder()
                    .active(false)
                    .build();
            assertThat(binding.isEffectiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Should return true when no time constraints")
        void shouldReturnTrueWithNoConstraints() {
            RuleBinding binding = RuleBinding.builder().active(true).build();
            assertThat(binding.isEffectiveAt(Instant.now())).isTrue();
        }

        @Test
        @DisplayName("Should return false on excluded date")
        void shouldReturnFalseOnExcludedDate() {
            Instant now = Instant.parse("2026-01-15T10:00:00Z");
            RuleBinding binding = RuleBinding.builder()
                    .active(true)
                    .timezone("UTC")
                    .excludedDates(List.of("2026-01-15"))
                    .build();
            assertThat(binding.isEffectiveAt(now)).isFalse();
        }
    }

    @Nested
    @DisplayName("appliesToProduct()")
    class AppliesToProductTests {
        @Test
        @DisplayName("Should apply to all when includedAll is true")
        void shouldApplyToAllWhenIncludedAll() {
            RuleBinding binding = RuleBinding.builder().includedAll(true).build();
            assertThat(binding.appliesToProduct("p1", "c1", "b1")).isTrue();
        }

        @Test
        @DisplayName("Should not apply when product is excluded and includedAll")
        void shouldNotApplyWhenProductExcluded() {
            RuleBinding binding = RuleBinding.builder()
                    .includedAll(true)
                    .excludedProducts(List.of("p1"))
                    .build();
            assertThat(binding.appliesToProduct("p1", "c1", "b1")).isFalse();
        }

        @Test
        @DisplayName("Should apply when product is in included list")
        void shouldApplyWhenProductIncluded() {
            RuleBinding binding = RuleBinding.builder()
                    .includedAll(false)
                    .includedProducts(List.of("p1", "p2"))
                    .build();
            assertThat(binding.appliesToProduct("p1", null, null)).isTrue();
        }

        @Test
        @DisplayName("Should not apply when product not in any list")
        void shouldNotApplyWhenNotInAnyList() {
            RuleBinding binding = RuleBinding.builder()
                    .includedAll(false)
                    .includedProducts(List.of("p1"))
                    .build();
            assertThat(binding.appliesToProduct("p2", null, null)).isFalse();
        }

        @Test
        @DisplayName("Should apply when category is included")
        void shouldApplyWhenCategoryIncluded() {
            RuleBinding binding = RuleBinding.builder()
                    .includedAll(false)
                    .includedCategories(List.of("cat-1"))
                    .build();
            assertThat(binding.appliesToProduct("p1", "cat-1", null)).isTrue();
        }

        @Test
        @DisplayName("Should apply when brand is included")
        void shouldApplyWhenBrandIncluded() {
            RuleBinding binding = RuleBinding.builder()
                    .includedAll(false)
                    .includedBrands(List.of("brand-1"))
                    .build();
            assertThat(binding.appliesToProduct("p1", null, "brand-1")).isTrue();
        }
    }

    @Nested
    @DisplayName("shouldApplyToTraffic()")
    class ShouldApplyToTrafficTests {
        @Test
        @DisplayName("Should return true when trafficPercent is 100")
        void shouldReturnTrueAt100Percent() {
            RuleBinding binding = RuleBinding.builder().trafficPercent(100).build();
            assertThat(binding.shouldApplyToTraffic("customer-1")).isTrue();
        }

        @Test
        @DisplayName("Should return true when trafficPercent is null")
        void shouldReturnTrueWhenNull() {
            RuleBinding binding = new RuleBinding();
            binding.setTrafficPercent(null);
            assertThat(binding.shouldApplyToTraffic("customer-1")).isTrue();
        }

        @Test
        @DisplayName("Should return false when trafficPercent is 0")
        void shouldReturnFalseAt0Percent() {
            RuleBinding binding = RuleBinding.builder().trafficPercent(0).build();
            assertThat(binding.shouldApplyToTraffic("customer-1")).isFalse();
        }
    }

    @Nested
    @DisplayName("hasTemporalConstraints()")
    class HasTemporalConstraintsTests {
        @Test
        @DisplayName("Should return false when no constraints")
        void shouldReturnFalseWhenNoConstraints() {
            RuleBinding binding = RuleBinding.builder().build();
            assertThat(binding.hasTemporalConstraints()).isFalse();
        }

        @Test
        @DisplayName("Should return true when validFrom is set")
        void shouldReturnTrueWhenValidFromSet() {
            RuleBinding binding = RuleBinding.builder().validFrom(Instant.now()).build();
            assertThat(binding.hasTemporalConstraints()).isTrue();
        }

        @Test
        @DisplayName("Should return true when rrule is set")
        void shouldReturnTrueWhenRruleSet() {
            RuleBinding binding = RuleBinding.builder().rrule("FREQ=DAILY").build();
            assertThat(binding.hasTemporalConstraints()).isTrue();
        }
    }

    @Nested
    @DisplayName("Enums")
    class EnumTests {
        @Test
        @DisplayName("StickyKeyStrategy should have all values")
        void stickyKeyStrategyShouldHaveAllValues() {
            assertThat(RuleBinding.StickyKeyStrategy.values()).containsExactly(
                    RuleBinding.StickyKeyStrategy.CUSTOMER_ID,
                    RuleBinding.StickyKeyStrategy.ORDER_ID,
                    RuleBinding.StickyKeyStrategy.DEVICE_ID
            );
        }

        @Test
        @DisplayName("ObjectType should have all values")
        void objectTypeShouldHaveAllValues() {
            assertThat(RuleBinding.ObjectType.values()).hasSize(6);
        }
    }
}
