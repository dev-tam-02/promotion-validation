package vn.viettel.vds.promotion.validation.application.port.out;

import java.util.Map;

/**
 * Port for reading {@code operator_categories} localized text from the service-wide
 * {@code translations} table (entity_type = {@code OPERATOR_CATEGORY}). Replaces the
 * inline {@code name}/{@code name_vi}/{@code description}/{@code description_vi}
 * columns the rule-builder catalog used to read straight off the entity.
 *
 * <p>Returned shape: {@code category_id -> field -> locale -> value}, where field is
 * {@code name} or {@code description} and locale is {@code en} / {@code vi}.
 */
public interface OperatorCategoryLabelPort {

    /**
     * Load every {@code OPERATOR_CATEGORY} translation, indexed by category id, then
     * field ({@code name}/{@code description}), then locale. Callers reuse the map
     * across all categories in a catalog response to avoid N queries.
     */
    Map<String, Map<String, Map<String, String>>> loadCategoryLabels();
}
