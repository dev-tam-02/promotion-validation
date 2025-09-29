package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidateValidationSettingRequest(
        String validationRuleId,
        String ruleType,
        Object timeframe,
        List<Integer> validityDaysOfWeek,
        List<Object> validityHoursPerDay
) {
}