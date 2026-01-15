package vn.viettel.vds.promotion.validation.application.service;

import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.util.List;

/**
 * Request object for temporal policy operations.
 * <p>
 * Refactored to use RuleBinding.TimeWindow instead of TimeOfDayWindow.
 */
public record TemporalPolicyRequest(
        String name,
        String timezone,
        String rrule,
        List<String> rdate,
        String exrule,
        List<String> exdate,
        List<RuleBinding.TimeWindow> timeWindows
) {
}
