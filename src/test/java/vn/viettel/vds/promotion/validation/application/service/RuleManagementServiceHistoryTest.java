package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RuleHistoryPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleHistoryEntry;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying that {@link RuleManagementService} correctly records history entries
 * via {@link RuleHistoryPersistencePort} on create and update operations.
 *
 * <p>Note: DrlCompiler throws when the node list is empty. Tests that exercise the
 * createRule path with no nodes will catch the expected {@link DrlCompiler.DrlCompileException}.
 * History must still be recorded BEFORE compilation (pre-compile snapshot).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RuleManagementService — history recording (V2)")
class RuleManagementServiceHistoryTest {

    @Mock
    private RulePersistencePort rulePort;

    @Mock
    private OperatorPersistencePort operatorPort;

    @Mock
    private RuleEngineClient ruleEngineClient;

    @Mock
    private RuleHistoryPersistencePort historyPort;

    private RuleManagementService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        RuleTreeAssembler assembler = new RuleTreeAssembler();
        RuleValidator validator = new RuleValidator(objectMapper);
        DslGenerator dslGenerator = new DslGenerator(objectMapper);
        DrlCompiler drlCompiler = new DrlCompiler();

        service = new RuleManagementService(
                rulePort, assembler, validator, dslGenerator, drlCompiler,
                ruleEngineClient, operatorPort, Optional.of(historyPort));

        when(rulePort.save(any(Rule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(operatorPort.findGlobalOperatorsByStatus(any())).thenReturn(List.of());
    }

    // -----------------------------------------------------------------------
    // createRule tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createRule — history recording")
    class CreateRuleHistory {

        @Test
        @DisplayName("createRule: INSERT CREATE history entry with version=1, even when DRL compile fails")
        void createRule_insertsCreateHistoryEntry() {
            // DrlCompiler throws when node list is empty — wrap the call
            assertThatThrownBy(() ->
                    service.createRule("Test Rule", null, Rule.LogicType.ALL, List.of(), "admin"))
                    .isInstanceOf(DrlCompiler.DrlCompileException.class);

            // History is recorded BEFORE compilation; verify it was called
            ArgumentCaptor<RuleHistoryEntry> captor = ArgumentCaptor.forClass(RuleHistoryEntry.class);
            verify(historyPort, atLeastOnce()).save(captor.capture());

            RuleHistoryEntry saved = captor.getAllValues().stream()
                    .filter(e -> e.getChangeType() == RuleHistoryEntry.ChangeType.CREATE)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected CREATE history entry"));

            assertThat(saved.getChangeType()).isEqualTo(RuleHistoryEntry.ChangeType.CREATE);
            assertThat(saved.getRuleVersion()).isEqualTo(1L);
            assertThat(saved.getChangedBy()).isEqualTo("admin");
            assertThat(saved.getId()).isNotBlank();
        }

        @Test
        @DisplayName("createRule: history recording failure must not abort rule creation flow")
        void createRule_historyFailure_doesNotAbortCreation() {
            doThrow(new RuntimeException("DB failure")).when(historyPort).save(any());

            // DrlCompiler throws for empty nodes — that's OK for this test too;
            // the important assertion is that rulePort.save was called (rule was created)
            try {
                service.createRule("Test Rule", null, Rule.LogicType.ALL, List.of(), "admin");
            } catch (DrlCompiler.DrlCompileException ignored) {
                // expected — no nodes
            }

            // Rule was persisted (initial save happened before DRL step)
            verify(rulePort, atLeastOnce()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // updateRule tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("updateRule — history recording")
    class UpdateRuleHistory {

        private Rule existingRule;

        @BeforeEach
        void setupExistingRule() {
            existingRule = Rule.builder()
                    .id("rule-001")
                    .name("Original Name")
                    .logic(Rule.LogicType.ALL)
                    .state(Rule.RuleState.PUBLISHED)
                    .ruleVersion(1L)
                    .bundleHash("hash-v1")
                    .nodes(List.of())   // empty so compile path is skipped
                    .build();

            when(rulePort.findById("rule-001")).thenReturn(Optional.of(existingRule));
        }

        @Test
        @DisplayName("updateRule: INSERT UPDATE history entry capturing pre-update state (version=1)")
        void updateRule_insertsUpdateHistoryEntry_preUpdate() {
            // Pass nodes=null so existing nodes (empty) are used → compilePipelineAndSave skipped
            service.updateRule("rule-001", "Updated Name", null, null, "dev");

            ArgumentCaptor<RuleHistoryEntry> captor = ArgumentCaptor.forClass(RuleHistoryEntry.class);
            verify(historyPort, atLeastOnce()).save(captor.capture());

            RuleHistoryEntry updateEntry = captor.getAllValues().stream()
                    .filter(e -> e.getChangeType() == RuleHistoryEntry.ChangeType.UPDATE)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected UPDATE history entry"));

            // Should capture pre-update version (1)
            assertThat(updateEntry.getRuleVersion()).isEqualTo(1L);
            // bundleHash captured from before update
            assertThat(updateEntry.getBundleHash()).isEqualTo("hash-v1");
            assertThat(updateEntry.getChangedBy()).isEqualTo("dev");
        }

        @Test
        @DisplayName("updateRule: ruleVersion incremented to 2 after update")
        void updateRule_ruleVersionIncremented() {
            service.updateRule("rule-001", "Updated", null, null, "dev");

            ArgumentCaptor<Rule> ruleCaptor = ArgumentCaptor.forClass(Rule.class);
            verify(rulePort, atLeastOnce()).save(ruleCaptor.capture());

            boolean hasVersionTwo = ruleCaptor.getAllValues().stream()
                    .anyMatch(r -> r.getRuleVersion() != null && r.getRuleVersion() == 2L);
            assertThat(hasVersionTwo).as("ruleVersion should be incremented to 2 after update").isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // No history port (backward compat)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createRule without history port: no NPE, history silently skipped")
    void createRule_noHistoryPort_silentlySkipped() {
        ObjectMapper om = new ObjectMapper();
        RuleManagementService serviceNoHistory = new RuleManagementService(
                rulePort, new RuleTreeAssembler(), new RuleValidator(om),
                new DslGenerator(om), new DrlCompiler(),
                ruleEngineClient, operatorPort
                // no historyPort arg — uses Optional.empty()
        );

        // DrlCompiler throws for empty nodes — expected
        assertThatThrownBy(() ->
                serviceNoHistory.createRule("Test Rule", null, Rule.LogicType.ALL, List.of(), "admin"))
                .isInstanceOf(DrlCompiler.DrlCompileException.class);

        // historyPort mock never called
        verifyNoInteractions(historyPort);
    }
}
