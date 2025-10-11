package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Discount request for validation.
 * <p>
 * Represents a single discount that should be validated as part of a stackable discount set.
 * </p>
 *
 * @param objectType       type of discount object (CASHBACK, CAMPAIGN, VOUCHER, COUPON, etc.)
 * @param objectId         unique identifier of the discount object
 * @param priority         application priority (lower number = higher priority)
 * @param expectedDiscount expected discount amount
 * @param maxDiscountCap   maximum discount cap
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscountRequest(
        @NotNull(message = "Discount object type is required")
        DiscountObjectType objectType,

        @NotBlank(message = "Discount object ID is required")
        String objectId,

        Integer priority,
        BigDecimal expectedDiscount,
        BigDecimal maxDiscountCap
) {

    /**
     * Creates a discount request with minimum required fields.
     *
     * @param objectType discount type
     * @param objectId   discount ID
     * @return discount request
     */
    public static DiscountRequest of(DiscountObjectType objectType, String objectId) {
        return new DiscountRequest(objectType, objectId, null, null, null);
    }

    /**
     * Creates a discount request with priority.
     *
     * @param objectType discount type
     * @param objectId   discount ID
     * @param priority   application priority
     * @return discount request
     */
    public static DiscountRequest withPriority(
            DiscountObjectType objectType,
            String objectId,
            int priority
    ) {
        return new DiscountRequest(objectType, objectId, priority, null, null);
    }

    /**
     * Checks if expected discount is specified.
     *
     * @return true if expected discount is set
     */
    public boolean hasExpectedDiscount() {
        return expectedDiscount != null && expectedDiscount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if discount cap is specified.
     *
     * @return true if max discount cap is set
     */
    public boolean hasDiscountCap() {
        return maxDiscountCap != null && maxDiscountCap.compareTo(BigDecimal.ZERO) > 0;
    }
}
