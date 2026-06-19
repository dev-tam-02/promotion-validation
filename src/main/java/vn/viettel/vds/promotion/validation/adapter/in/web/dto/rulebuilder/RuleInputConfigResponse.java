package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input configuration for a rule.
 * Defines how the UI should render the input field.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleInputConfigResponse(
        String dataSourceType,
        String dataSourceEndpoint,
        String dataLoaderType,
        String dataLoaderConfig,
        I18nLabel label,
        I18nLabel placeholder,
        Boolean multiple,
        Boolean searchable,
        String inputType,
        String minValue,
        String maxValue,
        String step,
        Integer minLength,
        Integer maxLength,
        Integer exactLength,
        List<BigDecimal> allowedNumbers,
        List<BigDecimal> excludedNumbers
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dataSourceType;
        private String dataSourceEndpoint;
        private String dataLoaderType;
        private String dataLoaderConfig;
        private I18nLabel label;
        private I18nLabel placeholder;
        private Boolean multiple;
        private Boolean searchable;
        private String inputType;
        private String minValue;
        private String maxValue;
        private String step;
        private Integer minLength;
        private Integer maxLength;
        private Integer exactLength;
        private List<BigDecimal> allowedNumbers;
        private List<BigDecimal> excludedNumbers;

        public Builder dataSourceType(String dataSourceType) {
            this.dataSourceType = dataSourceType;
            return this;
        }

        public Builder dataSourceEndpoint(String dataSourceEndpoint) {
            this.dataSourceEndpoint = dataSourceEndpoint;
            return this;
        }

        public Builder dataLoaderType(String dataLoaderType) {
            this.dataLoaderType = dataLoaderType;
            return this;
        }

        public Builder dataLoaderConfig(String dataLoaderConfig) {
            this.dataLoaderConfig = dataLoaderConfig;
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

        public Builder minLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder exactLength(Integer exactLength) {
            this.exactLength = exactLength;
            return this;
        }

        public Builder allowedNumbers(List<BigDecimal> allowedNumbers) {
            this.allowedNumbers = allowedNumbers;
            return this;
        }

        public Builder excludedNumbers(List<BigDecimal> excludedNumbers) {
            this.excludedNumbers = excludedNumbers;
            return this;
        }

        public RuleInputConfigResponse build() {
            return new RuleInputConfigResponse(
                    dataSourceType,
                    dataSourceEndpoint,
                    dataLoaderType,
                    dataLoaderConfig,
                    label,
                    placeholder,
                    multiple,
                    searchable,
                    inputType,
                    minValue,
                    maxValue,
                    step,
                    minLength,
                    maxLength,
                    exactLength,
                    allowedNumbers,
                    excludedNumbers
            );
        }
    }
}
