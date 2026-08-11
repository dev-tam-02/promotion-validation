package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.application.port.out.RuleContextPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleContextOption;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RuleContextService}.
 * Verifies the service correctly delegates to persistence port for listing contexts
 * and checking context activation status.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleContextService Tests")
class RuleContextServiceTest {

    @Mock
    private RuleContextPersistencePort ruleContextPort;

    private RuleContextService sut;

    @BeforeEach
    void setUp() {
        sut = new RuleContextService(ruleContextPort);
    }

    @Nested
    @DisplayName("listContexts()")
    class ListContextsTests {

        @Test
        @DisplayName("Should return active contexts from port")
        void shouldReturnActiveContexts() {
            // Given
            List<RuleContextOption> contexts = List.of(
                    RuleContextOption.builder()
                            .code("GENERAL_USAGE")
                            .status("ACTIVE")
                            .names(Map.of("vi", "Sử dụng chung", "en", "General Usage"))
                            .build(),
                    RuleContextOption.builder()
                            .code("PROMOTION_ELIGIBILITY")
                            .status("ACTIVE")
                            .names(Map.of("vi", "Điều kiện khuyến mại", "en", "Promotion Eligibility"))
                            .build()
            );
            when(ruleContextPort.findActiveContexts()).thenReturn(contexts);

            // When
            List<RuleContextOption> result = sut.listContexts();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCode()).isEqualTo("GENERAL_USAGE");
            assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
            assertThat(result.get(0).getNames()).containsEntry("vi", "Sử dụng chung");
            assertThat(result.get(1).getCode()).isEqualTo("PROMOTION_ELIGIBILITY");
            verify(ruleContextPort).findActiveContexts();
        }

        @Test
        @DisplayName("Should return empty list when no contexts")
        void shouldReturnEmptyList() {
            // Given
            when(ruleContextPort.findActiveContexts()).thenReturn(List.of());

            // When
            List<RuleContextOption> result = sut.listContexts();

            // Then
            assertThat(result).isEmpty();
            verify(ruleContextPort).findActiveContexts();
        }

        @Test
        @DisplayName("Should return single context when only one active")
        void shouldReturnSingleContext() {
            // Given
            List<RuleContextOption> contexts = List.of(
                    RuleContextOption.builder()
                            .code("GENERAL_USAGE")
                            .status("ACTIVE")
                            .names(Map.of("vi", "Sử dụng chung"))
                            .build()
            );
            when(ruleContextPort.findActiveContexts()).thenReturn(contexts);

            // When
            List<RuleContextOption> result = sut.listContexts();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("GENERAL_USAGE");
        }
    }

    @Nested
    @DisplayName("isActiveContext()")
    class IsActiveContextTests {

        @Test
        @DisplayName("Should return true for active context")
        void shouldReturnTrueForActive() {
            // Given
            when(ruleContextPort.isActiveContext("GENERAL_USAGE")).thenReturn(true);

            // When
            boolean result = sut.isActiveContext("GENERAL_USAGE");

            // Then
            assertThat(result).isTrue();
            verify(ruleContextPort).isActiveContext("GENERAL_USAGE");
        }

        @ParameterizedTest(name = "Should return false for context code: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"UNKNOWN"})
        void shouldReturnFalseForInvalidContext(String contextCode) {
            // Given
            when(ruleContextPort.isActiveContext(contextCode)).thenReturn(false);

            // When
            boolean result = sut.isActiveContext(contextCode);

            // Then
            assertThat(result).isFalse();
            verify(ruleContextPort).isActiveContext(contextCode);
        }
    }
}
