package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FactRequest(
    String customerId,
    String orderId,
    CandidateInfo candidate,
    String timezone,
    Map<String, Object> embeddedPayload,
    Map<String, Object> context,
    FetchOptions fetchOptions
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String customerId;
        private String orderId;
        private CandidateInfo candidate;
        private String timezone;
        private Map<String, Object> embeddedPayload;
        private Map<String, Object> context;
        private FetchOptions fetchOptions;

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder candidate(CandidateInfo candidate) {
            this.candidate = candidate;
            return this;
        }

        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder embeddedPayload(Map<String, Object> embeddedPayload) {
            this.embeddedPayload = embeddedPayload;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public Builder fetchOptions(FetchOptions fetchOptions) {
            this.fetchOptions = fetchOptions;
            return this;
        }

        public FactRequest build() {
            return new FactRequest(customerId, orderId, candidate, timezone, embeddedPayload, context, fetchOptions);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CandidateInfo(
        String type,
        String key,
        String campaignId
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FetchOptions(
        Boolean allowPartial,
        Boolean enableFallback,
        Long timeoutMs,
        Boolean skipCache
    ) {}
}