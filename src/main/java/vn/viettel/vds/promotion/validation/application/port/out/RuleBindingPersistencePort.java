package vn.viettel.vds.promotion.validation.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Output port for RuleBinding persistence operations.
 * <p>
 * This port defines the interface for persisting and querying rule bindings.
 * Implementations are provided by the persistence adapter layer.
 */
public interface RuleBindingPersistencePort {

    // ========== Save Operations ==========

    /**
     * Save a rule binding (create or update)
     */
    RuleBinding save(RuleBinding binding);

    /**
     * Save multiple bindings
     */
    List<RuleBinding> saveAll(List<RuleBinding> bindings);

    // ========== Find by ID ==========

    /**
     * Find binding by ID
     */
    Optional<RuleBinding> findById(String id);

    // ========== Find by Object ==========

    /**
     * Find all bindings for a specific object
     */
    List<RuleBinding> findByObject(String objectType, String objectId);

    /**
     * Find all active bindings for an object, ordered by priority
     */
    List<RuleBinding> findActiveByObject(String objectType, String objectId);

    /**
     * Find bindings for multiple objects
     */
    List<RuleBinding> findByObjectTypeAndObjectIdIn(String objectType, List<String> objectIds);

    /**
     * Find the highest priority active binding for an object
     */
    Optional<RuleBinding> findTopActiveByObject(String objectType, String objectId);

    // ========== Find by Rule ==========

    /**
     * Find all bindings for a rule
     */
    List<RuleBinding> findByRuleId(String ruleId);

    /**
     * Find all active bindings for a rule
     */
    List<RuleBinding> findActiveByRuleId(String ruleId);

    // ========== Find by Time Range ==========

    /**
     * Find bindings effective at a specific time
     */
    List<RuleBinding> findEffectiveAtTime(String objectType, Instant timestamp);

    /**
     * Find object IDs with bindings in a time range
     */
    List<String> findObjectIdsByTypeAndTimeRange(String objectType, Instant startTs, Instant endTs);

    // ========== Search with Pagination ==========

    /**
     * Search bindings with filters and pagination
     */
    Page<RuleBinding> search(String objectType, String objectId, String ruleId, Boolean active, Pageable pageable);

    /**
     * Find all bindings for an object type with pagination
     */
    Page<RuleBinding> findByObjectType(String objectType, Pageable pageable);

    // ========== Existence Checks ==========

    /**
     * Check if a binding exists for an object and rule combination
     */
    boolean existsByObjectAndRule(String objectType, String objectId, String ruleId);

    /**
     * Check if any active binding exists for an object
     */
    boolean existsActiveByObject(String objectType, String objectId);

    // ========== Find by Active Status ==========

    /**
     * Find all bindings by active status
     */
    List<RuleBinding> findByActive(boolean active);

    // ========== Count Operations ==========

    /**
     * Count bindings for an object
     */
    long countByObject(String objectType, String objectId);

    /**
     * Count active bindings for a rule
     */
    long countActiveByRuleId(String ruleId);

    /**
     * Count all bindings (active and inactive) for a rule
     */
    long countByRuleId(String ruleId);

    /**
     * Count bindings by active status
     */
    long countByActive(boolean active);

    // ========== Delete Operations ==========

    /**
     * Delete binding by ID
     */
    void deleteById(String id);

    /**
     * Delete all bindings for an object
     */
    int deleteByObject(String objectType, String objectId);

    /**
     * Delete all bindings for an object, matching objectType case-insensitively.
     * Use canonical UPPERCASE objectType values (CAMPAIGN, DISCOUNT_COUPON, CASHBACK, …).
     */
    int deleteByObjectIgnoreCase(String objectType, String objectId);

    /**
     * Find all bindings for an object, matching objectType case-insensitively.
     * Use canonical UPPERCASE objectType values (CAMPAIGN, DISCOUNT_COUPON, CASHBACK, …).
     */
    List<RuleBinding> findByObjectIgnoreCase(String objectType, String objectId);

    /**
     * Delete binding by object and rule
     */
    int deleteByObjectAndRule(String objectType, String objectId, String ruleId);

    /**
     * Soft delete: deactivate binding
     */
    int deactivate(String id, String updatedBy);

    // ========== Find Single ==========

    /**
     * Find a specific binding by object and rule
     */
    Optional<RuleBinding> findByObjectAndRule(String objectType, String objectId, String ruleId);
}
