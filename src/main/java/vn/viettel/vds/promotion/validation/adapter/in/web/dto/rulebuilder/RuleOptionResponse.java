package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Individual option for a rule.
 * Represents a selectable value (e.g., a specific segment, product, etc).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleOptionResponse(
        String value,
        I18nLabel label,
        I18nLabel description,
        Map<String, Object> metadata
) {
    public static RuleOptionResponse of(String value, String enLabel, String viLabel) {
        return new RuleOptionResponse(
                value,
                I18nLabel.of(enLabel, viLabel),
                null,
                null
        );
    }

    public static RuleOptionResponse of(String value, String enLabel, String viLabel, Map<String, Object> metadata) {
        return new RuleOptionResponse(
                value,
                I18nLabel.of(enLabel, viLabel),
                null,
                metadata
        );
    }

    public static RuleOptionResponse withDescription(String value, String enLabel, String viLabel, String enDesc, String viDesc) {
        return new RuleOptionResponse(
                value,
                I18nLabel.of(enLabel, viLabel),
                I18nLabel.of(enDesc, viDesc),
                null
        );
    }
}
