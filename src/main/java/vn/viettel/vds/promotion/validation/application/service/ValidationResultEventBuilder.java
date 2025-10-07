package vn.viettel.vds.promotion.validation.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.port.in.command.ValidateStackableDiscountCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.ValidateStackableDiscountResult;
import vn.viettel.vds.promotion.schema.redemption.event.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builder for constructing Avro validation result events.
 * <p>
 * This component converts application DTOs into Avro schema objects
 * for publishing to Kafka.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class ValidationResultEventBuilder {

    /**
     * Builds validation result event from command and result.
     *
     * @param command original validation command
     * @param result validation result
     * @return Avro result event
     */
    public ValidateStackableDiscountResultEvent buildResultEvent(
            ValidateStackableDiscountCommand command,
            ValidateStackableDiscountResult result
    ) {
        log.debug("Building validation result event: idempotencyKey={}, decision={}",
                command.idempotencyKey(), result.decision());

        return ValidateStackableDiscountResultEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setAggregate("Redemption")
                .setType("ValidateStackableDiscountResultEvent")
                .setSource("validation-service")
                .setSubject(command.orderInfo().orderId())
                .setOccurredAt(Instant.ofEpochMilli(System.currentTimeMillis()))
                .setVersion(1)
                .setPayload(buildPayload(command, result))
                .setMetadata(buildMetadata(command, result))
                .build();
    }

    /**
     * Builds event payload.
     * Using simplified structure based on actual Avro schema.
     */
    private ValidateStackableDiscountResultPayload buildPayload(
            ValidateStackableDiscountCommand command,
            ValidateStackableDiscountResult result
    ) {
        // Get currency from order info
        String currency = command.orderInfo().currency();

        // Build Money objects for amounts
        vn.viettel.vds.promotion.schema.common.Money totalDiscount =
                vn.viettel.vds.promotion.schema.common.Money.newBuilder()
                        .setAmount(result.totalDiscountAmount().toString())
                        .setCurrencyCode(currency)
                        .build();

        vn.viettel.vds.promotion.schema.common.Money finalAmount =
                vn.viettel.vds.promotion.schema.common.Money.newBuilder()
                        .setAmount(result.finalOrderAmount().toString())
                        .setCurrencyCode(currency)
                        .build();

        // Build validation summary
        ValidationSummary validationSummary = ValidationSummary.newBuilder()
                .setOverallValid(result.isApproved())
                .setCanStack(result.isApproved() || result.isPartial())
                .setTotalDiscountAmount(totalDiscount)
                .setFinalAmount(finalAmount)
                .setEffectiveDiscountRate(calculateDiscountRate(result))
                .setValidationSummary(buildSummaryText(result))
                .build();

        // Build stacking analysis (simplified)
        StackingAnalysis stackingAnalysis = StackingAnalysis.newBuilder()
                .setStackableGroups(List.of())
                .setConflicts(List.of())
                .setExclusions(List.of())
                .build();

        return ValidateStackableDiscountResultPayload.newBuilder()
                .setValidationSummary(validationSummary)
                .setDecisionToken(null)
                .setStackingAnalysis(stackingAnalysis)
                .setIdempotencyKey(command.idempotencyKey())
                .setCorrelationId(command.correlationId())
                .setProcessingTimeMs(result.processingTimeMs())
                .build();
    }

    /**
     * Calculates effective discount rate.
     */
    private double calculateDiscountRate(ValidateStackableDiscountResult result) {
        if (result.originalOrderAmount().doubleValue() == 0) {
            return 0.0;
        }
        return (result.totalDiscountAmount().doubleValue() / result.originalOrderAmount().doubleValue()) * 100.0;
    }

    /**
     * Builds human-readable summary text.
     */
    private String buildSummaryText(ValidateStackableDiscountResult result) {
        if (result.explanation() != null && result.explanation().summary() != null) {
            return result.explanation().summary();
        }
        return result.decision().name() + ": " + result.getValidatedCount() + " discounts validated";
    }

    /**
     * Builds event metadata.
     */
    private Map<String, String> buildMetadata(
            ValidateStackableDiscountCommand command,
            ValidateStackableDiscountResult result
    ) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("customerId", command.customerInfo().customerId());
        metadata.put("orderId", command.orderInfo().orderId());
        metadata.put("discountCount", String.valueOf(command.getDiscountCount()));
        metadata.put("validatedCount", String.valueOf(result.getValidatedCount()));
        metadata.put("rejectedCount", String.valueOf(result.getRejectedCount()));
        return metadata;
    }
}
