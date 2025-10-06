package vn.viettel.vds.promotion.validation.application.port.in.dto;

/**
 * Validation decision for stackable discount validation.
 *
 * @author Validation Team
 * @since 1.0.0
 */
public enum ValidationDecision {
    /**
     * All discounts validated successfully and can be redeemed
     */
    APPROVED,

    /**
     * Validation failed - discounts cannot be redeemed
     */
    REJECTED,

    /**
     * Partial validation - some discounts approved, some rejected
     */
    PARTIAL,

    /**
     * Validation encountered a system error
     */
    ERROR
}
