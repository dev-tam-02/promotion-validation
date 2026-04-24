package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.in.RuleManagementUseCase;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.RuleNotFoundException;
import vn.viettel.vds.promotion.validation.domain.model.GroupNode;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link RuleManagementUseCase}.
 * Delegates persistence to {@link RulePersistencePort} and uses
 * {@link RuleTreeAssembler} to reconstruct trees for GET operations.
 */
@Service
public class RuleManagementService implements RuleManagementUseCase {

    private static final Logger log = LoggerFactory.getLogger(RuleManagementService.class);

    private final RulePersistencePort rulePort;
    private final RuleTreeAssembler assembler;
    private final RuleValidator ruleValidator;

    public RuleManagementService(RulePersistencePort rulePort,
                                 RuleTreeAssembler assembler,
                                 RuleValidator ruleValidator) {
        this.rulePort = rulePort;
        this.assembler = assembler;
        this.ruleValidator = ruleValidator;
    }

    @Override
    @Transactional
    public Rule createRule(String name, String description, Rule.LogicType logic,
                           List<RuleNode> nodes, String createdBy) {
        log.info("createRule: name={} createdBy={}", name, createdBy);

        // Validate tree structure
        if (nodes != null && !nodes.isEmpty()) {
            ruleValidator.checkTreeDepth(nodes);
            ruleValidator.checkNoCircular(nodes);
            ruleValidator.checkAllGroupsHaveChildren(nodes);
        }

        Rule rule = Rule.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .description(description)
                .logic(logic != null ? logic : Rule.LogicType.ALL)
                .state(Rule.RuleState.DRAFT)
                .nodes(nodes)
                .active(false)
                .ruleVersion(1L)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();

        Rule saved = rulePort.save(rule);
        log.info("createRule: saved id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Rule updateRule(String ruleId, String name, Rule.LogicType logic,
                           List<RuleNode> nodes, String updatedBy) {
        log.info("updateRule: id={} updatedBy={}", ruleId, updatedBy);

        Rule existing = rulePort.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));

        if (existing.getState() == Rule.RuleState.ARCHIVED) {
            throw new vn.viettel.vds.promotion.validation.domain.exception.RuleStateNotEditableException(
                    ruleId, existing.getState().name());
        }

        // Validate tree if provided
        List<RuleNode> newNodes = nodes != null ? nodes : existing.getNodes();
        if (newNodes != null && !newNodes.isEmpty()) {
            ruleValidator.checkTreeDepth(newNodes);
            ruleValidator.checkNoCircular(newNodes);
            ruleValidator.checkAllGroupsHaveChildren(newNodes);
        }

        Rule updated = existing.toBuilder()
                .name(name != null ? name : existing.getName())
                .logic(logic != null ? logic : existing.getLogic())
                .nodes(newNodes)
                .updatedAt(Instant.now())
                .updatedBy(updatedBy)
                .build();

        return rulePort.save(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Rule getRuleTree(String ruleId) {
        log.debug("getRuleTree: id={}", ruleId);
        Rule rule = rulePort.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        // Tree is already assembled by persistence layer via RuleNodeEntityMapper
        return rule;
    }

    @Override
    @Transactional
    public Rule archiveRule(String ruleId, String archivedBy) {
        log.info("archiveRule: id={} by={}", ruleId, archivedBy);
        Rule existing = rulePort.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        existing.archive(archivedBy);
        return rulePort.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Rule> listRules(Rule.RuleState state, String name, Pageable pageable) {
        log.debug("listRules: state={} name={}", state, name);
        return rulePort.findWithFilters(state, null, name, pageable);
    }
}
