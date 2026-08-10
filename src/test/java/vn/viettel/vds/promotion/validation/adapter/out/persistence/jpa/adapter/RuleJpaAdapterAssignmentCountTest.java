package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleListFilter;
import vn.viettel.vds.promotion.validation.application.port.out.RuleListRow;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * PROM-1368 regression: the rule-list "Số lượng gán" column must count EVERY
 * binding for the rule, not only active ones. A binding is deactivated
 * (active=false) when its campaign finishes/pauses, yet that campaign is still
 * shown in the rule detail "Danh sách chiến dịch đã gán" tab (which queries
 * bindings with active=null). Filtering active=true in the count made the list
 * show 0 while the detail tab listed the campaign — count vs its own drill-down
 * disagreed. The fix drops the active filter and counts distinct object_id.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleJpaAdapter.findWithFilters() — PROM-1368 assignment count includes inactive bindings")
class RuleJpaAdapterAssignmentCountTest {

    @Mock
    private RuleJpaRepository repository;
    @Mock
    private RuleEntityMapper mapper;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    @InjectMocks
    private RuleJpaAdapter adapter;

    @BeforeEach
    void wireEntityManager() {
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
    }

    @Test
    @DisplayName("assignment-count SQL counts all bindings (no active=true) → inactive binding still counted")
    void assignmentCount_countsInactiveBindings() {
        RuleJpaEntity entity = new RuleJpaEntity();
        entity.setId("rule-1");

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        // Total-count query (first call) must be > 0 so the page query runs.
        when(query.getSingleResult()).thenReturn(1L);
        when(query.setFirstResult(anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        // One page row: id, node_count, assignment_count. The rule has a single
        // binding whose campaign has finished (active=false), so the DB-side
        // COUNT(DISTINCT object_id) — with the active filter removed — returns 1.
        when(query.getResultList()).thenReturn(List.<Object[]>of(new Object[]{"rule-1", 2, 1L}));
        when(repository.findAllById(List.of("rule-1"))).thenReturn(List.of(entity));
        Rule rule = Rule.builder().id("rule-1").code("VRUL-1").build();
        when(mapper.toDomain(any(RuleJpaEntity.class))).thenReturn(rule);

        RuleListFilter filter = new RuleListFilter(null, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        Page<RuleListRow> page = adapter.findWithFilters(filter, pageable);

        // The count surfaced to the list = 1, matching the detail tab (the bug was 0).
        assertThat(page.getContent()).singleElement()
                .extracting(RuleListRow::assignmentCount)
                .isEqualTo(1L);

        // Pin the SQL: the assignment-count subquery must not restrict to active
        // bindings — that restriction is exactly what produced the PROM-1368 zero.
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(entityManager, org.mockito.Mockito.atLeastOnce())
                .createNativeQuery(sqlCaptor.capture());
        String pageSelect = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.startsWith("SELECT r.id"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("page select query not issued"));
        assertThat(pageSelect)
                .as("assignment count must count all bindings without active filter (PROM-1368)")
                .contains("COUNT(DISTINCT b.object_id) FROM rule_bindings b WHERE b.rule_id = r.id")
                .doesNotContain("b.active = true");
    }
}
