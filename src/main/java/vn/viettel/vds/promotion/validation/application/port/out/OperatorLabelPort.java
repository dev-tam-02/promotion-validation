package vn.viettel.vds.promotion.validation.application.port.out;

import java.util.Map;

/**
 * Port for reading canonical-operator display labels from the service-wide
 * {@code translations} table (entity_type = {@code CONDITION_OPERATOR}).
 *
 * <p>Returned shape: {@code entity_key -> field -> locale -> value}, e.g.
 * {@code GREATER_THAN -> {label -> {vi -> "Lớn hơn", en -> "greater than"},
 * label_date -> {vi -> "Sau"}}}. The {@code entity_key} is the canonical
 * {@code ConditionOperator} code (optionally {@code <field>:<CANONICAL>} for a
 * per-field override).
 */
public interface OperatorLabelPort {

    /**
     * Load every {@code CONDITION_OPERATOR} translation, indexed by entity_key,
     * then field ({@code label}/{@code label_date}), then locale.
     */
    Map<String, Map<String, Map<String, String>>> loadOperatorLabels();
}
