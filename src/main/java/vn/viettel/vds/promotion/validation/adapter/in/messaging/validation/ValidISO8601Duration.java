package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a string value is a valid ISO 8601 duration format.
 *
 * <p>Valid formats include:</p>
 * <ul>
 *   <li>Period format: P[n]Y[n]M[n]D (e.g., "P1D" for 1 day, "P7D" for 7 days, "P1M" for 1 month)</li>
 *   <li>Duration format: PT[n]H[n]M[n]S (e.g., "PT1H" for 1 hour, "PT30M" for 30 minutes)</li>
 *   <li>Combined format: P[n]Y[n]M[n]DT[n]H[n]M[n]S (e.g., "P1DT12H" for 1 day and 12 hours)</li>
 * </ul>
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>Null or empty values pass validation (use @NotNull/@NotBlank for required fields)</li>
 *   <li>Non-empty values must be valid ISO 8601 duration format</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @ValidISO8601Duration(message = "INTERVAL_INVALID_FORMAT", groups = FormatCheck.class)
 * private String interval;
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ISO8601DurationValidator.class)
@Documented
public @interface ValidISO8601Duration {

    String message() default "INVALID_ISO8601_DURATION_FORMAT";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
