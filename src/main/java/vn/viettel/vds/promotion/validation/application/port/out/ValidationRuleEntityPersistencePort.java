package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.Optional;

/**
 * Outbound port for Rule entity persistence operations.
 * This port handles persistence of Rule domain models.
 */
public interface ValidationRuleEntityPersistencePort {

    /**
     * Save or update a validation rule
     */
    Rule save(Rule rule);

    /**
     * Find validation rule by ID
     */
    Optional<Rule> findById(String id);

    /**
     * Find rule by code
     */
    Optional<Rule> findByCode(String code);

    /**
     * Check if rule exists by code
     */
    boolean existsByCode(String code);

    /**
     * Find latest version by code
     */
    Optional<Rule> findTopByCodeOrderByVersionDesc(String code);

    /**
     * Delete rule by ID
     */
    void deleteById(String id);

    /**
     * Check if rule exists by ID
     */
    boolean existsById(String id);
}
