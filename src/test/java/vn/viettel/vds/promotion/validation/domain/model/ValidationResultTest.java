package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationResult Tests")
class ValidationResultTest {

    @Nested
    @DisplayName("allow()")
    class AllowTests {

        @Test
        @DisplayName("Should create ALLOW result with empty reason codes")
        void shouldCreateAllowResult() {
            var result = ValidationResult.allow("val-1");

            assertThat(result.getValidationId()).isEqualTo("val-1");
            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.ALLOW);
            assertThat(result.getReasonCodes()).isEmpty();
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.isAllowed()).isTrue();
            assertThat(result.isDenied()).isFalse();
        }
    }

    @Nested
    @DisplayName("deny()")
    class DenyTests {

        @Test
        @DisplayName("Should create DENY result with reason code and explanation")
        void shouldCreateDenyResult() {
            var result = ValidationResult.deny("val-2", "INVALID_INPUT", "Input is invalid");

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.DENY);
            assertThat(result.getReasonCodes()).containsExactly("INVALID_INPUT");
            assertThat(result.getExplanations()).containsExactly("Input is invalid");
            assertThat(result.isDenied()).isTrue();
            assertThat(result.isAllowed()).isFalse();
        }
    }

    @Nested
    @DisplayName("error()")
    class ErrorTests {

        @Test
        @DisplayName("Should create ERROR result")
        void shouldCreateErrorResult() {
            var result = ValidationResult.error("val-3", "Something went wrong");

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.ERROR);
            assertThat(result.getReasonCodes()).containsExactly("VALIDATION_ERROR");
            assertThat(result.hasError()).isTrue();
        }
    }

    @Nested
    @DisplayName("skipped()")
    class SkippedTests {

        @Test
        @DisplayName("Should create PENDING result with SKIPPED reason code")
        void shouldCreateSkippedResult() {
            var result = ValidationResult.skipped("rule-1", "Rule not published");

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.PENDING);
            assertThat(result.getReasonCodes()).containsExactly("SKIPPED");
            assertThat(result.isPending()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Rule not published");
        }
    }

    @Nested
    @DisplayName("passed()")
    class PassedTests {

        @Test
        @DisplayName("Should create ALLOW result with message")
        void shouldCreatePassedResult() {
            var result = ValidationResult.passed("rule-1", "Rule passed");

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.ALLOW);
            assertThat(result.isAllowed()).isTrue();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Rule passed");
        }
    }

    @Nested
    @DisplayName("failed()")
    class FailedTests {

        @Test
        @DisplayName("Should create DENY result with VALIDATION_FAILED reason")
        void shouldCreateFailedResult() {
            var result = ValidationResult.failed("rule-1", "Rule failed");

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.DENY);
            assertThat(result.isDenied()).isTrue();
            assertThat(result.getReasonCodes()).containsExactly("VALIDATION_FAILED");
        }
    }

    @Nested
    @DisplayName("getPrimaryReasonCode()")
    class PrimaryReasonCodeTests {

        @Test
        @DisplayName("Should return first reason code")
        void shouldReturnFirstReasonCode() {
            var result = ValidationResult.deny("val-1", "CODE_1", "Explanation");
            assertThat(result.getPrimaryReasonCode()).isEqualTo("CODE_1");
        }

        @Test
        @DisplayName("Should return null when no reason codes")
        void shouldReturnNull_whenNoReasonCodes() {
            var result = ValidationResult.allow("val-1");
            assertThat(result.getPrimaryReasonCode()).isNull();
        }
    }

    @Nested
    @DisplayName("getPrimaryExplanation()")
    class PrimaryExplanationTests {

        @Test
        @DisplayName("Should return first explanation")
        void shouldReturnFirstExplanation() {
            var result = ValidationResult.deny("val-1", "CODE", "First explanation");
            assertThat(result.getPrimaryExplanation()).isEqualTo("First explanation");
        }

        @Test
        @DisplayName("Should return null when no explanations")
        void shouldReturnNull_whenNoExplanations() {
            var result = ValidationResult.builder()
                    .validationId("val-1")
                    .decision(ValidationResult.Decision.ALLOW)
                    .build();
            assertThat(result.getPrimaryExplanation()).isNull();
        }
    }

    @Nested
    @DisplayName("hasReasonCode()")
    class HasReasonCodeTests {

        @Test
        @DisplayName("Should return true when reason code exists")
        void shouldReturnTrue_whenExists() {
            var result = ValidationResult.deny("val-1", "MY_CODE", "explanation");
            assertThat(result.hasReasonCode("MY_CODE")).isTrue();
        }

        @Test
        @DisplayName("Should return false when reason code does not exist")
        void shouldReturnFalse_whenNotExists() {
            var result = ValidationResult.allow("val-1");
            assertThat(result.hasReasonCode("ANYTHING")).isFalse();
        }
    }
}
