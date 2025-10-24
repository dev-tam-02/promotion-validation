package vn.viettel.vds.promotion.validation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.application.port.in.command.ValidateStackableDiscountCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.CustomerInfo;
import vn.viettel.vds.promotion.validation.application.port.in.dto.OrderInfo;
import vn.viettel.vds.promotion.validation.application.port.in.dto.OrderItemInfo;
import vn.viettel.vds.promotion.validation.domain.fact.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for constructing FactPack from validation commands.
 * <p>
 * This component converts application DTOs into domain facts required
 * for validation processing.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FactPackBuilder {

    /**
     * Builds a FactPack from validation command.
     *
     * @param command validation command
     * @return populated fact pack
     */
    public FactPack buildFromCommand(ValidateStackableDiscountCommand command) {
        log.debug("Building FactPack from command: idempotencyKey={}", command.idempotencyKey());

        CustomerFact customerFact = buildCustomerFact(command.customerInfo());
        OrderFact orderFact = buildOrderFact(command.orderInfo());
        MetadataFact metadataFact = buildMetadataFact(command);
        ProvenanceInfo provenanceInfo = buildProvenanceInfo(command);

        return FactPack.builder()
                .factPackVersion("1.0")
                .timestamp(command.requestedAt())
                .customer(customerFact)
                .order(orderFact)
                .metadata(metadataFact)
                .provenance(provenanceInfo)
                .build();
    }

    /**
     * Builds customer fact from customer info.
     */
    private CustomerFact buildCustomerFact(CustomerInfo customerInfo) {
        Map<String, Object> attributes = new HashMap<>();

        if (customerInfo.segment() != null) {
            attributes.put("segment", customerInfo.segment());
        }
        if (customerInfo.customerType() != null) {
            attributes.put("customerType", customerInfo.customerType());
        }

        return CustomerFact.builder()
                .customerId(customerInfo.customerId())
                .tier(customerInfo.tier())
                .isActive(true)
                .attributes(attributes)
                .build();
    }

    /**
     * Builds order fact from order info.
     */
    private OrderFact buildOrderFact(OrderInfo orderInfo) {
        List<OrderItemFact> orderItems = null;

        if (orderInfo.items() != null && !orderInfo.items().isEmpty()) {
            orderItems = orderInfo.items().stream()
                    .map(this::buildOrderItemFact)
                    .toList();
        }

        Map<String, Object> metadata = new HashMap<>();
        if (orderInfo.channel() != null) {
            metadata.put("channel", orderInfo.channel());
        }
        if (orderInfo.location() != null) {
            metadata.put("location", orderInfo.location());
        }

        return OrderFact.builder()
                .orderId(orderInfo.orderId())
                .totalAmount(orderInfo.orderValue())
                .currency(orderInfo.currency())
                .orderDate(orderInfo.orderDate())
                .items(orderItems)
                .metadata(metadata)
                .build();
    }

    /**
     * Builds order item fact from order item info.
     */
    private OrderItemFact buildOrderItemFact(OrderItemInfo itemInfo) {
        return OrderItemFact.builder()
                .sku(itemInfo.sku())
                .quantity(itemInfo.quantity())
                .unitPrice(itemInfo.price())
                .totalPrice(itemInfo.getTotalAmount())
                .build();
    }

    /**
     * Builds metadata fact from command.
     */
    private MetadataFact buildMetadataFact(ValidateStackableDiscountCommand command) {
        Map<String, Object> context = new HashMap<>();
        context.put("validationType", "STACKABLE_DISCOUNT");
        context.put("discountCount", command.getDiscountCount());
        context.put("checkBudget", command.shouldCheckBudget());
        context.put("optimizeOrder", command.shouldOptimizeOrder());
        context.put("correlationId", command.correlationId());
        context.put("idempotencyKey", command.idempotencyKey());

        return MetadataFact.builder()
                .context(context)
                .build();
    }

    /**
     * Builds provenance info from command.
     */
    private ProvenanceInfo buildProvenanceInfo(ValidateStackableDiscountCommand command) {
        // Simplified - using null for optional fields
        return new ProvenanceInfo(
                List.of(), // sources
                command.requestedAt(), // timestamp
                null, // cacheKey
                null, // cacheTTL
                null, // fetchPolicy
                null  // tags
        );
    }
}
