package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator for {@link ValidDaysOfWeek} annotation.
 * Validates that a List of Integer contains valid days of week values (1-7).
 *
 * <p>Validation behavior:</p>
 * <ul>
 *   <li>Returns true if list is null or empty (optional field handling)</li>
 *   <li>Returns false if any element is null</li>
 *   <li>Returns false if any element is outside range 1-7</li>
 *   <li>Returns false if there are duplicate values</li>
 * </ul>
 */
public class DaysOfWeekValidator implements ConstraintValidator<ValidDaysOfWeek, List<Integer>> {

    private static final Logger logger = LoggerFactory.getLogger(DaysOfWeekValidator.class);
    private static final int MIN_DAY = 1;
    private static final int MAX_DAY = 7;

    @Override
    public void initialize(ValidDaysOfWeek constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(List<Integer> values, ConstraintValidatorContext context) {
        // Null or empty list is valid (optional field - use @NotNull/@NotEmpty for required)
        if (values == null || values.isEmpty()) {
            return true;
        }

        Set<Integer> seen = new HashSet<>();

        for (Integer value : values) {
            // Check for null element
            if (value == null) {
                logger.debug("Days of week validation failed: null element found in list");
                return false;
            }

            // Check range 1-7
            if (value < MIN_DAY || value > MAX_DAY) {
                logger.debug("Days of week validation failed: value {} is outside valid range [{}-{}]",
                        value, MIN_DAY, MAX_DAY);
                return false;
            }

            // Check for duplicates
            if (!seen.add(value)) {
                logger.debug("Days of week validation failed: duplicate value {} found", value);
                return false;
            }
        }

        return true;
    }
}
