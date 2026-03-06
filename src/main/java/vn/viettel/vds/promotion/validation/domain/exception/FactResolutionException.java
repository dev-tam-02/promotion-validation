package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ExternalServiceException;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when fact resolution operations fail.
 * Used for resolving facts from external data sources or embedded payloads.
 * HTTP Status: 502 Bad Gateway
 */
public class FactResolutionException extends ExternalServiceException {

    private static final String ERROR_CODE = "FACT_RESOLUTION_FAILED";
    private static final String SERVICE_NAME = "fact-resolver";

    private final String contextName;
    private final String resolutionMode;

    public FactResolutionException(String message) {
        super(ERROR_CODE, message);
        this.contextName = null;
        this.resolutionMode = null;
    }

    public FactResolutionException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
        this.contextName = null;
        this.resolutionMode = null;
    }

    public FactResolutionException(String message, String contextName, String resolutionMode) {
        super(ERROR_CODE, message, buildParams(contextName, resolutionMode));
        this.contextName = contextName;
        this.resolutionMode = resolutionMode;
    }

    public FactResolutionException(String message, Throwable cause, String contextName, String resolutionMode) {
        super(ERROR_CODE, SERVICE_NAME, message, cause);
        this.contextName = contextName;
        this.resolutionMode = resolutionMode;
    }

    public String getContextName() {
        return contextName;
    }

    public String getResolutionMode() {
        return resolutionMode;
    }

    private static Map<String, Object> buildParams(String contextName, String resolutionMode) {
        Map<String, Object> params = new HashMap<>();
        params.put("serviceName", SERVICE_NAME);
        if (contextName != null) {
            params.put("contextName", contextName);
        }
        if (resolutionMode != null) {
            params.put("resolutionMode", resolutionMode);
        }
        return params;
    }
}
