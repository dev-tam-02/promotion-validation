package vn.viettel.vds.promotion.validation.adapter.out.external.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vn.viettel.vds.promotion.validation.application.port.out.ExternalServicePort;
import vn.viettel.vds.promotion.validation.domain.model.ValidationContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of ExternalServicePort for integrating with external services
 */
@Component
public class ExternalServiceAdapter implements ExternalServicePort {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceAdapter.class);

    private final RestTemplate restTemplate;
    private final String customerServiceUrl;
    private final String orderServiceUrl;

    public ExternalServiceAdapter(
            RestTemplate restTemplate,
            @Value("${promix.validation.external.customer-service-url:http://customer:8080}") String customerServiceUrl,
            @Value("${promix.validation.external.order-service-url:http://order:8080}") String orderServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.customerServiceUrl = customerServiceUrl;
        this.orderServiceUrl = orderServiceUrl;
    }

    @Override
    public Optional<Map<String, Object>> fetchCustomerData(String customerId) {
        try {
            String url = customerServiceUrl + "/api/v1/customers/" + customerId;

            log.debug("Fetching customer data from: {}", url);

            Map<String, Object> customerData = restTemplate.getForObject(url, Map.class);

            if (customerData != null) {
                log.debug("Successfully fetched customer data for: {}", customerId);
                return Optional.of(customerData);
            }

            log.warn("No customer data found for: {}", customerId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Failed to fetch customer data for: {}", customerId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Map<String, Object>> fetchOrderData(String orderId) {
        try {
            String url = orderServiceUrl + "/api/v1/orders/" + orderId;

            log.debug("Fetching order data from: {}", url);

            Map<String, Object> orderData = restTemplate.getForObject(url, Map.class);

            if (orderData != null) {
                log.debug("Successfully fetched order data for: {}", orderId);
                return Optional.of(orderData);
            }

            log.warn("No order data found for: {}", orderId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Failed to fetch order data for: {}", orderId, e);
            return Optional.empty();
        }
    }

    @Override
    public ValidationContext enrichContext(ValidationContext context) {
        ValidationContext enrichedContext = context;

        try {
            // Enrich customer context if customer ID is available
            if (context.getCustomer() != null && context.getCustomer().getCustomerId() != null) {
                enrichedContext = enrichCustomerContext(enrichedContext);
            }

            // Enrich order context if order ID is available
            if (context.getOrder() != null && context.getOrder().getOrderId() != null) {
                enrichedContext = enrichOrderContext(enrichedContext);
            }

            return enrichedContext;

        } catch (Exception e) {
            log.error("Failed to enrich validation context", e);
            // Return original context if enrichment fails
            return context;
        }
    }

    @Override
    public void notifyValidationResult(String externalSystemId, Map<String, Object> result) {
        try {
            // This could be implemented to notify external systems
            // about validation results via webhooks, REST calls, etc.

            log.debug("Notifying external system {} with validation result", externalSystemId);

        } catch (Exception e) {
            log.error("Failed to notify external system: {}", externalSystemId, e);
        }
    }

    private ValidationContext enrichCustomerContext(ValidationContext context) {
        String customerId = context.getCustomer().getCustomerId();

        Optional<Map<String, Object>> customerData = fetchCustomerData(customerId);

        if (customerData.isPresent()) {
            Map<String, Object> data = customerData.get();

            // Create enriched customer context
            ValidationContext.CustomerContext enrichedCustomer =
                    ValidationContext.CustomerContext.builder()
                            .customerId(customerId)
                            .segment(getStringValue(data, "segment", context.getCustomer().getSegment()))
                            .tier(getStringValue(data, "tier", context.getCustomer().getTier()))
                            .totalPurchaseAmount(getBigDecimalValue(data, "totalPurchaseAmount",
                                    context.getCustomer().getTotalPurchaseAmount()))
                            .transactionCount(getIntegerValue(data, "transactionCount",
                                    context.getCustomer().getTransactionCount()))
                            .build();

            // Create new context with enriched customer data
            return ValidationContext.builder()
                    .customer(enrichedCustomer)
                    .order(context.getOrder())
                    .metadata(context.getMetadata())
                    .requestTime(context.getRequestTime())
                    .build();
        }

        return context;
    }

    private ValidationContext enrichOrderContext(ValidationContext context) {
        String orderId = context.getOrder().getOrderId();

        Optional<Map<String, Object>> orderData = fetchOrderData(orderId);

        if (orderData.isPresent()) {
            Map<String, Object> data = orderData.get();

            // Create enriched order context
            ValidationContext.OrderContext enrichedOrder =
                    ValidationContext.OrderContext.builder()
                            .orderId(orderId)
                            .orderValue(getBigDecimalValue(data, "orderValue", context.getOrder().getOrderValue()))
                            .itemCount(getIntegerValue(data, "itemCount", context.getOrder().getItemCount()))
                            .channel(getStringValue(data, "channel", context.getOrder().getChannel()))
                            .productCategories((Map<String, BigDecimal>) data.get("productCategories"))
                            .build();

            // Create new context with enriched order data
            return ValidationContext.builder()
                    .customer(context.getCustomer())
                    .order(enrichedOrder)
                    .metadata(context.getMetadata())
                    .requestTime(context.getRequestTime())
                    .build();
        }

        return context;
    }

    // Utility methods for safe data extraction
    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> data, String key, BigDecimal defaultValue) {
        Object value = data.get(key);
        if (value != null) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid numeric value for key {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    private Integer getIntegerValue(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value != null) {
            try {
                return Integer.valueOf(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for key {}: {}", key, value);
            }
        }
        return defaultValue;
    }
}