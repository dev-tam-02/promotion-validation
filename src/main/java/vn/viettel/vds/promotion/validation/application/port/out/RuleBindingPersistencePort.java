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

    // ========== Find by Target ==========

    /**
     * Find all bindings for a specific target
     */
    List<RuleBinding> findByTarget(String targetType, String targetId);

    /**
     * Find all active bindings for a target, ordered by priority
     */
    List<RuleBinding> findActiveByTarget(String targetType, String targetId);

    /**
     * Find bindings for multiple targets
     */
    List<RuleBinding> findByTargetTypeAndTargetIdIn(String targetType, List<String> targetIds);

    /**
     * Find the highest priority active binding for a target
     */
    Optional<RuleBinding> findTopActiveByTarget(String targetType, String targetId);

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
    List<RuleBinding> findEffectiveAtTime(String targetType, Instant timestamp);

    /**
     * Find target IDs with bindings in a time range
     */
    List<String> findTargetIdsByTypeAndTimeRange(String targetType, Instant startTs, Instant endTs);

    // ========== Search with Pagination ==========

    /**
     * Search bindings with filters and pagination
     */
    Page<RuleBinding> search(String targetType, String targetId, String ruleId, Boolean active, Pageable pageable);

    /**
     * Find all bindings for a target type with pagination
     */
    Page<RuleBinding> findByTargetType(String targetType, Pageable pageable);

    // ========== Existence Checks ==========

    /**
     * Check if a binding exists for a target and rule combination
     */
    boolean existsByTargetAndRule(String targetType, String targetId, String ruleId);

    /**
     * Check if any active binding exists for a target
     */
    boolean existsActiveByTarget(String targetType, String targetId);

    // ========== Find by Active Status ==========

    /**
     * Find all bindings by active status
     */
    List<RuleBinding> findByActive(boolean active);

    // ========== Count Operations ==========

    /**
     * Count bindings for a target
     */
    long countByTarget(String targetType, String targetId);

    /**
     * Count active bindings for a rule
     */
    long countActiveByRuleId(String ruleId);

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
     * Delete all bindings for a target
     */
    int deleteByTarget(String targetType, String targetId);

    /**
     * Delete binding by target and rule
     */
    int deleteByTargetAndRule(String targetType, String targetId, String ruleId);

    /**
     * Soft delete: deactivate binding
     */
    int deactivate(String id, String updatedBy);

    // ========== Find Single ==========

    /**
     * Find a specific binding by target and rule
     */
    Optional<RuleBinding> findByTargetAndRule(String targetType, String targetId, String ruleId);
}
