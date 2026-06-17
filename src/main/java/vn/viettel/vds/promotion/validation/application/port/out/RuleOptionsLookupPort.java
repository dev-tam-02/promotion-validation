package vn.viettel.vds.promotion.validation.application.port.out;

import java.util.List;

/**
 * Output port for looking up rule option values from external data sources.
 * Implementations route by dataSourceType (e.g., "SEGMENT") and call the
 * corresponding external service.
 */
public interface RuleOptionsLookupPort {

    /**
     * Look up a paginated list of selectable options for a rule.
     *
     * @param dataSourceType     the type of data source (e.g., "SEGMENT")
     * @param dataSourceEndpoint the full endpoint path stored in DB (metadata/audit only)
     * @param search             optional name filter
     * @param page               0-based page number
     * @param size               page size
     * @param tenantId           the current tenant identifier
     * @return paginated options; never null
     */
    RuleOptionsPage lookup(String dataSourceType, String dataSourceEndpoint,
                           String search, int page, int size, String tenantId);

    /**
     * Resolve a specific set of value ids to their display names + entity type,
     * by querying each id directly in the downstream service — NOT by paging the
     * full catalog and matching client-side. This is the display/edit path: a
     * persisted condition stores only ids (no type), so each id is looked up
     * across the source's sub-types (PRODUCT → product / sku / collection) and the
     * matched {@code metadata.type} is returned so the UI can render the badge.
     *
     * @param dataSourceType     the type of data source (e.g., "PRODUCT")
     * @param dataSourceEndpoint the full endpoint path stored in DB (metadata/audit only)
     * @param ids                the raw value ids to resolve
     * @param tenantId           the current tenant identifier
     * @return options for the resolved ids (type-tagged); never null
     */
    RuleOptionsPage lookupByIds(String dataSourceType, String dataSourceEndpoint,
                                List<String> ids, String tenantId);
}
