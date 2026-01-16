package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Rule category grouping related rules together.
 * Categories organize rules into logical groups (Audience, Products, etc).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleCategoryResponse(
        String id,
        String code,
        I18nLabel name,
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
        private I18nLabel name;
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

        public Builder name(I18nLabel name) {
            this.name = name;
            return this;
        }

        public Builder name(String en, String vi) {
            this.name = I18nLabel.of(en, vi);
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
