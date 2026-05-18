package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign client for pp-metadata.
 *
 * <p>Used by RuleBuilderService to dynamically expand the 3 metadata
 * categories (customer/order/redemption) into one rule per field defined
 * in pp-metadata.
 *
 * <p>Full path inlined in {@code @GetMapping} because pp-metadata's
 * {@code context-path} (= {@code promotion/promotion-metadata}) is not
 * exposed via Feign's {@code path=}. Base URL comes from
 * {@code external.services.metadata.url} ({@code host:port}).
 */
@FeignClient(
        name = "metadata-service",
        url = "${external.services.metadata.url}",
        configuration = ExternalServiceFeignConfig.class,
        fallback = MetadataServiceFeignClientFallback.class
)
public interface MetadataServiceFeignClient {

    /**
     * GET /promotion/promotion-metadata/api/v1/schemas?type=STANDARD&name={name}
     * Returns a page of MetadataSchema summaries — used to look up the schemaId.
     */
    @GetMapping(
            value = "/promotion/promotion-metadata/api/v1/schemas",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Map<String, Object> listSchemas(
            @RequestParam(value = "type") String type,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    );

    /**
     * GET /promotion/promotion-metadata/api/v1/schemas/{schemaId}
     * Returns schema details including the list of field definitions.
     */
    @GetMapping(
            value = "/promotion/promotion-metadata/api/v1/schemas/{schemaId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Map<String, Object> getSchemaById(
            @PathVariable("schemaId") String schemaId,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") Integer size
    );
}
