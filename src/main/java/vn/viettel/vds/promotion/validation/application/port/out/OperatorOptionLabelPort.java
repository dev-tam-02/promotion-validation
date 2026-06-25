package vn.viettel.vds.promotion.validation.application.port.out;

import java.util.Map;

/**
 * Port for reading {@code operator_options} localized text from the service-wide
 * {@code translations} table (entity_type = {@code OPERATOR_OPTION}). Replaces the
 * inline {@code name_vi}/{@code description}/{@code label_*}/{@code placeholder_*}
 * columns the rule-builder catalog used to read straight off the entity.
 *
 * <p>Returned shape: {@code option_id -> field -> locale -> value}, where field is
 * one of {@code name} / {@code description} / {@code label} / {@code placeholder}
 * and locale is {@code en} / {@code vi}. The {@code option_id} is the
 * {@code operator_options.id} (its business key in the polymorphic translations table).
 */
public interface OperatorOptionLabelPort {

    /**
     * Load every {@code OPERATOR_OPTION} translation, indexed by option id, then
     * field ({@code name}/{@code description}/{@code label}/{@code placeholder}),
     * then locale. Callers reuse the returned map across all options in a catalog
     * response to avoid N queries.
     */
    Map<String, Map<String, Map<String, String>>> loadOptionLabels();
}
