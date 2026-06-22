package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleJpaEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.mapper.ValidationRuleMapper;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regression suite for <b>BUG-024</b>.
 *
 * <p>Commit {@code d7c189d} added {@link jakarta.persistence.Version @Version} to
 * {@link RuleJpaEntity#getVersion()}. After that change, Spring Data JPA's
 * {@code SimpleJpaRepository.save()} dispatches between
 * {@link jakarta.persistence.EntityManager#persist persist} and
 * {@link jakarta.persistence.EntityManager#merge merge} via {@link RuleJpaEntity#isNew()}.
 * Path B of {@code SettingValidationRuleCommandHandler.resolveOrCreateRule} (and
 * {@code RuleManagementService.createRule}) builds a brand-new domain {@link Rule} with
 * a non-null UUIDv7 id and default {@code version=0L}. Without the fix, that pair
 * resolves to {@code merge() → UPDATE → 0 rows → StaleObjectStateException}, rolling
 * back the entire saga and breaking every DISCOUNT_COUPON campaign creation E2E.
 *
 * <p>The adapter now probes the row by id; if the row is not yet persisted it forces
 * {@code version=null} so {@code isNew()} returns true and Spring Data routes the call
 * through {@code persist()} (INSERT). For the existing-rule path (Path A — load,
 * mutate, save) the version is passed through as-is so the optimistic-lock check still
 * fires inside {@code merge()}.
 */
@ExtendWith(MockitoExtension.class)
class ValidationRuleJpaAdapterBug024Test {

    @Mock
    private RuleJpaRepository jpaRepository;

    @Mock
    private ValidationRuleMapper mapper;

    @InjectMocks
    private ValidationRuleJpaAdapter adapter;

    @Nested
    @DisplayName("RuleJpaEntity.isNew()")
    class IsNewTests {

        @Test
        @DisplayName("isNew() returns true when @Version is null (fresh entity → persist())")
        void isNew_returnsTrue_whenVersionNull() {
            RuleJpaEntity entity = new RuleJpaEntity();
            entity.setId("rule-1");
            entity.setName("Test");
            entity.setRuleVersion(1L);
            entity.setVersion(null);

            assertThat(entity.isNew()).isTrue();
        }

        @Test
        @DisplayName("isNew() returns false when @Version is non-null (loaded entity → merge())")
        void isNew_returnsFalse_whenVersionNonNull() {
            RuleJpaEntity entity = new RuleJpaEntity();
            entity.setId("rule-1");
            entity.setName("Test");
            entity.setRuleVersion(1L);
            entity.setVersion(0L);

            assertThat(entity.isNew()).isFalse();
        }
    }

    @Nested
    @DisplayName("ValidationRuleJpaAdapter.save() — BUG-024 normalisation")
    class SaveTests {

        @Test
        @DisplayName("New rule (existsById=false): adapter forces version=null → isNew()=true → persist semantics")
        void save_newRule_setsVersionNull_soSpringDataPersists() {
            String ruleId = "0199df64-6-78b1-79a5-a50a-6bdccb70c035";
            Rule newRule = baseRule(ruleId)
                    // Caller built the domain rule with default 0L (Path B / createRule)
                    .version(0L)
                    .build();

            when(jpaRepository.existsById(ruleId)).thenReturn(false);
            when(jpaRepository.save(any(RuleJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.jpaEntityToDomain(any(RuleJpaEntity.class))).thenReturn(newRule);

            adapter.save(newRule);

            ArgumentCaptor<RuleJpaEntity> captor = ArgumentCaptor.forClass(RuleJpaEntity.class);
            org.mockito.Mockito.verify(jpaRepository).save(captor.capture());

            RuleJpaEntity persisted = captor.getValue();
            assertThat(persisted.getVersion())
                    .as("New rule must enter Spring Data save() with version=null so isNew()=true → persist()")
                    .isNull();
            assertThat(persisted.isNew())
                    .as("Persistable.isNew() must return true for fresh entity")
                    .isTrue();
        }

        @Test
        @DisplayName("Existing rule (existsById=true): version is preserved → isNew()=false → merge() with optimistic-lock check")
        void save_existingRule_keepsVersion_soSpringDataMerges() {
            String ruleId = "0199df64-6-78b1-79a5-a50a-6bdccb70c035";
            Rule loadedRule = baseRule(ruleId)
                    // Loaded from DB after a prior INSERT + n updates
                    .version(3L)
                    .build();

            when(jpaRepository.existsById(ruleId)).thenReturn(true);
            when(jpaRepository.save(any(RuleJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.jpaEntityToDomain(any(RuleJpaEntity.class))).thenReturn(loadedRule);

            adapter.save(loadedRule);

            ArgumentCaptor<RuleJpaEntity> captor = ArgumentCaptor.forClass(RuleJpaEntity.class);
            org.mockito.Mockito.verify(jpaRepository).save(captor.capture());

            RuleJpaEntity persisted = captor.getValue();
            assertThat(persisted.getVersion())
                    .as("Existing rule must keep its loaded version so optimistic-lock UPDATE matches the row")
                    .isEqualTo(3L);
            assertThat(persisted.isNew())
                    .as("Persistable.isNew() must return false for loaded entity")
                    .isFalse();
        }

        @Test
        @DisplayName("Path B brand-new rule with version=null: adapter still routes to persist() (idempotent)")
        void save_newRuleWithNullVersion_stillPersists() {
            String ruleId = "0199df64-9-67f0-71fb-a887-c4374896ec63";
            Rule freshRule = baseRule(ruleId)
                    .version(null)
                    .build();

            when(jpaRepository.existsById(ruleId)).thenReturn(false);
            when(jpaRepository.save(any(RuleJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.jpaEntityToDomain(any(RuleJpaEntity.class))).thenReturn(freshRule);

            adapter.save(freshRule);

            ArgumentCaptor<RuleJpaEntity> captor = ArgumentCaptor.forClass(RuleJpaEntity.class);
            org.mockito.Mockito.verify(jpaRepository).save(captor.capture());

            assertThat(captor.getValue().isNew()).isTrue();
        }

        private Rule.RuleBuilder baseRule(String ruleId) {
            Instant now = Instant.now();
            return Rule.builder()
                    .id(ruleId)
                    .code("CAMPAIGN_TEST")
                    .name("Auto-generated rule")
                    .active(true)
                    .ruleVersion(1L)
                    .logic(Rule.LogicType.ALL)
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy("system")
                    .updatedBy("system");
        }
    }
}
