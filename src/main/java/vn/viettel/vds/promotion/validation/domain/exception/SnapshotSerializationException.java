package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when snapshot serialization/deserialization fails
 */
public class SnapshotSerializationException extends RuntimeException {

    public SnapshotSerializationException(String message) {
        super(message);
    }

    public SnapshotSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
