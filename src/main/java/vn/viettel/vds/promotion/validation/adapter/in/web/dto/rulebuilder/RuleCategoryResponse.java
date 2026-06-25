package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Rule category grouping related rules together.
 * Categories organize rules into logical groups (Audience, Products, etc).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleCategoryResponse(
        String id,
        String code,
        Map<String, String> name,
        String icon,
        Integer order,
        List<RuleItemResponse> rules
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String code;
        private Map<String, String> name;
        private String icon;
        private Integer order;
        private List<RuleItemResponse> rules;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder name(String en, String vi) {
            this.name = LocaleText.of(en, vi);
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder order(Integer order) {
            this.order = order;
            return this;
        }

        public Builder rules(List<RuleItemResponse> rules) {
            this.rules = rules;
            return this;
        }

        public RuleCategoryResponse build() {
            return new RuleCategoryResponse(id, code, name, icon, order, rules);
        }
    }
}
