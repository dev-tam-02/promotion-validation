package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Instant;

/**
 * Validator for {@link ValidTimeRange} annotation.
 * Validates that the start date is before the expiration date.
 *
 * <p>This validator uses reflection to access the fields specified in the annotation,
 * allowing it to be reused for different DTOs with different field names.</p>
 *
 * <p>Validation behavior:</p>
 * <ul>
 *   <li>Returns true if either field is null (null checks handled by @NotNull)</li>
 *   <li>Returns true if startDate is strictly before expirationDate</li>
 *   <li>Returns false if startDate equals or is after expirationDate</li>
 * </ul>
 */
public class TimeRangeValidator implements ConstraintValidator<ValidTimeRange, Object> {

    private static final Logger logger = LoggerFactory.getLogger(TimeRangeValidator.class);

    private String startField;
    private String endField;
    private String message;

    @Override
    public void initialize(ValidTimeRange constraintAnnotation) {
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
            Instant startDate = getFieldValue(value, startField, Instant.class);
            Instant endDate = getFieldValue(value, endField, Instant.class);

            // If either is null, let @NotNull handle it
            if (startDate == null || endDate == null) {
                return true;
            }

            // Start date must be strictly before expiration date
            boolean isValid = startDate.isBefore(endDate);

            if (!isValid) {
                logger.debug("TimeRange validation failed: startDate={} is not before expirationDate={}",
                        startDate, endDate);

                // Disable default violation and add custom one
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(startField)
                        .addConstraintViolation();
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Error validating time range: {}", e.getMessage(), e);
            return false;
        }
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
