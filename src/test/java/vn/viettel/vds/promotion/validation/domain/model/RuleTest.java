package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Rule Tests")
class RuleTest {

    @Nested
    @DisplayName("isActive()")
    class IsActiveTests {

        @Test
        @DisplayName("Should return true when active is true")
        void shouldReturnTrue_whenActive() {
            var rule = TestFixtures.activeRule("r-1", "RULE_1");
            assertThat(rule.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should return false when active is false")
        void shouldReturnFalse_whenInactive() {
            var rule = TestFixtures.inactiveRule("r-2", "RULE_2");
            assertThat(rule.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should return false when active is null")
        void shouldReturnFalse_whenNull() {
            var rule = Rule.builder().id("r-3").build();
            assertThat(rule.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("isEffective(Instant)")
    class IsEffectiveTests {

        @Test
        @DisplayName("Should return true when active, published, and within time range")
        void shouldReturnTrue_whenEffective() {
            var rule = Rule.builder()
                    .id("r-1")
                    .active(true)
                    .state(Rule.RuleState.PUBLISHED)
                    .effectiveFrom(Instant.parse("2020-01-01T00:00:00Z"))
                    .effectiveTo(Instant.parse("2030-12-31T23:59:59Z"))
                    .build();

            assertThat(rule.isEffective(Instant.now())).isTrue();
        }

        @Test
        @DisplayName("Should return false when not active")
        void shouldReturnFalse_whenNotActive() {
            var rule = Rule.builder()
                    .id("r-1")
                    .active(false)
                    .state(Rule.RuleState.PUBLISHED)
                    .build();

            assertThat(rule.isEffective(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Should return false when not PUBLISHED state")
        void shouldReturnFalse_whenNotPublished() {
            var rule = Rule.builder()
                    .id("r-1")
                    .active(true)
                    .state(Rule.RuleState.DRAFT)
                    .build();

            assertThat(rule.isEffective(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Should return false when before effectiveFrom")
        void shouldReturnFalse_whenBeforeEffectiveFrom() {
            var rule = Rule.builder()
                    .id("r-1")
                    .active(true)
                    .state(Rule.RuleState.PUBLISHED)
                    .effectiveFrom(Instant.parse("2030-01-01T00:00:00Z"))
                    .build();

            assertThat(rule.isEffective(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Should return false when after effectiveTo")
        void shouldReturnFalse_whenAfterEffectiveTo() {
            var rule = Rule.builder()
                    .id("r-1")
                    .active(true)
                    .state(Rule.RuleState.PUBLISHED)
                    .effectiveTo(Instant.parse("2020-01-01T00:00:00Z"))
                    .build();

            assertThat(rule.isEffective(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Should return true when effectiveFrom and effectiveTo are null")
        void shouldReturnTrue_whenNoTimeConstraints() {
            var rule = Rule.builder()
                    .id("r-1")
                    .active(true)
                    .state(Rule.RuleState.PUBLISHED)
                    .build();

            assertThat(rule.isEffective(Instant.now())).isTrue();
        }

        @Test
        @DisplayName("Should use current time when checkTime is null")
        void shouldUseCurrentTime_whenCheckTimeNull() {
            var rule = Rule.builder()
                    .id("r-1")
                    .active(true)
                    .state(Rule.RuleState.PUBLISHED)
                    .build();

            assertThat(rule.isEffective(null)).isTrue();
        }
    }

    @Nested
    @DisplayName("appliesTo()")
    class AppliesToTests {

        @Test
        @DisplayName("Should return true when no target segments defined")
        void shouldReturnTrue_whenNoSegments() {
            var rule = TestFixtures.activeRule("r-1", "RULE_1");
            assertThat(rule.appliesTo("ANY_SEGMENT")).isTrue();
        }

        @Test
        @DisplayName("Should return true when segment is in target segments")
        void shouldReturnTrue_whenSegmentMatches() {
            var rule = TestFixtures.ruleWithSegments("r-1", "RULE_1", Set.of("VIP", "GOLD"));
            assertThat(rule.appliesTo("VIP")).isTrue();
        }

        @Test
        @DisplayName("Should return false when segment is not in target segments")
        void shouldReturnFalse_whenSegmentNotMatch() {
            var rule = TestFixtures.ruleWithSegments("r-1", "RULE_1", Set.of("VIP", "GOLD"));
            assertThat(rule.appliesTo("SILVER")).isFalse();
        }
    }

    @Nested
    @DisplayName("publish()")
    class PublishTests {

        @Test
        @DisplayName("Should set state to PUBLISHED and active to true")
        void shouldPublish() {
            var rule = Rule.builder()
                    .id("r-1")
                    .state(Rule.RuleState.DRAFT)
                    .active(false)
                    .build();

            rule.publish("publisher");

            assertThat(rule.getState()).isEqualTo(Rule.RuleState.PUBLISHED);
            assertThat(rule.isActive()).isTrue();
            assertThat(rule.getPublishedBy()).isEqualTo("publisher");
            assertThat(rule.getPublishedAt()).isNotNull();
            assertThat(rule.getUpdatedBy()).isEqualTo("publisher");
            assertThat(rule.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("archive()")
    class ArchiveTests {

        @Test
        @DisplayName("Should set state to ARCHIVED and active to false")
        void shouldArchive() {
            var rule = TestFixtures.activeRule("r-1", "RULE_1");

            rule.archive("admin");

            assertThat(rule.getState()).isEqualTo(Rule.RuleState.ARCHIVED);
            assertThat(rule.isActive()).isFalse();
            assertThat(rule.getUpdatedBy()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("deprecate()")
    class DeprecateTests {

        @Test
        @DisplayName("Should set state to DEPRECATED and active to false")
        void shouldDeprecate() {
            var rule = TestFixtures.activeRule("r-1", "RULE_1");

            rule.deprecate("admin");

            assertThat(rule.getState()).isEqualTo(Rule.RuleState.DEPRECATED);
            assertThat(rule.isActive()).isFalse();
            assertThat(rule.getUpdatedBy()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("getStatus()")
    class GetStatusTests {

        @Test
        @DisplayName("Should return state name")
        void shouldReturnStateName() {
            var rule = TestFixtures.activeRule("r-1", "RULE_1");
            assertThat(rule.getStatus()).isEqualTo("PUBLISHED");
        }

        @Test
        @DisplayName("Should return null when state is null")
        void shouldReturnNull_whenStateNull() {
            var rule = Rule.builder().id("r-1").build();
            assertThat(rule.getStatus()).isNull();
        }
    }

    @Nested
    @DisplayName("getRuleType()")
    class GetRuleTypeTests {

        @Test
        @DisplayName("Should return CUSTOM when type is null")
        void shouldReturnCustom_whenTypeNull() {
            var rule = Rule.builder().id("r-1").build();
            assertThat(rule.getRuleType()).isEqualTo(Rule.RuleType.CUSTOM);
        }

        @Test
        @DisplayName("Should return matching enum for valid type")
        void shouldReturnMatchingEnum() {
            var rule = Rule.builder().id("r-1").type("ELIGIBILITY").build();
            assertThat(rule.getRuleType()).isEqualTo(Rule.RuleType.ELIGIBILITY);
        }

        @Test
        @DisplayName("Should return CUSTOM for invalid type")
        void shouldReturnCustom_forInvalidType() {
            var rule = Rule.builder().id("r-1").type("INVALID_TYPE").build();
            assertThat(rule.getRuleType()).isEqualTo(Rule.RuleType.CUSTOM);
        }

        @Test
        @DisplayName("Should handle case insensitive type")
        void shouldHandleCaseInsensitive() {
            var rule = Rule.builder().id("r-1").type("required").build();
            assertThat(rule.getRuleType()).isEqualTo(Rule.RuleType.REQUIRED);
        }
    }

    @Nested
    @DisplayName("evaluate()")
    class EvaluateTests {

        @Test
        @DisplayName("Should throw UnsupportedOperationException")
        void shouldThrow() {
            var rule = TestFixtures.activeRule("r-1", "RULE_1");
            assertThatThrownBy(() -> rule.evaluate(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("UsageLimits")
    class UsageLimitsTests {

        @Test
        @DisplayName("Should detect limit reached when remaining is zero")
        void shouldDetectLimitReached() {
            var limits = Rule.UsageLimits.builder()
                    .perCodeTotal(100)
                    .remaining(0)
                    .build();

            assertThat(limits.hasReachedLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return false when remaining is positive")
        void shouldReturnFalse_whenRemainingPositive() {
            var limits = Rule.UsageLimits.builder()
                    .perCodeTotal(100)
                    .remaining(50)
                    .build();

            assertThat(limits.hasReachedLimit()).isFalse();
        }

        @Test
        @DisplayName("Should return false when perCodeTotal is null")
        void shouldReturnFalse_whenPerCodeTotalNull() {
            var limits = Rule.UsageLimits.builder()
                    .remaining(50)
                    .build();

            assertThat(limits.hasReachedLimit()).isFalse();
        }

        @Test
        @DisplayName("Should return false when remaining is null")
        void shouldReturnFalse_whenRemainingNull() {
            var limits = Rule.UsageLimits.builder()
                    .perCodeTotal(100)
                    .build();

            assertThat(limits.hasReachedLimit()).isFalse();
        }

        @Test
        @DisplayName("Should decrement remaining")
        void shouldDecrementRemaining() {
            var limits = Rule.UsageLimits.builder()
                    .remaining(5)
                    .build();

            limits.decrementRemaining();

            assertThat(limits.getRemaining()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should not decrement below zero")
        void shouldNotDecrementBelowZero() {
            var limits = Rule.UsageLimits.builder()
                    .remaining(0)
                    .build();

            limits.decrementRemaining();

            assertThat(limits.getRemaining()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should not decrement when remaining is null")
        void shouldNotDecrement_whenNull() {
            var limits = Rule.UsageLimits.builder().build();

            limits.decrementRemaining();

            assertThat(limits.getRemaining()).isNull();
        }
    }
}
