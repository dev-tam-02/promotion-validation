package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityRule;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ObjectType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommandMappingService}.
 * Verifies applicability statistics calculation from command data.
 */
@DisplayName("CommandMappingService Tests")
class CommandMappingServiceTest {

    private CommandMappingService sut;

    @BeforeEach
    void setUp() {
        sut = new CommandMappingService();
    }

    @Nested
    @DisplayName("calculateApplicabilityStats()")
    class CalculateApplicabilityStatsTests {

        @Test
        @DisplayName("Should return zeros when input is null")
        void shouldReturnZerosForNull() {
            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(null);

            // Then
            assertThat(stats.getIncludedItemsCount()).isZero();
            assertThat(stats.getExcludedItemsCount()).isZero();
            assertThat(stats.isIncludedAll()).isFalse();
        }

        @Test
        @DisplayName("Should count included items correctly")
        void shouldCountIncludedItems() {
            // Given
            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(List.of(
                            ApplicabilityRule.builder()
                                    .object(ObjectType.PRODUCT)
                                    .id("p-1")
                                    .build(),
                            ApplicabilityRule.builder()
                                    .object(ObjectType.PRODUCT)
                                    .id("p-2")
                                    .build(),
                            ApplicabilityRule.builder()
                                    .object(ObjectType.COLLECTION)
                                    .id("c-1")
                                    .build()
                    ))
                    .excluded(List.of())
                    .includedAll(false)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isEqualTo(3);
            assertThat(stats.getExcludedItemsCount()).isZero();
            assertThat(stats.isIncludedAll()).isFalse();
        }

        @Test
        @DisplayName("Should count excluded items correctly")
        void shouldCountExcludedItems() {
            // Given
            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(List.of())
                    .excluded(List.of(
                            ApplicabilityRule.builder()
                                    .object(ObjectType.PRODUCT)
                                    .id("p-1")
                                    .build(),
                            ApplicabilityRule.builder()
                                    .object(ObjectType.SKU)
                                    .id("s-1")
                                    .build()
                    ))
                    .includedAll(false)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isZero();
            assertThat(stats.getExcludedItemsCount()).isEqualTo(2);
            assertThat(stats.isIncludedAll()).isFalse();
        }

        @Test
        @DisplayName("Should count both included and excluded items")
        void shouldCountBothIncludedAndExcluded() {
            // Given
            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(List.of(
                            ApplicabilityRule.builder()
                                    .object(ObjectType.COLLECTION)
                                    .id("c-1")
                                    .build(),
                            ApplicabilityRule.builder()
                                    .object(ObjectType.COLLECTION)
                                    .id("c-2")
                                    .build()
                    ))
                    .excluded(List.of(
                            ApplicabilityRule.builder()
                                    .object(ObjectType.PRODUCT)
                                    .id("p-1")
                                    .build()
                    ))
                    .includedAll(false)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isEqualTo(2);
            assertThat(stats.getExcludedItemsCount()).isEqualTo(1);
            assertThat(stats.isIncludedAll()).isFalse();
        }

        @Test
        @DisplayName("Should detect includedAll flag")
        void shouldDetectIncludedAllFlag() {
            // Given
            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(List.of())
                    .excluded(List.of())
                    .includedAll(true)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isZero();
            assertThat(stats.getExcludedItemsCount()).isZero();
            assertThat(stats.isIncludedAll()).isTrue();
        }

        @Test
        @DisplayName("Should handle includedAll with excluded items")
        void shouldHandleIncludedAllWithExcluded() {
            // Given
            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(List.of())
                    .excluded(List.of(
                            ApplicabilityRule.builder()
                                    .object(ObjectType.PRODUCT)
                                    .id("p-1")
                                    .build()
                    ))
                    .includedAll(true)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isZero();
            assertThat(stats.getExcludedItemsCount()).isEqualTo(1);
            assertThat(stats.isIncludedAll()).isTrue();
        }

        @Test
        @DisplayName("Should handle null included list")
        void shouldHandleNullIncludedList() {
            // Given
            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(null)
                    .excluded(List.of(
                            ApplicabilityRule.builder()
                                    .object(ObjectType.PRODUCT)
                                    .id("p-1")
                                    .build()
                    ))
                    .includedAll(false)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isZero();
            assertThat(stats.getExcludedItemsCount()).isEqualTo(1);
            assertThat(stats.isIncludedAll()).isFalse();
        }

        @Test
        @DisplayName("Should handle null excluded list")
        void shouldHandleNullExcludedList() {
            // Given
            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(List.of(
                            ApplicabilityRule.builder()
                                    .object(ObjectType.PRODUCT)
                                    .id("p-1")
                                    .build()
                    ))
                    .excluded(null)
                    .includedAll(false)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isEqualTo(1);
            assertThat(stats.getExcludedItemsCount()).isZero();
            assertThat(stats.isIncludedAll()).isFalse();
        }

        @Test
        @DisplayName("Should handle null includedAll flag")
        void shouldHandleNullIncludedAllFlag() {
            // Given
            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(List.of())
                    .excluded(List.of())
                    .includedAll(null)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isZero();
            assertThat(stats.getExcludedItemsCount()).isZero();
            assertThat(stats.isIncludedAll()).isFalse();
        }

        @Test
        @DisplayName("Should handle empty scope")
        void shouldHandleEmptyScope() {
            // Given
            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(List.of())
                    .excluded(List.of())
                    .includedAll(false)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isZero();
            assertThat(stats.getExcludedItemsCount()).isZero();
            assertThat(stats.isIncludedAll()).isFalse();
        }

        @Test
        @DisplayName("Should handle large number of items")
        void shouldHandleLargeNumberOfItems() {
            // Given
            List<ApplicabilityRule> includedRules = List.of(
                    ApplicabilityRule.builder().object(ObjectType.PRODUCT).id("p-1").build(),
                    ApplicabilityRule.builder().object(ObjectType.PRODUCT).id("p-2").build(),
                    ApplicabilityRule.builder().object(ObjectType.PRODUCT).id("p-3").build(),
                    ApplicabilityRule.builder().object(ObjectType.PRODUCT).id("p-4").build(),
                    ApplicabilityRule.builder().object(ObjectType.PRODUCT).id("p-5").build()
            );
            List<ApplicabilityRule> excludedRules = List.of(
                    ApplicabilityRule.builder().object(ObjectType.SKU).id("s-1").build(),
                    ApplicabilityRule.builder().object(ObjectType.SKU).id("s-2").build()
            );

            ApplicabilityScope scope = ApplicabilityScope.builder()
                    .included(includedRules)
                    .excluded(excludedRules)
                    .includedAll(false)
                    .build();

            // When
            CommandMappingService.ApplicabilityStats stats = sut.calculateApplicabilityStats(scope);

            // Then
            assertThat(stats.getIncludedItemsCount()).isEqualTo(5);
            assertThat(stats.getExcludedItemsCount()).isEqualTo(2);
            assertThat(stats.isIncludedAll()).isFalse();
        }
    }

    @Nested
    @DisplayName("ApplicabilityStats")
    class ApplicabilityStatsTests {

        @Test
        @DisplayName("Should create stats with values")
        void shouldCreateStats() {
            // When
            CommandMappingService.ApplicabilityStats stats =
                    new CommandMappingService.ApplicabilityStats(5, 3, true);

            // Then
            assertThat(stats.getIncludedItemsCount()).isEqualTo(5);
            assertThat(stats.getExcludedItemsCount()).isEqualTo(3);
            assertThat(stats.isIncludedAll()).isTrue();
        }

        @Test
        @DisplayName("Should format toString correctly")
        void shouldFormatToString() {
            // Given
            CommandMappingService.ApplicabilityStats stats =
                    new CommandMappingService.ApplicabilityStats(2, 1, false);

            // When
            String string = stats.toString();

            // Then
            assertThat(string)
                    .contains("included=2")
                    .contains("excluded=1")
                    .contains("includedAll=false");
        }

        @Test
        @DisplayName("Should handle zero values")
        void shouldHandleZeroValues() {
            // When
            CommandMappingService.ApplicabilityStats stats =
                    new CommandMappingService.ApplicabilityStats(0, 0, false);

            // Then
            assertThat(stats.getIncludedItemsCount()).isZero();
            assertThat(stats.getExcludedItemsCount()).isZero();
            assertThat(stats.isIncludedAll()).isFalse();
        }

        @Test
        @DisplayName("Should handle includedAll with counts")
        void shouldHandleIncludedAllWithCounts() {
            // When
            CommandMappingService.ApplicabilityStats stats =
                    new CommandMappingService.ApplicabilityStats(0, 5, true);

            // Then
            assertThat(stats.getIncludedItemsCount()).isZero();
            assertThat(stats.getExcludedItemsCount()).isEqualTo(5);
            assertThat(stats.isIncludedAll()).isTrue();
        }

        @Test
        @DisplayName("Should be immutable value object")
        void shouldBeImmutableValueObject() {
            // Given
            CommandMappingService.ApplicabilityStats stats1 =
                    new CommandMappingService.ApplicabilityStats(5, 3, true);
            CommandMappingService.ApplicabilityStats stats2 =
                    new CommandMappingService.ApplicabilityStats(5, 3, true);

            // Then
            assertThat(stats1.getIncludedItemsCount()).isEqualTo(stats2.getIncludedItemsCount());
            assertThat(stats1.getExcludedItemsCount()).isEqualTo(stats2.getExcludedItemsCount());
            assertThat(stats1.isIncludedAll()).isEqualTo(stats2.isIncludedAll());
        }
    }
}
