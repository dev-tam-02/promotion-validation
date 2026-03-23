package vn.viettel.vds.promotion.validation.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationContext;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationDomainService Tests")
class ValidationDomainServiceTest {

    private ValidationDomainService sut;

    @BeforeEach
    void setUp() {
        sut = new ValidationDomainService();
    }

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @Test
        @DisplayName("Should deny when context is invalid")
        void shouldDeny_whenContextInvalid() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .timestamp(Instant.now())
                    .build();

            var result = sut.validate(request, Collections.emptyList());

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.DENY);
            assertThat(result.getReasonCodes()).contains("INVALID_CONTEXT");
        }

        @Test
        @DisplayName("Should deny when request is expired")
        void shouldDeny_whenExpired() {
            var request = TestFixtures.expiredRequest();

            var result = sut.validate(request, Collections.emptyList());

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.DENY);
            assertThat(result.getReasonCodes()).contains("REQUEST_EXPIRED");
        }

        @Test
        @DisplayName("Should allow when all rules pass")
        void shouldAllow_whenAllRulesPass() {
            var request = TestFixtures.validRequest();
            var rule = TestFixtures.activeRule("r-1", "RULE_1");

            var result = sut.validate(request, List.of(rule));

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.ALLOW);
        }

        @Test
        @DisplayName("Should allow when rules list is empty")
        void shouldAllow_whenNoRules() {
            var request = TestFixtures.validRequest();

            var result = sut.validate(request, Collections.emptyList());

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.ALLOW);
        }

        @Test
        @DisplayName("Should allow when rule is inactive")
        void shouldAllow_whenRuleInactive() {
            var request = TestFixtures.validRequest();
            var rule = TestFixtures.inactiveRule("r-1", "RULE_1");

            var result = sut.validate(request, List.of(rule));

            assertThat(result.getDecision()).isEqualTo(ValidationResult.Decision.ALLOW);
        }

        @Test
        @DisplayName("Should include processing time in result")
        void shouldIncludeProcessingTime() {
            var request = TestFixtures.validRequest();

            var result = sut.validate(request, Collections.emptyList());

            assertThat(result.getProcessingTimeMs()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("preValidateFastCheck()")
    class PreValidateFastCheckTests {

        @Test
        @DisplayName("Should allow when request is eligible for fast check")
        void shouldAllow_whenEligible() {
            var request = TestFixtures.validRequest();

            var result = sut.preValidateFastCheck(request);

            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("Should deny when not eligible for fast check")
        void shouldDeny_whenNotEligible() {
            var request = TestFixtures.highValueRequest();

            var result = sut.preValidateFastCheck(request);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getReasonCodes()).contains("NOT_ELIGIBLE");
        }

        @Test
        @DisplayName("Should deny when customer context is null")
        void shouldDeny_whenCustomerNull() {
            var context = ValidationContext.builder()
                    .requestTime(Instant.now())
                    .build();
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .timestamp(Instant.now())
                    .orderValue(BigDecimal.valueOf(50000))
                    .validationContext(context)
                    .build();

            var result = sut.preValidateFastCheck(request);

            assertThat(result.isDenied()).isTrue();
        }
    }

    @Nested
    @DisplayName("shouldBypassValidation()")
    class ShouldBypassValidationTests {

        @Test
        @DisplayName("Should return true when bypass_validation is true in context")
        void shouldReturnTrue_whenBypassEnabled() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .context(Map.of("bypass_validation", true))
                    .build();

            assertThat(sut.shouldBypassValidation(request)).isTrue();
        }

        @Test
        @DisplayName("Should return false when bypass_validation is false")
        void shouldReturnFalse_whenBypassDisabled() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .context(Map.of("bypass_validation", false))
                    .build();

            assertThat(sut.shouldBypassValidation(request)).isFalse();
        }

        @Test
        @DisplayName("Should return false when context map is null")
        void shouldReturnFalse_whenContextNull() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .build();

            assertThat(sut.shouldBypassValidation(request)).isFalse();
        }

        @Test
        @DisplayName("Should return false when bypass key not present")
        void shouldReturnFalse_whenKeyNotPresent() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .context(Map.of("other_key", "value"))
                    .build();

            assertThat(sut.shouldBypassValidation(request)).isFalse();
        }
    }

    @Nested
    @DisplayName("determineStrategy()")
    class DetermineStrategyTests {

        @Test
        @DisplayName("Should return STRICT for high value transaction")
        void shouldReturnStrict_forHighValue() {
            var request = TestFixtures.highValueRequest();

            assertThat(sut.determineStrategy(request)).isEqualTo(ValidationDomainService.ValidationStrategy.STRICT);
        }

        @Test
        @DisplayName("Should return FAST for fast-check eligible request")
        void shouldReturnFast_forFastCheckEligible() {
            var request = TestFixtures.validRequest();

            assertThat(sut.determineStrategy(request)).isEqualTo(ValidationDomainService.ValidationStrategy.FAST);
        }

        @Test
        @DisplayName("Should return STANDARD for regular request")
        void shouldReturnStandard_forRegularRequest() {
            var request = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .timestamp(Instant.now())
                    .build();

            assertThat(sut.determineStrategy(request)).isEqualTo(ValidationDomainService.ValidationStrategy.STANDARD);
        }
    }
}
