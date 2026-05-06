package vn.viettel.vds.promotion.validation.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationRequestDto;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResponseDto;
import vn.viettel.vds.promotion.validation.adapter.in.web.mapper.ValidationWebMapper;
import vn.viettel.vds.promotion.validation.application.port.in.ValidateDataUseCase;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationController Tests")
class ValidationControllerTest {

    @Mock
    private ValidateDataUseCase validateDataUseCase;

    @Mock
    private ValidationWebMapper mapper;

    private ValidationController sut;

    @BeforeEach
    void setUp() {
        sut = new ValidationController(validateDataUseCase, mapper);
    }

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @Test
        @DisplayName("Should return OK with validation response")
        void shouldReturnOk() {
            // Given
            var requestDto = ValidationRequestDto.builder()
                    .transactionId("txn-1")
                    .build();
            var domainRequest = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .build();
            var result = ValidationResult.allow("txn-1");
            var responseDto = ValidationResponseDto.success("txn-1");

            when(mapper.toDomain(requestDto)).thenReturn(domainRequest);
            when(validateDataUseCase.validate(domainRequest)).thenReturn(result);
            when(mapper.toDto(result)).thenReturn(responseDto);

            // When
            var response = sut.validate(requestDto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDecision()).isEqualTo("ALLOW");
            verify(validateDataUseCase).validate(domainRequest);
        }
    }

    @Nested
    @DisplayName("fastCheck()")
    class FastCheckTests {

        @Test
        @DisplayName("Should return OK with fast check response")
        void shouldReturnOk() {
            // Given
            var requestDto = ValidationRequestDto.builder()
                    .promotionId("promo-1")
                    .build();
            var domainRequest = ValidationRequest.builder()
                    .promotionId("promo-1")
                    .build();
            var result = ValidationResult.allow("txn-1");
            var responseDto = ValidationResponseDto.success("txn-1");

            when(mapper.toDomain(requestDto)).thenReturn(domainRequest);
            when(validateDataUseCase.performFastCheck(domainRequest)).thenReturn(result);
            when(mapper.toDto(result)).thenReturn(responseDto);

            // When
            var response = sut.fastCheck(requestDto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(validateDataUseCase).performFastCheck(domainRequest);
        }
    }

    @Nested
    @DisplayName("fullValidation()")
    class FullValidationTests {

        @Test
        @DisplayName("Should return OK with full validation response")
        void shouldReturnOk() {
            // Given
            var requestDto = ValidationRequestDto.builder()
                    .transactionId("txn-1")
                    .build();
            var domainRequest = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .build();
            var result = ValidationResult.deny("txn-1", "FAILED", "Rule failed");
            var responseDto = ValidationResponseDto.deny("txn-1", "FAILED", "Rule failed");

            when(mapper.toDomain(requestDto)).thenReturn(domainRequest);
            when(validateDataUseCase.executeFullValidation(domainRequest)).thenReturn(result);
            when(mapper.toDto(result)).thenReturn(responseDto);

            // When
            var response = sut.fullValidation(requestDto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getDecision()).isEqualTo("DENY");
        }
    }

    @Nested
    @DisplayName("validateWithRuleSet()")
    class ValidateWithRuleSetTests {

        @Test
        @DisplayName("Should pass ruleSetId to use case")
        void shouldPassRuleSetId() {
            // Given
            var requestDto = ValidationRequestDto.builder()
                    .transactionId("txn-1")
                    .build();
            var domainRequest = ValidationRequest.builder()
                    .transactionId("txn-1")
                    .build();
            var result = ValidationResult.allow("txn-1");
            var responseDto = ValidationResponseDto.success("txn-1");

            when(mapper.toDomain(requestDto)).thenReturn(domainRequest);
            when(validateDataUseCase.validateWithRuleSet(domainRequest, "ruleset-1")).thenReturn(result);
            when(mapper.toDto(result)).thenReturn(responseDto);

            // When
            var response = sut.validateWithRuleSet("ruleset-1", requestDto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(validateDataUseCase).validateWithRuleSet(domainRequest, "ruleset-1");
        }
    }

    @Nested
    @DisplayName("batchValidate()")
    class BatchValidateTests {

        @Test
        @DisplayName("Should validate multiple requests and return map of responses")
        void shouldValidateBatch() {
            // Given
            var dto1 = ValidationRequestDto.builder().transactionId("txn-1").build();
            var dto2 = ValidationRequestDto.builder().transactionId("txn-2").build();
            var requests = Map.of("req-1", dto1, "req-2", dto2);

            var domainReq1 = ValidationRequest.builder().transactionId("txn-1").build();
            var domainReq2 = ValidationRequest.builder().transactionId("txn-2").build();

            var result1 = ValidationResult.allow("txn-1");
            var result2 = ValidationResult.deny("txn-2", "INVALID", "Invalid");

            var resp1 = ValidationResponseDto.success("txn-1");
            var resp2 = ValidationResponseDto.deny("txn-2", "INVALID", "Invalid");

            when(mapper.toDomain(dto1)).thenReturn(domainReq1);
            when(mapper.toDomain(dto2)).thenReturn(domainReq2);
            when(validateDataUseCase.validateBatch(any())).thenReturn(Map.of("req-1", result1, "req-2", result2));
            when(mapper.toDto(result1)).thenReturn(resp1);
            when(mapper.toDto(result2)).thenReturn(resp2);

            // When
            var response = sut.batchValidate(requests);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("health()")
    class HealthTests {

        @Test
        @DisplayName("Should return UP status")
        void shouldReturnUp() {
            var response = sut.health();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("status", "UP");
            assertThat(response.getBody()).containsEntry("service", "validation");
            assertThat(response.getBody()).containsKey("timestamp");
        }
    }
}
