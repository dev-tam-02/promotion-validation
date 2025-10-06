package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.application.port.in.dto.RuleResponse;
import vn.viettel.vds.promotion.validation.application.port.in.query.GetRuleByIdQuery;

/**
 * Use case interface for retrieving a rule by ID
 */
public interface GetRuleByIdUseCase {

    /**
     * Execute the get rule by ID use case
     *
     * @param query The get rule by ID query
     * @return The rule response
     */
    RuleResponse execute(GetRuleByIdQuery query);
}