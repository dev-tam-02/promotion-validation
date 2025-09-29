package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ValidateEngineOperatorsResponse {

    @JsonProperty("valid")
    private boolean valid;

    @JsonProperty("supportedOperators")
    private List<String> supportedOperators;

    @JsonProperty("unsupportedOperators")
    private List<UnsupportedOperator> unsupportedOperators;

    @JsonProperty("message")
    private String message;

    public ValidateEngineOperatorsResponse() {
    }

    public ValidateEngineOperatorsResponse(boolean valid, List<String> supportedOperators,
                                         List<UnsupportedOperator> unsupportedOperators, String message) {
        this.valid = valid;
        this.supportedOperators = supportedOperators;
        this.unsupportedOperators = unsupportedOperators;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getSupportedOperators() {
        return supportedOperators;
    }

    public void setSupportedOperators(List<String> supportedOperators) {
        this.supportedOperators = supportedOperators;
    }

    public List<UnsupportedOperator> getUnsupportedOperators() {
        return unsupportedOperators;
    }

    public void setUnsupportedOperators(List<UnsupportedOperator> unsupportedOperators) {
        this.unsupportedOperators = unsupportedOperators;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class UnsupportedOperator {
        @JsonProperty("operatorName")
        private String operatorName;

        @JsonProperty("version")
        private Integer version;

        @JsonProperty("reason")
        private String reason;

        public UnsupportedOperator() {
        }

        public UnsupportedOperator(String operatorName, Integer version, String reason) {
            this.operatorName = operatorName;
            this.version = version;
            this.reason = reason;
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

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}