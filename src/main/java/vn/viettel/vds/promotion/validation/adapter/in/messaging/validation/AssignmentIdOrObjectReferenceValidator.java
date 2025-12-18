package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Validator for {@link ValidAssignmentIdOrObjectReference} annotation.
 * Validates that either assignmentId is provided OR both objectId and objectType are provided.
 *
 * <p>Validation behavior:</p>
 * <ul>
 *   <li>If both objectId AND objectType have values → valid (assignmentId optional)</li>
 *   <li>If objectId OR objectType is missing → assignmentId must have value</li>
 *   <li>If assignmentId is also missing when required → validation fails</li>
 * </ul>
 */
public class AssignmentIdOrObjectReferenceValidator
        implements ConstraintValidator<ValidAssignmentIdOrObjectReference, Object> {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentIdOrObjectReferenceValidator.class);

    private String assignmentIdField;
    private String objectIdField;
    private String objectTypeField;
    private String message;

    @Override
    public void initialize(ValidAssignmentIdOrObjectReference constraintAnnotation) {
        this.assignmentIdField = constraintAnnotation.assignmentIdField();
        this.objectIdField = constraintAnnotation.objectIdField();
        this.objectTypeField = constraintAnnotation.objectTypeField();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            String assignmentId = getFieldValue(value, assignmentIdField);
            String objectId = getFieldValue(value, objectIdField);
            String objectType = getFieldValue(value, objectTypeField);

            boolean hasObjectReference = hasValue(objectId) && hasValue(objectType);
            boolean hasAssignmentId = hasValue(assignmentId);

            // If both objectId and objectType are provided, assignmentId is optional
            if (hasObjectReference) {
                return true;
            }

            // If object reference is incomplete, assignmentId is required
            if (!hasAssignmentId) {
                logger.debug("Validation failed: assignmentId is required when objectId or objectType is missing. " +
                                "objectId={}, objectType={}, assignmentId={}",
                        objectId, objectType, assignmentId);

                // Disable default violation and add custom one on assignmentId field
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(assignmentIdField)
                        .addConstraintViolation();
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("Error validating assignmentId or object reference: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if a string has value (non-null and non-blank).
     */
    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Get field value using reflection.
     */
    private String getFieldValue(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            if (!field.canAccess(object) && !field.trySetAccessible()) {
                logger.error("Cannot access field '{}' - security restriction", fieldName);
                return null;
            }
            Object fieldValue = field.get(object);
            if (fieldValue == null) {
                return null;
            }
            if (fieldValue instanceof String stringValue) {
                return stringValue;
            }
            return null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Failed to access field '{}': {}", fieldName, e.getMessage());
            return null;
        }
    }
}
