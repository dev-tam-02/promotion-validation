package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.InternalException;

import java.util.Map;

/**
 * Exception thrown when configuration validation fails.
 * HTTP Status: 500 Internal Server Error
 */
public class ConfigurationInvalidException extends InternalException {

    private static final String ERROR_CODE = "CONFIGURATION_INVALID";

    public ConfigurationInvalidException(String message) {
        super(ERROR_CODE, message);
    }

    public ConfigurationInvalidException(String configKey, String message) {
        super(ERROR_CODE, message, Map.of("configKey", configKey));
    }

    public ConfigurationInvalidException(String configKey, Object value, String message) {
        super(ERROR_CODE, message, Map.of("configKey", configKey, "value", String.valueOf(value)));
    }
}
