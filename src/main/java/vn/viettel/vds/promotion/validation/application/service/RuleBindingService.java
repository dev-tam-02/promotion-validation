package vn.viettel.vds.promotion.validation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.BindingAlreadyExistsException;
import vn.viettel.vds.promotion.validation.domain.exception.BindingDeactivationException;
import vn.viettel.vds.promotion.validation.domain.exception.BindingNotFoundException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleNotFoundException;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
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
    private final RuleValidator ruleValidator;

    // ========== Create Operations ==========

    /**
     * Create a new rule binding
     */
    public RuleBinding createBinding(RuleBinding binding, String createdBy) {
        log.info("Creating rule binding: objectType={}, objectId={}, ruleId={}",
                binding.getObjectType(), binding.getObjectId(), binding.getRuleId());

        // Load rule to validate existence and capture current version for pinning
        Rule rule = rulePersistencePort.findById(binding.getRuleId())
                .orElseThrow(() -> new RuleNotFoundException(binding.getRuleId()));

        // Check for duplicate binding
        if (bindingPersistencePort.existsByObjectAndRule(
                binding.getObjectType(), binding.getObjectId(), binding.getRuleId())) {
            throw new BindingAlreadyExistsException(
                    binding.getObjectType(), binding.getObjectId(), binding.getRuleId());
        }

        // V3: validate structured scope fields against JSON Schema spec
        ruleValidator.checkBindingScopeSchema(
                binding.getScopeTimeWindows(),
                binding.getScopeProductScope(),
                binding.getScopeTrafficControl());

        // V2: pin the rule version at bind time (null-safe: default to 1 if not set)
        Integer pinnedVersion = rule.getRuleVersion() != null
                ? rule.getRuleVersion().intValue()
                : 1;

        // Set defaults
        RuleBinding toSave = binding.toBuilder()
                .id(binding.getId() != null ? binding.getId() : UUID.randomUUID().toString())
                .ruleVersionPinned(binding.getRuleVersionPinned() != null
                        ? binding.getRuleVersionPinned()
                        : pinnedVersion)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .version(0L)
                .build();

        RuleBinding saved = bindingPersistencePort.save(toSave);
        log.info("Rule binding created successfully: id={}, ruleVersionPinned={}",
                saved.getId(), saved.getRuleVersionPinned());
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
                .orElseThrow(() -> new BindingNotFoundException(id));
    }

    /**
     * Get all bindings for an object
     */
    @Transactional(readOnly = true)
    public List<RuleBinding> getBindingsByObject(String objectType, String objectId) {
        log.debug("Getting bindings for object: type={}, id={}", objectType, objectId);
        return bindingPersistencePort.findByObject(objectType, objectId);
    }

    /**
     * Get active bindings for an object (ordered by priority)
     */
    @Transactional(readOnly = true)
    public List<RuleBinding> getActiveBindingsByObject(String objectType, String objectId) {
        log.debug("Getting active bindings for object: type={}, id={}", objectType, objectId);
        return bindingPersistencePort.findActiveByObject(objectType, objectId);
    }

    /**
     * Get bindings for multiple objects (batch)
     */
    @Transactional(readOnly = true)
    public Map<String, List<RuleBinding>> getBindingsByObjects(String objectType, List<String> objectIds) {
        log.debug("Getting bindings for {} objects of type {}", objectIds.size(), objectType);

        List<RuleBinding> bindings = bindingPersistencePort.findByObjectTypeAndObjectIdIn(objectType, objectIds);

        return bindings.stream()
                .collect(Collectors.groupingBy(RuleBinding::getObjectId));
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
     * Get effective bindings for an object type at a specific time
     */
    @Transactional(readOnly = true)
    public List<RuleBinding> getEffectiveBindings(String objectType, Instant timestamp) {
        log.debug("Getting effective bindings for type {} at {}", objectType, timestamp);
        return bindingPersistencePort.findEffectiveAtTime(objectType, timestamp);
    }

    /**
     * Find object IDs with bindings in a time range
     */
    @Transactional(readOnly = true)
    public List<String> findObjectIdsByTimeRange(String objectType, Instant startTs, Instant endTs) {
        log.debug("Finding object IDs by time range: type={}, start={}, end={}", objectType, startTs, endTs);
        return bindingPersistencePort.findObjectIdsByTypeAndTimeRange(objectType, startTs, endTs);
    }

    /**
     * Search bindings with filters
     */
    @Transactional(readOnly = true)
    public Page<RuleBinding> searchBindings(String objectType, String objectId, String ruleId,
                                            Boolean active, Pageable pageable) {
        log.debug("Searching bindings: objectType={}, objectId={}, ruleId={}, active={}",
                objectType, objectId, ruleId, active);
        return bindingPersistencePort.search(objectType, objectId, ruleId, active, pageable);
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
     * Delete binding by object and rule
     */
    public void deleteBindingByObjectAndRule(String objectType, String objectId, String ruleId, String deletedBy) {
        log.info("Deleting rule binding: objectType={}, objectId={}, ruleId={}, deletedBy={}",
                objectType, objectId, ruleId, deletedBy);

        int deleted = bindingPersistencePort.deleteByObjectAndRule(objectType, objectId, ruleId);

        if (deleted == 0) {
            throw new BindingNotFoundException(objectType, objectId, ruleId);
        }

        log.info("Rule binding deleted successfully: objectType={}, objectId={}, ruleId={}",
                objectType, objectId, ruleId);
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
            throw new BindingDeactivationException(id);
        }

        log.info("Rule binding deactivated successfully: id={}", id);
    }

    /**
     * Delete all bindings for an object
     */
    public int deleteBindingsByObject(String objectType, String objectId, String deletedBy) {
        log.info("Deleting all bindings for object: type={}, id={}, deletedBy={}",
                objectType, objectId, deletedBy);

        int deleted = bindingPersistencePort.deleteByObject(objectType, objectId);
        log.info("Deleted {} bindings for object: type={}, id={}", deleted, objectType, objectId);
        return deleted;
    }

    // ========== Validation Methods ==========

    /**
     * Validate that a rule exists
     */
    private void validateRuleExists(String ruleId) {
        if (!rulePersistencePort.existsById(ruleId)) {
            throw new RuleNotFoundException(ruleId);
        }
    }

    // ========== Business Logic ==========

    /**
     * Get the effective binding for an object (highest priority, currently effective)
     */
    @Transactional(readOnly = true)
    public Optional<RuleBinding> getEffectiveBindingForObject(String objectType, String objectId) {
        log.debug("Getting effective binding for object: type={}, id={}", objectType, objectId);

        return bindingPersistencePort.findTopActiveByObject(objectType, objectId)
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
        return trafficKey == null || binding.shouldApplyToTraffic(trafficKey);
    }
}
