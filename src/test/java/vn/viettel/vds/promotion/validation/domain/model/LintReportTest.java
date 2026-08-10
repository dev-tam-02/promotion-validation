package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LintReport Tests")
class LintReportTest {

    @Nested
    @DisplayName("clean()")
    class CleanFactoryTests {
        @Test
        @DisplayName("Should create clean report with no issues")
        void shouldCreateCleanReport() {
            LintReport report = LintReport.clean();
            assertThat(report.warnings()).isEmpty();
            assertThat(report.errors()).isEmpty();
            assertThat(report.isClean()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasErrors()")
    class HasErrorsTests {
        @Test
        @DisplayName("Should return true when errors exist")
        void shouldReturnTrueWhenErrorsExist() {
            LintReport report = new LintReport(
                    List.of(),
                    List.of(LintIssue.error(LintCode.CONTRADICTION, "conflict", "node-1"))
            );
            assertThat(report.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("Should return false when errors are empty")
        void shouldReturnFalseWhenNoErrors() {
            LintReport report = new LintReport(List.of(), List.of());
            assertThat(report.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("Should return false when errors are null")
        void shouldReturnFalseWhenErrorsNull() {
            LintReport report = new LintReport(List.of(), null);
            assertThat(report.hasErrors()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasWarnings()")
    class HasWarningsTests {
        @Test
        @DisplayName("Should return true when warnings exist")
        void shouldReturnTrueWhenWarningsExist() {
            LintReport report = new LintReport(
                    List.of(LintIssue.warning(LintCode.REDUNDANCY, "duplicate", "node-1")),
                    List.of()
            );
            assertThat(report.hasWarnings()).isTrue();
        }

        @Test
        @DisplayName("Should return false when warnings are empty")
        void shouldReturnFalseWhenNoWarnings() {
            LintReport report = new LintReport(List.of(), List.of());
            assertThat(report.hasWarnings()).isFalse();
        }
    }

    @Nested
    @DisplayName("isClean()")
    class IsCleanTests {
        @Test
        @DisplayName("Should return false when both warnings and errors exist")
        void shouldReturnFalseWhenBothExist() {
            LintReport report = new LintReport(
                    List.of(LintIssue.warning(LintCode.REDUNDANCY, "warn", "n1")),
                    List.of(LintIssue.error(LintCode.CONTRADICTION, "err", "n2"))
            );
            assertThat(report.isClean()).isFalse();
        }

        @Test
        @DisplayName("Should return false when only warnings exist")
        void shouldReturnFalseWhenOnlyWarnings() {
            LintReport report = new LintReport(
                    List.of(LintIssue.warning(LintCode.TAUTOLOGY, "warn", "n1")),
                    List.of()
            );
            assertThat(report.isClean()).isFalse();
        }
    }
}
