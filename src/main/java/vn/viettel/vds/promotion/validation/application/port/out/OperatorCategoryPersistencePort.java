package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;

import java.util.List;
import java.util.Optional;

/**
 * Port for operator category persistence operations.
 */
public interface OperatorCategoryPersistencePort {

    /**
     * Find all active categories with their options.
     */
    List<OperatorCategory> findAllActiveWithOptions();

    /**
     * Find all categories (including inactive).
     */
    List<OperatorCategory> findAll();

    /**
     * Find category by ID.
     */
    Optional<OperatorCategory> findById(String id);

    /**
     * Find category by code.
     */
    Optional<OperatorCategory> findByCode(String code);

    /**
     * Find category by code with options.
     */
    Optional<OperatorCategory> findByCodeWithOptions(String code);

    /**
     * Find metadata categories.
     */
    List<OperatorCategory> findMetadataCategories();

    /**
     * Find non-metadata categories.
     */
    List<OperatorCategory> findNonMetadataCategories();

    /**
     * Save category.
     */
    OperatorCategory save(OperatorCategory category);

    /**
     * Check if category exists by code.
     */
    boolean existsByCode(String code);

    /**
     * Delete category by ID.
     */
    void deleteById(String id);
}
