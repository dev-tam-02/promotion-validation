package vn.viettel.vds.promotion.validation.domain.enums;

/**
 * Enum defining supported validation rule contexts.
 *
 * <p>Each value represents a scenario in which the rule applies. Returned
 * by {@code GET /v1/rules/contexts} for the Step 1 dropdown of the rule
 * builder; {@code label}/{@code labelVi} is what the FE renders.
 *
 * <p>Must stay in sync with the FE {@code ValidationRuleContext} enum.
 */
public enum RuleContextType {

    GENERAL_USAGE("General usage", "Chung");

    private final String label;
    private final String labelVi;

    RuleContextType(String label, String labelVi) {
        this.label = label;
        this.labelVi = labelVi;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelVi() {
        return labelVi;
    }
}
