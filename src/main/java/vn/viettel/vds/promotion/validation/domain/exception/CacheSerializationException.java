package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.InternalException;

/**
 * Exception thrown when cache serialization/deserialization fails.
 * HTTP Status: 500 Internal Server Error
 */
public class CacheSerializationException extends InternalException {

    private static final String ERROR_CODE = "CACHE_SERIALIZATION_FAILED";

    public CacheSerializationException(String message) {
        super(ERROR_CODE, message);
    }

    public CacheSerializationException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}