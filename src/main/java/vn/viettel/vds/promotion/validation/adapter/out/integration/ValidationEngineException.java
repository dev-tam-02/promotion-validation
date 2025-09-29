package vn.viettel.vds.promotion.validation.adapter.out.integration;

public class ValidationEngineException extends RuntimeException {

    private final String operation;
    private final int statusCode;

    public ValidationEngineException(String message, String operation) {
        super(message);
        this.operation = operation;
        this.statusCode = 0;
    }

    public ValidationEngineException(String message, String operation, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.statusCode = 0;
    }

    public ValidationEngineException(String message, String operation, int statusCode, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.statusCode = statusCode;
    }

    public String getOperation() {
        return operation;
    }

    public int getStatusCode() {
        return statusCode;
    }
}