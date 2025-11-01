package vn.viettel.vds.promotion.validation.adapter.in.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.viettel.vds.promotion.validation.domain.exception.AssignmentAlreadyDeletedException;
import vn.viettel.vds.promotion.validation.domain.exception.AssignmentNotFoundException;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationRuleNotFoundException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler cho ValidationRuleAssignment operations.
 * Map domain exceptions sang appropriate HTTP responses.
 */
@Slf4j
@RestControllerAdvice
public class ValidationAssignmentExceptionHandler {

    /**
     * Handle ValidationRuleNotFoundException
     */
    @ExceptionHandler(ValidationRuleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleValidationRuleNotFound(
            ValidationRuleNotFoundException ex) {

        log.warn("Validation rule not found: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("code", "VALIDATION_RULE_NOT_FOUND");
        response.put("success", false);
        response.put("message", ex.getMessage());
        response.put("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle AssignmentNotFoundException
     */
    @ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAssignmentNotFound(
            AssignmentNotFoundException ex) {

        log.warn("Assignment not found: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("code", "ASSIGNMENT_VALIDATION_NOT_FOUND");
        response.put("success", false);
        response.put("message", ex.getMessage());
        response.put("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle AssignmentAlreadyDeletedException (Optimistic Locking)
     */
    @ExceptionHandler({
            AssignmentAlreadyDeletedException.class,
            OptimisticLockingFailureException.class
    })
    public ResponseEntity<Map<String, Object>> handleAssignmentAlreadyDeleted(
            Exception ex) {

        log.warn("Assignment already deleted (optimistic locking): {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("code", "ASSIGNMENT_ALREADY_DELETED");
        response.put("success", false);
        response.put("message", "Assignment has been deleted by another process");
        response.put("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle IllegalArgumentException (validation errors)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("Validation error: {}", ex.getMessage());

        // Parse error code từ message (format: "ERROR_CODE: message")
        String errorCode = "BAD_REQUEST";
        String message = ex.getMessage();

        if (message != null && message.contains(":")) {
            String[] parts = message.split(":", 2);
            errorCode = parts[0].trim();
            message = parts.length > 1 ? parts[1].trim() : message;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("code", errorCode);
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
