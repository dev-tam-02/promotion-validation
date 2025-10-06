package vn.viettel.vds.promotion.validation.application.port.in.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import vn.viettel.vds.promotion.validation.application.port.in.dto.*;

import java.time.Instant;
import java.util.List;

/**
 * Command for validating stackable discounts.
 * <p>
 * This command contains all necessary information to perform validation of
 * multiple discounts that are intended to be stacked together on a single order.
 * </p>
 * <p>
 * The command is immutable and uses Java record for conciseness and built-in
 * immutability guarantees.
 * </p>
 *
 * @param idempotencyKey unique key to ensure idempotent validation processing
 * @param customerInfo customer information for validation context
 * @param orderInfo order information including items and total value
 * @param discountRequests list of discounts to validate for stacking
 * @param validationOptions options controlling validation behavior
 * @param correlationId correlation ID for distributed tracing
 * @param requestedAt timestamp when validation was requested
 *
 * @author Validation Team
 * @since 1.0.0
 */
public record ValidateStackableDiscountCommand(
        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @NotNull(message = "Customer information is required")
        @Valid
        CustomerInfo customerInfo,

        @NotNull(message = "Order information is required")
        @Valid
        OrderInfo orderInfo,

        @NotEmpty(message = "At least one discount request is required")
        @Valid
        List<DiscountRequest> discountRequests,

        @NotNull(message = "Validation options are required")
        @Valid
        ValidationOptions validationOptions,

        @NotBlank(message = "Correlation ID is required")
        String correlationId,

        @NotNull(message = "Requested timestamp is required")
        Instant requestedAt
) {

    /**
     * Creates a command with default validation options.
     *
     * @param idempotencyKey unique key for idempotency
     * @param customerInfo customer information
     * @param orderInfo order information
     * @param discountRequests list of discount requests
     * @param correlationId correlation ID
     * @return command with default options
     */
    public static ValidateStackableDiscountCommand withDefaults(
            String idempotencyKey,
            CustomerInfo customerInfo,
            OrderInfo orderInfo,
            List<DiscountRequest> discountRequests,
            String correlationId
    ) {
        return new ValidateStackableDiscountCommand(
                idempotencyKey,
                customerInfo,
                orderInfo,
                discountRequests,
                ValidationOptions.defaults(),
                correlationId,
                Instant.now()
        );
    }

    /**
     * Returns the number of discounts to validate.
     *
     * @return discount count
     */
    public int getDiscountCount() {
        return discountRequests != null ? discountRequests.size() : 0;
    }

    /**
     * Checks if budget availability check is requested.
     *
     * @return true if budget check is enabled
     */
    public boolean shouldCheckBudget() {
        return validationOptions != null && validationOptions.checkBudgetAvailability();
    }

    /**
     * Checks if discount order optimization is requested.
     *
     * @return true if optimization is enabled
     */
    public boolean shouldOptimizeOrder() {
        return validationOptions != null && validationOptions.optimizeOrder();
    }
}
