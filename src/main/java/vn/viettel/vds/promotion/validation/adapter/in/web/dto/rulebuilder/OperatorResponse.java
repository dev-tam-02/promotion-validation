package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Operator definition for a rule. One canonical vocabulary end-to-end: {@code value}
 * and {@code canonical} are the same {@code ConditionOperator} code (SCREAMING_SNAKE),
 * used verbatim as wire by FE / operator_options / rule_nodes.operator_name / engine.
 * {@code label} is resolved (localized) from the {@code translations} table.
 * {@code valueShape} is the value form the operator needs (NONE/SINGLE/MULTI/RANGE).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OperatorResponse(
        String value,
        I18nLabel label,
        I18nLabel description,
        String canonical,
        String valueShape
) {
    public static OperatorResponse of(String value, String enLabel, String viLabel) {
        return new OperatorResponse(
                value,
                I18nLabel.of(enLabel, viLabel),
                null,
                null,
                null
        );
    }

    public static OperatorResponse of(String value, String enLabel, String viLabel,
                                      String canonical, String valueShape) {
        return new OperatorResponse(
                value,
                I18nLabel.of(enLabel, viLabel),
                null,
                canonical,
                valueShape
        );
    }
}
