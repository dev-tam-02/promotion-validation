package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Internationalized label supporting multiple languages.
 * Used throughout Rule Builder API for multilingual support.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record I18nLabel(
        String en,
        String vi
) {
    /**
     * Create I18n label with English only.
     */
    public static I18nLabel en(String text) {
        return new I18nLabel(text, null);
    }

    /**
     * Create I18n label with both English and Vietnamese.
     */
    public static I18nLabel of(String en, String vi) {
        return new I18nLabel(en, vi);
    }

    /**
     * Create I18n label with same text for all languages.
     */
    public static I18nLabel same(String text) {
        return new I18nLabel(text, text);
    }
}
