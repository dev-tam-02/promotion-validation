package vn.viettel.vds.promotion.validation.config.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator for {@link ValidRuleId} annotation.
 * <p>
 * This validator checks that a Rule ID:
 * <ul>
 *   <li>Contains only alphanumeric characters and hyphens (a-z, A-Z, 0-9, -)</li>
 *   <li>Does not exceed 36 characters (UUID format)</li>
 * </ul>
 * <p>
 * Validation logic:
 * <ol>
 *   <li>Null values are considered valid (use @NotNull for null checking)</li>
 *   <li>Trim leading and trailing whitespaces</li>
 *   <li>Check length does not exceed 36 characters</li>
 *   <li>Check format matches pattern: [a-zA-Z0-9-]+</li>
 * </ol>
 */
public class RuleIdValidator implements ConstraintValidator<ValidRuleId, String> {

    /**
     * Maximum length for Rule ID (UUID format).
     */
    private static final int MAX_LENGTH = 36;

    /**
     * Pattern for valid Rule ID format: alphanumeric and hyphens only.
     */
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9-]+$");

    @Override
    public void initialize(ValidRuleId constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are considered valid (use @NotNull separately if needed)
        if (value == null) {
            return true;
        }

        // Trim whitespace before validation
        String trimmedValue = value.trim();

        // If empty after trim, consider invalid
        if (trimmedValue.isEmpty()) {
            buildCustomViolation(context, "VALIDATION_RULE_ID_INVALID",
                "ID quy tắc không được để trống");
            return false;
        }

        // Check length constraint
        if (trimmedValue.length() > MAX_LENGTH) {
            buildCustomViolation(context, "VALIDATION_RULE_LENGTH_EXCEEDED",
                "ID quy tắc không được vượt quá 36 ký tự");
            return false;
        }

        // Check format constraint (alphanumeric and hyphen only)
        if (!VALID_PATTERN.matcher(trimmedValue).matches()) {
            buildCustomViolation(context, "VALIDATION_RULE_ID_INVALID",
                "ID quy tắc không đúng định dạng, cho phép nhập ký tự chữ, số và ký tự \"-\"");
            return false;
        }

        return true;
    }

    /**
     * Builds a custom constraint violation with specific error code and message.
     *
     * @param context the constraint validator context
     * @param errorCode the error code
     * @param errorMessage the error message
     */
    private void buildCustomViolation(ConstraintValidatorContext context, String errorCode, String errorMessage) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorCode + ": " + errorMessage)
                .addConstraintViolation();
    }
}
