package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when cache serialization/deserialization fails
 */
public class CacheSerializationException extends RuntimeException {

    public CacheSerializationException(String message) {
        super(message);
    }

    public CacheSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}