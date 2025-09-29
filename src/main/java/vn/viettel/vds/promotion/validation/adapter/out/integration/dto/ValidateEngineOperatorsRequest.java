package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ValidateEngineOperatorsRequest {

    @JsonProperty("operators")
    private List<OperatorValidationItem> operators;

    public ValidateEngineOperatorsRequest() {
    }

    public ValidateEngineOperatorsRequest(List<OperatorValidationItem> operators) {
        this.operators = operators;
    }

    public List<OperatorValidationItem> getOperators() {
        return operators;
    }

    public void setOperators(List<OperatorValidationItem> operators) {
        this.operators = operators;
    }

    public static class OperatorValidationItem {
        @JsonProperty("operatorName")
        private String operatorName;

        @JsonProperty("version")
        private Integer version;

        public OperatorValidationItem() {
        }

        public OperatorValidationItem(String operatorName, Integer version) {
            this.operatorName = operatorName;
            this.version = version;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }
}