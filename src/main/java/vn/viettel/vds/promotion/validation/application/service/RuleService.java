package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ResponseInfo;
import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleService.class);

    private final RulePersistencePort rulePersistencePort;
    private final AuditService auditService;
    private final AssignmentService assignmentService;

    public RuleService(RulePersistencePort rulePersistencePort, AuditService auditService,
                       @Lazy AssignmentService assignmentService) {
        this.rulePersistencePort = rulePersistencePort;
        this.auditService = auditService;
        this.assignmentService = assignmentService;
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
        logger.info("Creating rule: code={}", code);

        // Check if rule with same code already exists
        if (rulePersistencePort.existsByCode(code)) {
            throw new BusinessException(new ResponseInfo("RULE_CODE_EXISTS",
                    "Rule with code '" + code + "' already exists", 400));
        }

        // Validate rule nodes
        validateRuleNodes(nodes);

        Rule rule = new Rule();
        rule.setId(generateRuleId(code));
        rule.setCode(code);
        rule.setName(name);
        rule.setState(Rule.RuleState.DRAFT);
        rule.setLatestVersion(0);
        rule.setLogic(logic);
        rule.setNodes(nodes);
        rule.setCreatedAt(Instant.now());
        rule.setCreatedBy(createdBy);
        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(createdBy);

        Rule saved = rulePersistencePort.save(rule);

        // Log audit event
        auditService.logRuleCreated(saved.getId(), createdBy);

        logger.info("Rule created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Update an existing rule (only if in DRAFT state)
     */
    public Rule updateRule(String ruleId, String name, Rule.LogicType logic,
                           List<RuleNode> nodes, String updatedBy) {
        logger.info("Updating rule: id={}", ruleId);

        Rule rule = getRuleById(ruleId);

        // Only allow updates to draft rules
        if (rule.getState() != Rule.RuleState.DRAFT) {
            throw new BusinessException(new ResponseInfo("RULE_STATE_LOCKED",
                    "Cannot update rule in state: " + rule.getState(), 400));
        }

        // Validate rule nodes if provided
        if (nodes != null) {
            validateRuleNodes(nodes);
            rule.setNodes(nodes);
        }

        if (name != null) {
            rule.setName(name);
        }

        if (logic != null) {
            rule.setLogic(logic);
        }

        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(updatedBy);

        Rule saved = rulePersistencePort.save(rule);

        // Log audit event
        auditService.logRuleUpdated(saved.getId(), updatedBy);

        logger.info("Rule updated successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Clone an existing rule with new code and name
     */
    public Rule cloneRule(String sourceRuleId, String newCode, String newName, String createdBy) {
        logger.info("Cloning rule: sourceId={}, newCode={}", sourceRuleId, newCode);

        Rule sourceRule = getRuleById(sourceRuleId);

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

        Rule rule = getRuleById(ruleId);

        // Only allow activating draft rules
        if (rule.getState() != Rule.RuleState.DRAFT) {
            throw new BusinessException(new ResponseInfo("RULE_STATE_INVALID",
                    "Can only activate rules in DRAFT state. Current state: " + rule.getState(), 400));
        }

        // Validate rule is complete before activation
        validateRuleNodes(rule.getNodes());

        rule.setState(Rule.RuleState.PUBLISHED);
        rule.setActive(true);
        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(activatedBy);

        Rule saved = rulePersistencePort.save(rule);

        // Log audit event
        auditService.logRuleUpdated(saved.getId(), activatedBy);

        logger.info("Rule activated successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Archive a rule
     */
    public Rule archiveRule(String ruleId, String archivedBy) {
        logger.info("Archiving rule: id={}", ruleId);

        Rule rule = getRuleById(ruleId);

        rule.setState(Rule.RuleState.ARCHIVED);
        rule.setActive(false);
        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(archivedBy);

        Rule saved = rulePersistencePort.save(rule);

        // Log audit event
        auditService.logRuleArchived(saved.getId(), archivedBy);

        logger.info("Rule archived successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Get rule by ID
     */
    @Transactional(readOnly = true)
    public Rule getRuleById(String ruleId) {
        return rulePersistencePort.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException());
    }

    /**
     * Get rule by code
     */
    @Transactional(readOnly = true)
    public Optional<Rule> getRuleByCode(String code) {
        return rulePersistencePort.findByCode(code);
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
     * Mark rule as published (called by PublishService)
     */
    public Rule markRuleAsPublished(String ruleId, int newVersion) {
        logger.info("Marking rule as published: id={}, version={}", ruleId, newVersion);

        Rule rule = getRuleById(ruleId);
        rule.setState(Rule.RuleState.PUBLISHED);
        rule.setLatestVersion(newVersion);
        rule.setUpdatedAt(Instant.now());

        return rulePersistencePort.save(rule);
    }

    private void validateRuleNodes(List<RuleNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException(new ResponseInfo("INVALID_RULE_STRUCTURE", "Rule must have at least one node", 400));
        }

        // Validate each node
        for (RuleNode node : nodes) {
            validateRuleNode(node);
        }

        // Check for node ID uniqueness
        long uniqueIds = nodes.stream().map(RuleNode::getId).distinct().count();
        if (uniqueIds != nodes.size()) {
            throw new BusinessException(new ResponseInfo("INVALID_RULE_STRUCTURE", "Rule node IDs must be unique", 400));
        }
    }

    private void validateRuleNode(RuleNode node) {
        if (node.getId() == null || node.getId().trim().isEmpty()) {
            throw new BusinessException(new ResponseInfo("INVALID_RULE_STRUCTURE", "Node ID is required", 400));
        }

        if (node.getType() == null) {
            throw new BusinessException(new ResponseInfo("INVALID_RULE_STRUCTURE", "Node type is required", 400));
        }

        switch (node.getType()) {
            case GROUP:
                if (node.getGroupLogic() == null) {
                    throw new BusinessException(new ResponseInfo("INVALID_RULE_STRUCTURE",
                            "Group logic is required for GROUP nodes", 400));
                }
                break;
            case COND:
                if (node.getOperatorName() == null || node.getOperatorName().trim().isEmpty()) {
                    throw new BusinessException(new ResponseInfo("INVALID_RULE_STRUCTURE",
                            "Operator name is required for COND nodes", 400));
                }
                if (node.getReasonCode() == null || node.getReasonCode().trim().isEmpty()) {
                    throw new BusinessException(new ResponseInfo("INVALID_RULE_STRUCTURE",
                            "Reason code is required for COND nodes", 400));
                }
                break;
        }
    }

    private String generateRuleId(String code) {
        return "rul_" + code + "_" + System.currentTimeMillis();
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

        // Find assignment for this object
        Optional<vn.viettel.vds.promotion.validation.domain.model.Assignment> assignment =
                assignmentService.findBySubjectTypeAndKey(objectType, objectId);

        if (assignment.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        // Get the rule from assignment
        String ruleId = assignment.get().getId();
        return getRuleById(ruleId);
    }

    /**
     * Get all rules by object type and object ID
     * Returns all rules (active and inactive) assigned to the specified object
     */
    @Transactional(readOnly = true)
    public List<Rule> getAllRulesByObject(String objectType, String objectId) {
        logger.info("Getting all rules by object: type={}, id={}", objectType, objectId);

        // Find all assignments for this object
        List<vn.viettel.vds.promotion.validation.domain.model.Assignment> assignments =
                assignmentService.findAllBySubjectTypeAndKey(objectType, objectId);

        if (assignments.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        // Get all rules from assignments
        return assignments.stream()
                .map(assignment -> {
                    try {
                        return getRuleById(assignment.getId());
                    } catch (Exception e) {
                        logger.warn("Failed to get rule {}: {}", assignment.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(rule -> rule != null)
                .toList();
    }
}