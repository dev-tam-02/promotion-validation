package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerFact(
        String customerId,
        String email,
        String phone,
        String tier,
        Instant registrationDate,
        Instant lastActivityDate,
        Boolean isActive,
        String region,
        String language,
        String currency,
        List<String> tags,
        Map<String, Object> attributes,
        Map<String, Object> preferences
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String customerId;
        private String email;
        private String phone;
        private String tier;
        private Instant registrationDate;
        private Instant lastActivityDate;
        private Boolean isActive;
        private String region;
        private String language;
        private String currency;
        private List<String> tags;
        private Map<String, Object> attributes;
        private Map<String, Object> preferences;

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder tier(String tier) {
            this.tier = tier;
            return this;
        }

        public Builder registrationDate(Instant registrationDate) {
            this.registrationDate = registrationDate;
            return this;
        }

        public Builder lastActivityDate(Instant lastActivityDate) {
            this.lastActivityDate = lastActivityDate;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder preferences(Map<String, Object> preferences) {
            this.preferences = preferences;
            return this;
        }

        public CustomerFact build() {
            return new CustomerFact(
                    customerId, email, phone, tier, registrationDate, lastActivityDate,
                    isActive, region, language, currency, tags, attributes, preferences
            );
        }
    }
}