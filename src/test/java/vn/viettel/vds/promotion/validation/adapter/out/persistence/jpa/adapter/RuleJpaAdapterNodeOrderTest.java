package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleNodeEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleNodeRepository;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * PROM-1350 regression: {@code rule_nodes.display_order} must be persisted (= {@code node_order},
 * the sibling position within the parent) on every save. Before the fix, {@link RuleNodeEntity}
 * had no {@code display_order} mapping so the column was left at its DB default (0) for every node,
 * regardless of the tree layout (see screenshot on the ticket: display_order = 0 for all rows).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleJpaAdapter.save() — PROM-1350 display_order persistence")
class RuleJpaAdapterNodeOrderTest {

    @Mock
    private RuleJpaRepository repository;
    @Mock
    private RuleEntityMapper mapper;
    @Mock
    private RuleNodeRepository nodeRepository;
    @Mock
    private RuleNodeEntityMapper nodeMapper;
    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private RuleJpaAdapter adapter;

    @org.junit.jupiter.api.BeforeEach
    void wireEntityManager() {
        // @PersistenceContext field injection is skipped by @InjectMocks once the constructor
        // path is used, so wire the EntityManager mock explicitly.
        org.springframework.test.util.ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
    }

    /** Tree: rootGROUP(ANY)[ cond-a , subGROUP(ALL)[ cond-c , cond-d ] ]. */
    private Rule tree() {
        RuleNode condA = RuleNode.builder().nodeId("cond-a").type(RuleNode.NodeType.COND)
                .operatorName("customer.segment.IN").build();
        RuleNode condC = RuleNode.builder().nodeId("cond-c").type(RuleNode.NodeType.COND)
                .operatorName("budget.orders_value.total.LESS_OR_EQUAL").build();
        RuleNode condD = RuleNode.builder().nodeId("cond-d").type(RuleNode.NodeType.COND)
                .operatorName("budget.redemptions.per_customer.in_campaign.LESS_OR_EQUAL").build();
        RuleNode subGroup = RuleNode.builder().nodeId("group-sub").type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ALL).children(List.of(condC, condD)).build();
        RuleNode root = RuleNode.builder().nodeId("group-root").type(RuleNode.NodeType.GROUP)
                .groupLogic(Rule.LogicType.ANY).children(List.of(condA, subGroup)).build();
        return Rule.builder().id("rule-1").code("VRUL-1").nodes(List.of(root)).build();
    }

    private void stubRuleRow() {
        RuleJpaEntity saved = new RuleJpaEntity();
        saved.setId("rule-1");
        when(mapper.toEntity(any(Rule.class))).thenReturn(saved);
        when(repository.save(any(RuleJpaEntity.class))).thenReturn(saved);
        when(mapper.toDomain(any(RuleJpaEntity.class))).thenReturn(new Rule());
        when(entityManager.getReference(eq(ValidationRuleEntity.class), any()))
                .thenReturn(new ValidationRuleEntity());
        // Fresh, minimal entity per node so saveNodeDfs sets order/displayOrder on it.
        when(nodeMapper.toEntity(any(RuleNode.class), any()))
                .thenAnswer(inv -> {
                    RuleNode n = inv.getArgument(0);
                    return new RuleNodeEntity(n.getNodeId(), n.getType().name());
                });
        when(nodeRepository.save(any(RuleNodeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("new nodes → display_order persisted = node_order (sibling position), never left null/0")
    void newNodes_displayOrderEqualsNodeOrder() {
        stubRuleRow();
        // No existing rows → every node takes the insert branch.
        when(nodeRepository.findByValidationRuleIdOrderByOrder("rule-1")).thenReturn(List.of());

        adapter.save(tree());

        ArgumentCaptor<RuleNodeEntity> captor = ArgumentCaptor.forClass(RuleNodeEntity.class);
        org.mockito.Mockito.verify(nodeRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        List<RuleNodeEntity> saved = captor.getAllValues();

        assertThat(saved).hasSize(5);
        // Every node: display_order set and equal to node_order (the fix).
        assertThat(saved).allSatisfy(e -> {
            assertThat(e.getDisplayOrder()).as("display_order must be persisted, not null").isNotNull();
            assertThat(e.getDisplayOrder()).as("display_order must alias node_order").isEqualTo(e.getOrder());
        });
        // Sibling positions reflect the tree: not everything is 0 (the bug symptom).
        Map<String, Integer> orderByNode = saved.stream()
                .collect(java.util.stream.Collectors.toMap(RuleNodeEntity::getNodeId, RuleNodeEntity::getOrder));
        assertThat(orderByNode).containsEntry("group-root", 0)
                .containsEntry("cond-a", 0)
                .containsEntry("group-sub", 1)
                .containsEntry("cond-c", 0)
                .containsEntry("cond-d", 1);
    }

    @Test
    @DisplayName("existing node updated in place → display_order refreshed = node_order")
    void existingNode_displayOrderRefreshed() {
        stubRuleRow();
        // subGroup already exists at a stale position with display_order untouched (legacy row).
        RuleNodeEntity stale = new RuleNodeEntity("group-sub", "GROUP");
        stale.setOrder(9);
        stale.setDisplayOrder(null);
        when(nodeRepository.findByValidationRuleIdOrderByOrder("rule-1")).thenReturn(List.of(stale));

        adapter.save(tree());

        // The managed 'stale' entity is updated in place to its real sibling position (1).
        assertThat(stale.getOrder()).isEqualTo(1);
        assertThat(stale.getDisplayOrder()).as("update branch must also set display_order").isEqualTo(1);
    }
}
