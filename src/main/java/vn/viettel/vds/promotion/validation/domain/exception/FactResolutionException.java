package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when fact resolution operations fail.
 * Used for resolving facts from external data sources or embedded payloads.
 */
public class FactResolutionException extends RuntimeException {

    private final String contextName;
    private final String resolutionMode;

    public FactResolutionException(String message) {
        super(message);
        this.contextName = null;
        this.resolutionMode = null;
    }

    public FactResolutionException(String message, Throwable cause) {
        super(message, cause);
        this.contextName = null;
        this.resolutionMode = null;
    }

    public FactResolutionException(String message, String contextName, String resolutionMode) {
        super(message);
        this.contextName = contextName;
        this.resolutionMode = resolutionMode;
    }

    public FactResolutionException(String message, Throwable cause, String contextName, String resolutionMode) {
        super(message, cause);
        this.contextName = contextName;
        this.resolutionMode = resolutionMode;
    }

    public String getContextName() {
        return contextName;
    }

    public String getResolutionMode() {
        return resolutionMode;
    }
}
