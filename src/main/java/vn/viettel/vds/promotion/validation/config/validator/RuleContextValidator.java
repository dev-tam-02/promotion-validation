package vn.viettel.vds.promotion.validation.config.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import vn.viettel.vds.promotion.validation.application.port.in.GetRuleContextsUseCase;

/**
 * Validator for {@link ValidRuleContext}.
 *
 * <p>Spring-managed (instantiated via {@code SpringConstraintValidatorFactory}), so it
 * can resolve {@link GetRuleContextsUseCase} to check the value against ACTIVE rows of
 * the {@code rule_contexts} table. Accepts null/blank (caller adds {@code @NotBlank}).</p>
 */
public class RuleContextValidator implements ConstraintValidator<ValidRuleContext, String> {

    private final GetRuleContextsUseCase getRuleContextsUseCase;

    public RuleContextValidator(GetRuleContextsUseCase getRuleContextsUseCase) {
        this.getRuleContextsUseCase = getRuleContextsUseCase;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null → valid (use @NotNull / @NotBlank separately)
        if (value == null) {
            return true;
        }

        // blank/empty → valid (PATCH "clear" semantics; @NotBlank enforces required on CREATE)
        if (value.isBlank()) {
            return true;
        }

        return getRuleContextsUseCase.isActiveContext(value);
    }
}
