package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Getter;
import vn.viettel.vds.promotion.validation.domain.fact.CustomerFact;
import vn.viettel.vds.promotion.validation.domain.fact.DiscountFact;
import vn.viettel.vds.promotion.validation.domain.fact.OrderFact;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing the context for stackable discount validation.
 * <p>
 * This is a rich domain model that encapsulates all the facts and context
 * needed to perform stackable discount validation. It provides methods
 * to query and reason about the validation context.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Getter
@Builder
public class StackingContext {

    private final String validationId;
    private final String correlationId;
    private final CustomerFact customer;
    private final OrderFact order;
    private final List<DiscountFact> discounts;
    private final boolean checkBudget;
    private final boolean optimizeOrder;
    private final Instant requestedAt;
    private final Map<String, Object> metadata;

    /**
     * Gets the number of discounts to validate.
     *
     * @return discount count
     */
    public int getDiscountCount() {
        return discounts != null ? discounts.size() : 0;
    }

    /**
     * Checks if context has customer information.
     *
     * @return true if customer is present
     */
    public boolean hasCustomer() {
        return customer != null && customer.customerId() != null;
    }

    /**
     * Checks if context has order information.
     *
     * @return true if order is present
     */
    public boolean hasOrder() {
        return order != null && order.orderId() != null;
    }

    /**
     * Checks if context has discounts to validate.
     *
     * @return true if discounts are present
     */
    public boolean hasDiscounts() {
        return discounts != null && !discounts.isEmpty();
    }

    /**
     * Checks if the validation context is complete and valid.
     *
     * @return true if context is valid for validation
     */
    public boolean isValid() {
        return validationId != null &&
                hasCustomer() &&
                hasOrder() &&
                hasDiscounts();
    }

    /**
     * Gets customer segment for rule filtering.
     *
     * @return customer segment or null
     */
    public String getCustomerSegment() {
        // Customer segment is determined from tier or attributes
        if (customer == null) {
            return null;
        }
        // Try to get from tier first
        if (customer.tier() != null) {
            return customer.tier();
        }
        // Try to get from attributes
        if (customer.attributes() != null && customer.attributes().containsKey("segment")) {
            return customer.attributes().get("segment").toString();
        }
        return null;
    }

    /**
     * Gets customer tier for eligibility checks.
     *
     * @return customer tier or null
     */
    public String getCustomerTier() {
        return customer != null ? customer.tier() : null;
    }

    /**
     * Checks if the request is within acceptable time window.
     *
     * @param maxAgeSeconds maximum age in seconds
     * @return true if request is fresh
     */
    public boolean isRequestFresh(long maxAgeSeconds) {
        if (requestedAt == null) {
            return false;
        }
        long ageSeconds = Instant.now().getEpochSecond() - requestedAt.getEpochSecond();
        return ageSeconds <= maxAgeSeconds;
    }

    /**
     * Creates a validation error for invalid context.
     *
     * @return validation result indicating invalid context
     */
    public ValidationResult createInvalidContextError() {
        StringBuilder message = new StringBuilder("Invalid stacking context: ");
        if (validationId == null) message.append("missing validation ID; ");
        if (!hasCustomer()) message.append("missing customer; ");
        if (!hasOrder()) message.append("missing order; ");
        if (!hasDiscounts()) message.append("missing discounts; ");

        return ValidationResult.deny(
                validationId != null ? validationId : "UNKNOWN",
                "INVALID_CONTEXT",
                message.toString()
        );
    }
}
