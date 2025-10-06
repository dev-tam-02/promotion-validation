package vn.viettel.vds.promotion.validation.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a validation issue found during validation.
 *
 * @param code issue code
 * @param title short title of the issue
 * @param message detailed message
 * @param severity issue severity level
 *
 * @author Validation Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationIssue(
        String code,
        String title,
        String message,
        Severity severity
) {

    /**
     * Creates an error-level issue.
     *
     * @param code issue code
     * @param message issue message
     * @return validation issue
     */
    public static ValidationIssue error(String code, String message) {
        return new ValidationIssue(code, "Validation Error", message, Severity.ERROR);
    }

    /**
     * Creates a warning-level issue.
     *
     * @param code issue code
     * @param message issue message
     * @return validation issue
     */
    public static ValidationIssue warning(String code, String message) {
        return new ValidationIssue(code, "Validation Warning", message, Severity.WARNING);
    }

    /**
     * Creates an info-level issue.
     *
     * @param code issue code
     * @param message issue message
     * @return validation issue
     */
    public static ValidationIssue info(String code, String message) {
        return new ValidationIssue(code, "Validation Info", message, Severity.INFO);
    }

    /**
     * Issue severity levels.
     */
    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }
}
