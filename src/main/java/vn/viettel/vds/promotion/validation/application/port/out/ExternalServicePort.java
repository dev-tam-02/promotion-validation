package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.ValidationContext;

import java.util.Map;
import java.util.Optional;

/**
 * Port for external service integrations
 */
public interface ExternalServicePort {

    /**
     * Fetch customer data
     */
    Optional<Map<String, Object>> fetchCustomerData(String customerId);

    /**
     * Fetch order data
     */
    Optional<Map<String, Object>> fetchOrderData(String orderId);

    /**
     * Enrich validation context with external data
     */
    ValidationContext enrichContext(ValidationContext context);

    /**
     * Notify external systems of validation result
     */
    void notifyValidationResult(String externalSystemId, Map<String, Object> result);
}