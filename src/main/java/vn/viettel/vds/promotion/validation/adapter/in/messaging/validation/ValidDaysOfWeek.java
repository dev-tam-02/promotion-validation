package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a List of Integer contains valid days of week values.
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>Null or empty list passes validation (use @NotNull/@NotEmpty for required fields)</li>
 *   <li>Each element must be between 1 (Monday) and 7 (Sunday) - ISO-8601 standard</li>
 *   <li>No duplicate values allowed</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @ValidDaysOfWeek(message = "VALIDITY_DAYS_OF_WEEK_INVALID", groups = FormatCheck.class)
 * private List<Integer> validityDaysOfWeek;
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DaysOfWeekValidator.class)
@Documented
public @interface ValidDaysOfWeek {

    String message() default "INVALID_DAYS_OF_WEEK";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
