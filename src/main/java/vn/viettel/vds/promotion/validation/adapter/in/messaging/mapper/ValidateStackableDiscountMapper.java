package vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.port.in.command.ValidateStackableDiscountCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.*;

import java.math.BigDecimal;
import java.util.List;
/**
 * Mapper for converting Avro ValidateStackableDiscountCommand to domain command.
 * <p>
 * This component handles the transformation of Kafka message payloads
 * into application-layer command objects.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class ValidateStackableDiscountMapper {

    /**
     * Converts Avro command to application command.
     *
     * @param avroCommand Avro command from Kafka
     * @return Application command
     */
    public ValidateStackableDiscountCommand toCommand(
            vn.viettel.vds.promotion.schema.redemption.command.ValidateStackableDiscountCommand avroCommand
    ) {
        log.debug("Mapping Avro command to domain command: id={}", avroCommand.getId());

        var payload = avroCommand.getPayload();

        return new ValidateStackableDiscountCommand(
                payload.getIdempotencyKey(),
                mapCustomerInfo(payload.getCustomerInfo()),
                mapOrderInfo(payload.getOrderInfo()),
                mapDiscountRequests(payload.getDiscountRequests()),
                mapValidationOptions(payload.getValidationOptions()),
                payload.getCorrelationId(),
                payload.getRequestedAt()
        );
    }

    /**
     * Maps customer info from Avro to domain DTO.
     */
    private CustomerInfo mapCustomerInfo(
            vn.viettel.vds.promotion.schema.redemption.command.ValidateCustomerInfo avroCustomerInfo
    ) {
        return new CustomerInfo(
                avroCustomerInfo.getCustomerId(),
                avroCustomerInfo.getCustomerType(),
                avroCustomerInfo.getSegment(),
                avroCustomerInfo.getTier()
        );
    }

    /**
     * Maps order info from Avro to domain DTO.
     */
    private OrderInfo mapOrderInfo(
            vn.viettel.vds.promotion.schema.redemption.command.ValidateOrderInfo avroOrderInfo
    ) {
        List<OrderItemInfo> items = null;
        if (avroOrderInfo.getItems() != null) {
            items = avroOrderInfo.getItems().stream()
                    .map(this::mapOrderItem)
                    .toList();
        }

        // Extract currency and amount from Money object
        vn.viettel.vds.promotion.schema.common.Money orderValueMoney = avroOrderInfo.getOrderValue();
        BigDecimal orderValue = new BigDecimal(orderValueMoney.getAmount());
        String currency = orderValueMoney.getCurrencyCode();

        return new OrderInfo(
                avroOrderInfo.getOrderId(),
                orderValue,
                currency,
                avroOrderInfo.getOrderDate(),
                avroOrderInfo.getChannel(),
                avroOrderInfo.getLocation(),
                items
        );
    }

    /**
     * Maps order item from Avro to domain DTO.
     */
    private OrderItemInfo mapOrderItem(
            vn.viettel.vds.promotion.schema.redemption.command.ValidateOrderItem avroItem
    ) {
        // Extract price from Money object
        vn.viettel.vds.promotion.schema.common.Money priceMoney = avroItem.getPrice();
        BigDecimal price = new BigDecimal(priceMoney.getAmount());

        return new OrderItemInfo(
                avroItem.getSku(),
                avroItem.getQuantity(),
                price,
                avroItem.getCategory()
        );
    }

    /**
     * Maps discount requests from Avro to domain DTOs.
     */
    private List<DiscountRequest> mapDiscountRequests(
            List<vn.viettel.vds.promotion.schema.redemption.command.ValidateDiscountRequest> avroRequests
    ) {
        return avroRequests.stream()
                .map(this::mapDiscountRequest)
                .toList();
    }

    /**
     * Maps single discount request from Avro to domain DTO.
     */
    private DiscountRequest mapDiscountRequest(
            vn.viettel.vds.promotion.schema.redemption.command.ValidateDiscountRequest avroRequest
    ) {
        // Extract expected discount from Money object
        BigDecimal expectedDiscount = null;
        if (avroRequest.getExpectedDiscount() != null) {
            expectedDiscount = new BigDecimal(avroRequest.getExpectedDiscount().getAmount());
        }

        // Extract max discount cap from Money object
        BigDecimal maxDiscountCap = null;
        if (avroRequest.getMaxDiscountCap() != null) {
            maxDiscountCap = new BigDecimal(avroRequest.getMaxDiscountCap().getAmount());
        }

        return new DiscountRequest(
                mapDiscountObjectType(avroRequest.getObjectType()),
                avroRequest.getObjectId(),
                avroRequest.getPriority(),
                expectedDiscount,
                maxDiscountCap
        );
    }

    /**
     * Maps discount object type from Avro enum to domain enum.
     */
    private DiscountObjectType mapDiscountObjectType(
            vn.viettel.vds.promotion.schema.redemption.command.DiscountObjectType avroType
    ) {
        return switch (avroType) {
            case CASHBACK -> DiscountObjectType.CASHBACK;
            case CAMPAIGN -> DiscountObjectType.CAMPAIGN;
            case VOUCHER -> DiscountObjectType.VOUCHER;
            case COUPON -> DiscountObjectType.COUPON;
            case PROMOTION_STACK -> DiscountObjectType.PROMOTION_STACK;
            case DISCOUNT_CODE -> DiscountObjectType.DISCOUNT_CODE;
            default -> throw new IllegalArgumentException("Unknown discount object type: " + avroType);
        };
    }

    /**
     * Maps validation options from Avro to domain DTO.
     */
    private ValidationOptions mapValidationOptions(
            vn.viettel.vds.promotion.schema.redemption.command.ValidateOptions avroOptions
    ) {
        return new ValidationOptions(
                avroOptions.getCheckBudgetAvailability(),
                avroOptions.getOptimizeOrder(),
                mapExplainLevel(avroOptions.getExplainLevel()),
                avroOptions.getIncludeAlternatives(),
                avroOptions.getDryRun()
        );
    }

    /**
     * Maps explain level from Avro enum to domain enum.
     */
    private ValidationOptions.ExplainLevel mapExplainLevel(
            vn.viettel.vds.promotion.schema.redemption.command.ValidateExplainLevel avroLevel
    ) {
        return switch (avroLevel) {
            case NONE -> ValidationOptions.ExplainLevel.NONE;
            case BASIC -> ValidationOptions.ExplainLevel.BASIC;
            case FULL -> ValidationOptions.ExplainLevel.FULL;
            default -> throw new IllegalArgumentException("Unknown explain level: " + avroLevel);
        };
    }
}
