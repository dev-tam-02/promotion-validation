package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LintIssue Tests")
class LintIssueTest {

    @Nested
    @DisplayName("warning()")
    class WarningFactoryTests {
        @Test
        @DisplayName("Should create warning issue with correct severity")
        void shouldCreateWarning() {
            LintIssue issue = LintIssue.warning(LintCode.REDUNDANCY, "duplicate condition", "node-1");
            assertThat(issue.severity()).isEqualTo("WARNING");
            assertThat(issue.code()).isEqualTo("REDUNDANCY");
            assertThat(issue.message()).isEqualTo("duplicate condition");
            assertThat(issue.nodeId()).isEqualTo("node-1");
        }
    }

    @Nested
    @DisplayName("error()")
    class ErrorFactoryTests {
        @Test
        @DisplayName("Should create error issue with correct severity")
        void shouldCreateError() {
            LintIssue issue = LintIssue.error(LintCode.CONTRADICTION, "conflict detected", "node-2");
            assertThat(issue.severity()).isEqualTo("ERROR");
            assertThat(issue.code()).isEqualTo("CONTRADICTION");
            assertThat(issue.message()).isEqualTo("conflict detected");
            assertThat(issue.nodeId()).isEqualTo("node-2");
        }
    }

    @Nested
    @DisplayName("Record equality")
    class EqualityTests {
        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenFieldsMatch() {
            LintIssue a = LintIssue.warning(LintCode.TAUTOLOGY, "msg", "n1");
            LintIssue b = LintIssue.warning(LintCode.TAUTOLOGY, "msg", "n1");
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("Should not be equal when severity differs")
        void shouldNotBeEqualWhenSeverityDiffers() {
            LintIssue warning = LintIssue.warning(LintCode.REDUNDANCY, "msg", "n1");
            LintIssue error = LintIssue.error(LintCode.REDUNDANCY, "msg", "n1");
            assertThat(warning).isNotEqualTo(error);
        }
    }
}
