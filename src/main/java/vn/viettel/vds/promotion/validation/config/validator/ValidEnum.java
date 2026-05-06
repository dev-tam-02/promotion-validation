package vn.viettel.vds.promotion.validation.config.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that the annotated String is one of the declared names of a given {@link Enum}.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>Null values → valid (use {@code @NotNull} / {@code @NotBlank} separately when required)</li>
 *   <li>Blank/empty values → valid (allows PATCH "clear" semantics; {@code @NotBlank} guards CREATE)</li>
 *   <li>Non-blank values → must match one of the enum's {@code name()} values (case-insensitive)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @NotBlank(message = "VALIDATION_RULE_CONTEXT_REQUIRED")
 * @ValidEnum(value = RuleContext.class, message = "VALIDATION_RULE_CONTEXT_INVALID")
 * private String context;
 * }</pre>
 *
 * @see ValidEnumValidator
 */
@Documented
@Constraint(validatedBy = ValidEnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {

    /**
     * The enum class whose constants define the allowed values.
     */
    Class<? extends Enum<?>> value();

    /**
     * Error message / code returned on validation failure.
     */
    String message() default "VALIDATION_ENUM_INVALID";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload for clients to assign custom payload objects.
     */
    Class<? extends Payload>[] payload() default {};
}
