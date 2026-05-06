package vn.viettel.vds.promotion.validation.domain.model;

import java.util.List;

/**
 * Aggregated result of a lint pass over a rule tree.
 *
 * @param warnings non-blocking issues the admin should review
 * @param errors   blocking issues that must be resolved before the rule can be saved
 */
public record LintReport(List<LintIssue> warnings, List<LintIssue> errors) {

    /**
     * Convenience factory for a clean (no-issue) report.
     */
    public static LintReport clean() {
        return new LintReport(List.of(), List.of());
    }

    /**
     * Returns true when there are errors that block saving.
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Returns true when there are warnings the admin should review.
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * Returns true when no issues were found.
     */
    public boolean isClean() {
        return !hasErrors() && !hasWarnings();
    }
}
