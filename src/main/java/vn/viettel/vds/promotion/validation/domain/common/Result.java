package vn.viettel.vds.promotion.validation.domain.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generic Result type for operation outcomes.
 *
 * Provides a type-safe way to handle success and failure cases without exceptions.
 * Inspired by functional programming Result/Either types.
 *
 * Usage:
 * <pre>
 * // Success case
 * Result<User> result = Result.success(user);
 *
 * // Failure case
 * Result<User> result = Result.failure(ErrorCode.USER_NOT_FOUND, "User not found: " + userId);
 *
 * // With multiple errors
 * Result<User> result = Result.failure(errors);
 *
 * // Pattern matching
 * return result.map(user -> user.getName())
 *              .orElse("Unknown");
 * </pre>
 *
 * @param <T> the type of the success value
 */
public class Result<T> {

    private final T value;
    private final List<Error> errors;
    private final boolean success;

    private Result(T value, List<Error> errors, boolean success) {
        this.value = value;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.success = success;
    }

    /**
     * Create a successful result
     */
    public static <T> Result<T> success(T value) {
        return new Result<>(value, null, true);
    }

    /**
     * Create a failed result with single error
     */
    public static <T> Result<T> failure(ErrorCode errorCode, String message) {
        List<Error> errors = new ArrayList<>();
        errors.add(new Error(errorCode, message, null));
        return new Result<>(null, errors, false);
    }

    /**
     * Create a failed result with single error and details
     */
    public static <T> Result<T> failure(ErrorCode errorCode, String message, String details) {
        List<Error> errors = new ArrayList<>();
        errors.add(new Error(errorCode, message, details));
        return new Result<>(null, errors, false);
    }

    /**
     * Create a failed result with multiple errors
     */
    public static <T> Result<T> failure(List<Error> errors) {
        return new Result<>(null, errors, false);
    }

    /**
     * Create a failed result with single error object
     */
    public static <T> Result<T> failure(Error error) {
        List<Error> errors = new ArrayList<>();
        errors.add(error);
        return new Result<>(null, errors, false);
    }

    /**
     * Check if the result is successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if the result is a failure
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Get the success value (may be null if failure)
     */
    public T getValue() {
        return value;
    }

    /**
     * Get the value as Optional
     */
    public Optional<T> getValueOptional() {
        return Optional.ofNullable(value);
    }

    /**
     * Get the list of errors (empty if success)
     */
    public List<Error> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Get the first error (if any)
     */
    public Optional<Error> getFirstError() {
        return errors.isEmpty() ? Optional.empty() : Optional.of(errors.get(0));
    }

    /**
     * Get the first error message (if any)
     */
    public Optional<String> getFirstErrorMessage() {
        return getFirstError().map(Error::getMessage);
    }

    /**
     * Get the first error code (if any)
     */
    public Optional<ErrorCode> getFirstErrorCode() {
        return getFirstError().map(Error::getCode);
    }

    /**
     * Transform the success value using a mapper function
     */
    public <U> Result<U> map(Function<T, U> mapper) {
        if (isSuccess()) {
            return Result.success(mapper.apply(value));
        } else {
            return Result.failure(errors);
        }
    }

    /**
     * Flat map - transform success value to another Result
     */
    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        if (isSuccess()) {
            return mapper.apply(value);
        } else {
            return Result.failure(errors);
        }
    }

    /**
     * Execute consumer if success
     */
    public Result<T> ifSuccess(Consumer<T> consumer) {
        if (isSuccess() && value != null) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Execute consumer if failure
     */
    public Result<T> ifFailure(Consumer<List<Error>> consumer) {
        if (isFailure()) {
            consumer.accept(errors);
        }
        return this;
    }

    /**
     * Get value or default
     */
    public T orElse(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    /**
     * Get value or compute default
     */
    public T orElseGet(Function<List<Error>, T> defaultSupplier) {
        return isSuccess() ? value : defaultSupplier.apply(errors);
    }

    /**
     * Get value or throw exception
     */
    public T orElseThrow() {
        if (isSuccess()) {
            return value;
        }
        throw new ResultException(errors);
    }

    /**
     * Get value or throw custom exception
     */
    public <X extends Throwable> T orElseThrow(Function<List<Error>, X> exceptionSupplier) throws X {
        if (isSuccess()) {
            return value;
        }
        throw exceptionSupplier.apply(errors);
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "Result.Success(" + value + ")";
        } else {
            return "Result.Failure(" + errors + ")";
        }
    }

    /**
     * Error details
     */
    public static class Error {
        private final ErrorCode code;
        private final String message;
        private final String details;

        public Error(ErrorCode code, String message, String details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public Error(ErrorCode code, String message) {
            this(code, message, null);
        }

        public ErrorCode getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Error{code=").append(code);
            sb.append(", message='").append(message).append("'");
            if (details != null) {
                sb.append(", details='").append(details).append("'");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Exception thrown when calling orElseThrow() on a failed Result
     */
    public static class ResultException extends RuntimeException {
        private final List<Error> errors;

        public ResultException(List<Error> errors) {
            super(formatErrors(errors));
            this.errors = errors;
        }

        public List<Error> getErrors() {
            return new ArrayList<>(errors);
        }

        private static String formatErrors(List<Error> errors) {
            if (errors.isEmpty()) {
                return "Operation failed with no error details";
            }
            if (errors.size() == 1) {
                Error error = errors.get(0);
                return error.getCode() + ": " + error.getMessage();
            }
            StringBuilder sb = new StringBuilder("Operation failed with " + errors.size() + " errors:\n");
            for (Error error : errors) {
                sb.append("  - ").append(error.getCode()).append(": ").append(error.getMessage()).append("\n");
            }
            return sb.toString();
        }
    }
}
