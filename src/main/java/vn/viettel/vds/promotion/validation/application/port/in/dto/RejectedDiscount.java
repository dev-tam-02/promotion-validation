package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Represents a discount that failed validation.
 *
 * @param objectType type of discount object
 * @param objectId discount object identifier
 * @param rejectionReasons reasons why discount was rejected
 *
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RejectedDiscount(
        DiscountObjectType objectType,
        String objectId,
        List<String> rejectionReasons
) {

    /**
     * Creates a rejected discount.
     *
     * @param objectType discount type
     * @param objectId discount ID
     * @param reasons rejection reasons
     * @return rejected discount
     */
    public static RejectedDiscount of(
            DiscountObjectType objectType,
            String objectId,
            List<String> reasons
    ) {
        return new RejectedDiscount(objectType, objectId, reasons);
    }

    /**
     * Creates a rejected discount with single reason.
     *
     * @param objectType discount type
     * @param objectId discount ID
     * @param reason rejection reason
     * @return rejected discount
     */
    public static RejectedDiscount withReason(
            DiscountObjectType objectType,
            String objectId,
            String reason
    ) {
        return new RejectedDiscount(objectType, objectId, List.of(reason));
    }
}
