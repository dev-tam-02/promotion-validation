package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LimitsFact(
    List<LimitInfo> globalLimits,
    List<LimitInfo> customerLimits,
    List<LimitInfo> campaignLimits,
    Map<String, UsageCounter> counters,
    Instant snapshotAt
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<LimitInfo> globalLimits;
        private List<LimitInfo> customerLimits;
        private List<LimitInfo> campaignLimits;
        private Map<String, UsageCounter> counters;
        private Instant snapshotAt;

        public Builder globalLimits(List<LimitInfo> globalLimits) {
            this.globalLimits = globalLimits;
            return this;
        }

        public Builder customerLimits(List<LimitInfo> customerLimits) {
            this.customerLimits = customerLimits;
            return this;
        }

        public Builder campaignLimits(List<LimitInfo> campaignLimits) {
            this.campaignLimits = campaignLimits;
            return this;
        }

        public Builder counters(Map<String, UsageCounter> counters) {
            this.counters = counters;
            return this;
        }

        public Builder snapshotAt(Instant snapshotAt) {
            this.snapshotAt = snapshotAt;
            return this;
        }

        public LimitsFact build() {
            return new LimitsFact(globalLimits, customerLimits, campaignLimits, counters, snapshotAt);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LimitInfo(
        String type,
        String scope,
        String period,
        BigDecimal limit,
        BigDecimal used,
        BigDecimal remaining
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UsageCounter(
        String key,
        BigDecimal count,
        String period,
        Instant lastUpdated,
        Instant resetAt
    ) {}
}