package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("unused")
public class ValidationSettingsDTO {
    private Timeframe timeframe;
    private String ruleId;
    private Boolean active;
    private Integer trafficPercent;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Timeframe timeframe;
        private String ruleId;
        private Boolean active;
        private Integer trafficPercent;

        public Builder timeframe(Timeframe timeframe) {
            this.timeframe = timeframe;
            return this;
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder trafficPercent(Integer trafficPercent) {
            this.trafficPercent = trafficPercent;
            return this;
        }

        public ValidationSettingsDTO build() {
            return new ValidationSettingsDTO(timeframe, ruleId, active, trafficPercent);
        }
    }

    @Data
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings("unused")
    public static class Timeframe {
        private ValidityTimeframe validityTimeframe;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ValidityTimeframe validityTimeframe;

            public Builder validityTimeframe(ValidityTimeframe validityTimeframe) {
                this.validityTimeframe = validityTimeframe;
                return this;
            }

            public Timeframe build() {
                return new Timeframe(validityTimeframe);
            }
        }
    }

    @Data
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings("unused")
    public static class ValidityTimeframe {
        private String startDate;
        private String expirationDate;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String startDate;
            private String expirationDate;

            public Builder startDate(String startDate) {
                this.startDate = startDate;
                return this;
            }

            public Builder expirationDate(String expirationDate) {
                this.expirationDate = expirationDate;
                return this;
            }

            public ValidityTimeframe build() {
                return new ValidityTimeframe(startDate, expirationDate);
            }
        }
    }
}
