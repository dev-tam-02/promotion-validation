package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.InternalException;

/**
 * Exception thrown when snapshot serialization/deserialization fails.
 * HTTP Status: 500 Internal Server Error
 */
public class SnapshotSerializationException extends InternalException {

    private static final String ERROR_CODE = "SNAPSHOT_SERIALIZATION_FAILED";

    public SnapshotSerializationException(String message) {
        super(ERROR_CODE, message);
    }

    public SnapshotSerializationException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
