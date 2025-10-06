package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SegmentsFact(
        List<String> segmentIds,
        List<SegmentInfo> segments,
        Instant evaluatedAt,
        String evaluationContext
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> segmentIds;
        private List<SegmentInfo> segments;
        private Instant evaluatedAt;
        private String evaluationContext;

        public Builder segmentIds(List<String> segmentIds) {
            this.segmentIds = segmentIds;
            return this;
        }

        public Builder segments(List<SegmentInfo> segments) {
            this.segments = segments;
            return this;
        }

        public Builder evaluatedAt(Instant evaluatedAt) {
            this.evaluatedAt = evaluatedAt;
            return this;
        }

        public Builder evaluationContext(String evaluationContext) {
            this.evaluationContext = evaluationContext;
            return this;
        }

        public SegmentsFact build() {
            return new SegmentsFact(segmentIds, segments, evaluatedAt, evaluationContext);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SegmentInfo(
            String id,
            String name,
            String type,
            Double score,
            Map<String, Object> properties
    ) {
    }
}