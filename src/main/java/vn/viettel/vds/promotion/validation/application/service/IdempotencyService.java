package vn.viettel.vds.promotion.validation.application.service;

import java.util.Optional;

/**
 * Service for managing idempotency of command processing.
 * Prevents duplicate processing of the same command by tracking processed command IDs.
 */
public interface IdempotencyService {

    /**
     * Check if a command has already been processed.
     *
     * @param idempotencyKey Unique identifier for the command (typically commandId)
     * @return true if the command has been processed, false otherwise
     */
    boolean isProcessed(String idempotencyKey);

    /**
     * Mark a command as processed and store the result.
     *
     * @param idempotencyKey Unique identifier for the command
     * @param result         Processing result to store (can be serialized to JSON)
     */
    void markAsProcessed(String idempotencyKey, Object result);

    /**
     * Retrieve the result of a previously processed command.
     *
     * @param idempotencyKey Unique identifier for the command
     * @param <T>            Type of the result
     * @return Optional containing the result if found, empty otherwise
     */
    <T> Optional<T> getProcessedResult(String idempotencyKey);

    /**
     * Remove idempotency record (for cleanup or testing).
     *
     * @param idempotencyKey Unique identifier for the command
     */
    void remove(String idempotencyKey);
}
