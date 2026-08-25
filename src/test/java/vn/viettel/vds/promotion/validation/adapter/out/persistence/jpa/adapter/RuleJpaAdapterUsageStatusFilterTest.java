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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.RuleEntityMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleListFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * PROM-1479 regression: the "Trạng thái sử dụng" filter must use the same notion of
 * "đã gán" as the displayed "Số lượng gán" column.
 *
 * <p>SRS VRUL001 control 4 defines the filter ON the column ("Chưa gán" = Số lượng
 * gán = 0). PROM-1368 widened {@code ASSIGNMENT_COUNT_SUBQUERY} to count every
 * binding (a binding is deactivated when its campaign finishes/pauses, yet the
 * campaign is still listed in the rule's drill-down), but left this filter on
 * {@code active = true}. A rule whose bindings had all been deactivated then
 * matched UNASSIGNED while its column showed 1 — the reported symptom.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleJpaAdapter.findWithFilters() — PROM-1479 usageStatus filter matches the assignment-count column")
class RuleJpaAdapterUsageStatusFilterTest {

    private static final String BINDING_EXISTS = "(SELECT 1 FROM rule_bindings b WHERE b.rule_id = r.id)";

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

    /**
     * Runs the count query only (total = 0 short-circuits before the page query),
     * then returns the SQL that was issued so the WHERE clause can be asserted.
     */
    private String captureCountSql(RuleListFilter.UsageStatus usageStatus) {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0L);

        RuleListFilter filter = new RuleListFilter(null, null, null, null, null, usageStatus);
        Pageable pageable = PageRequest.of(0, 10);
        adapter.findWithFilters(filter, pageable);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(entityManager, Mockito.atLeastOnce()).createNativeQuery(sqlCaptor.capture());
        List<String> issued = sqlCaptor.getAllValues();
        return issued.stream()
                .filter(sql -> sql.startsWith("SELECT COUNT(*)"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("count query not issued, got: " + issued));
    }

    @Test
    @DisplayName("UNASSIGNED excludes any rule that has a binding, even a deactivated one")
    void unassigned_excludesRulesWithInactiveBindings() {
        String sql = captureCountSql(RuleListFilter.UsageStatus.UNASSIGNED);

        assertThat(sql)
                .as("\"Chưa gán\" = no binding at all, mirroring ASSIGNMENT_COUNT_SUBQUERY (PROM-1479)")
                .contains(" AND NOT EXISTS " + BINDING_EXISTS)
                .doesNotContain("b.active = true");
    }

    @Test
    @DisplayName("ASSIGNED includes a rule whose only binding was deactivated")
    void assigned_includesRulesWithOnlyInactiveBindings() {
        String sql = captureCountSql(RuleListFilter.UsageStatus.ASSIGNED);

        assertThat(sql)
                .as("\"Đã gán\" = has any binding, mirroring ASSIGNMENT_COUNT_SUBQUERY (PROM-1479)")
                .contains(" AND EXISTS " + BINDING_EXISTS)
                .doesNotContain("b.active = true");
    }

    @Test
    @DisplayName("No usage-status filter → no binding predicate at all")
    void noUsageStatus_leavesQueryUnfiltered() {
        String sql = captureCountSql(null);

        assertThat(sql).doesNotContain("rule_bindings");
    }
}
