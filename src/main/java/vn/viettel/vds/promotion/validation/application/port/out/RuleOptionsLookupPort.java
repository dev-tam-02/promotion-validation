package vn.viettel.vds.promotion.validation.application.port.out;

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
}
