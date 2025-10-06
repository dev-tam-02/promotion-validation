package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRule;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for ValidationRule entity persistence operations.
 * This port handles persistence of ValidationRule entities (not domain models).
 */
public interface ValidationRuleEntityPersistencePort {

    /**
     * Save or update a validation rule entity
     */
    ValidationRule save(ValidationRule validationRule);

    /**
     * Find validation rule by ID
     */
    Optional<ValidationRule> findById(String id);

    /**
     * Find rule by code
     */
    Optional<ValidationRule> findByCode(String code);

    /**
     * Find rules by state
     */
    List<ValidationRule> findByState(String state);

    /**
     * Find rules by state with pagination
     */
    Page<ValidationRule> findByState(String state, Pageable pageable);

    /**
     * Find published rules by version
     */
    List<ValidationRule> findByStateAndVersionGreaterThan(String state, Integer version);

    /**
     * Check if rule exists by code
     */
    boolean existsByCode(String code);

    /**
     * Find latest version by code
     */
    Optional<ValidationRule> findTopByCodeOrderByVersionDesc(String code);

    /**
     * Find rules by state ordered by version
     */
    List<ValidationRule> findByStateOrderByVersionDesc(String state);

    /**
     * Delete rule by ID
     */
    void deleteById(String id);

    /**
     * Check if rule exists by ID
     */
    boolean existsById(String id);
}
