package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.in.RuleManagementUseCase;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.RuleNotFoundException;
import vn.viettel.vds.promotion.validation.domain.model.GroupNode;
import vn.viettel.vds.promotion.validation.domain.model.Operator;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RuleManagementUseCase}.
 * Delegates persistence to {@link RulePersistencePort} and uses
 * {@link RuleTreeAssembler} to reconstruct trees for GET operations.
 *
 * <p>Task 07 additions: after persisting rule + nodes, generates DSL snapshot
 * via {@link DslGenerator}, compiles DRL via {@link DrlCompiler}, and registers
 * with pp-rule-engine via {@link RuleEngineClient}.  On success the rule moves
 * to ACTIVE state with bundleHash populated. On failure the rule stays DRAFT.
 */
@Service
public class RuleManagementService implements RuleManagementUseCase {

    private static final Logger log = LoggerFactory.getLogger(RuleManagementService.class);

    private final RulePersistencePort rulePort;
    private final RuleTreeAssembler assembler;
    private final RuleValidator ruleValidator;
    private final DslGenerator dslGenerator;
    private final DrlCompiler drlCompiler;
    private final RuleEngineClient ruleEngineClient;
    private final OperatorPersistencePort operatorPort;

    public RuleManagementService(RulePersistencePort rulePort,
                                 RuleTreeAssembler assembler,
                                 RuleValidator ruleValidator,
                                 DslGenerator dslGenerator,
                                 DrlCompiler drlCompiler,
                                 RuleEngineClient ruleEngineClient,
                                 OperatorPersistencePort operatorPort) {
        this.rulePort = rulePort;
        this.assembler = assembler;
        this.ruleValidator = ruleValidator;
        this.dslGenerator = dslGenerator;
        this.drlCompiler = drlCompiler;
        this.ruleEngineClient = ruleEngineClient;
        this.operatorPort = operatorPort;
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

        // Persist rule + nodes in transaction
        Rule saved = rulePort.save(rule);
        log.info("createRule: saved id={}", saved.getId());

        // Generate DSL snapshot and compile + register DRL
        saved = compilePipelineAndSave(saved, nodes != null ? nodes : List.of(), createdBy);

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

        Rule saved = rulePort.save(updated);

        // Re-compile + re-register with rule engine
        if (newNodes != null && !newNodes.isEmpty()) {
            saved = compilePipelineAndSave(saved, newNodes, updatedBy);
        }

        return saved;
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

    // ---------- private helpers ----------

    /**
     * Generate DSL snapshot, compile DRL, and register with pp-rule-engine.
     *
     * <p>On success: sets rule.dsl, rule.bundleHash, rule.state=ACTIVE and saves.
     * On failure (compile error or engine down): leaves rule in DRAFT with bundleHash=null.
     *
     * @param rule   already-persisted rule
     * @param nodes  assembled node tree (first element = root)
     * @param userId user performing the action (for audit)
     * @return updated rule
     */
    private Rule compilePipelineAndSave(Rule rule, List<RuleNode> nodes, String userId) {
        String ruleId = rule.getId();

        // Step 1: Generate DSL snapshot
        Map<String, Object> dsl = dslGenerator.generate(rule, nodes);
        Rule withDsl = rule.toBuilder().dsl(dsl).build();

        // Step 2: Compile DRL from Mustache templates
        Map<String, Operator> operatorMap = loadOperatorMap();
        String drl;
        try {
            drl = drlCompiler.compile(withDsl, nodes, operatorMap);
        } catch (DrlCompiler.DrlCompileException ex) {
            log.error("DRL compilation failed for ruleId={}: {}", ruleId, ex.getMessage());
            // Save DSL but leave state=DRAFT, bundleHash=null
            Rule draftWithDsl = withDsl.toBuilder()
                    .updatedAt(Instant.now())
                    .updatedBy(userId)
                    .build();
            rulePort.save(draftWithDsl);
            throw ex;
        }

        // Step 3: Register DRL with pp-rule-engine
        try {
            String bundleHash;
            if (rule.getBundleHash() != null) {
                // Rule already registered — update it
                bundleHash = ruleEngineClient.update(ruleId, drl);
            } else {
                bundleHash = ruleEngineClient.register(ruleId, drl);
            }

            Rule active = withDsl.toBuilder()
                    .bundleHash(bundleHash)
                    .state(Rule.RuleState.PUBLISHED)
                    .active(true)
                    .updatedAt(Instant.now())
                    .updatedBy(userId)
                    .build();

            Rule saved = rulePort.save(active);
            log.info("Rule compiled and registered: ruleId={}, bundleHash={}", ruleId, bundleHash);
            return saved;

        } catch (RuleEngineClient.RuleEngineException ex) {
            log.error("Rule engine registration failed for ruleId={}: {}; rule stays DRAFT",
                    ruleId, ex.getMessage());
            // Save DSL but leave state=DRAFT, bundleHash=null
            Rule draftWithDsl = withDsl.toBuilder()
                    .updatedAt(Instant.now())
                    .updatedBy(userId)
                    .build();
            return rulePort.save(draftWithDsl);
        }
    }

    /**
     * Load all active operators and index by name for DRL compilation.
     */
    private Map<String, Operator> loadOperatorMap() {
        try {
            List<Operator> operators = operatorPort.findGlobalOperatorsByStatus(Operator.OperatorStatus.ACTIVE);
            return operators.stream()
                    .collect(Collectors.toMap(Operator::getName, o -> o, (a, b) -> a));
        } catch (Exception ex) {
            log.warn("Failed to load operators for DRL compilation: {}; proceeding with empty map", ex.getMessage());
            return Map.of();
        }
    }
}
