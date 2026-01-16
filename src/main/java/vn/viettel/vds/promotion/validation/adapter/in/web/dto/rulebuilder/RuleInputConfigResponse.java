package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Input configuration for a rule.
 * Defines how the UI should render the input field.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleInputConfigResponse(
        String dataSourceType,
        String dataSourceEndpoint,
        I18nLabel label,
        I18nLabel placeholder,
        Boolean multiple,
        Boolean searchable,
        String inputType,
        String minValue,
        String maxValue,
        String step
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dataSourceType;
        private String dataSourceEndpoint;
        private I18nLabel label;
        private I18nLabel placeholder;
        private Boolean multiple;
        private Boolean searchable;
        private String inputType;
        private String minValue;
        private String maxValue;
        private String step;

        public Builder dataSourceType(String dataSourceType) {
            this.dataSourceType = dataSourceType;
            return this;
        }

        public Builder dataSourceEndpoint(String dataSourceEndpoint) {
            this.dataSourceEndpoint = dataSourceEndpoint;
            return this;
        }

        public Builder label(I18nLabel label) {
            this.label = label;
            return this;
        }

        public Builder label(String en, String vi) {
            this.label = I18nLabel.of(en, vi);
            return this;
        }

        public Builder placeholder(I18nLabel placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder placeholder(String en, String vi) {
            this.placeholder = I18nLabel.of(en, vi);
            return this;
        }

        public Builder multiple(Boolean multiple) {
            this.multiple = multiple;
            return this;
        }

        public Builder searchable(Boolean searchable) {
            this.searchable = searchable;
            return this;
        }

        public Builder inputType(String inputType) {
            this.inputType = inputType;
            return this;
        }

        public Builder minValue(String minValue) {
            this.minValue = minValue;
            return this;
        }

        public Builder maxValue(String maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public Builder step(String step) {
            this.step = step;
            return this;
        }

        public RuleInputConfigResponse build() {
            return new RuleInputConfigResponse(
                    dataSourceType,
                    dataSourceEndpoint,
                    label,
                    placeholder,
                    multiple,
                    searchable,
                    inputType,
                    minValue,
                    maxValue,
                    step
            );
        }
    }
}
