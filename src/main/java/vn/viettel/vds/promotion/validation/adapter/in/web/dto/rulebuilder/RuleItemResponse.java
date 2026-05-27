package vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Individual rule definition within a category.
 * Represents a specific validation rule that can be configured.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleItemResponse(
        String id,
        String code,
        I18nLabel name,
        I18nLabel description,
        String type,
        Boolean autoApply,
        String defaultOperator,
        String operatorName,
        String defaultComparator,
        RuleInputConfigResponse inputConfig,
        List<OperatorResponse> operators,
        /**
         * Static params the FE must relay verbatim into the saved COND node's
         * params (in addition to the user-chosen comparator + value). Used by
         * metadata rules driving the {@code metadata.access} operator:
         * {@code {schema_type, field_key, data_type}}. Null for static rules.
         */
        Map<String, Object> operatorParams
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String code;
        private I18nLabel name;
        private I18nLabel description;
        private String type;
        private Boolean autoApply;
        private String defaultOperator;
        private String operatorName;
        private String defaultComparator;
        private RuleInputConfigResponse inputConfig;
        private List<OperatorResponse> operators;
        private Map<String, Object> operatorParams;

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

        public Builder description(I18nLabel description) {
            this.description = description;
            return this;
        }

        public Builder description(String en, String vi) {
            this.description = I18nLabel.of(en, vi);
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder autoApply(Boolean autoApply) {
            this.autoApply = autoApply;
            return this;
        }

        public Builder defaultOperator(String defaultOperator) {
            this.defaultOperator = defaultOperator;
            return this;
        }

        public Builder operatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        public Builder defaultComparator(String defaultComparator) {
            this.defaultComparator = defaultComparator;
            return this;
        }

        public Builder inputConfig(RuleInputConfigResponse inputConfig) {
            this.inputConfig = inputConfig;
            return this;
        }

        public Builder operators(List<OperatorResponse> operators) {
            this.operators = operators;
            return this;
        }

        public Builder operatorParams(Map<String, Object> operatorParams) {
            this.operatorParams = operatorParams;
            return this;
        }

        public RuleItemResponse build() {
            return new RuleItemResponse(id, code, name, description, type, autoApply, defaultOperator, operatorName, defaultComparator, inputConfig, operators, operatorParams);
        }
    }
}
