package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Operator definition for a rule.
 * Defines how values should be compared (is, is_not, greater_than, etc).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OperatorResponse(
        String value,
        I18nLabel label,
        I18nLabel description
) {
    public static OperatorResponse of(String value, String enLabel, String viLabel) {
        return new OperatorResponse(
                value,
                I18nLabel.of(enLabel, viLabel),
                null
        );
    }

    public static OperatorResponse of(String value, String enLabel, String viLabel, String enDesc, String viDesc) {
        return new OperatorResponse(
                value,
                I18nLabel.of(enLabel, viLabel),
                I18nLabel.of(enDesc, viDesc)
        );
    }
}
