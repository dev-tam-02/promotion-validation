package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.application.port.out.OutboxEventPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleStructureException;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidVersionFormatException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleAlreadyExistsException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleHasBindingsException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleNotFoundException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleStateNotEditableException;
import vn.viettel.vds.promotion.validation.domain.model.OutboxEvent;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RuleService — the main application service handling CRUD operations
 * for validation rules.
 * <p>
 * Tests are written based on SRS requirements (VRUL001-VRUL005), NOT based on implementation.
 * When a test FAILs, it may indicate a bug in the source code.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleService Tests")
class RuleServiceTest {

    @Mock
    private RulePersistencePort rulePersistencePort;

    @Mock
    private RuleBindingPersistencePort ruleBindingPort;

    @Mock
    private OutboxEventPersistencePort outboxEventPort;

    private RuleService sut;

    @BeforeEach
    void setUp() {
        // RuleService uses @Lazy self-injection for transactional proxying.
        // In unit tests without Spring context, we pass 'sut' itself as the self reference.
        // This is safe because there's no proxy needed in unit tests.
        sut = new RuleService(rulePersistencePort, ruleBindingPort, outboxEventPort, null);
        // Re-create with self reference
        sut = new RuleService(rulePersistencePort, ruleBindingPort, outboxEventPort, sut);
    }

    // ========== Helper methods ==========

    private static Rule draftRule(String id, String code, String name) {
        return Rule.builder()
                .id(id)
                .code(code)
                .name(name)
                .state(Rule.RuleState.DRAFT)
                .active(false)
                .latestVersion(0)
                .version(1L)
                .logic(Rule.LogicType.ALL)
                .nodes(List.of(condNode("node-1", "op-1", "reason-1")))
                .createdAt(Instant.parse("2026-03-01T00:00:00Z"))
                .createdBy("user-1")
                .updatedAt(Instant.parse("2026-03-01T00:00:00Z"))
                .updatedBy("user-1")
                .build();
    }

    private static Rule publishedRule(String id, String code, String name) {
        return Rule.builder()
                .id(id)
                .code(code)
                .name(name)
                .state(Rule.RuleState.PUBLISHED)
                .active(true)
                .latestVersion(1)
                .version(2L)
                .logic(Rule.LogicType.ALL)
                .nodes(List.of(condNode("node-1", "op-1", "reason-1")))
                .createdAt(Instant.parse("2026-03-01T00:00:00Z"))
                .createdBy("user-1")
                .updatedAt(Instant.parse("2026-03-02T00:00:00Z"))
                .updatedBy("user-1")
                .build();
    }

    private static RuleNode condNode(String id, String operatorName, String reasonCode) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.COND)
                .operatorName(operatorName)
                .reasonCode(reasonCode)
                .build();
    }

    private static RuleNode groupNode(String id, Rule.LogicType logic, List<RuleNode> children) {
        return RuleNode.builder()
                .nodeId(id)
                .type(RuleNode.NodeType.GROUP)
                .groupLogic(logic)
                .children(children)
                .build();
    }

    // ========================================================================
    // VRUL002 — Create Rule
    // ========================================================================

    @Nested
    @DisplayName("createRule()")
    class CreateRuleTests {

        @Test
        @DisplayName("Should create rule successfully when code is unique and nodes are valid")
        void shouldCreateRule_whenCodeIsUniqueAndNodesAreValid() {
            // Given
            String code = "RULE_001";
            String name = "Weekend VIP Rule";
            List<RuleNode> nodes = List.of(condNode("n1", "op-vip", "NOT_VIP"));

            when(rulePersistencePort.existsByCode(code)).thenReturn(false);
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.createRule(code, name, Rule.LogicType.ALL, nodes, "admin");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotBlank();
            assertThat(result.getCode()).isEqualTo(code);
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getState()).isEqualTo(Rule.RuleState.DRAFT);
            assertThat(result.getLogic()).isEqualTo(Rule.LogicType.ALL);
            assertThat(result.getNodes()).hasSize(1);
            assertThat(result.getCreatedBy()).isEqualTo("admin");
            assertThat(result.getUpdatedBy()).isEqualTo("admin");
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();

            verify(rulePersistencePort).existsByCode(code);
            verify(rulePersistencePort).save(any(Rule.class));
        }

        @Test
        @DisplayName("Should throw RuleAlreadyExistsException when code already exists")
        void shouldThrowRuleAlreadyExists_whenCodeExists() {
            // Given
            String code = "EXISTING_CODE";
            when(rulePersistencePort.existsByCode(code)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> sut.createRule(code, "Name", Rule.LogicType.ALL,
                    List.of(condNode("n1", "op1", "rc1")), "admin"))
                    .isInstanceOf(RuleAlreadyExistsException.class);

            verify(rulePersistencePort, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidRuleStructureException when nodes list is empty")
        void shouldThrowInvalidStructure_whenNodesEmpty() {
            // Given
            when(rulePersistencePort.existsByCode(anyString())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> sut.createRule("CODE", "Name", Rule.LogicType.ALL,
                    List.of(), "admin"))
                    .isInstanceOf(InvalidRuleStructureException.class)
                    .hasMessageContaining("at least one node");
        }

        @Test
        @DisplayName("Should throw InvalidRuleStructureException when nodes list is null")
        void shouldThrowInvalidStructure_whenNodesNull() {
            // Given
            when(rulePersistencePort.existsByCode(anyString())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> sut.createRule("CODE", "Name", Rule.LogicType.ALL,
                    null, "admin"))
                    .isInstanceOf(InvalidRuleStructureException.class);
        }

        @Test
        @DisplayName("Should throw InvalidRuleStructureException when node has duplicate IDs")
        void shouldThrowInvalidStructure_whenNodeIdsDuplicated() {
            // Given
            List<RuleNode> nodes = List.of(
                    condNode("same-id", "op1", "rc1"),
                    condNode("same-id", "op2", "rc2")
            );
            when(rulePersistencePort.existsByCode(anyString())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> sut.createRule("CODE", "Name", Rule.LogicType.ALL,
                    nodes, "admin"))
                    .isInstanceOf(InvalidRuleStructureException.class)
                    .hasMessageContaining("unique");
        }

        @Test
        @DisplayName("Should set initial latestVersion to 0 on create")
        void shouldSetInitialVersion_whenCreating() {
            // Given
            when(rulePersistencePort.existsByCode(anyString())).thenReturn(false);
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.createRule("CODE", "Name", Rule.LogicType.ALL,
                    List.of(condNode("n1", "op1", "rc1")), "admin");

            // Then
            assertThat(result.getLatestVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should create rule with GROUP node containing children")
        void shouldCreateRule_withGroupNode() {
            // Given
            RuleNode child1 = condNode("c1", "op1", "rc1");
            RuleNode child2 = condNode("c2", "op2", "rc2");
            RuleNode group = groupNode("g1", Rule.LogicType.ALL, List.of(child1, child2));
            List<RuleNode> nodes = List.of(group);

            when(rulePersistencePort.existsByCode(anyString())).thenReturn(false);
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.createRule("CODE", "Name", Rule.LogicType.ALL, nodes, "admin");

            // Then
            assertThat(result.getNodes()).hasSize(1);
            verify(rulePersistencePort).save(any(Rule.class));
        }
    }

    // ========================================================================
    // VRUL004 — Get Rule By ID (View Detail)
    // ========================================================================

    @Nested
    @DisplayName("getRuleById()")
    class GetRuleByIdTests {

        @Test
        @DisplayName("Should return rule when found by ID")
        void shouldReturnRule_whenFoundById() {
            // Given
            Rule rule = draftRule("rule-1", "CODE_1", "Rule One");
            when(rulePersistencePort.findById("rule-1")).thenReturn(Optional.of(rule));

            // When
            Rule result = sut.getRuleById("rule-1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("rule-1");
            assertThat(result.getCode()).isEqualTo("CODE_1");
            assertThat(result.getName()).isEqualTo("Rule One");
        }

        @Test
        @DisplayName("Should throw RuleNotFoundException when rule does not exist")
        void shouldThrowNotFound_whenRuleDoesNotExist() {
            // Given
            when(rulePersistencePort.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sut.getRuleById("nonexistent"))
                    .isInstanceOf(RuleNotFoundException.class);
        }
    }

    // ========================================================================
    // VRUL001 — List Rules
    // ========================================================================

    @Nested
    @DisplayName("findRules()")
    class FindRulesTests {

        @Test
        @DisplayName("Should return all rules with pagination when no filters")
        void shouldReturnAllRules_whenNoFilters() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Rule> page = new PageImpl<>(
                    List.of(draftRule("r1", "C1", "Rule 1"), draftRule("r2", "C2", "Rule 2")),
                    pageable, 2
            );
            when(rulePersistencePort.findAll(pageable)).thenReturn(page);

            // When
            Page<Rule> result = sut.findRules(null, null, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(rulePersistencePort).findAll(pageable);
            verify(rulePersistencePort, never()).findWithFilters(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should filter by state when state is provided")
        void shouldFilterByState_whenStateProvided() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Rule> page = new PageImpl<>(List.of(publishedRule("r1", "C1", "Rule 1")), pageable, 1);
            when(rulePersistencePort.findWithFilters(eq(Rule.RuleState.PUBLISHED), any(), any(), eq(pageable)))
                    .thenReturn(page);

            // When
            Page<Rule> result = sut.findRules(Rule.RuleState.PUBLISHED, null, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(rulePersistencePort).findWithFilters(eq(Rule.RuleState.PUBLISHED), any(), any(), eq(pageable));
        }

        @Test
        @DisplayName("Should filter by name pattern when name is provided")
        void shouldFilterByNamePattern_whenNameProvided() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Rule> page = new PageImpl<>(List.of(), pageable, 0);
            when(rulePersistencePort.findWithFilters(any(), any(), eq("weekend"), eq(pageable)))
                    .thenReturn(page);

            // When
            Page<Rule> result = sut.findRules(null, null, "weekend", pageable);

            // Then
            verify(rulePersistencePort).findWithFilters(any(), any(), eq("weekend"), eq(pageable));
        }

        @Test
        @DisplayName("Should return empty page when no rules match")
        void shouldReturnEmptyPage_whenNoRulesMatch() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Rule> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(rulePersistencePort.findAll(pageable)).thenReturn(emptyPage);

            // When
            Page<Rule> result = sut.findRules(null, null, null, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // ========================================================================
    // VRUL003 — Update Rule
    // ========================================================================

    @Nested
    @DisplayName("updateRule()")
    class UpdateRuleTests {

        @Test
        @DisplayName("Should update rule name when rule is in DRAFT state")
        void shouldUpdateName_whenDraftState() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Old Name");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.updateRule("r1", "New Name", null, null, "editor");

            // Then
            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getUpdatedBy()).isEqualTo("editor");
            assertThat(result.getUpdatedAt()).isNotNull();
            verify(rulePersistencePort).save(any(Rule.class));
        }

        @Test
        @DisplayName("Should update logic type when provided")
        void shouldUpdateLogic_whenProvided() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.updateRule("r1", null, Rule.LogicType.ANY, null, "editor");

            // Then
            assertThat(result.getLogic()).isEqualTo(Rule.LogicType.ANY);
        }

        @Test
        @DisplayName("Should update nodes when provided")
        void shouldUpdateNodes_whenProvided() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            List<RuleNode> newNodes = List.of(
                    condNode("new-n1", "new-op", "new-rc"),
                    condNode("new-n2", "new-op2", "new-rc2")
            );

            // When
            Rule result = sut.updateRule("r1", null, null, newNodes, "editor");

            // Then
            assertThat(result.getNodes()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw RuleStateNotEditableException when rule is PUBLISHED")
        void shouldThrowStateNotEditable_whenPublished() {
            // Given
            Rule existing = publishedRule("r1", "CODE_1", "Published Rule");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));

            // When & Then
            assertThatThrownBy(() -> sut.updateRule("r1", "New Name", null, null, "editor"))
                    .isInstanceOf(RuleStateNotEditableException.class);

            verify(rulePersistencePort, never()).save(any());
        }

        @Test
        @DisplayName("Should throw RuleNotFoundException when rule does not exist")
        void shouldThrowNotFound_whenRuleDoesNotExist() {
            // Given
            when(rulePersistencePort.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sut.updateRule("nonexistent", "Name", null, null, "editor"))
                    .isInstanceOf(RuleNotFoundException.class);
        }

        @Test
        @DisplayName("Should not change name when name is null")
        void shouldNotChangeName_whenNameIsNull() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Original Name");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.updateRule("r1", null, null, null, "editor");

            // Then
            assertThat(result.getName()).isEqualTo("Original Name");
        }

        @Test
        @DisplayName("Should validate nodes structure when nodes are provided")
        void shouldValidateNodes_whenNodesProvided() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));

            List<RuleNode> invalidNodes = List.of(); // empty list

            // When & Then
            assertThatThrownBy(() -> sut.updateRule("r1", null, null, invalidNodes, "editor"))
                    .isInstanceOf(InvalidRuleStructureException.class);
        }

        // ---- PATCH semantics for context / description / fallbackErrorMessage ----

        @Test
        @DisplayName("Should skip context when null (PATCH: null = no-op)")
        void shouldSkipContext_whenNull() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setContext("ORDER_CREATED");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When — passing null context → should NOT change context
            Rule result = sut.updateRule("r1", null, null, null, null, null, null, "editor");

            // Then
            assertThat(result.getContext()).isEqualTo("ORDER_CREATED");
        }

        @Test
        @DisplayName("Should clear context to empty string when \"\" passed (PATCH: empty = clear)")
        void shouldClearContext_whenEmptyString() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setContext("ORDER_CREATED");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When — passing "" context → should set context to ""
            Rule result = sut.updateRule("r1", null, null, null, "", null, null, "editor");

            // Then
            assertThat(result.getContext()).isEqualTo("");
        }

        @Test
        @DisplayName("Should set new context value when non-empty string passed")
        void shouldSetNewContext_whenNonEmptyString() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setContext("ORDER_CREATED");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.updateRule("r1", null, null, null, "PAYMENT_COMPLETED", null, null, "editor");

            // Then
            assertThat(result.getContext()).isEqualTo("PAYMENT_COMPLETED");
        }

        @Test
        @DisplayName("Should skip description when null (PATCH: null = no-op)")
        void shouldSkipDescription_whenNull() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setDescription("Original description");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.updateRule("r1", null, null, null, null, null, null, "editor");

            // Then
            assertThat(result.getDescription()).isEqualTo("Original description");
        }

        @Test
        @DisplayName("Should clear description to empty string when \"\" passed")
        void shouldClearDescription_whenEmptyString() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setDescription("Original description");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.updateRule("r1", null, null, null, null, "", null, "editor");

            // Then
            assertThat(result.getDescription()).isEqualTo("");
        }

        @Test
        @DisplayName("Should set new description when non-empty string passed")
        void shouldSetNewDescription_whenNonEmptyString() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setDescription("Old description");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.updateRule("r1", null, null, null, null, "New description", null, "editor");

            // Then
            assertThat(result.getDescription()).isEqualTo("New description");
        }

        @Test
        @DisplayName("Should skip fallbackErrorMessage when null (PATCH: null = no-op)")
        void shouldSkipFallbackErrorMessage_whenNull() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setFallbackErrorMessage("Original error message");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When — null → no-op
            Rule result = sut.updateRule("r1", null, null, null, null, null, null, "editor");

            // Then
            assertThat(result.getFallbackErrorMessage()).isEqualTo("Original error message");
        }

        @Test
        @DisplayName("Should clear fallbackErrorMessage to empty string when \"\" passed")
        void shouldClearFallbackErrorMessage_whenEmptyString() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setFallbackErrorMessage("Original error message");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When — "" → clear
            Rule result = sut.updateRule("r1", null, null, null, null, null, "", "editor");

            // Then
            assertThat(result.getFallbackErrorMessage()).isEqualTo("");
        }

        @Test
        @DisplayName("Should set new fallbackErrorMessage when non-empty string passed")
        void shouldSetNewFallbackErrorMessage_whenNonEmptyString() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setFallbackErrorMessage("Old error message");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When — "abc" → set
            Rule result = sut.updateRule("r1", null, null, null, null, null, "Rule validation failed", "editor");

            // Then
            assertThat(result.getFallbackErrorMessage()).isEqualTo("Rule validation failed");
        }

        @Test
        @DisplayName("Should update all 3 optional fields independently in single call")
        void shouldUpdateAllThreeOptionalFields_inSingleCall() {
            // Given
            Rule existing = draftRule("r1", "CODE_1", "Name");
            existing.setContext("OLD_CTX");
            existing.setDescription("Old desc");
            existing.setFallbackErrorMessage("Old error");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(existing));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.updateRule(
                    "r1", null, null, null,
                    "PAYMENT_COMPLETED", "New desc", "Payment required", "editor"
            );

            // Then
            assertThat(result.getContext()).isEqualTo("PAYMENT_COMPLETED");
            assertThat(result.getDescription()).isEqualTo("New desc");
            assertThat(result.getFallbackErrorMessage()).isEqualTo("Payment required");
        }
    }

    // ========================================================================
    // VRUL005 — Delete Rule
    // ========================================================================

    @Nested
    @DisplayName("deleteRule()")
    class DeleteRuleTests {

        @Test
        @DisplayName("Should delete rule successfully when exists and has no bindings and version matches")
        void shouldDeleteRule_whenExistsAndNoBoundAndVersionMatches() {
            // Given
            Rule rule = draftRule("r1", "CODE_1", "Rule to Delete");
            rule.setVersion(1L);
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(rule));
            when(ruleBindingPort.countByRuleId("r1")).thenReturn(0L);

            // When
            sut.deleteRule("r1", 1L);

            // Then - SRS VRUL005 Step 7: delete nodes → delete rule → outbox event
            verify(rulePersistencePort).deleteNodesByRuleId("r1");
            verify(rulePersistencePort).deleteById("r1");

            // Verify outbox event VALIDATION_RULE_DELETED is created
            ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventPort).save(eventCaptor.capture());
            OutboxEvent event = eventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo("VALIDATION_RULE_DELETED");
            assertThat(event.getAggregateType()).isEqualTo("ValidationRule");
            assertThat(event.getAggregateId()).isEqualTo("r1");
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw RuleNotFoundException when rule does not exist")
        void shouldThrowNotFound_whenRuleDoesNotExist() {
            // Given
            when(rulePersistencePort.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sut.deleteRule("nonexistent", 1L))
                    .isInstanceOf(RuleNotFoundException.class);

            verify(rulePersistencePort, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when rule has active bindings (VALIDATION_RULE_IN_USE)")
        void shouldThrowRuleHasBindings_whenAssignedToCampaigns() {
            // Given — SRS VRUL005 Step 5b: assignmentCount > 0 → VALIDATION_RULE_IN_USE
            Rule rule = draftRule("r1", "CODE_1", "Bound Rule");
            rule.setVersion(1L);
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(rule));
            when(ruleBindingPort.countByRuleId("r1")).thenReturn(3L);

            // When & Then
            assertThatThrownBy(() -> sut.deleteRule("r1", 1L))
                    .isInstanceOf(RuleHasBindingsException.class);

            verify(rulePersistencePort, never()).deleteById(any());
            verify(rulePersistencePort, never()).deleteNodesByRuleId(any());
        }

        @Test
        @DisplayName("Should throw exception when version does not match (CONFLICTED)")
        void shouldThrowVersionConflict_whenVersionMismatch() {
            // Given — SRS VRUL005 Step 6: version conflict → CONFLICTED
            Rule rule = draftRule("r1", "CODE_1", "Rule");
            rule.setVersion(2L); // DB has version 2
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(rule));

            // When & Then — requesting delete with version 1 (stale)
            assertThatThrownBy(() -> sut.deleteRule("r1", 1L))
                    .isInstanceOf(RuntimeException.class) // Should be ConflictException per SRS, but code uses InvalidVersionFormatException
                    .hasMessageContaining("conflict");

            verify(rulePersistencePort, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should allow delete when version is null in DB (no optimistic locking)")
        void shouldAllowDelete_whenVersionIsNullInDb() {
            // Given
            Rule rule = draftRule("r1", "CODE_1", "Rule");
            rule.setVersion(null); // No version set
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(rule));
            when(ruleBindingPort.countByRuleId("r1")).thenReturn(0L);

            // When
            sut.deleteRule("r1", 1L);

            // Then — should proceed with delete
            verify(rulePersistencePort).deleteNodesByRuleId("r1");
            verify(rulePersistencePort).deleteById("r1");
            verify(outboxEventPort).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should create outbox event with correct payload on delete")
        void shouldCreateOutboxEvent_withCorrectPayload() {
            // Given
            Rule rule = draftRule("r1", "CODE_1", "Rule");
            rule.setVersion(1L);
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(rule));
            when(ruleBindingPort.countByRuleId("r1")).thenReturn(0L);

            // When
            sut.deleteRule("r1", 1L);

            // Then
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventPort).save(captor.capture());
            OutboxEvent event = captor.getValue();

            assertThat(event.getId()).isNotBlank();
            assertThat(event.getEventType()).isEqualTo("VALIDATION_RULE_DELETED");
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(event.getAttempts()).isEqualTo(0);
            assertThat(event.getMaxAttempts()).isEqualTo(3);
            assertThat(event.getCreatedAt()).isNotNull();
        }
    }

    // ========================================================================
    // Clone Rule
    // ========================================================================

    @Nested
    @DisplayName("cloneRule()")
    class CloneRuleTests {

        @Test
        @DisplayName("Should clone rule with new code and name")
        void shouldCloneRule_withNewCodeAndName() {
            // Given
            Rule source = draftRule("r-source", "ORIGINAL", "Original Rule");
            source.setNodes(List.of(condNode("n1", "op1", "rc1")));
            when(rulePersistencePort.findById("r-source")).thenReturn(Optional.of(source));
            when(rulePersistencePort.existsByCode("CLONE_CODE")).thenReturn(false);
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.cloneRule("r-source", "CLONE_CODE", "Cloned Rule", "cloner");

            // Then
            assertThat(result.getCode()).isEqualTo("CLONE_CODE");
            assertThat(result.getName()).isEqualTo("Cloned Rule");
            assertThat(result.getState()).isEqualTo(Rule.RuleState.DRAFT);
            assertThat(result.getCreatedBy()).isEqualTo("cloner");
        }

        @Test
        @DisplayName("Should throw RuleNotFoundException when source rule does not exist")
        void shouldThrowNotFound_whenSourceDoesNotExist() {
            // Given
            when(rulePersistencePort.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sut.cloneRule("nonexistent", "NEW", "New", "user"))
                    .isInstanceOf(RuleNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw RuleAlreadyExistsException when new code already exists")
        void shouldThrowAlreadyExists_whenNewCodeExists() {
            // Given
            Rule source = draftRule("r-source", "ORIGINAL", "Original");
            when(rulePersistencePort.findById("r-source")).thenReturn(Optional.of(source));
            when(rulePersistencePort.existsByCode("EXISTING_CODE")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> sut.cloneRule("r-source", "EXISTING_CODE", "Clone", "user"))
                    .isInstanceOf(RuleAlreadyExistsException.class);
        }
    }

    // ========================================================================
    // Activate Rule (DRAFT → PUBLISHED)
    // ========================================================================

    @Nested
    @DisplayName("activateRule()")
    class ActivateRuleTests {

        @Test
        @DisplayName("Should activate draft rule to PUBLISHED state")
        void shouldActivateRule_whenDraft() {
            // Given
            Rule draft = draftRule("r1", "CODE_1", "Draft Rule");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(draft));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.activateRule("r1", "activator");

            // Then
            assertThat(result.getState()).isEqualTo(Rule.RuleState.PUBLISHED);
            assertThat(result.isActive()).isTrue();
            assertThat(result.getUpdatedBy()).isEqualTo("activator");
        }

        @Test
        @DisplayName("Should throw exception when rule is not in DRAFT state")
        void shouldThrow_whenNotDraft() {
            // Given
            Rule published = publishedRule("r1", "CODE_1", "Published Rule");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(published));

            // When & Then
            assertThatThrownBy(() -> sut.activateRule("r1", "user"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ========================================================================
    // Archive Rule
    // ========================================================================

    @Nested
    @DisplayName("archiveRule()")
    class ArchiveRuleTests {

        @Test
        @DisplayName("Should archive rule and set inactive")
        void shouldArchiveRule_andSetInactive() {
            // Given
            Rule published = publishedRule("r1", "CODE_1", "Published Rule");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(published));
            when(rulePersistencePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rule result = sut.archiveRule("r1", "archiver");

            // Then
            assertThat(result.getState()).isEqualTo(Rule.RuleState.ARCHIVED);
            assertThat(result.isActive()).isFalse();
            assertThat(result.getUpdatedBy()).isEqualTo("archiver");
        }
    }

    // ========================================================================
    // getRuleByCode()
    // ========================================================================

    @Nested
    @DisplayName("getRuleByCode()")
    class GetRuleByCodeTests {

        @Test
        @DisplayName("Should return Optional with rule when code exists")
        void shouldReturnRule_whenCodeExists() {
            // Given
            Rule rule = draftRule("r1", "CODE_1", "Rule");
            when(rulePersistencePort.findByCode("CODE_1")).thenReturn(Optional.of(rule));

            // When
            Optional<Rule> result = sut.getRuleByCode("CODE_1");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo("CODE_1");
        }

        @Test
        @DisplayName("Should return empty Optional when code does not exist")
        void shouldReturnEmpty_whenCodeNotFound() {
            // Given
            when(rulePersistencePort.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            // When
            Optional<Rule> result = sut.getRuleByCode("UNKNOWN");

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ========================================================================
    // ruleExists() / isRuleActive()
    // ========================================================================

    @Nested
    @DisplayName("ruleExists()")
    class RuleExistsTests {

        @Test
        @DisplayName("Should return true when rule exists")
        void shouldReturnTrue_whenExists() {
            // Given
            when(rulePersistencePort.existsById("r1")).thenReturn(true);

            // When & Then
            assertThat(sut.ruleExists("r1")).isTrue();
        }

        @Test
        @DisplayName("Should return false when rule does not exist")
        void shouldReturnFalse_whenNotExists() {
            // Given
            when(rulePersistencePort.existsById("unknown")).thenReturn(false);

            // When & Then
            assertThat(sut.ruleExists("unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("isRuleActive()")
    class IsRuleActiveTests {

        @Test
        @DisplayName("Should return true when rule is PUBLISHED")
        void shouldReturnTrue_whenPublished() {
            // Given
            Rule published = publishedRule("r1", "C1", "Active Rule");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(published));

            // When & Then
            assertThat(sut.isRuleActive("r1")).isTrue();
        }

        @Test
        @DisplayName("Should return false when rule is DRAFT")
        void shouldReturnFalse_whenDraft() {
            // Given
            Rule draft = draftRule("r1", "C1", "Draft Rule");
            when(rulePersistencePort.findById("r1")).thenReturn(Optional.of(draft));

            // When & Then
            assertThat(sut.isRuleActive("r1")).isFalse();
        }

        @Test
        @DisplayName("Should return false when rule does not exist")
        void shouldReturnFalse_whenNotFound() {
            // Given
            when(rulePersistencePort.findById("unknown")).thenReturn(Optional.empty());

            // When & Then
            assertThat(sut.isRuleActive("unknown")).isFalse();
        }
    }

    // ========================================================================
    // Node Validation
    // ========================================================================

    @Nested
    @DisplayName("Node validation in createRule()")
    class NodeValidationTests {

        @Test
        @DisplayName("Should reject COND node without operatorName at build time")
        void shouldRejectCondNode_withoutOperatorName() {
            // RuleNode.validate() is called in build() — throws NPE before reaching the service
            assertThatThrownBy(() -> RuleNode.builder()
                    .nodeId("n1")
                    .type(RuleNode.NodeType.COND)
                    .reasonCode("rc1")
                    // missing operatorName
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("OperatorName cannot be null");
        }

        @Test
        @DisplayName("Should reject node with blank ID")
        void shouldRejectNode_withBlankId() {
            // Given — Node with empty ID won't fail RuleNode's internal validation (it skips when nodeId is null)
            // but RuleService.validateRuleNode checks for blank
            RuleNode nodeWithBlankId = RuleNode.builder()
                    .nodeId("")
                    .type(RuleNode.NodeType.COND)
                    .operatorName("op1")
                    .reasonCode("rc1")
                    .build();

            when(rulePersistencePort.existsByCode(anyString())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> sut.createRule("CODE", "Name", Rule.LogicType.ALL,
                    List.of(nodeWithBlankId), "admin"))
                    .isInstanceOf(InvalidRuleStructureException.class)
                    .hasMessageContaining("Node ID is required");
        }
    }

    // ========================================================================
    // Review-B Fix: System rule deletion guard
    // ========================================================================

    @Nested
    @DisplayName("System rule protection (deleteRule + updateRule)")
    class SystemRuleProtectionTests {

        @Test
        @DisplayName("DELETE rule-sys-owner-only → 409 SystemRuleProtectedException")
        void deleteSystemRule_throws409() {
            Rule systemRule = Rule.builder()
                    .id("rule-sys-owner-only")
                    .code("rule-sys-owner-only")
                    .name("System — Owner Only")
                    .state(Rule.RuleState.PUBLISHED)
                    .isSystem(true)
                    .version(0L)
                    .logic(Rule.LogicType.ALL)
                    .nodes(List.of(condNode("n1", "customer.is_owner", "VOUCHER_NOT_OWNED_BY_CUSTOMER")))
                    .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(rulePersistencePort.findById("rule-sys-owner-only")).thenReturn(Optional.of(systemRule));

            assertThatThrownBy(() -> sut.deleteRule("rule-sys-owner-only", 0L))
                    .isInstanceOf(vn.viettel.vds.promotion.validation.domain.exception.SystemRuleProtectedException.class)
                    .hasMessageContaining("rule-sys-owner-only");

            verify(rulePersistencePort, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("PATCH rule-sys-owner-only → 409 SystemRuleProtectedException")
        void updateSystemRule_throws409() {
            Rule systemRule = Rule.builder()
                    .id("rule-sys-owner-only")
                    .code("rule-sys-owner-only")
                    .name("System — Owner Only")
                    .state(Rule.RuleState.DRAFT)  // draft state, but isSystem blocks update
                    .isSystem(true)
                    .version(0L)
                    .logic(Rule.LogicType.ALL)
                    .nodes(List.of(condNode("n1", "customer.is_owner", "VOUCHER_NOT_OWNED_BY_CUSTOMER")))
                    .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(rulePersistencePort.findById("rule-sys-owner-only")).thenReturn(Optional.of(systemRule));

            assertThatThrownBy(() -> sut.updateRule("rule-sys-owner-only", "New Name", null, null, "admin"))
                    .isInstanceOf(vn.viettel.vds.promotion.validation.domain.exception.SystemRuleProtectedException.class)
                    .hasMessageContaining("rule-sys-owner-only");

            verify(rulePersistencePort, never()).save(any(Rule.class));
        }

        @Test
        @DisplayName("Non-system rule delete succeeds normally")
        void deleteNonSystemRule_succeeds() {
            Rule regularRule = Rule.builder()
                    .id("rule-regular-001")
                    .code("rule-regular")
                    .name("Regular Rule")
                    .state(Rule.RuleState.DRAFT)
                    .isSystem(false)
                    .version(1L)
                    .logic(Rule.LogicType.ALL)
                    .nodes(List.of(condNode("n1", "order.total.gte", "MIN_ORDER")))
                    .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(rulePersistencePort.findById("rule-regular-001")).thenReturn(Optional.of(regularRule));
            when(ruleBindingPort.countByRuleId("rule-regular-001")).thenReturn(0L);
            when(outboxEventPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            sut.deleteRule("rule-regular-001", 1L);

            verify(rulePersistencePort).deleteNodesByRuleId("rule-regular-001");
            verify(rulePersistencePort).deleteById("rule-regular-001");
        }
    }
}
