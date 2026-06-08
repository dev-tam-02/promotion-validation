package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleNodeEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleNodeRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mapper.ValidationRuleMapper;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * PA1 regression: {@link ValidationRuleJpaAdapter#findById} must hydrate the rule's nodes from
 * the {@code rule_nodes} table. Previously it only mapped the base row, so a rule WITH conditions
 * was seen as empty by the coupon-creation saga's deployRuleToEngine → wrongly skipped → falsely
 * reported as COMPILE_DEPLOY_ERROR (campaigns GEN-2024-001123123, TT123).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationRuleJpaAdapter.findById node hydration")
class ValidationRuleJpaAdapterFindByIdNodesTest {

    @Mock
    private RuleJpaRepository jpaRepository;
    @Mock
    private ValidationRuleMapper mapper;
    @Mock
    private RuleNodeRepository nodeRepository;
    @Mock
    private RuleNodeEntityMapper nodeMapper;

    @InjectMocks
    private ValidationRuleJpaAdapter adapter;

    @Test
    @DisplayName("rule with nodes → nodes hydrated onto the returned Rule")
    void findById_hydratesNodes() {
        String ruleId = "019ea4a1-28d8-76c3-bebd-6ad14f8406e5";
        RuleJpaEntity entity = mock(RuleJpaEntity.class);
        Rule rule = new Rule();
        rule.setId(ruleId);

        when(jpaRepository.findById(ruleId)).thenReturn(Optional.of(entity));
        when(mapper.jpaEntityToDomain(entity)).thenReturn(rule);

        List<RuleNodeEntity> nodeEntities = List.of(mock(RuleNodeEntity.class), mock(RuleNodeEntity.class));
        when(nodeRepository.findByValidationRuleIdOrderByOrder(ruleId)).thenReturn(nodeEntities);
        List<RuleNode> domainNodes = List.of(mock(RuleNode.class));
        when(nodeMapper.toDomainList(nodeEntities)).thenReturn(domainNodes);

        Optional<Rule> result = adapter.findById(ruleId);

        assertThat(result).isPresent();
        assertThat(result.get().getNodes()).isSameAs(domainNodes);
    }

    @Test
    @DisplayName("rule with no nodes → nodes left untouched, mapper not invoked")
    void findById_noNodes() {
        String ruleId = "rule-empty";
        RuleJpaEntity entity = mock(RuleJpaEntity.class);
        Rule rule = new Rule();
        rule.setId(ruleId);

        when(jpaRepository.findById(ruleId)).thenReturn(Optional.of(entity));
        when(mapper.jpaEntityToDomain(entity)).thenReturn(rule);
        when(nodeRepository.findByValidationRuleIdOrderByOrder(ruleId)).thenReturn(List.of());

        Optional<Rule> result = adapter.findById(ruleId);

        assertThat(result).isPresent();
        assertThat(result.get().getNodes()).isNull();
        verify(nodeMapper, never()).toDomainList(any());
    }
}
