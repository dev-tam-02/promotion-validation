package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationResponse Tests")
class ValidationResponseTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {
        @Test
        @DisplayName("Should build response with all fields")
        void shouldBuildWithAllFields() {
            Instant now = Instant.now();
            ValidationResponse response = ValidationResponse.builder()
                    .transactionId("txn-1")
                    .valid(true)
                    .message("Validation passed")
                    .errorCode(null)
                    .timestamp(now)
                    .executionTimeMs(50L)
                    .rulesFired(List.of("rule-1", "rule-2"))
                    .metadata(Map.of("key", "value"))
                    .build();

            assertThat(response.getTransactionId()).isEqualTo("txn-1");
            assertThat(response.isValid()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Validation passed");
            assertThat(response.getErrorCode()).isNull();
            assertThat(response.getTimestamp()).isEqualTo(now);
            assertThat(response.getExecutionTimeMs()).isEqualTo(50L);
            assertThat(response.getRulesFired()).containsExactly("rule-1", "rule-2");
            assertThat(response.getMetadata()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("Should build invalid response")
        void shouldBuildInvalidResponse() {
            ValidationResponse response = ValidationResponse.builder()
                    .transactionId("txn-2")
                    .valid(false)
                    .errorCode("ERR_001")
                    .message("Validation failed")
                    .build();

            assertThat(response.isValid()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("ERR_001");
        }
    }

    @Nested
    @DisplayName("Setters")
    class SetterTests {
        @Test
        @DisplayName("Should allow mutation via setters")
        void shouldAllowMutation() {
            ValidationResponse response = new ValidationResponse();
            response.setTransactionId("txn-1");
            response.setValid(true);
            response.setMessage("OK");

            assertThat(response.getTransactionId()).isEqualTo("txn-1");
            assertThat(response.isValid()).isTrue();
            assertThat(response.getMessage()).isEqualTo("OK");
        }
    }
}
