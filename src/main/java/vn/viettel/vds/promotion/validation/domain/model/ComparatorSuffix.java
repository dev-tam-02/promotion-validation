package vn.viettel.vds.promotion.validation.domain.model;

import java.util.Map;
import java.util.Optional;

/**
 * Convention mapping from UI comparator label to operator_name suffix.
 *
 * <p>BE composes the effective operator_name (matched against DRL templates)
 * by concatenating an operator_options canonical name with the suffix returned
 * here, e.g. {@code "order.total" + "." + "gt"} → {@code "order.total.gt"}.
 */
public final class ComparatorSuffix {

    private static final Map<String, String> SUFFIX_BY_COMPARATOR = Map.of(
            "is_more_than", "gt",
            "is_more_than_or_equal_to", "gte",
            "is_exactly", "equals",
            "is_less_than", "lt",
            "is_less_than_or_equal_to", "lte",
            "is_between", "between"
    );

    private ComparatorSuffix() {
    }

    public static Optional<String> of(String comparator) {
        if (comparator == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(SUFFIX_BY_COMPARATOR.get(comparator));
    }

    public static String resolve(String canonical, String comparator) {
        if (canonical == null || canonical.isBlank()) {
            throw new IllegalArgumentException("canonical operator_name must not be blank");
        }
        return of(comparator)
                .map(suffix -> canonical + "." + suffix)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown comparator: " + comparator
                                + " (supported: is_more_than, is_more_than_or_equal_to, is_exactly, is_less_than, is_less_than_or_equal_to, is_between)"));
    }

    public static boolean isSupported(String comparator) {
        return comparator != null && SUFFIX_BY_COMPARATOR.containsKey(comparator);
    }

    private static final Map<String, String> COMPARATOR_BY_SUFFIX = Map.of(
            "gt", "is_more_than",
            "gte", "is_more_than_or_equal_to",
            "equals", "is_exactly",
            "lt", "is_less_than",
            "lte", "is_less_than_or_equal_to",
            "between", "is_between"
    );

    public static Optional<String> comparatorFromOperatorName(String operatorName) {
        if (operatorName == null) {
            return Optional.empty();
        }
        int dot = operatorName.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String suffix = operatorName.substring(dot + 1);
        return Optional.ofNullable(COMPARATOR_BY_SUFFIX.get(suffix));
    }

    public static boolean hasComparatorSuffix(String operatorName) {
        return comparatorFromOperatorName(operatorName).isPresent();
    }
}
