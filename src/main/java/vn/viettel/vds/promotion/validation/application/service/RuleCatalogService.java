package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.application.port.in.RuleCatalogUseCase;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorCategoryPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;

import java.util.List;

/**
 * Implementation of {@link RuleCatalogUseCase}.
 * Provides read-only browsing of operator categories and operators for the FE RuleBuilder.
 */
@Service
public class RuleCatalogService implements RuleCatalogUseCase {

    private static final Logger log = LoggerFactory.getLogger(RuleCatalogService.class);

    private final OperatorCategoryPersistencePort categoryPort;
    private final OperatorPersistencePort operatorPort;

    public RuleCatalogService(OperatorCategoryPersistencePort categoryPort,
                              OperatorPersistencePort operatorPort) {
        this.categoryPort = categoryPort;
        this.operatorPort = operatorPort;
    }

    @Override
    public List<OperatorCategory> listCategories() {
        log.debug("listCategories called");
        return categoryPort.findAllActiveWithOptions();
    }

    @Override
    public List<Operator> listOperators(String categoryId) {
        log.debug("listOperators called: categoryId={}", categoryId);
        // Use global (tenantId=null) active operators
        List<Operator> all = operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE);
        if (categoryId == null || categoryId.isBlank()) {
            return all;
        }
        // Filter by categoryId via context (categories map to contexts in existing model)
        return all.stream()
                .filter(op -> categoryId.equalsIgnoreCase(op.getContext()))
                .toList();
    }
}
