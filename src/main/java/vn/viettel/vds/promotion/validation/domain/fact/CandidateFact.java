package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CandidateFact(
        String type,
        String key,
        String campaignId,
        String promotionId,
        String name,
        String description,
        String status,
        Instant startDate,
        Instant endDate,
        Map<String, Object> configuration,
        Map<String, Object> constraints
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String key;
        private String campaignId;
        private String promotionId;
        private String name;
        private String description;
        private String status;
        private Instant startDate;
        private Instant endDate;
        private Map<String, Object> configuration;
        private Map<String, Object> constraints;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder campaignId(String campaignId) {
            this.campaignId = campaignId;
            return this;
        }

        public Builder promotionId(String promotionId) {
            this.promotionId = promotionId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder configuration(Map<String, Object> configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder constraints(Map<String, Object> constraints) {
            this.constraints = constraints;
            return this;
        }

        public CandidateFact build() {
            return new CandidateFact(
                    type, key, campaignId, promotionId, name, description,
                    status, startDate, endDate, configuration, constraints
            );
        }
    }
}