package vn.viettel.vds.promotion.validation.application.usecase.impl;

import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.application.port.in.ListRulesUseCase;
import vn.viettel.vds.promotion.validation.application.port.in.dto.PagedRulesResponse;
import vn.viettel.vds.promotion.validation.application.port.in.query.ListRulesQuery;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;

/**
 * Implementation of ListRulesUseCase
 */
@Service
public class ListRulesUseCaseImpl implements ListRulesUseCase {

    private final RulePersistencePort rulePersistencePort;

    public ListRulesUseCaseImpl(RulePersistencePort rulePersistencePort) {
        this.rulePersistencePort = rulePersistencePort;
    }

    @Override
    public PagedRulesResponse execute(ListRulesQuery query) {
        // Note: This use case is not currently in use - uses incompatible domain model
        // Minimal implementation to allow compilation
        throw new UnsupportedOperationException("This use case is deprecated and not currently supported");
    }
}