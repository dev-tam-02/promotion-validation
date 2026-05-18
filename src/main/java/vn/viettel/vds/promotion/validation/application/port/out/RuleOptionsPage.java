package vn.viettel.vds.promotion.validation.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * Port-level domain object representing a paginated page of rule options.
 * Returned by RuleOptionsLookupPort, independent of any web DTO.
 */
public record RuleOptionsPage(
        List<ValueOption> items,
        long totalElements
) {

    public static RuleOptionsPage empty() {
        return new RuleOptionsPage(List.of(), 0L);
    }

    /**
     * A single selectable option with i18n labels.
     * {@code metadata} carries adapter-specific hints (e.g. {@code {type: "SKU"}}
     * for Products rules, so the UI can render a grouped autocomplete).
     */
    public record ValueOption(
            String value,
            String labelEn,
            String labelVi,
            Map<String, Object> metadata
    ) {
        public ValueOption(String value, String labelEn, String labelVi) {
            this(value, labelEn, labelVi, Map.of());
        }
    }
}
