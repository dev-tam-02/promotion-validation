package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorCategoryEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for operator categories.
 */
@ConditionalOnPromixJpa
@Repository
public interface OperatorCategoryJpaRepository extends JpaRepository<OperatorCategoryEntity, String> {

    /**
     * Find category by code.
     */
    Optional<OperatorCategoryEntity> findByCode(String code);

    /**
     * Find all active categories ordered by display order.
     */
    List<OperatorCategoryEntity> findByActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find all categories ordered by display order.
     */
    List<OperatorCategoryEntity> findAllByOrderByDisplayOrderAsc();

    /**
     * Find metadata categories.
     */
    List<OperatorCategoryEntity> findByMetadataCategoryTrueAndActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find non-metadata categories.
     */
    List<OperatorCategoryEntity> findByMetadataCategoryFalseAndActiveTrueOrderByDisplayOrderAsc();

    /**
     * Check if category exists by code.
     */
    boolean existsByCode(String code);

    /**
     * Find categories with their options eagerly loaded.
     */
    @Query("SELECT DISTINCT c FROM OperatorCategoryEntity c LEFT JOIN FETCH c.options WHERE c.active = true ORDER BY c.displayOrder")
    List<OperatorCategoryEntity> findAllWithOptions();
}
