package vn.viettel.vds.promotion.validation.domain.exception;

import java.util.List;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends DomainException {

    private final List<String> errors;

    public ValidationException(String message) {
        super(message, "VALIDATION_FAILED");
        this.errors = List.of(message);
    }

    public ValidationException(String message, List<String> errors) {
        super(message, "VALIDATION_FAILED");
        this.errors = errors != null ? errors : List.of();
    }

    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_FAILED", cause);
        this.errors = List.of(message);
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasMultipleErrors() {
        return errors.size() > 1;
    }
}