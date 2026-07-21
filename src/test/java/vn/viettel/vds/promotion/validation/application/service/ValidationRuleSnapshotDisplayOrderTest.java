package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleSnapshotEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleBindingJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleSnapshotRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * PROM-1350 regression on the revert path: a version restore must put back
 * {@code rule_nodes.display_order} together with {@code node_order}. The snapshot DTO
 * carried only {@code order}, so restoring a version wrote the tree position back into
 * node_order while leaving display_order at null/0 — reintroducing exactly the symptom
 * the ticket reports on the save path.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationRuleSnapshotService — PROM-1350 display_order round-trip")
class ValidationRuleSnapshotDisplayOrderTest {

    private static final String RULE_ID = "rule-1";

    @Mock
    private ValidationRuleSnapshotRepository snapshotRepository;
    @Mock
    private ValidationRuleJpaRepository validationRuleRepository;
    @Mock
    private RuleBindingJpaRepository ruleBindingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ValidationRuleSnapshotService service() {
        return new ValidationRuleSnapshotService(
                snapshotRepository, validationRuleRepository, ruleBindingRepository, objectMapper);
    }

    /** Rule whose two sibling nodes sit at positions 0 and 1 of the same parent. */
    private ValidationRuleEntity ruleWithOrderedNodes() {
        ValidationRuleEntity rule = new ValidationRuleEntity();
        rule.setId(RULE_ID);
        rule.setCode("RULE_1");
        rule.setName("rule 1");
        rule.setRuleVersion(3L);
        rule.setLogic("ALL");
        rule.setNodes(new java.util.ArrayList<>(List.of(
                node("n0", 0),
                node("n1", 1))));
        return rule;
    }

    private RuleNodeEntity node(String nodeId, int order) {
        RuleNodeEntity node = new RuleNodeEntity();
        node.setId("id-" + nodeId);
        node.setNodeId(nodeId);
        node.setType("COND");
        node.setOrder(order);
        node.setDisplayOrder(order);
        return node;
    }

    private String capturedSnapshotJson() {
        ArgumentCaptor<ValidationRuleSnapshotEntity> captor =
                ArgumentCaptor.forClass(ValidationRuleSnapshotEntity.class);
        org.mockito.Mockito.verify(snapshotRepository).save(captor.capture());
        return captor.getValue().getSnapshotData();
    }

    @Test
    @DisplayName("capture then restore keeps display_order aligned with node_order")
    void restoreKeepsDisplayOrder() {
        ValidationRuleEntity rule = ruleWithOrderedNodes();
        when(validationRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(snapshotRepository.findByValidationRuleIdAndVersion(RULE_ID, 3L))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any(ValidationRuleSnapshotEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service().createSnapshot(RULE_ID, null, "saga-1", "corr-1",
                ValidationRuleSnapshotEntity.SnapshotReason.BEFORE_UPDATE);

        String json = capturedSnapshotJson();
        assertThat(json).contains("\"displayOrder\":1");

        // The rule has since been edited: both columns were reshuffled/zeroed.
        rule.getNodes().forEach(n -> {
            n.setOrder(9);
            n.setDisplayOrder(0);
        });

        when(snapshotRepository.findByValidationRuleIdAndVersion(RULE_ID, 3L))
                .thenReturn(Optional.of(snapshotEntity(json)));
        when(validationRuleRepository.save(any(ValidationRuleEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ValidationRuleEntity restored = service().restoreFromSnapshot(RULE_ID, 3L);

        assertThat(restored.getNodes())
                .extracting(RuleNodeEntity::getNodeId, RuleNodeEntity::getOrder,
                        RuleNodeEntity::getDisplayOrder)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("n0", 0, 0),
                        org.assertj.core.api.Assertions.tuple("n1", 1, 1));
    }

    @Test
    @DisplayName("snapshot taken before the fix (no displayOrder) falls back to order")
    void legacySnapshotFallsBackToOrder() {
        ValidationRuleEntity rule = ruleWithOrderedNodes();
        // Snapshot JSON as written before PROM-1350: node carries `order` but no `displayOrder`.
        String legacyJson = """
                {"id":"rule-1","code":"RULE_1","name":"rule 1","ruleVersion":3,"logic":"ALL",
                 "nodes":[{"id":"id-n0","nodeId":"n0","type":"COND","order":0},
                          {"id":"id-n1","nodeId":"n1","type":"COND","order":1}]}
                """;

        when(snapshotRepository.findByValidationRuleIdAndVersion(RULE_ID, 3L))
                .thenReturn(Optional.of(snapshotEntity(legacyJson)));
        when(validationRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(validationRuleRepository.save(any(ValidationRuleEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ValidationRuleEntity restored = service().restoreFromSnapshot(RULE_ID, 3L);

        assertThat(restored.getNodes())
                .extracting(RuleNodeEntity::getDisplayOrder)
                .containsExactly(0, 1);
    }

    private ValidationRuleSnapshotEntity snapshotEntity(String json) {
        return ValidationRuleSnapshotEntity.builder()
                .id("snap-1")
                .validationRuleId(RULE_ID)
                .version(3L)
                .snapshotData(json)
                .build();
    }
}
