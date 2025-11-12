package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception thrown when command processing fails.
 * <p>
 * This dedicated exception is used instead of generic RuntimeException
 * to provide better error handling and comply with SonarQube rules.
 * </p>
 */
public class CommandProcessingException extends DomainException {

    private static final String ERROR_CODE = "COMMAND_PROCESSING_FAILED";

    public CommandProcessingException(String message) {
        super(message, ERROR_CODE);
    }

    public CommandProcessingException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}
