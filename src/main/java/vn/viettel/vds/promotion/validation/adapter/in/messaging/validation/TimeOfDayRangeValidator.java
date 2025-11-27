package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Validator for {@link ValidTimeOfDayRange} annotation.
 * Validates that the start time is before the expiration time within a day.
 *
 * <p>This validator parses time strings in various formats and compares them.</p>
 *
 * <p>Supported time formats:</p>
 * <ul>
 *   <li>"HH:mm" - e.g., "09:00"</li>
 *   <li>"HH:mm:ss" - e.g., "09:00:00"</li>
 *   <li>"HH:mm:ss+TZ" or "HH:mm:ss-TZ" - e.g., "09:00:00+07:00" (timezone stripped)</li>
 * </ul>
 *
 * <p>Validation behavior:</p>
 * <ul>
 *   <li>Returns true if either field is null or empty (null/empty checks handled by @NotNull/@NotBlank)</li>
 *   <li>Returns true if startTime is strictly before expirationTime</li>
 *   <li>Returns false if startTime equals or is after expirationTime</li>
 *   <li>Returns false if time format is invalid</li>
 * </ul>
 */
public class TimeOfDayRangeValidator implements ConstraintValidator<ValidTimeOfDayRange, Object> {

    private static final Logger logger = LoggerFactory.getLogger(TimeOfDayRangeValidator.class);

    private static final DateTimeFormatter TIME_FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String startField;
    private String endField;
    private String message;

    @Override
    public void initialize(ValidTimeOfDayRange constraintAnnotation) {
        this.startField = constraintAnnotation.startField();
        this.endField = constraintAnnotation.endField();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            String startTimeStr = getFieldValue(value, startField, String.class);
            String endTimeStr = getFieldValue(value, endField, String.class);

            // If either is null or empty, let @NotNull/@NotBlank handle it
            if (isNullOrEmpty(startTimeStr) || isNullOrEmpty(endTimeStr)) {
                return true;
            }

            // Parse times
            LocalTime startTime = parseTime(startTimeStr);
            LocalTime endTime = parseTime(endTimeStr);

            if (startTime == null || endTime == null) {
                logger.debug("Failed to parse time: startTime={}, endTime={}", startTimeStr, endTimeStr);
                addViolation(context, "TIME_FORMAT_INVALID");
                return false;
            }

            // Start time must be strictly before expiration time
            boolean isValid = startTime.isBefore(endTime);

            if (!isValid) {
                logger.debug("TimeOfDayRange validation failed: startTime={} is not before expirationTime={}",
                        startTimeStr, endTimeStr);
                addViolation(context, message);
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Error validating time of day range: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Parse time string to LocalTime.
     * Strips timezone information if present.
     *
     * @param timeStr the time string to parse
     * @return LocalTime or null if parsing fails
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null) {
            return null;
        }

        // Strip timezone offset if present (e.g., "09:00:00+07:00" -> "09:00:00")
        String normalizedTime = stripTimezone(timeStr);

        // Try different formats
        try {
            // Try HH:mm:ss first
            return LocalTime.parse(normalizedTime, TIME_FORMATTER_HH_MM_SS);
        } catch (DateTimeParseException e1) {
            try {
                // Try HH:mm
                return LocalTime.parse(normalizedTime, TIME_FORMATTER_HH_MM);
            } catch (DateTimeParseException e2) {
                logger.debug("Failed to parse time '{}' with any known format", timeStr);
                return null;
            }
        }
    }

    /**
     * Strip timezone offset from time string.
     * "09:00:00+07:00" -> "09:00:00"
     * "09:00:00-05:00" -> "09:00:00"
     * "09:00" -> "09:00"
     */
    private String stripTimezone(String timeStr) {
        if (timeStr == null) {
            return null;
        }

        // Find timezone offset marker (+ or - after time portion)
        int plusIndex = timeStr.lastIndexOf('+');
        int minusIndex = timeStr.lastIndexOf('-');

        // Only strip if the marker is after position 5 (to avoid stripping from "HH:mm")
        // "09:00:00+07:00" - plus at position 8
        // "09:00+07:00" - plus at position 5
        int offsetIndex = -1;
        if (plusIndex > 4) {
            offsetIndex = plusIndex;
        } else if (minusIndex > 4) {
            offsetIndex = minusIndex;
        }

        if (offsetIndex > 0) {
            return timeStr.substring(0, offsetIndex);
        }

        return timeStr;
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void addViolation(ConstraintValidatorContext context, String errorMessage) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorMessage)
                .addPropertyNode(startField)
                .addConstraintViolation();
    }

    /**
     * Get field value using reflection.
     */
    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object object, String fieldName, Class<T> type) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object fieldValue = field.get(object);
            if (fieldValue == null) {
                return null;
            }
            if (type.isInstance(fieldValue)) {
                return (T) fieldValue;
            }
            return null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Failed to access field '{}': {}", fieldName, e.getMessage());
            return null;
        }
    }
}
