package vn.viettel.vds.promotion.validation.config.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.UUID;

/**
 * Validator for {@link ValidRuleId} annotation.
 * <p>
 * This validator checks that a Rule ID is a valid UUID format:
 * <ul>
 *   <li>Must be a valid UUID format (8-4-4-4-12 with hyphens)</li>
 *   <li>Exactly 36 characters in length</li>
 *   <li>Accepts any UUID version (v1, v2, v3, v4, v5, v6, v7)</li>
 * </ul>
 * <p>
 * Validation logic:
 * <ol>
 *   <li>Null values are considered valid (use @NotNull for null checking)</li>
 *   <li>Trim leading and trailing whitespaces</li>
 *   <li>Check if empty after trim</li>
 *   <li>Validate UUID format using UUID.fromString()</li>
 * </ol>
 * <p>
 * UUID format examples:
 * <ul>
 *   <li>UUIDv4: 550e8400-e29b-41d4-a716-446655440000</li>
 *   <li>UUIDv7: 01932b6f-0005-7000-8000-000000000001</li>
 * </ul>
 */
public class RuleIdValidator implements ConstraintValidator<ValidRuleId, String> {

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

        // Validate UUID format using Java's UUID parser
        try {
            UUID.fromString(trimmedValue);
            return true;
        } catch (IllegalArgumentException e) {
            buildCustomViolation(context, "VALIDATION_RULE_ID_INVALID",
                    "ID quy tắc không đúng định dạng UUID (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)");
            return false;
        }
    }

    /**
     * Builds a custom constraint violation with specific error code and message.
     *
     * @param context      the constraint validator context
     * @param errorCode    the error code
     * @param errorMessage the error message
     */
    private void buildCustomViolation(ConstraintValidatorContext context, String errorCode, String errorMessage) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorCode + ": " + errorMessage)
                .addConstraintViolation();
    }
}
