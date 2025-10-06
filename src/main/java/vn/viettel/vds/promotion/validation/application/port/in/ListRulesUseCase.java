package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.application.port.in.dto.PagedRulesResponse;
import vn.viettel.vds.promotion.validation.application.port.in.query.ListRulesQuery;

/**
 * Use case interface for listing rules with pagination and filtering
 */
public interface ListRulesUseCase {

    /**
     * Execute the list rules use case
     *
     * @param query The list rules query
     * @return The paged rules response
     */
    PagedRulesResponse execute(ListRulesQuery query);
}