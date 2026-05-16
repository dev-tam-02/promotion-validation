package vn.viettel.vds.promotion.validation.application.port.out;

import java.util.List;

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
     */
    public record ValueOption(
            String value,
            String labelEn,
            String labelVi
    ) {}
}
