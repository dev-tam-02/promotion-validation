package vn.viettel.vds.promotion.validation.application.usecase.impl;

import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.application.port.in.GetRuleByIdUseCase;
import vn.viettel.vds.promotion.validation.application.port.in.dto.RuleResponse;
import vn.viettel.vds.promotion.validation.application.port.in.query.GetRuleByIdQuery;

/**
 * Implementation of GetRuleByIdUseCase
 */
@Service
public class GetRuleByIdUseCaseImpl implements GetRuleByIdUseCase {

    @Override
    public RuleResponse execute(GetRuleByIdQuery query) {
        // Note: This use case is not currently in use - uses incompatible domain model
        // Minimal implementation to allow compilation
        throw new UnsupportedOperationException("This use case is deprecated and not currently supported");
    }
}