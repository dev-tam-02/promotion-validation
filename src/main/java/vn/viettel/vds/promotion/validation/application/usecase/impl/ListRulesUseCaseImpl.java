package vn.viettel.vds.promotion.validation.application.usecase.impl;

import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.application.port.in.ListRulesUseCase;
import vn.viettel.vds.promotion.validation.application.port.in.dto.PagedRulesResponse;
import vn.viettel.vds.promotion.validation.application.port.in.dto.RuleResponse;
import vn.viettel.vds.promotion.validation.application.port.in.query.ListRulesQuery;
import vn.viettel.vds.promotion.validation.application.port.out.PagedResult;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.SortDirection;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.valueobject.TenantId;

import java.util.List;
import java.util.stream.Collectors;

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