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
import vn.viettel.vds.promotion.validation.adapter.out.persistence.repository.RuleRepository;
import vn.viettel.vds.promotion.validation.domain.entity.Rule;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleService.class);

    private final RuleRepository ruleRepository;
    private final AuditService auditService;
    private final AssignmentService assignmentService;

    public RuleService(RuleRepository ruleRepository, AuditService auditService,
                      @Lazy AssignmentService assignmentService) {
        this.ruleRepository = ruleRepository;
        this.auditService = auditService;
        this.assignmentService = assignmentService;
    }

    /**
     * Create a new rule
     */
    public Rule createRule(String tenantId, String code, String name, Rule.LogicType logic,
                          List<Rule.RuleNode> nodes, String createdBy) {
        logger.info("Creating rule: tenant={}, code={}", tenantId, code);

        // Check if rule with same code already exists
        if (ruleRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new BusinessException(new ResponseInfo("RULE_CODE_EXISTS",
                "Rule with code '" + code + "' already exists for tenant " + tenantId, 400));
        }

        // Validate rule nodes
        validateRuleNodes(nodes);

        Rule rule = new Rule();
        rule.setId(generateRuleId(tenantId, code));
        rule.setTenantId(tenantId);
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

        Rule saved = ruleRepository.save(rule);

        // Log audit event
        auditService.logRuleCreated(tenantId, saved.getId(), createdBy);

        logger.info("Rule created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Update an existing rule (only if in DRAFT state)
     */
    public Rule updateRule(String ruleId, String name, Rule.LogicType logic,
                          List<Rule.RuleNode> nodes, String updatedBy) {
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

        Rule saved = ruleRepository.save(rule);

        // Log audit event
        auditService.logRuleUpdated(rule.getTenantId(), saved.getId(), updatedBy);

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
            sourceRule.getTenantId(),
            newCode,
            newName,
            sourceRule.getLogic(),
            sourceRule.getNodes(),
            createdBy
        );
    }

    /**
     * Archive a rule
     */
    public Rule archiveRule(String ruleId, String archivedBy) {
        logger.info("Archiving rule: id={}", ruleId);

        Rule rule = getRuleById(ruleId);

        rule.setState(Rule.RuleState.ARCHIVED);
        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(archivedBy);

        Rule saved = ruleRepository.save(rule);

        // Log audit event
        auditService.logRuleArchived(rule.getTenantId(), saved.getId(), archivedBy);

        logger.info("Rule archived successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Get rule by ID
     */
    @Transactional(readOnly = true)
    public Rule getRuleById(String ruleId) {
        return ruleRepository.findById(ruleId)
            .orElseThrow(() -> new ResourceNotFoundException());
    }

    /**
     * Get rule by tenant and code
     */
    @Transactional(readOnly = true)
    public Optional<Rule> getRuleByCode(String tenantId, String code) {
        return ruleRepository.findByTenantIdAndCode(tenantId, code);
    }

    /**
     * Find rules with filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<Rule> findRules(String tenantId, Rule.RuleState state, String codePattern,
                               String namePattern, Pageable pageable) {
        if (state != null || codePattern != null || namePattern != null) {
            return ruleRepository.findByTenantIdWithFilters(tenantId, state, codePattern, namePattern, pageable);
        } else {
            return ruleRepository.findAll(pageable);
        }
    }

    /**
     * Get rules by tenant and state
     */
    @Transactional(readOnly = true)
    public Page<Rule> getRulesByState(String tenantId, Rule.RuleState state, Pageable pageable) {
        return ruleRepository.findByTenantIdAndState(tenantId, state, pageable);
    }

    /**
     * Get all rules for tenant
     */
    @Transactional(readOnly = true)
    public List<Rule> getAllRulesByTenant(String tenantId) {
        return ruleRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
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

        return ruleRepository.save(rule);
    }

    private void validateRuleNodes(List<Rule.RuleNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException(new ResponseInfo("INVALID_RULE_STRUCTURE", "Rule must have at least one node", 400));
        }

        // Validate each node
        for (Rule.RuleNode node : nodes) {
            validateRuleNode(node);
        }

        // Check for node ID uniqueness
        long uniqueIds = nodes.stream().map(Rule.RuleNode::getId).distinct().count();
        if (uniqueIds != nodes.size()) {
            throw new BusinessException(new ResponseInfo("INVALID_RULE_STRUCTURE", "Rule node IDs must be unique", 400));
        }
    }

    private void validateRuleNode(Rule.RuleNode node) {
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

    private String generateRuleId(String tenantId, String code) {
        return "rul_" + tenantId + "_" + code;
    }

    /**
     * Check if rule exists by ID
     */
    @Transactional(readOnly = true)
    public boolean ruleExists(String ruleId) {
        return ruleRepository.existsById(ruleId);
    }

    /**
     * Check if rule is active
     */
    @Transactional(readOnly = true)
    public boolean isRuleActive(String ruleId) {
        Optional<Rule> rule = ruleRepository.findById(ruleId);
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
        Optional<vn.viettel.vds.promotion.validation.domain.entity.Assignment> assignment =
            assignmentService.findBySubjectTypeAndKey(objectType, objectId);

        if (assignment.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        // Get the rule from assignment
        String ruleId = assignment.get().getRuleId();
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
        List<vn.viettel.vds.promotion.validation.domain.entity.Assignment> assignments =
            assignmentService.findAllBySubjectTypeAndKey(objectType, objectId);

        if (assignments.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        // Get all rules from assignments
        return assignments.stream()
            .map(assignment -> {
                try {
                    return getRuleById(assignment.getRuleId());
                } catch (Exception e) {
                    logger.warn("Failed to get rule {}: {}", assignment.getRuleId(), e.getMessage());
                    return null;
                }
            })
            .filter(rule -> rule != null)
            .toList();
    }
}