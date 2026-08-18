package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleNodeEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleNodeRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnPromixJpa
public class RuleJpaAdapter implements RulePersistencePort {

    private static final Logger logger = LoggerFactory.getLogger(RuleJpaAdapter.class);

    private final RuleJpaRepository repository;
    private final RuleEntityMapper mapper;
    private final RuleNodeRepository nodeRepository;
    private final RuleNodeEntityMapper nodeMapper;

    public RuleJpaAdapter(RuleJpaRepository repository, RuleEntityMapper mapper,
                          RuleNodeRepository nodeRepository, RuleNodeEntityMapper nodeMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.nodeRepository = nodeRepository;
        this.nodeMapper = nodeMapper;
    }

    @Override
    public Rule save(Rule rule) {
        logger.debug("[RULE_SAVE] Starting save rule: id={}, code={}", rule.getId(), rule.getCode());
        RuleJpaEntity entity = mapper.toEntity(rule);
        logger.trace("[RULE_SAVE] Mapped rule to entity: id={}, state={}", entity.getId(), entity.getState());
        RuleJpaEntity saved = repository.save(entity);
        logger.info("[RULE_SAVE] Rule saved successfully: id={}, code={}", saved.getId(), saved.getCode());
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Rule> findById(String id) {
        logger.debug("[RULE_LOAD] Starting load rule by ID: {}", id);
        long startTime = System.currentTimeMillis();

        Optional<RuleJpaEntity> entityOpt = repository.findById(id);
        if (entityOpt.isEmpty()) {
            logger.warn("[RULE_LOAD] Rule not found by ID: {}", id);
            return Optional.empty();
        }

        return entityOpt.map(entity -> {
            logger.debug("[RULE_LOAD] Found rule entity: id={}, code={}, state={}",
                    entity.getId(), entity.getCode(), entity.getState());

            Rule rule = mapper.toDomain(entity);
            logger.trace("[RULE_LOAD] Mapped entity to domain: id={}, code={}", rule.getId(), rule.getCode());

            // Load nodes from rule_nodes table
            logger.debug("[RULE_LOAD] Loading nodes for rule: {}", id);
            List<RuleNodeEntity> nodeEntities = nodeRepository.findByValidationRuleIdOrderByOrder(id);

            if (nodeEntities != null && !nodeEntities.isEmpty()) {
                logger.info("[RULE_LOAD] Found {} node entities for rule: {}", nodeEntities.size(), id);
                logNodeEntities(nodeEntities);

                List<RuleNode> nodes = nodeMapper.toDomainList(nodeEntities);
                logger.debug("[RULE_LOAD] Converted to {} domain nodes (root level)", nodes.size());
                rule.setNodes(nodes);
            } else {
                logger.debug("[RULE_LOAD] No nodes found for rule: {}", id);
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("[RULE_LOAD] Rule loaded successfully: id={}, code={}, nodeCount={}, duration={}ms",
                    rule.getId(), rule.getCode(),
                    rule.getNodes() != null ? rule.getNodes().size() : 0,
                    duration);
            return rule;
        });
    }

    @Override
    public Optional<Rule> findByCode(String code) {
        logger.debug("[RULE_LOAD] Starting load rule by code: {}", code);
        long startTime = System.currentTimeMillis();

        Optional<RuleJpaEntity> entityOpt = repository.findByCode(code);
        if (entityOpt.isEmpty()) {
            logger.warn("[RULE_LOAD] Rule not found by code: {}", code);
            return Optional.empty();
        }

        return entityOpt.map(entity -> {
            logger.debug("[RULE_LOAD] Found rule entity: id={}, code={}, state={}",
                    entity.getId(), entity.getCode(), entity.getState());

            Rule rule = mapper.toDomain(entity);
            logger.trace("[RULE_LOAD] Mapped entity to domain: id={}, code={}", rule.getId(), rule.getCode());

            // Load nodes from rule_nodes table
            logger.debug("[RULE_LOAD] Loading nodes for rule: {} (code={})", rule.getId(), code);
            List<RuleNodeEntity> nodeEntities = nodeRepository.findByValidationRuleIdOrderByOrder(rule.getId());

            if (nodeEntities != null && !nodeEntities.isEmpty()) {
                logger.info("[RULE_LOAD] Found {} node entities for rule: {} (code={})",
                        nodeEntities.size(), rule.getId(), code);
                logNodeEntities(nodeEntities);

                List<RuleNode> nodes = nodeMapper.toDomainList(nodeEntities);
                logger.debug("[RULE_LOAD] Converted to {} domain nodes (root level)", nodes.size());
                rule.setNodes(nodes);
            } else {
                logger.debug("[RULE_LOAD] No nodes found for rule: {} (code={})", rule.getId(), code);
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("[RULE_LOAD] Rule loaded successfully: id={}, code={}, nodeCount={}, duration={}ms",
                    rule.getId(), rule.getCode(),
                    rule.getNodes() != null ? rule.getNodes().size() : 0,
                    duration);
            return rule;
        });
    }

    @Override
    public Page<Rule> findByState(Rule.RuleState state, Pageable pageable) {
        return findWithFilters(state, null, null, pageable);
    }

    @Override
    public Page<Rule> findWithFilters(Rule.RuleState state, String codePattern, String namePattern, Pageable pageable) {
        String stateName = state != null ? state.name() : null;
        return repository.findWithFilters(stateName, codePattern, namePattern, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<Rule> findAllOrderByUpdatedAtDesc() {
        return repository.findAll().stream()
                .sorted((e1, e2) -> e2.getUpdatedAt().compareTo(e1.getUpdatedAt()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public long countByState(Rule.RuleState state) {
        return repository.findByState(state.name()).size();
    }

    @Override
    public List<Rule> findByStateNot(Rule.RuleState state) {
        return repository.findAll().stream()
                .filter(e -> !state.name().equals(e.getState()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Rule> findByType(String type) {
        // Note: type field no longer exists in validation_rules table
        // This method will return empty list - consider removing from port interface
        return List.of();
    }

    @Override
    public List<Rule> findByRuleSetId(String ruleSetId) {
        // Note: ruleSetId field no longer exists in validation_rules table
        // This method will return empty list - consider removing from port interface
        return List.of();
    }

    @Override
    public List<Rule> findByCampaignId(String campaignId) {
        // Note: campaignId field no longer exists in validation_rules table
        // This method will return empty list - consider removing from port interface
        return List.of();
    }

    @Override
    public List<Rule> findByPriorityBetween(int minPriority, int maxPriority) {
        // Note: priority field no longer exists in validation_rules table
        // This method will return empty list - consider removing from port interface
        return List.of();
    }

    @Override
    public List<Rule> findByTargetSegmentsContaining(String segment) {
        // JPA doesn't have this query - filter manually
        return repository.findAll().stream()
                .filter(e -> e.getTargetSegments() != null && e.getTargetSegments().contains(segment))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Rule> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public void delete(Rule rule) {
        repository.deleteById(rule.getId());
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    /**
     * Log detailed information about node entities for debugging purposes.
     */
    private void logNodeEntities(List<RuleNodeEntity> nodeEntities) {
        if (!logger.isDebugEnabled()) {
            return;
        }

        logger.debug("[NODE_LOAD] === Node Entities Summary ===");
        for (RuleNodeEntity node : nodeEntities) {
            String parentId = node.getParent() != null ? node.getParent().getNodeId() : "null (root)";
            String childrenIds = node.getChildrenIds() != null
                    ? String.join(", ", node.getChildrenIds())
                    : "none";

            if ("GROUP".equalsIgnoreCase(node.getType())) {
                logger.debug("[NODE_LOAD] GROUP Node: nodeId={}, groupLogic={}, parentId={}, childrenIds=[{}], order={}",
                        node.getNodeId(),
                        node.getGroupLogic(),
                        parentId,
                        childrenIds,
                        node.getOrder());
            } else if ("COND".equalsIgnoreCase(node.getType())) {
                logger.debug("[NODE_LOAD] COND Node: nodeId={}, operator={}, reasonCode={}, parentId={}, order={}, params={}",
                        node.getNodeId(),
                        node.getOperatorName(),
                        node.getReasonCode(),
                        parentId,
                        node.getOrder(),
                        node.getParams() != null ? node.getParams().toString() : "null");
            } else {
                logger.debug("[NODE_LOAD] UNKNOWN Node: nodeId={}, type={}, parentId={}, order={}",
                        node.getNodeId(),
                        node.getType(),
                        parentId,
                        node.getOrder());
            }
        }
        logger.debug("[NODE_LOAD] === End Node Entities Summary ===");
    }
}
