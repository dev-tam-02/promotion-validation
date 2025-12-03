package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;

/**
 * Validator for {@link ValidISO8601Duration} annotation.
 * Validates that a string is a valid ISO 8601 duration format.
 *
 * <p>Supports both Period (date-based) and Duration (time-based) formats:</p>
 * <ul>
 *   <li>Period: P[n]Y[n]M[n]D - parsed by {@link Period#parse(CharSequence)}</li>
 *   <li>Duration: PT[n]H[n]M[n]S - parsed by {@link Duration#parse(CharSequence)}</li>
 * </ul>
 *
 * <p>Validation behavior:</p>
 * <ul>
 *   <li>Returns true if value is null or empty (optional field handling)</li>
 *   <li>Returns true if value can be parsed as Period or Duration</li>
 *   <li>Returns false if value cannot be parsed as either format</li>
 * </ul>
 */
public class ISO8601DurationValidator implements ConstraintValidator<ValidISO8601Duration, String> {

    private static final Logger logger = LoggerFactory.getLogger(ISO8601DurationValidator.class);

    @Override
    public void initialize(ValidISO8601Duration constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null or empty values are valid (optional field - use @NotNull for required)
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        String trimmedValue = value.trim();

        // Try parsing as Duration first (handles PT... format)
        if (isValidDuration(trimmedValue)) {
            return true;
        }

        // Try parsing as Period (handles P...D/M/Y format without time component)
        if (isValidPeriod(trimmedValue)) {
            return true;
        }

        logger.debug("ISO 8601 duration validation failed: value='{}' is not a valid duration format", value);
        return false;
    }

    /**
     * Check if value is a valid ISO 8601 Duration (time-based).
     * Handles formats like: PT1H, PT30M, PT1H30M, P1DT12H
     */
    private boolean isValidDuration(String value) {
        try {
            Duration.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Check if value is a valid ISO 8601 Period (date-based).
     * Handles formats like: P1D, P7D, P1M, P1Y, P1Y2M3D
     */
    private boolean isValidPeriod(String value) {
        try {
            Period.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
