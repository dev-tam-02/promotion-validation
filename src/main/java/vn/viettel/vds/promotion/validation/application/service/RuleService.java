package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.util.IdGenerator;
import com.promix.platform.outbox.spi.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.BundleHashResponse;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleListFilter;
import vn.viettel.vds.promotion.validation.application.port.out.RuleListRow;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.*;
import vn.viettel.vds.promotion.validation.domain.model.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleService.class);

    private static final String OBJECT_RESOURCE_TYPE = "object";

    private final RulePersistencePort rulePersistencePort;
    private final RuleBindingPersistencePort ruleBindingPort;
    private final OutboxService outboxService;
    private final RuleService self;
    private final RuleLinter ruleLinter;
    private final RuleNodeSchemaValidator schemaValidator;
    private final String validationEventTopic;

    private static final String RULE_AGGREGATE_TYPE = "ValidationRule";

    public RuleService(RulePersistencePort rulePersistencePort,
                       RuleBindingPersistencePort ruleBindingPort,
                       OutboxService outboxService,
                       @Lazy RuleService self,
                       RuleNodeSchemaValidator schemaValidator,
                       @Value("${kafka.topics.validation-event}") String validationEventTopic) {
        this.rulePersistencePort = rulePersistencePort;
        this.ruleBindingPort = ruleBindingPort;
        this.outboxService = outboxService;
        this.self = self;
        this.ruleLinter = new RuleLinter();
        this.schemaValidator = schemaValidator;
        this.validationEventTopic = validationEventTopic;
    }

    /**
     * Get active rules for stackable discount validation by customer segment.
     * <p>
     * This method retrieves published, active rules that apply to stackable discount validation
     * for the specified customer segment.
     * </p>
     *
     * @param customerSegment customer segment (can be null for all segments)
     * @return list of active validation rules
     */
    public List<Rule> getActiveRulesForStackableDiscount(String customerSegment) {
        logger.debug("Retrieving active stackable discount rules for segment: {}", customerSegment);

        // For now, return empty list
        // In full implementation, this would query the database for active rules
        // filtered by segment and rule type
        return List.of();
    }

    /**
     * Create a new rule
     */
    public Rule createRule(String code, String name, Rule.LogicType logic,
                           List<RuleNode> nodes, String createdBy) {
        return createRule(code, name, logic, nodes, null, null, null, createdBy);
    }

    /**
     * Create a new rule with optional context, description and fallbackErrorMessage.
     * Auto-generates code if blank and defaults logic to ALL when null.
     */
    @SuppressWarnings("java:S107")
    public Rule createRule(String code, String name, Rule.LogicType logic,
                           List<RuleNode> nodes, String context, String description,
                           String fallbackErrorMessage, String createdBy) {
        // Auto-generate code if not provided
        String effectiveCode = (code != null && !code.isBlank()) ? code : generateRuleId();
        logger.info("Creating rule: code={}", effectiveCode);

        // Check if rule with same code already exists
        if (rulePersistencePort.existsByCode(effectiveCode)) {
            throw new RuleAlreadyExistsException(effectiveCode);
        }

        // Check if rule with same name already exists (system-wide, trimmed).
        // SRS VRUL002_B03 Bước 5: the create command must reject a duplicate name
        // — the proactive check-name endpoint only guards the happy path; this
        // closes the race / direct-API gap. There is no UNIQUE DB constraint, so
        // the guard lives here.
        String trimmedName = (name != null) ? name.trim() : null;
        if (trimmedName != null && rulePersistencePort.existsByName(trimmedName)) {
            throw new RuleNameAlreadyExistsException(trimmedName);
        }

        validateRuleNodes(nodes);

        // Default logic to ALL if not provided
        Rule.LogicType effectiveLogic = (logic != null) ? logic : Rule.LogicType.ALL;

        // Static lint analysis — errors block save, warnings are logged
        LintReport lintReport = ruleLinter.lint(null, nodes);
        if (lintReport.hasErrors()) {
            throw new RuleValidationFailedException(
                    "Rule has lint errors that must be fixed before saving: " + lintReport.errors());
        }
        if (lintReport.hasWarnings()) {
            logger.warn("createRule lint warnings for code={}: {}", effectiveCode, lintReport.warnings());
        }

        Rule rule = new Rule();
        rule.setId(generateRuleId());
        rule.setCode(effectiveCode);
        rule.setName(name);
        rule.setActive(true); // VRUL001: rules are usable on creation (no DRAFT state)
        rule.setLatestVersion(0);
        rule.setLogic(effectiveLogic);
        rule.setNodes(nodes);
        rule.setContext(context);
        rule.setDescription(description);
        rule.setFallbackErrorMessage(fallbackErrorMessage);
        rule.setCreatedAt(Instant.now());
        rule.setCreatedBy(createdBy);
        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(createdBy);

        Rule saved = rulePersistencePort.save(rule);

        // SRS VRUL002_B03 Bước 7 ③: emit a creation event to the promix outbox so the
        // change is propagated asynchronously to downstream services — the promix
        // outbox scheduler publishes it to Kafka (topic = destination).
        outboxService.createEvent(
                RULE_AGGREGATE_TYPE,
                saved.getId(),
                "VALIDATION_RULE_CREATED",
                ruleEventPayload(saved, "createdAt"),
                validationEventTopic);

        logger.info("Rule created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Check whether a rule with the exact given name already exists (cs_as).
     * Backs the create-screen proactive duplicate-name guard so the user can
     * rename before saving.
     */
    @Transactional(readOnly = true)
    public boolean isNameDuplicated(String name) {
        return rulePersistencePort.existsByName(name);
    }

    /**
     * Edit-screen variant of {@link #isNameDuplicated(String)} that ignores the
     * rule being edited so it is not flagged as a duplicate of itself.
     */
    @Transactional(readOnly = true)
    public boolean isNameDuplicated(String name, String excludeRuleId) {
        return rulePersistencePort.existsByName(name, excludeRuleId);
    }

    /**
     * Count binding assignments for a rule.
     * Only active bindings count — a binding deactivated when its campaign goes
     * inactive (active = false) must not inflate the assignment count.
     */
    @Transactional(readOnly = true)
    public long countBindingsForRule(String ruleId) {
        return ruleBindingPort.countActiveByRuleId(ruleId);
    }

    /**
     * Bulk count nodes for multiple rules in a single query. Used by the list
     * endpoint to populate {@code nodeCount} without loading every rule's tree.
     * Missing ids in the returned map mean zero nodes.
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> countNodesByRuleIds(Collection<String> ruleIds) {
        return rulePersistencePort.countNodesByRuleIds(ruleIds);
    }

    /**
     * Update an existing rule (only if in DRAFT state)
     */
    public Rule updateRule(String ruleId, String name, Rule.LogicType logic,
                           List<RuleNode> nodes, String updatedBy) {
        return updateRule(ruleId, name, logic, nodes, null, null, null, updatedBy);
    }

    /**
     * Update an existing rule with optional context, description and fallbackErrorMessage.
     * PATCH semantics: only fields that are non-null are updated.
     */
    @SuppressWarnings("java:S107")
    public Rule updateRule(String ruleId, String name, Rule.LogicType logic,
                           List<RuleNode> nodes, String context, String description,
                           String fallbackErrorMessage, String updatedBy) {
        return updateRule(ruleId, name, logic, nodes, context, description,
                fallbackErrorMessage, null, updatedBy);
    }

    /**
     * Update an existing rule with optimistic locking.
     * {@code expectedVersion} is the version the client loaded; when non-null and
     * it no longer matches the current persisted version, the update is rejected
     * with {@link RuleVersionConflictException} (409 CONFLICTED) so a concurrent
     * edit cannot silently overwrite a newer one. A null expectedVersion skips
     * the check (internal callers that carry no client view).
     */
    @SuppressWarnings("java:S107")
    public Rule updateRule(String ruleId, String name, Rule.LogicType logic,
                           List<RuleNode> nodes, String context, String description,
                           String fallbackErrorMessage, Long expectedVersion, String updatedBy) {
        logger.info("Updating rule: id={}", ruleId);

        Rule rule = self.getRuleById(ruleId);

        assertRuleEditable(rule, ruleId, expectedVersion);

        // Validate rule nodes if provided
        if (nodes != null) {
            validateRuleNodes(nodes);
            // Static lint analysis — errors block save, warnings are logged
            LintReport lintReport = ruleLinter.lint(rule, nodes);
            if (lintReport.hasErrors()) {
                throw new RuleValidationFailedException(
                        "Rule has lint errors that must be fixed before saving: " + lintReport.errors());
            }
            if (lintReport.hasWarnings()) {
                logger.warn("updateRule lint warnings for ruleId={}: {}", ruleId, lintReport.warnings());
            }
            rule.setNodes(nodes);
        }

        if (name != null) {
            // VRUL003 B03_14: guard duplicate name at the BE on the confirm step
            // (the FE Step-1 check is not authoritative). Exclude the rule itself.
            String trimmedName = name.trim();
            if (rulePersistencePort.existsByName(trimmedName, ruleId)) {
                throw new RuleNameAlreadyExistsException(trimmedName);
            }
            rule.setName(name);
        }

        if (logic != null) {
            rule.setLogic(logic);
        }

        if (context != null) {
            rule.setContext(context);
        }

        if (description != null) {
            rule.setDescription(description);
        }

        if (fallbackErrorMessage != null) {
            rule.setFallbackErrorMessage(fallbackErrorMessage);
        }

        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(updatedBy);

        Rule saved = rulePersistencePort.save(rule);

        // VRUL003 B03: emit an update event to the promix outbox so downstream
        // services (rule-engine, search index) sync the change — mirrors create/delete.
        outboxService.createEvent(
                RULE_AGGREGATE_TYPE,
                saved.getId(),
                "VALIDATION_RULE_UPDATED",
                ruleEventPayload(saved, "updatedAt"),
                validationEventTopic);

        logger.info("Rule updated successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Assert that {@code rule} may be edited, throwing the appropriate domain
     * exception otherwise: system rules are immutable, rules with an active
     * campaign binding are locked (VRUL003), and a stale {@code expectedVersion}
     * is rejected via optimistic locking (mirrors deleteRule's guard).
     */
    private void assertRuleEditable(Rule rule, String ruleId, Long expectedVersion) {
        // System rules are immutable
        if (rule.isSystem()) {
            throw new SystemRuleProtectedException(ruleId);
        }

        // VRUL003: a rule assigned to a campaign (active binding) is locked from
        // editing — reject with VALIDATION_RULE_NOT_EDITABLE.
        if (ruleBindingPort.countActiveByRuleId(ruleId) > 0) {
            logger.warn("Rejected edit of assigned rule: id={}", ruleId);
            throw new RuleNotEditableException(ruleId);
        }

        // Optimistic locking — reject a stale client version so a concurrent edit
        // cannot silently overwrite a newer save.
        Long currentVersion = rule.getVersion();
        if (expectedVersion != null && currentVersion != null
                && !currentVersion.equals(expectedVersion)) {
            logger.warn("Version conflict on update: id={}, expected={}, actual={}",
                    ruleId, expectedVersion, currentVersion);
            throw new RuleVersionConflictException(ruleId);
        }
    }

    /**
     * Clone an existing rule with new code and name
     */
    public Rule cloneRule(String sourceRuleId, String newCode, String newName, String createdBy) {
        logger.info("Cloning rule: sourceId={}, newCode={}", sourceRuleId, newCode);

        Rule sourceRule = self.getRuleById(sourceRuleId);

        return createRule(
                newCode,
                newName,
                sourceRule.getLogic(),
                sourceRule.getNodes(),
                createdBy
        );
    }

    /**
     * Activate a rule (move from DRAFT to PUBLISHED state, ready for publishing)
     */
    public Rule activateRule(String ruleId, String activatedBy) {
        logger.info("Activating rule: id={}", ruleId);

        Rule rule = self.getRuleById(ruleId);

        // Validate rule is complete before activation
        validateRuleNodes(rule.getNodes());

        // VRUL001: no lifecycle state — activation simply flags the rule usable.
        rule.setActive(true);
        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(activatedBy);

        Rule saved = rulePersistencePort.save(rule);

        logger.info("Rule activated successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Archive a rule
     */
    public Rule archiveRule(String ruleId, String archivedBy) {
        logger.info("Archiving rule: id={}", ruleId);

        Rule rule = self.getRuleById(ruleId);

        // VRUL001: no lifecycle state — archiving simply flags the rule unusable.
        rule.setActive(false);
        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(archivedBy);

        Rule saved = rulePersistencePort.save(rule);

        logger.info("Rule archived successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Get rule by ID
     */
    @Transactional(readOnly = true)
    public Rule getRuleById(String ruleId) {
        logger.debug("[RULE_SERVICE] getRuleById called: ruleId={}", ruleId);
        Rule rule = rulePersistencePort.findById(ruleId)
                .orElseThrow(() -> {
                    logger.warn("[RULE_SERVICE] Rule not found: ruleId={}", ruleId);
                    return new RuleNotFoundException(ruleId);
                });
        logger.debug("[RULE_SERVICE] Rule retrieved: id={}, code={}, nodeCount={}",
                rule.getId(), rule.getCode(),
                rule.getNodes() != null ? rule.getNodes().size() : 0);
        return rule;
    }

    /**
     * Get rule by code
     */
    @Transactional(readOnly = true)
    public Optional<Rule> getRuleByCode(String code) {
        logger.debug("[RULE_SERVICE] getRuleByCode called: code={}", code);
        Optional<Rule> ruleOpt = rulePersistencePort.findByCode(code);
        if (ruleOpt.isPresent()) {
            Rule rule = ruleOpt.get();
            logger.debug("[RULE_SERVICE] Rule found by code: id={}, code={}, nodeCount={}",
                    rule.getId(), rule.getCode(),
                    rule.getNodes() != null ? rule.getNodes().size() : 0);
        } else {
            logger.debug("[RULE_SERVICE] Rule not found by code: {}", code);
        }
        return ruleOpt;
    }

    /**
     * Find rules with filters and pagination. Filtering, sorting (including the
     * computed counts ruleCount/assignmentCount) and pagination are all performed
     * in the database by the persistence adapter.
     */
    @Transactional(readOnly = true)
    public Page<RuleListRow> findRules(RuleListFilter filter, Pageable pageable) {
        return rulePersistencePort.findWithFilters(filter, pageable);
    }

    /**
     * Get all rules
     */
    @Transactional(readOnly = true)
    public List<Rule> getAllRules() {
        return rulePersistencePort.findAllOrderByUpdatedAtDesc();
    }

    /**
     * Delete a rule permanently (hard delete) with optimistic locking.
     * SRS VRUL005: Delete rule_nodes → delete validation_rules → insert VALIDATION_RULE_DELETED outbox event
     */
    @Transactional
    public void deleteRule(String ruleId, long version) {
        logger.info("Deleting rule: id={}, version={}", ruleId, version);

        // Step 1: Check rule exists
        Rule rule = rulePersistencePort.findById(ruleId)
                .orElseThrow(() -> {
                    logger.warn("Rule not found for delete: id={}", ruleId);
                    return new RuleNotFoundException(ruleId);
                });

        // Step 2: Guard — system rules cannot be deleted
        if (rule.isSystem()) {
            logger.warn("Rejected delete of system rule: id={}", ruleId);
            throw new SystemRuleProtectedException(ruleId);
        }

        // Step 3: Check version (optimistic locking) — a stale client version is a
        // concurrency conflict (409 CONFLICTED), not a malformed-version 400.
        Long currentVersion = rule.getVersion();
        if (currentVersion != null && currentVersion != version) {
            logger.warn("Version conflict on delete: id={}, expected={}, actual={}", ruleId, version, currentVersion);
            throw new RuleVersionConflictException(ruleId);
        }

        // Step 4: Check no bindings exist
        long bindingCount = ruleBindingPort.countByRuleId(ruleId);
        if (bindingCount > 0) {
            logger.warn("Cannot delete rule with bindings: id={}, bindingCount={}", ruleId, bindingCount);
            throw new RuleHasBindingsException(ruleId, bindingCount);
        }

        // Step 5: Transaction - delete nodes, delete rule, emit outbox event
        rulePersistencePort.deleteNodesByRuleId(ruleId);
        rulePersistencePort.deleteById(ruleId);

        outboxService.createEvent(
                RULE_AGGREGATE_TYPE,
                ruleId,
                "VALIDATION_RULE_DELETED",
                Map.of("ruleId", ruleId, "deletedAt", Instant.now().toString()),
                validationEventTopic);

        logger.info("Rule deleted successfully: id={}", ruleId);
    }

    /**
     * Mark rule as published (called by PublishService)
     */
    public Rule markRuleAsPublished(String ruleId, int newVersion) {
        logger.info("Marking rule as published: id={}, version={}", ruleId, newVersion);

        Rule rule = self.getRuleById(ruleId);
        rule.setActive(true); // VRUL001: no PUBLISHED state — publishing just flags usable
        rule.setLatestVersion(newVersion);
        rule.setUpdatedAt(Instant.now());

        return rulePersistencePort.save(rule);
    }

    private void validateRuleNodes(List<RuleNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new InvalidRuleStructureException("Rule must have at least one node");
        }

        // Validate each node
        for (RuleNode node : nodes) {
            validateRuleNode(node);
        }

        // Check for node ID uniqueness
        long uniqueIds = nodes.stream().map(RuleNode::getId).distinct().count();
        if (uniqueIds != nodes.size()) {
            throw new InvalidRuleStructureException("Rule node IDs must be unique");
        }

        // Schema-driven validation: tree must contain ≥ 1 COND, every COND's
        // operatorName must exist in operator_options, and every COND's params
        // must satisfy the operator's params_schema when one is defined.
        // Null-guard for unit tests that construct RuleService directly without DI.
        if (schemaValidator != null) {
            schemaValidator.validate(nodes);
        }
    }

    private void validateRuleNode(RuleNode node) {
        if (node.getId() == null || node.getId().trim().isEmpty()) {
            throw new InvalidRuleStructureException("Node ID is required");
        }

        if (node.getType() == null) {
            throw new InvalidRuleStructureException(node.getId(), "Node type is required");
        }

        if (node.getType() == RuleNode.NodeType.GROUP) {
            if (node.getGroupLogic() == null) {
                throw new InvalidRuleStructureException(node.getId(), "Group logic is required for GROUP nodes");
            }
        } else if (node.getType() == RuleNode.NodeType.COND) {
            if (node.getOperatorName() == null || node.getOperatorName().trim().isEmpty()) {
                throw new InvalidRuleStructureException(node.getId(), "Operator name is required for COND nodes");
            }
            if (node.getReasonCode() == null || node.getReasonCode().trim().isEmpty()) {
                throw new InvalidRuleStructureException(node.getId(), "Reason code is required for COND nodes");
            }
        }
    }

    private String generateRuleId() {
        return IdGenerator.generateId();
    }

    /**
     * Build the outbox payload for a rule lifecycle event. {@code timestampField}
     * is the key for the event time (e.g. "createdAt" / "updatedAt").
     */
    private Map<String, Object> ruleEventPayload(Rule rule, String timestampField) {
        return Map.of(
                "ruleId", rule.getId(),
                "code", rule.getCode() != null ? rule.getCode() : "",
                "name", rule.getName() != null ? rule.getName() : "",
                timestampField, Instant.now().toString());
    }

    /**
     * Check if rule exists by ID
     */
    @Transactional(readOnly = true)
    public boolean ruleExists(String ruleId) {
        return rulePersistencePort.existsById(ruleId);
    }

    /**
     * Check if rule is active
     */
    @Transactional(readOnly = true)
    public boolean isRuleActive(String ruleId) {
        // VRUL001: no PUBLISHED state — a rule that exists and is flagged active is usable.
        Optional<Rule> rule = rulePersistencePort.findById(ruleId);
        return rule.map(Rule::isActive).orElse(false);
    }

    /**
     * Get rule by object type and object ID
     * Returns the rule assigned to the specified object
     */
    @Transactional(readOnly = true)
    public Rule getRuleByObject(String objectType, String objectId) {
        logger.info("Getting rule by object: type={}, id={}", objectType, objectId);

        // Find binding for this object
        List<RuleBinding> bindings = ruleBindingPort.findActiveByObject(objectType, objectId);

        if (bindings.isEmpty()) {
            throw new RuleNotFoundException(OBJECT_RESOURCE_TYPE, objectType + ":" + objectId);
        }

        // Get the rule from binding (first active binding)
        String ruleId = bindings.get(0).getRuleId();
        return self.getRuleById(ruleId);
    }

    /**
     * Get all rules by object type and object ID
     * Returns all rules (active and inactive) assigned to the specified object
     */
    @Transactional(readOnly = true)
    public List<Rule> getAllRulesByObject(String objectType, String objectId) {
        logger.info("Getting all rules by object: type={}, id={}", objectType, objectId);

        // Find all bindings for this object
        List<RuleBinding> bindings = ruleBindingPort.findByObject(objectType, objectId);

        if (bindings.isEmpty()) {
            throw new RuleNotFoundException(OBJECT_RESOURCE_TYPE, objectType + ":" + objectId);
        }

        // Get all rules from bindings
        return bindings.stream()
                .map(binding -> {
                    try {
                        return self.getRuleById(binding.getRuleId());
                    } catch (Exception e) {
                        logger.warn("Failed to get rule {}: {}", binding.getRuleId(), e.getMessage());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Get bundle hash for an object.
     * Returns the latest compiled bundle hash for the specified object.
     *
     * @param objectType Object type (campaign, voucher, tier, reward)
     * @param objectId   Object identifier/key
     * @return BundleHashResponse with bundle hash and metadata
     * @throws ResourceNotFoundException if no bundle found for object
     */
    @Transactional(readOnly = true)
    public BundleHashResponse getBundleHashForObject(String objectType, String objectId) {
        logger.info("Getting bundle hash for object: type={}, id={}", objectType, objectId);

        // Find active binding for object
        List<RuleBinding> bindings = ruleBindingPort.findActiveByObject(objectType, objectId);
        if (bindings.isEmpty()) {
            logger.warn("No active binding found for object: type={}, id={}", objectType, objectId);
            throw new RuleNotFoundException(OBJECT_RESOURCE_TYPE, objectType + ":" + objectId);
        }

        // Get first binding (highest priority active binding)
        RuleBinding binding = bindings.get(0);

        // Check if binding has bundleHash
        if (binding.getBundleHash() == null || binding.getBundleHash().isEmpty()) {
            logger.warn("Binding has no bundleHash: bindingId={}", binding.getId());
            throw new RuleNotFoundException(OBJECT_RESOURCE_TYPE, objectType + ":" + objectId);
        }

        logger.debug("Found bundle hash {} for object {}:{}",
                binding.getBundleHash(), objectType, objectId);

        return BundleHashResponse.builder()
                .objectType(objectType)
                .objectId(objectId)
                .bundleHash(binding.getBundleHash())
                .ruleVersion(binding.getRuleVersionPinned() != null ? binding.getRuleVersionPinned().longValue() : null)
                .assignmentVersion(1)
                .compiledAt(binding.getUpdatedAt())
                .build();
    }

    /**
     * Get binding for an object
     *
     * @param objectType Object type (campaign, voucher, tier, reward)
     * @param objectId   Object identifier/key
     * @return Optional containing the active binding if found
     */
    @Transactional(readOnly = true)
    public Optional<RuleBinding> getBindingForObject(String objectType, String objectId) {
        logger.debug("Getting binding for object: type={}, id={}", objectType, objectId);
        List<RuleBinding> bindings = ruleBindingPort.findActiveByObject(objectType, objectId);
        return bindings.isEmpty() ? Optional.empty() : Optional.of(bindings.get(0));
    }

    /**
     * Get all bindings for an object
     *
     * @param objectType Object type (campaign, voucher, tier, reward)
     * @param objectId   Object identifier/key
     * @return List of all bindings for the object
     */
    @Transactional(readOnly = true)
    public List<RuleBinding> getAllBindingsForObject(String objectType, String objectId) {
        logger.debug("Getting all bindings for object: type={}, id={}", objectType, objectId);
        return ruleBindingPort.findByObject(objectType, objectId);
    }
}