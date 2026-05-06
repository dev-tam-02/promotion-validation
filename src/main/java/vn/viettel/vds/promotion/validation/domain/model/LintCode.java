package vn.viettel.vds.promotion.validation.domain.model;

/**
 * Lint issue codes emitted by {@link vn.viettel.vds.promotion.validation.application.service.RuleLinter}.
 */
public enum LintCode {

    /**
     * Same COND node (operator + params) appears more than once in the same GROUP.
     */
    REDUNDANCY,

    /**
     * Identical COND nodes inside an AND (ALL) group — logically equivalent to a single condition.
     */
    TAUTOLOGY,

    /**
     * Mutually-exclusive numeric constraints in the same AND group (e.g. gte 500 AND lte 100).
     */
    CONTRADICTION,

    /**
     * Dead branch — a GROUP that can be simplified (e.g. OR group with a single child).
     */
    UNREACHABLE
}
