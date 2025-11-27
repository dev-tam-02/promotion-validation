package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that startDate is before expirationDate in a ValidityTimeframeDTO.
 * This is a class-level constraint that validates the relationship between two fields.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @ValidTimeRange(message = "TIME_RANGE_INVALID", groups = FormatCheck.class)
 * public class ValidityTimeframeDTO {
 *     private Instant startDate;
 *     private Instant expirationDate;
 * }
 * }</pre>
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>If both startDate and expirationDate are null, validation passes (null handled by @NotNull)</li>
 *   <li>If only one is null, validation passes (individual null handled by @NotNull)</li>
 *   <li>If startDate >= expirationDate, validation fails with TIME_RANGE_INVALID</li>
 * </ul>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeRangeValidator.class)
@Documented
public @interface ValidTimeRange {

    String message() default "TIME_RANGE_INVALID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * The name of the start date field.
     */
    String startField() default "startDate";

    /**
     * The name of the end date field.
     */
    String endField() default "expirationDate";
}
