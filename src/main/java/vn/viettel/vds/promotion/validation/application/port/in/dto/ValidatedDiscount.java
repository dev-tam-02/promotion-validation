package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents a discount that passed validation.
 *
 * @param objectType        type of discount object
 * @param objectId          discount object identifier
 * @param discountAmount    calculated discount amount
 * @param appliedOrder      order in which discount should be applied
 * @param calculationMethod method used to calculate discount
 * @param metadata          additional metadata about the discount
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidatedDiscount(
        DiscountObjectType objectType,
        String objectId,
        BigDecimal discountAmount,
        int appliedOrder,
        String calculationMethod,
        Map<String, Object> metadata
) {

    /**
     * Creates a validated discount with minimum fields.
     *
     * @param objectType     discount type
     * @param objectId       discount ID
     * @param discountAmount calculated discount
     * @param appliedOrder   application order
     * @return validated discount
     */
    public static ValidatedDiscount of(
            DiscountObjectType objectType,
            String objectId,
            BigDecimal discountAmount,
            int appliedOrder
    ) {
        return new ValidatedDiscount(
                objectType,
                objectId,
                discountAmount,
                appliedOrder,
                null,
                null
        );
    }
}
