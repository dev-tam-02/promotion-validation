package vn.viettel.vds.promotion.validation.config.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that the annotated String is a valid Rule ID.
 * <p>
 * Valid Rule IDs must:
 * <ul>
 *   <li>Contain only alphanumeric characters (a-z, A-Z, 0-9) and hyphens (-)</li>
 *   <li>Not exceed 36 characters in length (UUID format)</li>
 *   <li>Leading and trailing whitespaces are trimmed before validation</li>
 * </ul>
 * <p>
 * Null values are considered valid.
 *
 * @see RuleIdValidator
 */
@Documented
@Constraint(validatedBy = RuleIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRuleId {

    /**
     * Error message when validation fails.
     * Default message code for invalid format.
     */
    String message() default "VALIDATION_RULE_ID_INVALID";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload for clients to assign custom payload objects.
     */
    Class<? extends Payload>[] payload() default {};
}
