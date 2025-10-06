package vn.viettel.vds.promotion.validation.adapter.in.web.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationRequestDto;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResponseDto;
import vn.viettel.vds.promotion.validation.domain.model.ValidationContext;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.util.ArrayList;

/**
 * Mapper for converting between web DTOs and domain models
 */
@Component
public class ValidationWebMapper {

    /**
     * Map ValidationRequestDto to ValidationRequest domain model
     */
    public ValidationRequest toDomain(ValidationRequestDto dto) {
        if (dto == null) {
            return null;
        }

        // Build validation context
        ValidationContext validationContext = buildValidationContext(dto);

        return ValidationRequest.builder()
                .transactionId(dto.getTransactionId())
                .promotionId(dto.getPromotionId())
                .customerId(dto.getCustomerId())
                .sessionId(dto.getSessionId())
                .orderValue(dto.getOrderValue())
                .rules(dto.getRules())
                .context(dto.getContext())
                .timestamp(dto.getTimestamp())
                .validationContext(validationContext)
                .build();
    }

    /**
     * Map ValidationResult domain model to ValidationResponseDto
     */
    public ValidationResponseDto toDto(ValidationResult result) {
        if (result == null) {
            return null;
        }

        return ValidationResponseDto.builder()
                .validationId(result.getValidationId())
                .decision(result.getDecision().toString())
                .ok(result.isAllowed())
                .reasonCodes(result.getReasonCodes() != null ?
                        result.getReasonCodes() : new ArrayList<>())
                .explanations(result.getExplanations() != null ?
                        result.getExplanations() : new ArrayList<>())
                .reasonCode(result.getPrimaryReasonCode())
                .explanation(result.getPrimaryExplanation())
                .metadata(result.getMetadata())
                .timestamp(result.getTimestamp())
                .processingTimeMs(result.getProcessingTimeMs())
                .build();
    }

    /**
     * Build ValidationContext from DTO
     */
    private ValidationContext buildValidationContext(ValidationRequestDto dto) {
        ValidationContext.ValidationContextBuilder contextBuilder =
                ValidationContext.builder();

        // Build customer context if available
        if (dto.getCustomerContext() != null) {
            ValidationContext.CustomerContext customerContext =
                    ValidationContext.CustomerContext.builder()
                            .customerId(dto.getCustomerContext().getCustomerId())
                            .segment(dto.getCustomerContext().getSegment())
                            .tier(dto.getCustomerContext().getTier())
                            .totalPurchaseAmount(
                                    dto.getCustomerContext().getTotalPurchaseAmount())
                            .transactionCount(
                                    dto.getCustomerContext().getTransactionCount())
                            .build();
            contextBuilder.customer(customerContext);
        } else if (dto.getCustomerId() != null) {
            // Fallback to basic customer context
            ValidationContext.CustomerContext customerContext =
                    ValidationContext.CustomerContext.builder()
                            .customerId(dto.getCustomerId())
                            .build();
            contextBuilder.customer(customerContext);
        }

        // Build order context if available
        if (dto.getOrderContext() != null) {
            ValidationContext.OrderContext orderContext =
                    ValidationContext.OrderContext.builder()
                            .orderId(dto.getOrderContext().getOrderId())
                            .orderValue(dto.getOrderContext().getOrderValue())
                            .itemCount(dto.getOrderContext().getItemCount())
                            .channel(dto.getOrderContext().getChannel())
                            .productCategories(
                                    dto.getOrderContext().getProductCategories())
                            .build();
            contextBuilder.order(orderContext);
        } else if (dto.getOrderValue() != null) {
            // Fallback to basic order context
            ValidationContext.OrderContext orderContext =
                    ValidationContext.OrderContext.builder()
                            .orderValue(dto.getOrderValue())
                            .build();
            contextBuilder.order(orderContext);
        }

        contextBuilder.metadata(dto.getContext());
        contextBuilder.requestTime(dto.getTimestamp());

        return contextBuilder.build();
    }
}