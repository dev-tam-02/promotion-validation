package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;

import java.util.List;

/**
 * Use case for browsing the operator catalog.
 * Used by the FE RuleBuilder to populate category/operator dropdowns.
 */
public interface RuleCatalogUseCase {

    /**
     * List all active operator categories ordered by display_order.
     */
    List<OperatorCategory> listCategories();

    /**
     * List active operators, optionally filtered by category code.
     *
     * @param categoryId optional category id to filter — null returns all
     */
    List<Operator> listOperators(String categoryId);
}
