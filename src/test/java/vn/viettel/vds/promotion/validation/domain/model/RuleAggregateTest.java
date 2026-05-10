package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleStateTransitionException;
import vn.viettel.vds.promotion.validation.domain.exception.RuleStateNotEditableException;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleCode;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleName;
import vn.viettel.vds.promotion.validation.domain.valueobject.Version;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RuleAggregate Tests")
class RuleAggregateTest {

    @Nested
    @DisplayName("Constructor (new rule)")
    class ConstructorTests {

        @Test
        @DisplayName("Should create rule with DRAFT status")
        void shouldCreateWithDraftStatus() {
            var rule = TestFixtures.draftRule("RULE_TEST", "Test Rule");

            assertThat(rule.getId()).isNotNull();
            assertThat(rule.getCode().getValue()).isEqualTo("RULE_TEST");
            assertThat(rule.getName().getValue()).isEqualTo("Test Rule");
            assertThat(rule.getStatus()).isEqualTo(RuleStatus.DRAFT);
            assertThat(rule.getVersion()).isEqualTo(Version.initial());
            assertThat(rule.getCreatedBy()).isEqualTo("test-user");
            assertThat(rule.getNodes()).isEmpty();
        }

        @Test
        @DisplayName("Should throw NullPointerException when code is null")
        void shouldThrow_whenCodeNull() {
            var name = RuleName.of("Name");

            assertThatThrownBy(() -> new RuleAggregate(null, name, LogicType.AND, "user"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw NullPointerException when name is null")
        void shouldThrow_whenNameNull() {
            var code = RuleCode.of("RULE_X");

            assertThatThrownBy(() -> new RuleAggregate(code, null, LogicType.AND, "user"))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Should update draft rule successfully")
        void shouldUpdateDraftRule() {
            var rule = TestFixtures.draftRule("RULE_TEST", "Old Name");
            var initialVersion = rule.getVersion();

            rule.update(RuleName.of("New Name"), "New desc", LogicType.OR, Collections.emptyList(), "updater");

            assertThat(rule.getName().getValue()).isEqualTo("New Name");
            assertThat(rule.getDescription()).isEqualTo("New desc");
            assertThat(rule.getLogicType()).isEqualTo(LogicType.OR);
            assertThat(rule.getUpdatedBy()).isEqualTo("updater");
            assertThat(rule.getVersion().isNewerThan(initialVersion)).isTrue();
        }

        @Test
        @DisplayName("Should throw RuleStateNotEditableException when published")
        void shouldThrow_whenPublished() {
            var rule = TestFixtures.ruleWithStatus("RULE_PUB", "Published", RuleStatus.PUBLISHED);
            var newName = RuleName.of("New Name");
            var emptyNodes = Collections.<RuleNode>emptyList();

            assertThatThrownBy(() -> rule.update(
                    newName, "desc", LogicType.AND, emptyNodes, "user"
            )).isInstanceOf(RuleStateNotEditableException.class);
        }

        @Test
        @DisplayName("Should increment patch version on update")
        void shouldIncrementPatchVersion() {
            var rule = TestFixtures.draftRule("RULE_V", "Version Test");
            var before = rule.getVersion();

            rule.update(RuleName.of("Updated"), null, LogicType.AND, null, "user");

            assertThat(rule.getVersion().getPatch()).isEqualTo(before.getPatch() + 1);
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("Should submit for review from DRAFT")
        void shouldSubmitForReview() {
            var rule = TestFixtures.draftRule("RULE_SUBMIT", "Submit Rule");

            rule.submitForReview("reviewer");

            assertThat(rule.getStatus()).isEqualTo(RuleStatus.PENDING_REVIEW);
            assertThat(rule.getUpdatedBy()).isEqualTo("reviewer");
        }

        @Test
        @DisplayName("Should throw when submit for review from non-DRAFT")
        void shouldThrow_whenSubmitFromNonDraft() {
            var rule = TestFixtures.ruleWithStatus("RULE_A", "Rule A", RuleStatus.APPROVED);

            assertThatThrownBy(() -> rule.submitForReview("user"))
                    .isInstanceOf(InvalidRuleStateTransitionException.class);
        }

        @Test
        @DisplayName("Should approve from PENDING_REVIEW")
        void shouldApprove() {
            var rule = TestFixtures.ruleWithStatus("RULE_APPROVE", "Approve", RuleStatus.PENDING_REVIEW);

            rule.approve("approver");

            assertThat(rule.getStatus()).isEqualTo(RuleStatus.APPROVED);
        }

        @Test
        @DisplayName("Should throw when approve from non-PENDING_REVIEW")
        void shouldThrow_whenApproveFromNonPending() {
            var rule = TestFixtures.draftRule("RULE_X", "Draft Rule");

            assertThatThrownBy(() -> rule.approve("user"))
                    .isInstanceOf(InvalidRuleStateTransitionException.class);
        }

        @Test
        @DisplayName("Should reject from PENDING_REVIEW back to DRAFT")
        void shouldReject() {
            var rule = TestFixtures.ruleWithStatus("RULE_REJECT", "Reject", RuleStatus.PENDING_REVIEW);

            rule.reject("reviewer");

            assertThat(rule.getStatus()).isEqualTo(RuleStatus.DRAFT);
        }

        @Test
        @DisplayName("Should publish from APPROVED")
        void shouldPublish() {
            var rule = TestFixtures.ruleWithStatus("RULE_PUB", "Publish", RuleStatus.APPROVED);
            var initialVersion = rule.getVersion();

            rule.publish("publisher");

            assertThat(rule.getStatus()).isEqualTo(RuleStatus.PUBLISHED);
            assertThat(rule.getPublishedBy()).isEqualTo("publisher");
            assertThat(rule.getPublishedAt()).isNotNull();
            assertThat(rule.getVersion().getMinor()).isEqualTo(initialVersion.getMinor() + 1);
        }

        @Test
        @DisplayName("Should throw when publish from non-APPROVED")
        void shouldThrow_whenPublishFromNonApproved() {
            var rule = TestFixtures.draftRule("RULE_Y", "Draft");

            assertThatThrownBy(() -> rule.publish("user"))
                    .isInstanceOf(InvalidRuleStateTransitionException.class);
        }

        @Test
        @DisplayName("Should deprecate from PUBLISHED")
        void shouldDeprecate() {
            var rule = TestFixtures.ruleWithStatus("RULE_DEP", "Deprecate", RuleStatus.PUBLISHED);

            rule.deprecate("admin");

            assertThat(rule.getStatus()).isEqualTo(RuleStatus.DEPRECATED);
        }

        @Test
        @DisplayName("Should archive from any non-ARCHIVED status")
        void shouldArchive() {
            var rule = TestFixtures.draftRule("RULE_ARC", "Archive");

            rule.archive("admin");

            assertThat(rule.getStatus()).isEqualTo(RuleStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Should throw when archive from ARCHIVED")
        void shouldThrow_whenArchiveFromArchived() {
            var rule = TestFixtures.ruleWithStatus("RULE_ARC2", "Archived", RuleStatus.ARCHIVED);

            assertThatThrownBy(() -> rule.archive("user"))
                    .isInstanceOf(InvalidRuleStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("canBeModified()")
    class CanBeModifiedTests {

        @Test
        @DisplayName("Should return true for DRAFT")
        void shouldReturnTrue_forDraft() {
            assertThat(TestFixtures.draftRule("RULE_MOD", "Mod Rule").canBeModified()).isTrue();
        }

        @Test
        @DisplayName("Should return false for PUBLISHED")
        void shouldReturnFalse_forPublished() {
            assertThat(TestFixtures.publishedRule("RULE_PUB", "Pub Rule").canBeModified()).isFalse();
        }
    }

    @Nested
    @DisplayName("isActive()")
    class IsActiveTests {

        @Test
        @DisplayName("Should return true for PUBLISHED")
        void shouldReturnTrue_forPublished() {
            assertThat(TestFixtures.publishedRule("RULE_ACT", "Active").isActive()).isTrue();
        }

        @Test
        @DisplayName("Should return false for DRAFT")
        void shouldReturnFalse_forDraft() {
            assertThat(TestFixtures.draftRule("RULE_INACT", "Inactive").isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("cloneAsDraft()")
    class CloneAsDraftTests {

        @Test
        @DisplayName("Should create new draft with COPY suffix in code")
        void shouldCloneAsDraft() {
            var original = TestFixtures.publishedRule("RULE_ORIG", "Original");

            var clone = original.cloneAsDraft("cloner");

            assertThat(clone.getId()).isNotEqualTo(original.getId());
            assertThat(clone.getCode().getValue()).isEqualTo("RULE_ORIG_COPY");
            assertThat(clone.getStatus()).isEqualTo(RuleStatus.DRAFT);
            assertThat(clone.getCreatedBy()).isEqualTo("cloner");
            assertThat(clone.getDescription()).contains("(Copy)");
        }
    }

    @Nested
    @DisplayName("getNodes()")
    class GetNodesTests {

        @Test
        @DisplayName("Should return unmodifiable list")
        void shouldReturnUnmodifiableList() {
            var rule = TestFixtures.draftRule("RULE_NODES", "Nodes Rule");
            var nodes = rule.getNodes();

            assertThatThrownBy(() -> nodes.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
