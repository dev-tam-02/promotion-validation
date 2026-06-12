package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleContextEntity;

import java.util.List;

/**
 * JPA repository for the {@code rule_contexts} lookup table.
 */
@ConditionalOnPromixJpa
@Repository
public interface RuleContextJpaRepository extends JpaRepository<RuleContextEntity, String> {

    /**
     * Find all contexts with the given status, ordered by code.
     */
    List<RuleContextEntity> findByStatusOrderByCodeAsc(String status);

    /**
     * Whether a context with the given code (case-insensitive) and status exists.
     */
    boolean existsByCodeIgnoreCaseAndStatus(String code, String status);
}
