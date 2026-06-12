package vn.viettel.vds.promotion.validation.config.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that the annotated String is the code of an ACTIVE row in the
 * {@code rule_contexts} table — loaded dynamically, NOT from a hardcoded enum.
 *
 * <p>Behaviour mirrors {@link ValidEnum}:</p>
 * <ul>
 *   <li>Null values → valid (use {@code @NotNull} / {@code @NotBlank} separately when required)</li>
 *   <li>Blank/empty values → valid (PATCH "clear" semantics; {@code @NotBlank} guards CREATE)</li>
 *   <li>Non-blank values → must match an active context code (case-insensitive)</li>
 * </ul>
 *
 * @see RuleContextValidator
 */
@Documented
@Constraint(validatedBy = RuleContextValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRuleContext {

    /**
     * Error message / code returned on validation failure.
     */
    String message() default "VALIDATION_RULE_CONTEXT_INVALID";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload for clients to assign custom payload objects.
     */
    Class<? extends Payload>[] payload() default {};
}
