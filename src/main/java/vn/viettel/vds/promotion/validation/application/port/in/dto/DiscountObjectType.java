package vn.viettel.vds.promotion.validation.application.port.in.dto;

/**
 * Types of discount objects that can be validated.
 *
 * @author Validation Team
 * @since 1.0.0
 */
public enum DiscountObjectType {
    /**
     * Cashback discount
     */
    CASHBACK,

    /**
     * Campaign discount
     */
    CAMPAIGN,

    /**
     * Voucher discount
     */
    VOUCHER,

    /**
     * Coupon discount
     */
    COUPON,

    /**
     * Promotion stack (group of promotions)
     */
    PROMOTION_STACK,

    /**
     * Discount code
     */
    DISCOUNT_CODE
}
