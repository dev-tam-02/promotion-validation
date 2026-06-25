package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the open {@code locale -> value} map the rule-builder catalog returns for
 * every translatable field (replaces the fixed {@code {en, vi}} struct). The map is
 * extensible: seeding a new locale into {@code translations} flows through to the FE
 * without a contract change. The FE resolves the value for the active language and
 * falls back across locales.
 */
public final class LocaleText {

    private LocaleText() {
    }

    /**
     * Build a locale map from English + Vietnamese values, skipping nulls. Returns
     * {@code null} when both are absent so {@code @JsonInclude(NON_NULL)} omits the field.
     */
    public static Map<String, String> of(String en, String vi) {
        Map<String, String> map = new LinkedHashMap<>();
        if (vi != null) {
            map.put("vi", vi);
        }
        if (en != null) {
            map.put("en", en);
        }
        return map.isEmpty() ? null : map;
    }
}
