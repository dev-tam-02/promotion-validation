package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

/**
 * Customer information for validation context.
 *
 * @param customerId unique customer identifier
 * @param customerType type of customer (REGULAR, VIP, PREMIUM, etc.)
 * @param segment customer segment for targeted validation
 * @param tier customer tier level
 *
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerInfo(
        @NotBlank(message = "Customer ID is required")
        String customerId,

        String customerType,
        String segment,
        String tier
) {

    /**
     * Creates customer info with only customer ID.
     *
     * @param customerId customer identifier
     * @return customer info
     */
    public static CustomerInfo of(String customerId) {
        return new CustomerInfo(customerId, null, null, null);
    }

    /**
     * Creates customer info with customer ID and segment.
     *
     * @param customerId customer identifier
     * @param segment customer segment
     * @return customer info
     */
    public static CustomerInfo withSegment(String customerId, String segment) {
        return new CustomerInfo(customerId, null, segment, null);
    }
}
