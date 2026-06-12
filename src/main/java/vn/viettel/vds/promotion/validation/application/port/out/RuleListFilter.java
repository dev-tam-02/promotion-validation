package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.time.Instant;

/**
 * Filter criteria for the rules list endpoint.
 *
 * <p>Name/code matching is case-sensitive substring (TC VRUL001_133). Context is
 * an exact match. Created range filters on {@code createdAt}. Usage status filters
 * by whether a rule has any active binding (resolved in SQL by the persistence
 * adapter via an EXISTS subquery).
 */
public record RuleListFilter(
        Rule.RuleState state,
        String codePattern,
        String namePattern,
        String context,
        Instant createdFrom,
        Instant createdTo,
        UsageStatus usageStatus) {

    public enum UsageStatus {
        ASSIGNED,
        UNASSIGNED
    }
}
