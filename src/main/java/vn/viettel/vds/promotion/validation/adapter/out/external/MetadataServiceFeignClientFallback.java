package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Fallback for {@link MetadataServiceFeignClient}.
 * Returns empty envelopes when pp-metadata is unavailable so the rule
 * builder UI degrades gracefully (metadata category renders with zero rules).
 */
@Component
public class MetadataServiceFeignClientFallback implements MetadataServiceFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(MetadataServiceFeignClientFallback.class);

    @Override
    public Map<String, Object> listSchemas(String type, String name, Integer page, Integer size) {
        logger.warn("Metadata service unavailable (listSchemas type={}, name={}); returning empty page", type, name);
        return emptyPage();
    }

    @Override
    public Map<String, Object> getSchemaById(String schemaId, Integer page, Integer size) {
        logger.warn("Metadata service unavailable (getSchemaById id={}); returning empty schema", schemaId);
        return Map.of(
                "data", Map.of(
                        "id", schemaId,
                        "definitions", Map.of("content", Collections.emptyList(), "totalElements", 0)
                )
        );
    }

    private Map<String, Object> emptyPage() {
        return Map.of(
                "data", Map.of(
                        "content", Collections.emptyList(),
                        "totalElements", 0
                )
        );
    }
}
