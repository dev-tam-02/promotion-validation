package vn.viettel.vds.promotion.validation.adapter.in.web.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationRequestDto;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationWebMapper Tests")
class ValidationWebMapperTest {

    private ValidationWebMapper sut;

    @BeforeEach
    void setUp() {
        sut = new ValidationWebMapper();
    }

    @Nested
    @DisplayName("toDomain()")
    class ToDomainTests {

        @Test
        @DisplayName("Should return null when dto is null")
        void shouldReturnNull_whenDtoNull() {
            assertThat(sut.toDomain(null)).isNull();
        }

        @Test
        @DisplayName("Should map basic fields")
        void shouldMapBasicFields() {
            var dto = ValidationRequestDto.builder()
                    .transactionId("txn-1")
                    .promotionId("promo-1")
                    .customerId("cust-1")
                    .sessionId("sess-1")
                    .orderValue(BigDecimal.valueOf(50000))
                    .rules(List.of("RULE_1", "RULE_2"))
                    .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            var result = sut.toDomain(dto);

            assertThat(result.getTransactionId()).isEqualTo("txn-1");
            assertThat(result.getPromotionId()).isEqualTo("promo-1");
            assertThat(result.getCustomerId()).isEqualTo("cust-1");
            assertThat(result.getSessionId()).isEqualTo("sess-1");
            assertThat(result.getOrderValue()).isEqualByComparingTo(BigDecimal.valueOf(50000));
            assertThat(result.getRules()).containsExactly("RULE_1", "RULE_2");
        }

        @Test
        @DisplayName("Should map customer context from customerContext DTO")
        void shouldMapCustomerContext() {
            var customerCtx = ValidationRequestDto.CustomerContextDto.builder()
                    .customerId("cust-1")
                    .segment("VIP")
                    .tier("GOLD")
                    .totalPurchaseAmount(BigDecimal.valueOf(500000))
                    .transactionCount(10)
                    .build();

            var dto = ValidationRequestDto.builder()
                    .transactionId("txn-1")
                    .customerContext(customerCtx)
                    .build();

            var result = sut.toDomain(dto);

            assertThat(result.getValidationContext()).isNotNull();
            assertThat(result.getValidationContext().getCustomer()).isNotNull();
            assertThat(result.getValidationContext().getCustomer().getCustomerId()).isEqualTo("cust-1");
            assertThat(result.getValidationContext().getCustomer().getSegment()).isEqualTo("VIP");
            assertThat(result.getValidationContext().getCustomer().getTier()).isEqualTo("GOLD");
        }

        @Test
        @DisplayName("Should fallback to customerId when customerContext is null")
        void shouldFallbackToCustomerId() {
            var dto = ValidationRequestDto.builder()
                    .transactionId("txn-1")
                    .customerId("cust-fallback")
                    .build();

            var result = sut.toDomain(dto);

            assertThat(result.getValidationContext().getCustomer()).isNotNull();
            assertThat(result.getValidationContext().getCustomer().getCustomerId()).isEqualTo("cust-fallback");
        }

        @Test
        @DisplayName("Should map order context from orderContext DTO")
        void shouldMapOrderContext() {
            var orderCtx = ValidationRequestDto.OrderContextDto.builder()
                    .orderId("ord-1")
                    .orderValue(BigDecimal.valueOf(100000))
                    .itemCount(5)
                    .channel("ONLINE")
                    .build();

            var dto = ValidationRequestDto.builder()
                    .transactionId("txn-1")
                    .orderContext(orderCtx)
                    .build();

            var result = sut.toDomain(dto);

            assertThat(result.getValidationContext().getOrder()).isNotNull();
            assertThat(result.getValidationContext().getOrder().getOrderId()).isEqualTo("ord-1");
            assertThat(result.getValidationContext().getOrder().getChannel()).isEqualTo("ONLINE");
        }

        @Test
        @DisplayName("Should fallback to orderValue when orderContext is null")
        void shouldFallbackToOrderValue() {
            var dto = ValidationRequestDto.builder()
                    .transactionId("txn-1")
                    .orderValue(BigDecimal.valueOf(75000))
                    .build();

            var result = sut.toDomain(dto);

            assertThat(result.getValidationContext().getOrder()).isNotNull();
            assertThat(result.getValidationContext().getOrder().getOrderValue())
                    .isEqualByComparingTo(BigDecimal.valueOf(75000));
        }

        @Test
        @DisplayName("Should not create customer or order context when neither provided")
        void shouldNotCreateContext_whenNothingProvided() {
            var dto = ValidationRequestDto.builder()
                    .transactionId("txn-1")
                    .build();

            var result = sut.toDomain(dto);

            assertThat(result.getValidationContext()).isNotNull();
            assertThat(result.getValidationContext().getCustomer()).isNull();
            assertThat(result.getValidationContext().getOrder()).isNull();
        }
    }

    @Nested
    @DisplayName("toDto()")
    class ToDtoTests {

        @Test
        @DisplayName("Should return null when result is null")
        void shouldReturnNull_whenResultNull() {
            assertThat(sut.toDto(null)).isNull();
        }

        @Test
        @DisplayName("Should map ALLOW result to DTO")
        void shouldMapAllowResult() {
            var result = ValidationResult.allow("val-1");

            var dto = sut.toDto(result);

            assertThat(dto.getValidationId()).isEqualTo("val-1");
            assertThat(dto.getDecision()).isEqualTo("ALLOW");
            assertThat(dto.getOk()).isTrue();
            assertThat(dto.getReasonCodes()).isEmpty();
        }

        @Test
        @DisplayName("Should map DENY result to DTO with reason codes")
        void shouldMapDenyResult() {
            var result = ValidationResult.deny("val-1", "INVALID", "Input is invalid");

            var dto = sut.toDto(result);

            assertThat(dto.getDecision()).isEqualTo("DENY");
            assertThat(dto.getOk()).isFalse();
            assertThat(dto.getReasonCodes()).containsExactly("INVALID");
            assertThat(dto.getExplanations()).containsExactly("Input is invalid");
            assertThat(dto.getReasonCode()).isEqualTo("INVALID");
            assertThat(dto.getExplanation()).isEqualTo("Input is invalid");
        }

        @Test
        @DisplayName("Should map ERROR result to DTO")
        void shouldMapErrorResult() {
            var result = ValidationResult.error("val-1", "Something broke");

            var dto = sut.toDto(result);

            assertThat(dto.getDecision()).isEqualTo("ERROR");
            assertThat(dto.getOk()).isFalse();
        }

        @Test
        @DisplayName("Should handle null reason codes and explanations")
        void shouldHandleNullReasonCodes() {
            var result = ValidationResult.builder()
                    .validationId("val-1")
                    .decision(ValidationResult.Decision.ALLOW)
                    .timestamp(Instant.now())
                    .build();

            var dto = sut.toDto(result);

            assertThat(dto.getReasonCodes()).isEmpty();
            assertThat(dto.getExplanations()).isEmpty();
        }
    }
}
