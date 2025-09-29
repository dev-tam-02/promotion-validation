package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FactPack(
    String factPackVersion,
    Instant timestamp,
    CustomerFact customer,
    OrderFact order,
    CandidateFact candidate,
    SegmentsFact segments,
    LimitsFact limits,
    MetadataFact metadata,
    GeoFact geo,
    Map<String, Object> derived,
    ProvenanceInfo provenance
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String factPackVersion = "1.0";
        private Instant timestamp = Instant.now();
        private CustomerFact customer;
        private OrderFact order;
        private CandidateFact candidate;
        private SegmentsFact segments;
        private LimitsFact limits;
        private MetadataFact metadata;
        private GeoFact geo;
        private Map<String, Object> derived;
        private ProvenanceInfo provenance;

        public Builder factPackVersion(String factPackVersion) {
            this.factPackVersion = factPackVersion;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder customer(CustomerFact customer) {
            this.customer = customer;
            return this;
        }

        public Builder order(OrderFact order) {
            this.order = order;
            return this;
        }

        public Builder candidate(CandidateFact candidate) {
            this.candidate = candidate;
            return this;
        }

        public Builder segments(SegmentsFact segments) {
            this.segments = segments;
            return this;
        }

        public Builder limits(LimitsFact limits) {
            this.limits = limits;
            return this;
        }

        public Builder metadata(MetadataFact metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder geo(GeoFact geo) {
            this.geo = geo;
            return this;
        }

        public Builder derived(Map<String, Object> derived) {
            this.derived = derived;
            return this;
        }

        public Builder provenance(ProvenanceInfo provenance) {
            this.provenance = provenance;
            return this;
        }

        public FactPack build() {
            return new FactPack(
                factPackVersion, timestamp, customer, order, candidate,
                segments, limits, metadata, geo, derived, provenance
            );
        }
    }
}