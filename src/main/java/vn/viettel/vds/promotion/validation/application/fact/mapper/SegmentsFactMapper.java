package vn.viettel.vds.promotion.validation.application.fact.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.SegmentsFact;

import java.util.Map;

@Component
public class SegmentsFactMapper {

    public SegmentsFact map(Object data) {
        if (data == null) {
            return null;
        }

        if (data instanceof SegmentsFact) {
            return (SegmentsFact) data;
        }

        if (data instanceof Map) {
            // Handle case where data comes as Map (from cache or other sources)
            Map<String, Object> mapData = (Map<String, Object>) data;
            return mapFromData(mapData);
        }

        return null;
    }

    private SegmentsFact mapFromData(Map<String, Object> data) {
        SegmentsFact.Builder builder = SegmentsFact.builder();

        if (data.get("segmentIds") instanceof java.util.List) {
            builder.segmentIds((java.util.List<String>) data.get("segmentIds"));
        }
        if (data.get("segments") instanceof java.util.List) {
            builder.segments((java.util.List<SegmentsFact.SegmentInfo>) data.get("segments"));
        }
        if (data.get("evaluatedAt") != null) {
            builder.evaluatedAt(java.time.Instant.parse(data.get("evaluatedAt").toString()));
        }
        if (data.get("evaluationContext") != null) {
            builder.evaluationContext((String) data.get("evaluationContext"));
        }

        return builder.build();
    }
}