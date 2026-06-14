package vn.viettel.vds.promotion.validation.domain.common;

/**
 * Standardized error codes for validation module.
 * <p>
 * Error code format: MODULE_CATEGORY_SPECIFIC_ERROR
 * - MODULE: VAL (Validation)
 * - CATEGORY: Business domain (RULE, CMD, etc.)
 * - SPECIFIC_ERROR: Specific error condition
 * <p>
 * HTTP Status mapping:
 * - 1xxx: Success (200)
 * - 2xxx: Client errors (400)
 * - 3xxx: Not found errors (404)
 * - 4xxx: Conflict errors (409)
 * - 5xxx: Server errors (500)
 */
public enum ErrorCode {

    // Success codes (1xxx)
    SUCCESS("VAL-1000", "Operation completed successfully", 200),

    // General validation errors (2xxx)
    INVALID_INPUT("VAL-2000", "Invalid input provided", 400),
    INVALID_PAYLOAD("VAL-2001", "Invalid or missing payload", 400),
    MISSING_REQUIRED_FIELD("VAL-2002", "Required field is missing", 400),
    INVALID_FORMAT("VAL-2003", "Invalid format", 400),
    VALIDATION_FAILED("VAL-2004", "Validation failed", 400),

    // Rule-specific errors (21xx)
    RULE_ALREADY_EXISTS("VAL-2100", "Rule already exists", 409),
    RULE_INVALID_STATE("VAL-2102", "Rule is in invalid state for this operation", 400),
    RULE_MISSING_APPLICABILITY_NODE("VAL-2103", "Rule must contain product.applicability.in condition", 400),
    RULE_EVALUATION_ERROR("VAL-2104", "Rule evaluation failed", 500),
    RULE_COMPILATION_ERROR("VAL-2105", "Rule compilation failed", 400),
    RULE_INVALID_EXPRESSION("VAL-2106", "Rule expression is invalid", 400),
    RULE_DUPLICATE_CODE("VAL-2107", "Rule code already exists", 409),
    RULE_DUPLICATE_NAME("VAL-2108", "Rule name already exists", 409),

    // Assignment errors (22xx)
    DUPLICATE_ASSIGNMENT_VALIDATION_RULE("VAL-2200", "Assignment already exists", 409),
    ASSIGNMENT_INVALID_CONFIG("VAL-2202", "Assignment configuration is invalid", 400),
    MISSING_CAMPAIGN_ID("VAL-2203", "Campaign ID is required", 400),
    MISSING_ASSIGN_RULE("VAL-2204", "Assignment rule data is required", 400),
    ASSIGNMENT_VALIDATION_NOT_FOUND("VAL-2205", "Validation rule assignment not found for this object", 404),
    OBJECT_NOT_FOUND("VAL-2206", "Object (campaign) not found", 404),
    VALIDATION_RULE_NOT_FOUND("VAL-2207", "Validation rule not found", 404),
    DELETED_ASSIGNMENT_VALIDATION_SUCCESS("VAL-2208", "Validation rule assignment deleted successfully", 200),

    // Command processing errors (23xx)
    COMMAND_PROCESSING_ERROR("VAL-2300", "Command processing failed", 500),
    COMMAND_INVALID_TYPE("VAL-2301", "Invalid command type", 400),
    COMMAND_MISSING_DATA("VAL-2302", "Command data is missing", 400),
    COMMAND_VALIDATION_ERROR("VAL-2303", "Command validation failed", 400),

    // Node tree errors (24xx)
    NODE_INVALID_STRUCTURE("VAL-2400", "Rule node structure is invalid", 400),
    NODE_MISSING_FIELD("VAL-2401", "Rule node is missing required field", 400),
    NODE_MISSING_OPERATOR("VAL-2402", "Rule node is missing operator", 400),
    NODE_INVALID_OPERATOR("VAL-2403", "Rule node has invalid operator", 400),
    NODE_EVALUATION_ERROR("VAL-2404", "Node evaluation failed", 500),

    // Fact resolution errors (25xx)
    FACT_RESOLVER_ERROR("VAL-2500", "Fact resolution failed", 500),
    FACT_RESOLVER_TIMEOUT("VAL-2501", "Fact resolution timed out", 504),
    FACT_NOT_FOUND("VAL-2502", "Required fact not found", 404),
    FACT_INVALID_TYPE("VAL-2503", "Fact has invalid type", 400),

    // Deployment errors (26xx)
    DEPLOYMENT_FAILED("VAL-2600", "Rule deployment failed", 500),
    DEPLOYMENT_ENGINE_UNAVAILABLE("VAL-2601", "Validation engine unavailable", 503),
    DEPLOYMENT_INVALID_RULE("VAL-2602", "Rule cannot be deployed in current state", 400),

    // Database errors (27xx)
    DATABASE_ERROR("VAL-2700", "Database operation failed", 500),
    DATABASE_CONNECTION_ERROR("VAL-2701", "Database connection failed", 503),
    DATABASE_CONSTRAINT_VIOLATION("VAL-2702", "Database constraint violation", 409),

    // Integration errors (28xx)
    INTEGRATION_ERROR("VAL-2800", "External integration failed", 500),
    SERVICE_UNAVAILABLE("VAL-2801", "External service unavailable", 503),
    TIMEOUT_ERROR("VAL-2802", "Operation timed out", 504),
    CIRCUIT_BREAKER_OPEN("VAL-2803", "Circuit breaker is open", 503),

    // Configuration errors (29xx)
    CONFIGURATION_ERROR("VAL-2900", "Configuration error", 500),
    CONFIGURATION_MISSING("VAL-2901", "Required configuration is missing", 500),
    CONFIGURATION_INVALID("VAL-2902", "Configuration is invalid", 500),

    // Processing errors (5xxx)
    PROCESSING_ERROR("VAL-5000", "Unexpected processing error", 500),
    INTERNAL_ERROR("VAL-5001", "Internal server error", 500),
    NOT_IMPLEMENTED("VAL-5002", "Feature not implemented", 501),
    UNKNOWN_ERROR("VAL-5999", "Unknown error occurred", 500);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    ErrorCode(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    /**
     * Find error code by code string
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Check if this is a success code
     */
    public boolean isSuccess() {
        return httpStatus >= 200 && httpStatus < 300;
    }

    /**
     * Check if this is a client error
     */
    public boolean isClientError() {
        return httpStatus >= 400 && httpStatus < 500;
    }

    /**
     * Check if this is a server error
     */
    public boolean isServerError() {
        return httpStatus >= 500;
    }

    @Override
    public String toString() {
        return code + ": " + defaultMessage;
    }
}
