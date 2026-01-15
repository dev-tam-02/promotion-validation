package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ResponseInfo;
import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing rule bindings.
 * <p>
 * This service provides business logic for:
 * - Creating, updating, and deleting rule bindings
 * - Querying bindings by target, rule, or time range
 * - Validating binding constraints
 * <p>
 * Replaces functionality from:
 * - AssignmentService
 * - TemporalPolicyService (binding-related parts)
 * - ApplicabilityRuleService
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RuleBindingService {

    private final RuleBindingPersistencePort bindingPersistencePort;
    private final RulePersistencePort rulePersistencePort;

    // ========== Create Operations ==========

    /**
     * Create a new rule binding
     */
    public RuleBinding createBinding(RuleBinding binding, String createdBy) {
        log.info("Creating rule binding: targetType={}, targetId={}, ruleId={}",
                binding.getTargetType(), binding.getTargetId(), binding.getRuleId());

        // Validate rule exists
        validateRuleExists(binding.getRuleId());

        // Check for duplicate binding
        if (bindingPersistencePort.existsByTargetAndRule(
                binding.getTargetType(), binding.getTargetId(), binding.getRuleId())) {
            throw new BusinessException(new ResponseInfo(
                    "BINDING_ALREADY_EXISTS",
                    String.format("A binding already exists for target %s/%s and rule %s",
                            binding.getTargetType(), binding.getTargetId(), binding.getRuleId()),
                    400));
        }

        // Set defaults
        RuleBinding toSave = binding.toBuilder()
                .id(binding.getId() != null ? binding.getId() : UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .version(0L)
                .build();

        RuleBinding saved = bindingPersistencePort.save(toSave);
        log.info("Rule binding created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Create multiple bindings
     */
    public List<RuleBinding> createBindings(List<RuleBinding> bindings, String createdBy) {
        log.info("Creating {} rule bindings", bindings.size());

        Instant now = Instant.now();
        List<RuleBinding> toSave = bindings.stream()
                .map(b -> b.toBuilder()
                        .id(b.getId() != null ? b.getId() : UUID.randomUUID().toString())
                        .createdAt(now)
                        .updatedAt(now)
                        .createdBy(createdBy)
                        .updatedBy(createdBy)
                        .version(0L)
                        .build())
                .toList();

        return bindingPersistencePort.saveAll(toSave);
    }

    // ========== Update Operations ==========

    /**
     * Update an existing binding
     */
    public RuleBinding updateBinding(String id, RuleBinding updates, String updatedBy) {
        log.info("Updating rule binding: id={}", id);

        RuleBinding existing = getBindingById(id);

        // If rule changed, validate new rule exists
        if (updates.getRuleId() != null && !updates.getRuleId().equals(existing.getRuleId())) {
            validateRuleExists(updates.getRuleId());
        }

        RuleBinding toSave = existing.toBuilder()
                .ruleId(updates.getRuleId() != null ? updates.getRuleId() : existing.getRuleId())
                .ruleVersionPinned(updates.getRuleVersionPinned())
                .priority(updates.getPriority() != null ? updates.getPriority() : existing.getPriority())
                .active(updates.getActive() != null ? updates.getActive() : existing.getActive())
                .validFrom(updates.getValidFrom())
                .validTo(updates.getValidTo())
                .timezone(updates.getTimezone() != null ? updates.getTimezone() : existing.getTimezone())
                .rrule(updates.getRrule())
                .timeWindows(updates.getTimeWindows())
                .excludedDates(updates.getExcludedDates())
                .includedAll(updates.getIncludedAll() != null ? updates.getIncludedAll() : existing.getIncludedAll())
                .includedProducts(updates.getIncludedProducts())
                .excludedProducts(updates.getExcludedProducts())
                .includedCategories(updates.getIncludedCategories())
                .excludedCategories(updates.getExcludedCategories())
                .includedBrands(updates.getIncludedBrands())
                .excludedBrands(updates.getExcludedBrands())
                .trafficPercent(updates.getTrafficPercent() != null ? updates.getTrafficPercent() : existing.getTrafficPercent())
                .stickyKeyStrategy(updates.getStickyKeyStrategy())
                .bundleHash(updates.getBundleHash())
                .updatedAt(Instant.now())
                .updatedBy(updatedBy)
                .build();

        RuleBinding saved = bindingPersistencePort.save(toSave);
        log.info("Rule binding updated successfully: id={}", saved.getId());
        return saved;
    }

    // ========== Read Operations ==========

    /**
     * Get binding by ID
     */
    @Transactional(readOnly = true)
    public RuleBinding getBindingById(String id) {
        return bindingPersistencePort.findById(id)
                .orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * Get all bindings for a target
     */
    @Transactional(readOnly = true)
    public List<RuleBinding> getBindingsByTarget(String targetType, String targetId) {
        log.debug("Getting bindings for target: type={}, id={}", targetType, targetId);
        return bindingPersistencePort.findByTarget(targetType, targetId);
    }

    /**
     * Get active bindings for a target (ordered by priority)
     */
    @Transactional(readOnly = true)
    public List<RuleBinding> getActiveBindingsByTarget(String targetType, String targetId) {
        log.debug("Getting active bindings for target: type={}, id={}", targetType, targetId);
        return bindingPersistencePort.findActiveByTarget(targetType, targetId);
    }

    /**
     * Get bindings for multiple targets (batch)
     */
    @Transactional(readOnly = true)
    public Map<String, List<RuleBinding>> getBindingsByTargets(String targetType, List<String> targetIds) {
        log.debug("Getting bindings for {} targets of type {}", targetIds.size(), targetType);

        List<RuleBinding> bindings = bindingPersistencePort.findByTargetTypeAndTargetIdIn(targetType, targetIds);

        return bindings.stream()
                .collect(Collectors.groupingBy(RuleBinding::getTargetId));
    }

    /**
     * Get bindings by rule ID
     */
    @Transactional(readOnly = true)
    public List<RuleBinding> getBindingsByRuleId(String ruleId) {
        log.debug("Getting bindings for rule: {}", ruleId);
        return bindingPersistencePort.findByRuleId(ruleId);
    }

    /**
     * Get effective bindings for a target type at a specific time
     */
    @Transactional(readOnly = true)
    public List<RuleBinding> getEffectiveBindings(String targetType, Instant timestamp) {
        log.debug("Getting effective bindings for type {} at {}", targetType, timestamp);
        return bindingPersistencePort.findEffectiveAtTime(targetType, timestamp);
    }

    /**
     * Find target IDs with bindings in a time range
     */
    @Transactional(readOnly = true)
    public List<String> findTargetIdsByTimeRange(String targetType, Instant startTs, Instant endTs) {
        log.debug("Finding target IDs by time range: type={}, start={}, end={}", targetType, startTs, endTs);
        return bindingPersistencePort.findTargetIdsByTypeAndTimeRange(targetType, startTs, endTs);
    }

    /**
     * Search bindings with filters
     */
    @Transactional(readOnly = true)
    public Page<RuleBinding> searchBindings(String targetType, String targetId, String ruleId,
                                            Boolean active, Pageable pageable) {
        log.debug("Searching bindings: targetType={}, targetId={}, ruleId={}, active={}",
                targetType, targetId, ruleId, active);
        return bindingPersistencePort.search(targetType, targetId, ruleId, active, pageable);
    }

    // ========== Delete Operations ==========

    /**
     * Delete binding by ID
     */
    public void deleteBinding(String id, String deletedBy) {
        log.info("Deleting rule binding: id={}, deletedBy={}", id, deletedBy);

        // Verify binding exists
        getBindingById(id);

        bindingPersistencePort.deleteById(id);
        log.info("Rule binding deleted successfully: id={}", id);
    }

    /**
     * Delete binding by target and rule
     */
    public void deleteBindingByTargetAndRule(String targetType, String targetId, String ruleId, String deletedBy) {
        log.info("Deleting rule binding: targetType={}, targetId={}, ruleId={}, deletedBy={}",
                targetType, targetId, ruleId, deletedBy);

        int deleted = bindingPersistencePort.deleteByTargetAndRule(targetType, targetId, ruleId);

        if (deleted == 0) {
            throw new ResourceNotFoundException();
        }

        log.info("Rule binding deleted successfully: targetType={}, targetId={}, ruleId={}",
                targetType, targetId, ruleId);
    }

    /**
     * Deactivate binding (soft delete)
     */
    public void deactivateBinding(String id, String updatedBy) {
        log.info("Deactivating rule binding: id={}, updatedBy={}", id, updatedBy);

        // Verify binding exists
        getBindingById(id);

        int updated = bindingPersistencePort.deactivate(id, updatedBy);

        if (updated == 0) {
            throw new BusinessException(new ResponseInfo(
                    "DEACTIVATION_FAILED",
                    "Failed to deactivate binding: " + id,
                    500));
        }

        log.info("Rule binding deactivated successfully: id={}", id);
    }

    /**
     * Delete all bindings for a target
     */
    public int deleteBindingsByTarget(String targetType, String targetId, String deletedBy) {
        log.info("Deleting all bindings for target: type={}, id={}, deletedBy={}",
                targetType, targetId, deletedBy);

        int deleted = bindingPersistencePort.deleteByTarget(targetType, targetId);
        log.info("Deleted {} bindings for target: type={}, id={}", deleted, targetType, targetId);
        return deleted;
    }

    // ========== Validation Methods ==========

    /**
     * Validate that a rule exists
     */
    private void validateRuleExists(String ruleId) {
        if (!rulePersistencePort.existsById(ruleId)) {
            throw new ResourceNotFoundException();
        }
    }

    // ========== Business Logic ==========

    /**
     * Get the effective binding for a target (highest priority, currently effective)
     */
    @Transactional(readOnly = true)
    public Optional<RuleBinding> getEffectiveBindingForTarget(String targetType, String targetId) {
        log.debug("Getting effective binding for target: type={}, id={}", targetType, targetId);

        return bindingPersistencePort.findTopActiveByTarget(targetType, targetId)
                .filter(RuleBinding::isEffective);
    }

    /**
     * Check if a binding should apply to a specific product and traffic key
     */
    @Transactional(readOnly = true)
    public boolean shouldApplyBinding(String bindingId, String productId, String categoryId,
                                      String brandId, String trafficKey) {
        RuleBinding binding = getBindingById(bindingId);

        // Check if effective
        if (!binding.isEffective()) {
            return false;
        }

        // Check product applicability
        if (!binding.appliesToProduct(productId, categoryId, brandId)) {
            return false;
        }

        // Check traffic control
        if (trafficKey != null && !binding.shouldApplyToTraffic(trafficKey)) {
            return false;
        }

        return true;
    }
}
