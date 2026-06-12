package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleNodeEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleNodeRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.RuleVersionConflictException;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnPromixJpa
public class RuleJpaAdapter implements RulePersistencePort {

    private static final Logger logger = LoggerFactory.getLogger(RuleJpaAdapter.class);
    private final RuleJpaRepository repository;
    private final RuleEntityMapper mapper;
    private final RuleNodeRepository nodeRepository;
    private final RuleNodeEntityMapper nodeMapper;
    @PersistenceContext
    private EntityManager entityManager;

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
        RuleJpaEntity saved;
        try {
            saved = repository.save(entity);
        } catch (OptimisticLockingFailureException ex) {
            logger.warn("[RULE_SAVE] Optimistic lock conflict for rule: id={}", rule.getId());
            throw new RuleVersionConflictException(rule.getId());
        }
        logger.info("[RULE_SAVE] Rule saved successfully: id={}, code={}", saved.getId(), saved.getCode());

        if (rule.getNodes() != null && !rule.getNodes().isEmpty()) {
            saveRuleNodes(saved.getId(), rule.getNodes());
        }

        // Reload nodes from DB so the returned Rule includes them in its response.
        // mapper.toDomain(saved) only maps the rule row — nodes live in a separate
        // table and must be fetched explicitly.
        Rule result = mapper.toDomain(saved);
        List<RuleNodeEntity> savedNodeEntities =
                nodeRepository.findByValidationRuleIdOrderByOrder(saved.getId());
        if (savedNodeEntities != null && !savedNodeEntities.isEmpty()) {
            logger.debug("[RULE_SAVE] Reloading {} node(s) into returned rule: id={}",
                    savedNodeEntities.size(), saved.getId());
            result.setNodes(nodeMapper.toDomainList(savedNodeEntities));
        }
        return result;
    }

    private void saveRuleNodes(String ruleId, List<RuleNode> nodes) {
        logger.debug("[RULE_SAVE] Saving nodes for rule: {}", ruleId);
        deleteExistingNodes(ruleId);

        Map<String, RuleNode> nodeMap = buildNodeMap(nodes);
        Set<String> childNodeIds = findChildNodeIds(nodeMap);
        Set<String> rootNodeIds = nodeMap.keySet().stream()
                .filter(id -> !childNodeIds.contains(id))
                .collect(Collectors.toSet());

        ValidationRuleEntity ruleRef = entityManager.getReference(ValidationRuleEntity.class, ruleId);
        List<RuleNodeEntity> savedNodes = new ArrayList<>();
        for (String rootId : rootNodeIds) {
            saveNodeDfs(rootId, nodeMap, ruleRef, null, savedNodes);
        }
        logger.info("[RULE_SAVE] Saved {} node entities for rule: {}", savedNodes.size(), ruleId);
    }

    private void deleteExistingNodes(String ruleId) {
        List<RuleNodeEntity> existing = nodeRepository.findByValidationRuleIdOrderByOrder(ruleId);
        if (!existing.isEmpty()) {
            nodeRepository.deleteAll(existing);
        }
    }

    private Map<String, RuleNode> buildNodeMap(List<RuleNode> nodes) {
        Map<String, RuleNode> nodeMap = new HashMap<>();
        collectNodes(nodes, nodeMap);
        return nodeMap;
    }

    /**
     * Walk the input list and recurse into GROUP children so the map contains
     * every node in the tree — not just the roots. This lets save() accept both
     * flat input (e.g. createRule with a request DTO list) and tree input
     * (e.g. activateRule after getRuleById returns roots-only with children embedded).
     */
    private void collectNodes(List<RuleNode> nodes, Map<String, RuleNode> out) {
        if (nodes == null) return;
        for (RuleNode node : nodes) {
            if (node == null || node.getNodeId() == null || node.getType() == null) {
                continue;
            }
            out.putIfAbsent(node.getNodeId(), node);
            if (node.getType() == RuleNode.NodeType.GROUP && node.getChildren() != null) {
                collectNodes(node.getChildren(), out);
            }
        }
    }

    private Set<String> findChildNodeIds(Map<String, RuleNode> nodeMap) {
        Set<String> childNodeIds = new HashSet<>();
        for (RuleNode node : nodeMap.values()) {
            if (node.getType() == RuleNode.NodeType.GROUP && node.getChildren() != null) {
                for (RuleNode child : node.getChildren()) {
                    if (child.getNodeId() != null) {
                        childNodeIds.add(child.getNodeId());
                    }
                }
            }
        }
        return childNodeIds;
    }

    /**
     * Save a node and its children recursively (DFS), setting proper parent references.
     */
    private void saveNodeDfs(String nodeId, Map<String, RuleNode> nodeMap,
                             ValidationRuleEntity ruleRef, RuleNodeEntity parentEntity,
                             List<RuleNodeEntity> savedEntities) {
        RuleNode node = nodeMap.get(nodeId);
        if (node == null) return;
        RuleNodeEntity entity = nodeMapper.toEntity(node, parentEntity);
        entity.setId(UUID.randomUUID().toString());
        entity.setValidationRule(ruleRef);
        RuleNodeEntity persistedEntity = nodeRepository.save(entity);
        savedEntities.add(persistedEntity);
        // Recursively save children of GROUP nodes
        if (node.getType() == RuleNode.NodeType.GROUP && node.getChildren() != null) {
            for (RuleNode childPlaceholder : node.getChildren()) {
                String childId = childPlaceholder.getNodeId();
                if (childId != null && nodeMap.containsKey(childId)) {
                    saveNodeDfs(childId, nodeMap, ruleRef, persistedEntity, savedEntities);
                }
            }
        }
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
        List<RuleJpaEntity> all = repository.findByState(state.name());
        return convertToPage(all, pageable);
    }

    @Override
    public Page<Rule> findWithFilters(Rule.RuleState state,
                                      String codePattern, String namePattern, Pageable pageable) {
        List<RuleJpaEntity> all = repository.findAll().stream()
                .filter(e -> state == null || state.name().equals(e.getState()))
                .filter(e -> codePattern == null ||
                        (e.getCode() != null && e.getCode().toLowerCase().contains(codePattern.toLowerCase())))
                .filter(e -> namePattern == null ||
                        (e.getName() != null && e.getName().toLowerCase().contains(namePattern.toLowerCase())))
                .toList();
        return convertToPage(all, pageable);
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
    public boolean existsByName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String target = name.trim();
        // The column collation may be case-/accent-insensitive, so findByName can
        // return a superset. Narrow it with a code-point exact comparison (cs_as),
        // mirroring the PP search-collation convention. Trim stored names so
        // trailing whitespace from storage doesn't break the match.
        return repository.findByName(target).stream()
                .anyMatch(e -> e.getName() != null && target.equals(e.getName().trim()));
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
        List<RuleJpaEntity> all = repository.findAll();
        return convertToPage(all, pageable);
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

    @Override
    public void deleteNodesByRuleId(String ruleId) {
        logger.debug("[RULE_DELETE] Deleting nodes for rule: {}", ruleId);
        nodeRepository.deleteByValidationRuleId(ruleId);
        logger.info("[RULE_DELETE] Deleted nodes for rule: {}", ruleId);
    }

    @Override
    public List<Rule> findPublishedWithNullBundleHash() {
        return repository.findPublishedWithNullBundleHash().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Map<String, Integer> countNodesByRuleIds(Collection<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = nodeRepository.countByRuleIds(ruleIds);
        Map<String, Integer> result = HashMap.newHashMap(rows.size());
        for (Object[] row : rows) {
            result.put((String) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    private Page<Rule> convertToPage(List<RuleJpaEntity> entities, Pageable pageable) {
        // Apply sorting from pageable
        List<RuleJpaEntity> sortedEntities = applySorting(entities, pageable);

        int start = (int) pageable.getOffset();
        // Handle case when start is beyond the list size (return empty page)
        if (start >= sortedEntities.size()) {
            return new PageImpl<>(List.of(), pageable, sortedEntities.size());
        }
        int end = Math.min((start + pageable.getPageSize()), sortedEntities.size());
        List<Rule> pageContent = sortedEntities.subList(start, end)
                .stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(pageContent, pageable, sortedEntities.size());
    }

    /**
     * Apply sorting from Pageable to the entity list.
     * Supports sorting by entity fields: id, code, name, state, ruleVersion,
     * logic, publishedAt, publishedBy, createdAt, updatedAt, createdBy, updatedBy.
     */
    private List<RuleJpaEntity> applySorting(List<RuleJpaEntity> entities, Pageable pageable) {
        if (pageable.getSort().isUnsorted() || entities.isEmpty()) {
            return entities;
        }

        java.util.Comparator<RuleJpaEntity> comparator = null;

        for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
            java.util.Comparator<RuleJpaEntity> fieldComparator = getFieldComparator(order.getProperty());

            if (fieldComparator == null) {
                logger.warn("Unknown sort field: {}, skipping", order.getProperty());
                continue;
            }

            if (order.isDescending()) {
                fieldComparator = fieldComparator.reversed();
            }

            if (comparator == null) {
                comparator = fieldComparator;
            } else {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }

        if (comparator == null) {
            return entities;
        }

        return entities.stream()
                .sorted(comparator)
                .toList();
    }

    /**
     * Get comparator for a specific field.
     * Returns null for unknown fields.
     */
    private java.util.Comparator<RuleJpaEntity> getFieldComparator(String field) {
        return switch (field) {
            case "id" -> java.util.Comparator.comparing(RuleJpaEntity::getId,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "code" -> java.util.Comparator.comparing(RuleJpaEntity::getCode,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "name" -> java.util.Comparator.comparing(RuleJpaEntity::getName,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "state" -> java.util.Comparator.comparing(RuleJpaEntity::getState,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "ruleVersion" -> java.util.Comparator.comparing(RuleJpaEntity::getRuleVersion,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "logic" -> java.util.Comparator.comparing(RuleJpaEntity::getLogic,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "publishedAt" -> java.util.Comparator.comparing(RuleJpaEntity::getPublishedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "publishedBy" -> java.util.Comparator.comparing(RuleJpaEntity::getPublishedBy,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "createdAt" -> java.util.Comparator.comparing(RuleJpaEntity::getCreatedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "updatedAt" -> java.util.Comparator.comparing(RuleJpaEntity::getUpdatedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "createdBy" -> java.util.Comparator.comparing(RuleJpaEntity::getCreatedBy,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "updatedBy" -> java.util.Comparator.comparing(RuleJpaEntity::getUpdatedBy,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            default -> null;
        };
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
