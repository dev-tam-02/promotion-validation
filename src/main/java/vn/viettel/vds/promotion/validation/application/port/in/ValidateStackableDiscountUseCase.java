package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.application.port.in.command.ValidateStackableDiscountCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.ValidateStackableDiscountResult;

/**
 * Input port (use case interface) for validating stackable discounts.
 * <p>
 * This use case validates a set of discounts that are intended to be stacked together,
 * ensuring they comply with business rules, budget constraints, and stacking policies.
 * </p>
 * <p>
 * The validation is non-destructive (dry-run) and does not consume budgets or limits.
 * It provides validation results that can be used by the redemption service to decide
 * whether to proceed with actual redemption.
 * </p>
 *
 * @author Validation Team
 * @since 1.0.0
 */
public interface ValidateStackableDiscountUseCase {

    /**
     * Validates a set of stackable discounts against business rules and constraints.
     * <p>
     * This method performs comprehensive validation including:
     * <ul>
     *   <li>Discount compatibility and stacking rules</li>
     *   <li>Budget availability checks (if requested)</li>
     *   <li>Customer eligibility verification</li>
     *   <li>Order criteria validation</li>
     *   <li>Temporal constraints (validity periods)</li>
     *   <li>Discount optimization (if requested)</li>
     * </ul>
     * </p>
     *
     * @param command the validation command containing customer, order, and discount information
     * @return validation result with decision, validated discounts, and explanations
     * @throws IllegalArgumentException if command is null or contains invalid data
     */
    ValidateStackableDiscountResult validate(ValidateStackableDiscountCommand command);
}
