package vn.viettel.vds.promotion.validation.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;
import vn.viettel.vds.promotion.validation.domain.model.LogicType;
import vn.viettel.vds.promotion.validation.domain.model.RuleAggregate;
import vn.viettel.vds.promotion.validation.domain.model.RuleStatus;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleCode;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleName;
import vn.viettel.vds.promotion.validation.domain.valueobject.Version;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RuleVersioningDomainService Tests")
class RuleVersioningDomainServiceTest {

    private RuleVersioningDomainService sut;

    @BeforeEach
    void setUp() {
        sut = new RuleVersioningDomainService();
    }

    @Nested
    @DisplayName("createNewVersion()")
    class CreateNewVersionTests {

        @Test
        @DisplayName("Should create MAJOR version from published rule")
        void shouldCreateMajorVersion() {
            var existingRule = TestFixtures.publishedRule("RULE_VER", "Version Rule");

            var newRule = sut.createNewVersion(existingRule, RuleVersioningDomainService.VersionType.MAJOR, "creator");

            assertThat(newRule.getStatus()).isEqualTo(RuleStatus.DRAFT);
            assertThat(newRule.getCode()).isEqualTo(existingRule.getCode());
            assertThat(newRule.getVersion().getMajor()).isEqualTo(existingRule.getVersion().getMajor() + 1);
            assertThat(newRule.getCreatedBy()).isEqualTo("creator");
        }

        @Test
        @DisplayName("Should create MINOR version from published rule")
        void shouldCreateMinorVersion() {
            var existingRule = TestFixtures.publishedRule("RULE_VER", "Version Rule");

            var newRule = sut.createNewVersion(existingRule, RuleVersioningDomainService.VersionType.MINOR, "creator");

            assertThat(newRule.getVersion().getMinor()).isEqualTo(existingRule.getVersion().getMinor() + 1);
        }

        @Test
        @DisplayName("Should create PATCH version from published rule")
        void shouldCreatePatchVersion() {
            var existingRule = TestFixtures.publishedRule("RULE_VER", "Version Rule");

            var newRule = sut.createNewVersion(existingRule, RuleVersioningDomainService.VersionType.PATCH, "creator");

            assertThat(newRule.getVersion().getPatch()).isEqualTo(existingRule.getVersion().getPatch() + 1);
        }

        @Test
        @DisplayName("Should throw when existing rule is null")
        void shouldThrow_whenExistingRuleNull() {
            assertThatThrownBy(() -> sut.createNewVersion(null, RuleVersioningDomainService.VersionType.MAJOR, "creator"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when version type is null")
        void shouldThrow_whenVersionTypeNull() {
            var rule = TestFixtures.publishedRule("RULE_VER", "Rule");

            assertThatThrownBy(() -> sut.createNewVersion(rule, null, "creator"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when createdBy is null")
        void shouldThrow_whenCreatedByNull() {
            var rule = TestFixtures.publishedRule("RULE_VER", "Rule");

            assertThatThrownBy(() -> sut.createNewVersion(rule, RuleVersioningDomainService.VersionType.MAJOR, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when existing rule is in DRAFT status")
        void shouldThrow_whenDraftStatus() {
            var rule = TestFixtures.draftRule("RULE_VER", "Draft Rule");

            assertThatThrownBy(() -> sut.createNewVersion(rule, RuleVersioningDomainService.VersionType.MAJOR, "creator"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should allow creating version from DEPRECATED rule")
        void shouldAllowFromDeprecated() {
            var rule = TestFixtures.ruleWithStatus("RULE_VER", "Deprecated Rule", RuleStatus.DEPRECATED);

            var newRule = sut.createNewVersion(rule, RuleVersioningDomainService.VersionType.MINOR, "creator");

            assertThat(newRule.getStatus()).isEqualTo(RuleStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("canReplace()")
    class CanReplaceTests {

        @Test
        @DisplayName("Should return true when new rule has same code and newer version")
        void shouldReturnTrue_whenValidReplacement() {
            var existingRule = TestFixtures.publishedRule("RULE_REP", "Existing");
            var newRule = TestFixtures.ruleWithStatus("RULE_REP", "New", RuleStatus.DRAFT);
            // Set newer version using builder
            var newerRule = RuleAggregate.builder()
                    .id(newRule.getId())
                    .code(newRule.getCode())
                    .name(newRule.getName())
                    .logicType(LogicType.AND)
                    .status(RuleStatus.DRAFT)
                    .version(Version.of(2, 0, 0))
                    .createdBy("user")
                    .build();

            assertThat(sut.canReplace(newerRule, existingRule)).isTrue();
        }

        @Test
        @DisplayName("Should return false when codes differ")
        void shouldReturnFalse_whenCodesDiffer() {
            var existing = TestFixtures.publishedRule("RULE_A", "Rule A");
            var newRule = TestFixtures.ruleWithStatus("RULE_B", "Rule B", RuleStatus.DRAFT);

            assertThat(sut.canReplace(newRule, existing)).isFalse();
        }

        @Test
        @DisplayName("Should return false when new version is not newer")
        void shouldReturnFalse_whenVersionNotNewer() {
            var existing = TestFixtures.publishedRule("RULE_X", "Existing");
            var newRule = TestFixtures.ruleWithStatus("RULE_X", "New", RuleStatus.DRAFT);

            assertThat(sut.canReplace(newRule, existing)).isFalse();
        }

        @Test
        @DisplayName("Should return false when existing rule is in DRAFT status")
        void shouldReturnFalse_whenExistingIsDraft() {
            var existing = TestFixtures.draftRule("RULE_X", "Draft");
            var newRule = RuleAggregate.builder()
                    .id(existing.getId())
                    .code(existing.getCode())
                    .name(existing.getName())
                    .logicType(LogicType.AND)
                    .status(RuleStatus.DRAFT)
                    .version(Version.of(2, 0, 0))
                    .createdBy("user")
                    .build();

            assertThat(sut.canReplace(newRule, existing)).isFalse();
        }
    }

    @Nested
    @DisplayName("mergeChanges()")
    class MergeChangesTests {

        @Test
        @DisplayName("Should overwrite target with source content")
        void shouldOverwrite() {
            var target = TestFixtures.draftRule("RULE_TGT", "Target Rule");
            var source = TestFixtures.draftRule("RULE_SRC", "Source Rule");

            var result = sut.mergeChanges(target, source, RuleVersioningDomainService.MergeStrategy.OVERWRITE);

            assertThat(result.getName()).isEqualTo(source.getName());
        }

        @Test
        @DisplayName("Should throw when target cannot be modified")
        void shouldThrow_whenTargetNotModifiable() {
            var target = TestFixtures.publishedRule("RULE_PUB", "Published");
            var source = TestFixtures.draftRule("RULE_SRC", "Source");

            assertThatThrownBy(() -> sut.mergeChanges(target, source, RuleVersioningDomainService.MergeStrategy.OVERWRITE))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should throw when target is null")
        void shouldThrow_whenTargetNull() {
            var source = TestFixtures.draftRule("RULE_SRC", "Source");

            assertThatThrownBy(() -> sut.mergeChanges(null, source, RuleVersioningDomainService.MergeStrategy.OVERWRITE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when source is null")
        void shouldThrow_whenSourceNull() {
            var target = TestFixtures.draftRule("RULE_TGT", "Target");

            assertThatThrownBy(() -> sut.mergeChanges(target, null, RuleVersioningDomainService.MergeStrategy.OVERWRITE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when strategy is null")
        void shouldThrow_whenStrategyNull() {
            var target = TestFixtures.draftRule("RULE_TGT", "Target");
            var source = TestFixtures.draftRule("RULE_SRC", "Source");

            assertThatThrownBy(() -> sut.mergeChanges(target, source, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("compareRules()")
    class CompareRulesTests {

        @Test
        @DisplayName("Should detect no differences for identical rules")
        void shouldDetectNoDifferences() {
            var rule1 = TestFixtures.draftRule("RULE_CMP", "Compare Rule");
            var rule2 = TestFixtures.draftRule("RULE_CMP", "Compare Rule");

            var diff = sut.compareRules(rule1, rule2);

            // Both have same name but different versions (new IDs)
            assertThat(diff.hasDifferences()).isFalse();
        }

        @Test
        @DisplayName("Should detect name differences")
        void shouldDetectNameDifferences() {
            var rule1 = TestFixtures.draftRule("RULE_CMP", "Name A");
            var rule2 = TestFixtures.draftRule("RULE_CMP", "Name B");

            var diff = sut.compareRules(rule1, rule2);

            assertThat(diff.hasDifferences()).isTrue();
            assertThat(diff.getDifferences()).containsKey("name");
        }

        @Test
        @DisplayName("Should detect description differences")
        void shouldDetectDescriptionDifferences() {
            var rule1 = TestFixtures.draftRule("RULE_CMP", "Same Name");
            var rule2 = TestFixtures.draftRule("RULE_CMP", "Same Name");
            rule1.update(rule1.getName(), "Desc A", rule1.getLogicType(), null, "user");
            rule2.update(rule2.getName(), "Desc B", rule2.getLogicType(), null, "user");

            var diff = sut.compareRules(rule1, rule2);

            assertThat(diff.getDifferences()).containsKey("description");
        }
    }

    @Nested
    @DisplayName("getVersionHistory()")
    class GetVersionHistoryTests {

        @Test
        @DisplayName("Should return rules matching code sorted by version desc")
        void shouldReturnSortedVersionHistory() {
            var rule1 = TestFixtures.draftRule("RULE_HIST", "Rule v1");
            var rule2 = TestFixtures.draftRule("RULE_HIST", "Rule v2");
            var other = TestFixtures.draftRule("RULE_OTHER", "Other Rule");

            var history = sut.getVersionHistory(List.of(rule1, rule2, other), RuleCode.of("RULE_HIST"));

            assertThat(history).hasSize(2);
            assertThat(history).allMatch(r -> r.getCode().equals(RuleCode.of("RULE_HIST")));
        }

        @Test
        @DisplayName("Should return empty list when no matching code")
        void shouldReturnEmpty_whenNoMatch() {
            var rule = TestFixtures.draftRule("RULE_A", "Rule A");

            var history = sut.getVersionHistory(List.of(rule), RuleCode.of("RULE_B"));

            assertThat(history).isEmpty();
        }
    }

    @Nested
    @DisplayName("findLatestVersion()")
    class FindLatestVersionTests {

        @Test
        @DisplayName("Should find latest version for matching code")
        void shouldFindLatest() {
            var rule1 = TestFixtures.draftRule("RULE_LATEST", "Rule v1");
            var rule2 = TestFixtures.publishedRule("RULE_LATEST", "Rule v2");

            var latest = sut.findLatestVersion(List.of(rule1, rule2), RuleCode.of("RULE_LATEST"));

            assertThat(latest).isPresent();
            assertThat(latest.get().getVersion().isNewerThan(rule1.getVersion())).isTrue();
        }

        @Test
        @DisplayName("Should return empty when no matching code")
        void shouldReturnEmpty_whenNoMatch() {
            var rule = TestFixtures.draftRule("RULE_A", "Rule A");

            var latest = sut.findLatestVersion(List.of(rule), RuleCode.of("RULE_B"));

            assertThat(latest).isEmpty();
        }
    }
}
