package vn.viettel.vds.promotion.validation.application.service;

import vn.viettel.vds.promotion.validation.domain.model.TimeOfDayWindow;

import java.util.List;

public record TemporalPolicyRequest(
        String name,
        String timezone,
        String rrule,
        List<String> rdate,
        String exrule,
        List<String> exdate,
        List<TimeOfDayWindow> timeWindows
) {
}
