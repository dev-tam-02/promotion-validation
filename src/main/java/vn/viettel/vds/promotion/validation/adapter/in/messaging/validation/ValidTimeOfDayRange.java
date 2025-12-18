package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that startTime is before expirationTime in a ValidityHoursPerDayDTO.
 * This is a class-level constraint that validates the relationship between two time fields.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @ValidTimeOfDayRange(message = "TIME_OF_DAY_RANGE_INVALID", groups = FormatCheck.class)
 * public class ValidityHoursPerDayDTO {
 *     private String startTime;      // Format: "HH:mm" or "HH:mm:ss"
 *     private String expirationTime; // Format: "HH:mm" or "HH:mm:ss"
 * }
 * }</pre>
 *
 * <p>Supported time formats:</p>
 * <ul>
 *   <li>"HH:mm" - e.g., "09:00"</li>
 *   <li>"HH:mm:ss" - e.g., "09:00:00"</li>
 *   <li>"HH:mm:ss+TZ" - e.g., "09:00:00+07:00" (timezone will be stripped)</li>
 * </ul>
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>If both times are null or empty, validation passes (handled by @NotNull/@NotBlank)</li>
 *   <li>If only one is null/empty, validation passes (individual checks handle this)</li>
 *   <li>If startTime >= expirationTime, validation fails with TIME_OF_DAY_RANGE_INVALID</li>
 * </ul>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeOfDayRangeValidator.class)
@Documented
public @interface ValidTimeOfDayRange {

    String message() default "TIME_OF_DAY_RANGE_INVALID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * The name of the start time field.
     */
    String startField() default "startTime";

    /**
     * The name of the end time field.
     */
    String endField() default "expirationTime";
}
