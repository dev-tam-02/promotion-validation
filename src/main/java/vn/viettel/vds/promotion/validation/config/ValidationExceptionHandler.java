package vn.viettel.vds.promotion.validation.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for validation errors.
 * <p>
 * This handler catches constraint violation exceptions thrown during
 * path variable, request parameter, and method parameter validation.
 * <p>
 * Handles:
 * <ul>
 *   <li>{@link ConstraintViolationException} - Bean validation violations</li>
 * </ul>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ValidationExceptionHandler.class);

    /**
     * Handles constraint violation exceptions from method parameter validation.
     * <p>
     * This exception is thrown when validation fails on:
     * <ul>
     *   <li>Path variables annotated with validation constraints</li>
     *   <li>Request parameters annotated with validation constraints</li>
     *   <li>Method parameters in @Validated controllers</li>
     * </ul>
     *
     * @param ex the constraint violation exception
     * @return response entity with 400 Bad Request and error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {

        logger.warn("Constraint violation: {}", ex.getMessage());

        // Extract first violation for error code and message
        ConstraintViolation<?> firstViolation = ex.getConstraintViolations().iterator().next();
        String violationMessage = firstViolation.getMessage();

        // Parse error code and message
        // Expected format: "ERROR_CODE: Error message"
        String errorCode = "VALIDATION_ERROR";
        String errorMessage = violationMessage;

        if (violationMessage.contains(": ")) {
            String[] parts = violationMessage.split(": ", 2);
            errorCode = parts[0];
            errorMessage = parts[1];
        }

        // Build detailed error message with all violations
        String detailedMessage = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    String message = violation.getMessage();
                    return String.format("%s: %s", path, message);
                })
                .collect(Collectors.joining("; "));

        logger.debug("Detailed violations: {}", detailedMessage);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.put("code", errorCode);
        errorResponse.put("message", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }
}
