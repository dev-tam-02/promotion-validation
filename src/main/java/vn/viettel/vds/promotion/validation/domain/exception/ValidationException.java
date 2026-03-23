package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.BusinessRuleException;

/**
 * General exception for validation service operations that don't fit other specific exceptions.
 * This is a catch-all exception for the validation service.
 * HTTP Status: 422 Unprocessable Entity
 *
 * @deprecated Use more specific exceptions instead:
 * - {@link FactResolutionException} for fact resolution failures
 * - {@link EventPublishingException} for event publishing failures
 * - {@link CacheSerializationException} for serialization failures
 */
@Deprecated(since = "1.0.0", forRemoval = true)
@SuppressWarnings("java:S1133")
public class ValidationException extends BusinessRuleException {

    private static final String ERROR_CODE = "VALIDATION_SERVICE_ERROR";

    public ValidationException(String message) {
        super(ERROR_CODE, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}