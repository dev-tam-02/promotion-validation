package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.BundleHashResponse;
import vn.viettel.vds.promotion.validation.application.port.out.OutboxEventPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;
import vn.viettel.vds.promotion.validation.domain.exception.*;
import vn.viettel.vds.promotion.validation.domain.model.*;

import java.time.Instant;
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
    private final OutboxEventPersistencePort outboxEventPort;
    private final RuleService self;
    private final RuleLinter ruleLinter;

    public RuleService(RulePersistencePort rulePersistencePort,
                       RuleBindingPersistencePort ruleBindingPort,
                       OutboxEventPersistencePort outboxEventPort,
                       @Lazy RuleService self) {
        this.rulePersistencePort = rulePersistencePort;
        this.ruleBindingPort = ruleBindingPort;
        this.outboxEventPort = outboxEventPort;
        this.self = self;
        this.ruleLinter = new RuleLinter();
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
        rule.setState(Rule.RuleState.DRAFT);
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

        logger.info("Rule created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Count binding assignments for a rule
     */
    @Transactional(readOnly = true)
    public long countBindingsForRule(String ruleId) {
        return ruleBindingPort.findByRuleId(ruleId).size();
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
        logger.info("Updating rule: id={}", ruleId);

        Rule rule = self.getRuleById(ruleId);

        // System rules are immutable
        if (rule.isSystem()) {
            throw new SystemRuleProtectedException(ruleId);
        }

        // Only allow updates to draft rules
        if (rule.getState() != Rule.RuleState.DRAFT) {
            throw new RuleStateNotEditableException(ruleId, rule.getState().name());
        }

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

        logger.info("Rule updated successfully: id={}", saved.getId());
        return saved;
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

        // Only allow activating draft rules
        if (rule.getState() != Rule.RuleState.DRAFT) {
            throw new InvalidRuleStateTransitionException("activate", rule.getState().name(), Rule.RuleState.DRAFT.name());
        }

        // Validate rule is complete before activation
        validateRuleNodes(rule.getNodes());

        rule.setState(Rule.RuleState.PUBLISHED);
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

        rule.setState(Rule.RuleState.ARCHIVED);
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
        logger.debug("[RULE_SERVICE] Rule retrieved: id={}, code={}, state={}, nodeCount={}",
                rule.getId(), rule.getCode(), rule.getState(),
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
            logger.debug("[RULE_SERVICE] Rule found by code: id={}, code={}, state={}, nodeCount={}",
                    rule.getId(), rule.getCode(), rule.getState(),
                    rule.getNodes() != null ? rule.getNodes().size() : 0);
        } else {
            logger.debug("[RULE_SERVICE] Rule not found by code: {}", code);
        }
        return ruleOpt;
    }

    /**
     * Find rules with filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<Rule> findRules(Rule.RuleState state, String codePattern,
                                String namePattern, Pageable pageable) {
        if (state != null || codePattern != null || namePattern != null) {
            return rulePersistencePort.findWithFilters(state, codePattern, namePattern, pageable);
        } else {
            return rulePersistencePort.findAll(pageable);
        }
    }

    /**
     * Get rules by state
     */
    @Transactional(readOnly = true)
    public Page<Rule> getRulesByState(Rule.RuleState state, Pageable pageable) {
        return rulePersistencePort.findByState(state, pageable);
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

        // Step 3: Check version (optimistic locking)
        Long currentVersion = rule.getVersion();
        if (currentVersion != null && currentVersion != version) {
            logger.warn("Version conflict on delete: id={}, expected={}, actual={}", ruleId, version, currentVersion);
            throw new InvalidVersionFormatException(
                    String.format("Version conflict: expected %d but found %d", version, currentVersion)
            );
        }

        // Step 4: Check no bindings exist
        long bindingCount = ruleBindingPort.countByRuleId(ruleId);
        if (bindingCount > 0) {
            logger.warn("Cannot delete rule with bindings: id={}, bindingCount={}", ruleId, bindingCount);
            throw new RuleHasBindingsException(ruleId, bindingCount);
        }

        // Step 5: Transaction - delete nodes, delete rule, insert outbox event
        rulePersistencePort.deleteNodesByRuleId(ruleId);
        rulePersistencePort.deleteById(ruleId);

        OutboxEvent deletedEvent = OutboxEvent.builder()
                .id(IdGenerator.generateId())
                .aggregateType("ValidationRule")
                .aggregateId(ruleId)
                .eventType("VALIDATION_RULE_DELETED")
                .payload(Map.of("ruleId", ruleId, "deletedAt", Instant.now().toString()))
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .createdAt(Instant.now())
                .build();
        outboxEventPort.save(deletedEvent);

        logger.info("Rule deleted successfully: id={}", ruleId);
    }

    /**
     * Mark rule as published (called by PublishService)
     */
    public Rule markRuleAsPublished(String ruleId, int newVersion) {
        logger.info("Marking rule as published: id={}, version={}", ruleId, newVersion);

        Rule rule = self.getRuleById(ruleId);
        rule.setState(Rule.RuleState.PUBLISHED);
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
        Optional<Rule> rule = rulePersistencePort.findById(ruleId);
        return rule.map(r -> r.getState() == Rule.RuleState.PUBLISHED).orElse(false);
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