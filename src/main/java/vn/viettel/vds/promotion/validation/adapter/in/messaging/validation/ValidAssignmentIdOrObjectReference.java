package vn.viettel.vds.promotion.validation.adapter.in.messaging.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that either assignmentId is provided OR both objectId and objectType are provided.
 * This is a class-level constraint that validates the relationship between fields.
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>If both objectId AND objectType are provided (non-null, non-blank) → assignmentId is optional</li>
 *   <li>If objectId OR objectType is missing (null or blank) → assignmentId is required</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @ValidAssignmentIdOrObjectReference(groups = RequiredCheck.class)
 * public class UpdateValidationRuleCommandDTO {
 *     private String assignmentId;
 *     private String objectId;
 *     private String objectType;
 * }
 * }</pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AssignmentIdOrObjectReferenceValidator.class)
@Documented
public @interface ValidAssignmentIdOrObjectReference {

    String message() default "ASSIGNMENT_ID_REQUIRED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * The name of the assignmentId field.
     */
    String assignmentIdField() default "assignmentId";

    /**
     * The name of the objectId field.
     */
    String objectIdField() default "objectId";

    /**
     * The name of the objectType field.
     */
    String objectTypeField() default "objectType";
}
