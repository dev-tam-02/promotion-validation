package vn.viettel.vds.promotion.validation.domain.model;

/**
 * A single lint finding produced by {@link vn.viettel.vds.promotion.validation.application.service.RuleLinter}.
 *
 * @param severity "WARNING" or "ERROR"
 * @param code     machine-readable {@link LintCode}
 * @param message  human-readable description for admin UI
 * @param nodeId   id of the offending node (may be null when the issue spans multiple nodes)
 */
public record LintIssue(String severity, String code, String message, String nodeId) {

    public static LintIssue warning(LintCode code, String message, String nodeId) {
        return new LintIssue("WARNING", code.name(), message, nodeId);
    }

    public static LintIssue error(LintCode code, String message, String nodeId) {
        return new LintIssue("ERROR", code.name(), message, nodeId);
    }
}
