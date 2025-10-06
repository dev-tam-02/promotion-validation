package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProvenanceInfo(
        List<SourceInfo> sources,
        Instant aggregatedAt,
        String aggregationId,
        Long processingTimeMs,
        FetchPolicy fetchPolicy,
        Map<String, String> versions
) {
    public static Builder builder() {
        return new Builder();
    }

    public enum FetchPolicy {
        FROM_IDS,
        EMBEDDED_PAYLOAD,
        HYBRID,
        PARTIAL
    }

    public static class Builder {
        private List<SourceInfo> sources;
        private Instant aggregatedAt;
        private String aggregationId;
        private Long processingTimeMs;
        private FetchPolicy fetchPolicy;
        private Map<String, String> versions;

        public Builder sources(List<SourceInfo> sources) {
            this.sources = sources;
            return this;
        }

        public Builder aggregatedAt(Instant aggregatedAt) {
            this.aggregatedAt = aggregatedAt;
            return this;
        }

        public Builder aggregationId(String aggregationId) {
            this.aggregationId = aggregationId;
            return this;
        }

        public Builder processingTimeMs(Long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public Builder fetchPolicy(FetchPolicy fetchPolicy) {
            this.fetchPolicy = fetchPolicy;
            return this;
        }

        public Builder versions(Map<String, String> versions) {
            this.versions = versions;
            return this;
        }

        public ProvenanceInfo build() {
            return new ProvenanceInfo(sources, aggregatedAt, aggregationId, processingTimeMs, fetchPolicy, versions);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SourceInfo(
            String name,
            String type,
            Instant fetchedAt,
            Long responseTimeMs,
            String status,
            String version,
            Boolean cached,
            String cacheKey
    ) {
    }
}