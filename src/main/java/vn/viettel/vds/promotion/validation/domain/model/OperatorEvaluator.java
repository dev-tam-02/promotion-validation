package vn.viettel.vds.promotion.validation.domain.model;

import vn.viettel.vds.promotion.validation.domain.exception.InvalidOperatorException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility class for evaluating operators in rule conditions
 */
public class OperatorEvaluator {

    private OperatorEvaluator() {
        // Utility class
    }

    public static boolean evaluate(String operator, Object fieldValue, Object expectedValue) {
        if (operator == null) {
            throw new InvalidOperatorException("Operator cannot be null");
        }

        return switch (operator.toUpperCase()) {
            case "EQUALS", "EQ", "==" -> evaluateEquals(fieldValue, expectedValue);
            case "NOT_EQUALS", "NEQ", "!=" -> !evaluateEquals(fieldValue, expectedValue);
            case "GREATER_THAN", "GT", ">" -> evaluateGreaterThan(fieldValue, expectedValue);
            case "GREATER_THAN_OR_EQUALS", "GTE", ">=" -> evaluateGreaterThanOrEquals(fieldValue, expectedValue);
            case "LESS_THAN", "LT", "<" -> evaluateLessThan(fieldValue, expectedValue);
            case "LESS_THAN_OR_EQUALS", "LTE", "<=" -> evaluateLessThanOrEquals(fieldValue, expectedValue);
            case "IN" -> evaluateIn(fieldValue, expectedValue);
            case "NOT_IN" -> !evaluateIn(fieldValue, expectedValue);
            case "CONTAINS" -> evaluateContains(fieldValue, expectedValue);
            case "NOT_CONTAINS" -> !evaluateContains(fieldValue, expectedValue);
            case "STARTS_WITH" -> evaluateStartsWith(fieldValue, expectedValue);
            case "ENDS_WITH" -> evaluateEndsWith(fieldValue, expectedValue);
            case "MATCHES", "REGEX" -> evaluateRegex(fieldValue, expectedValue);
            case "IS_NULL" -> fieldValue == null;
            case "IS_NOT_NULL" -> fieldValue != null;
            case "IS_EMPTY" -> evaluateIsEmpty(fieldValue);
            case "IS_NOT_EMPTY" -> !evaluateIsEmpty(fieldValue);
            case "BETWEEN" -> evaluateBetween(fieldValue, expectedValue);
            default -> throw new UnsupportedOperationException("Operator not supported: " + operator);
        };
    }

    private static boolean evaluateEquals(Object fieldValue, Object expectedValue) {
        if (fieldValue == null && expectedValue == null) {
            return true;
        }
        if (fieldValue == null || expectedValue == null) {
            return false;
        }

        // Handle numeric comparisons
        if (isNumeric(fieldValue) && isNumeric(expectedValue)) {
            return toBigDecimal(fieldValue).compareTo(toBigDecimal(expectedValue)) == 0;
        }

        return Objects.equals(fieldValue.toString(), expectedValue.toString());
    }

    private static boolean evaluateGreaterThan(Object fieldValue, Object expectedValue) {
        if (fieldValue == null || expectedValue == null) {
            return false;
        }

        if (isNumeric(fieldValue) && isNumeric(expectedValue)) {
            return toBigDecimal(fieldValue).compareTo(toBigDecimal(expectedValue)) > 0;
        }

        return fieldValue.toString().compareTo(expectedValue.toString()) > 0;
    }

    private static boolean evaluateGreaterThanOrEquals(Object fieldValue, Object expectedValue) {
        return evaluateEquals(fieldValue, expectedValue) || evaluateGreaterThan(fieldValue, expectedValue);
    }

    private static boolean evaluateLessThan(Object fieldValue, Object expectedValue) {
        if (fieldValue == null || expectedValue == null) {
            return false;
        }

        if (isNumeric(fieldValue) && isNumeric(expectedValue)) {
            return toBigDecimal(fieldValue).compareTo(toBigDecimal(expectedValue)) < 0;
        }

        return fieldValue.toString().compareTo(expectedValue.toString()) < 0;
    }

    private static boolean evaluateLessThanOrEquals(Object fieldValue, Object expectedValue) {
        return evaluateEquals(fieldValue, expectedValue) || evaluateLessThan(fieldValue, expectedValue);
    }

    private static boolean evaluateIn(Object fieldValue, Object expectedValue) {
        if (fieldValue == null || expectedValue == null) {
            return false;
        }

        if (expectedValue instanceof Collection) {
            Collection<?> collection = (Collection<?>) expectedValue;
            return collection.stream().anyMatch(item -> evaluateEquals(fieldValue, item));
        }

        if (expectedValue.getClass().isArray()) {
            Object[] array = (Object[]) expectedValue;
            for (Object item : array) {
                if (evaluateEquals(fieldValue, item)) {
                    return true;
                }
            }
            return false;
        }

        return evaluateEquals(fieldValue, expectedValue);
    }

    private static boolean evaluateContains(Object fieldValue, Object expectedValue) {
        if (fieldValue == null || expectedValue == null) {
            return false;
        }

        String fieldStr = fieldValue.toString();
        String expectedStr = expectedValue.toString();
        return fieldStr.contains(expectedStr);
    }

    private static boolean evaluateStartsWith(Object fieldValue, Object expectedValue) {
        if (fieldValue == null || expectedValue == null) {
            return false;
        }

        String fieldStr = fieldValue.toString();
        String expectedStr = expectedValue.toString();
        return fieldStr.startsWith(expectedStr);
    }

    private static boolean evaluateEndsWith(Object fieldValue, Object expectedValue) {
        if (fieldValue == null || expectedValue == null) {
            return false;
        }

        String fieldStr = fieldValue.toString();
        String expectedStr = expectedValue.toString();
        return fieldStr.endsWith(expectedStr);
    }

    private static boolean evaluateRegex(Object fieldValue, Object expectedValue) {
        if (fieldValue == null || expectedValue == null) {
            return false;
        }

        String fieldStr = fieldValue.toString();
        String patternStr = expectedValue.toString();
        Pattern pattern = Pattern.compile(patternStr);
        return pattern.matcher(fieldStr).matches();
    }

    private static boolean evaluateIsEmpty(Object fieldValue) {
        if (fieldValue == null) {
            return true;
        }

        if (fieldValue instanceof String string) {
            return string.isEmpty();
        }

        if (fieldValue instanceof Collection<?> collection) {
            return collection.isEmpty();
        }

        if (fieldValue.getClass().isArray()) {
            return ((Object[]) fieldValue).length == 0;
        }

        return false;
    }

    private static boolean evaluateBetween(Object fieldValue, Object expectedValue) {
        if (fieldValue == null || expectedValue == null) {
            return false;
        }

        if (expectedValue instanceof Object[] range) {
            if (range.length != 2) {
                throw new InvalidOperatorException("BETWEEN", "requires exactly 2 values");
            }
            return evaluateGreaterThanOrEquals(fieldValue, range[0]) &&
                    evaluateLessThanOrEquals(fieldValue, range[1]);
        }

        throw new InvalidOperatorException("BETWEEN", "requires an array of 2 values");
    }

    private static boolean isNumeric(Object value) {
        return value instanceof Number ||
                (value instanceof String string && isNumericString(string));
    }

    private static boolean isNumericString(String str) {
        try {
            new BigDecimal(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigdecimal) {
            return bigdecimal;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return new BigDecimal(value.toString());
    }
}