package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

/**
 * Common interface for operation responses that have a success status.
 * This interface provides a standard way to check if an operation was successful.
 */
public interface OperationResponse {

    /**
     * Check if the operation was successful.
     *
     * @return true if the operation succeeded, false otherwise
     */
    boolean isSuccess();

    /**
     * Get the response message.
     *
     * @return the response message describing the operation outcome
     */
    String getMessage();
}
