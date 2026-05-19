package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorOptionEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for operator options.
 */
@ConditionalOnPromixJpa
@Repository
public interface OperatorOptionJpaRepository extends JpaRepository<OperatorOptionEntity, String> {

    /**
     * Find options by category ID.
     */
    List<OperatorOptionEntity> findByCategoryIdOrderByDisplayOrderAsc(String categoryId);

    /**
     * Find active options by category ID.
     */
    List<OperatorOptionEntity> findByCategoryIdAndActiveTrueOrderByDisplayOrderAsc(String categoryId);

    /**
     * Find option by category ID and code.
     */
    Optional<OperatorOptionEntity> findByCategoryIdAndCode(String categoryId, String code);

    /**
     * First active option matching the given operator name, ordered by displayOrder.
     * Safe to call when multiple rows share the same canonical operator_name.
     */
    Optional<OperatorOptionEntity> findFirstByOperatorNameOrderByDisplayOrderAsc(String operatorName);

    /**
     * Find active options by operator name.
     */
    List<OperatorOptionEntity> findByOperatorNameAndActiveTrueOrderByDisplayOrderAsc(String operatorName);

    /**
     * Check if option exists by category ID and code.
     */
    boolean existsByCategoryIdAndCode(String categoryId, String code);

    /**
     * Find all active options.
     */
    List<OperatorOptionEntity> findByActiveTrueOrderByDisplayOrderAsc();
}
