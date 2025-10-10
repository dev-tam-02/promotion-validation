package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.List;
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
     * Find rules by state
     */
    List<Rule> findByState(String state);

    /**
     * Find rules by state with pagination
     */
    Page<Rule> findByState(String state, Pageable pageable);

    /**
     * Find published rules by version
     */
    List<Rule> findByStateAndVersionGreaterThan(String state, Integer version);

    /**
     * Check if rule exists by code
     */
    boolean existsByCode(String code);

    /**
     * Find latest version by code
     */
    Optional<Rule> findTopByCodeOrderByVersionDesc(String code);

    /**
     * Find rules by state ordered by version
     */
    List<Rule> findByStateOrderByVersionDesc(String state);

    /**
     * Delete rule by ID
     */
    void deleteById(String id);

    /**
     * Check if rule exists by ID
     */
    boolean existsById(String id);
}
