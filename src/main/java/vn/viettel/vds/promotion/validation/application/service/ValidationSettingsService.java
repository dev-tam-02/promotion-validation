package vn.viettel.vds.promotion.validation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidateValidationSettingRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResult;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Service for validating validation settings.
 * Handles validation of validation rule configurations and timeframe settings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationSettingsService {

    private final RuleService ruleService;

    /**
     * Validate validation settings including timeframe and validation rule.
     *
     * @param request Validation setting request
     * @return ValidationResult with validation outcome
     */
    public ValidationResult validateValidationSetting(ValidateValidationSettingRequest request) {
        log.debug("Validating validation setting for ruleId: {}, ruleType: {}",
                request.validationRuleId(), request.ruleType());

        try {
            // 1. Validate validation rule if provided
            if (request.validationRuleId() != null) {
                ValidationResult ruleValidation = validateRule(request.validationRuleId(), request.ruleType());
                if (!ruleValidation.isValid()) {
                    return ruleValidation;
                }
            }

            // 2. Validate timeframe configuration if provided
            if (request.timeframe() != null) {
                ValidationResult timeframeValidation = validateTimeframe(request.timeframe());
                if (!timeframeValidation.isValid()) {
                    return timeframeValidation;
                }
            }

            // 3. Validate validity days of week if provided
            if (request.validityDaysOfWeek() != null) {
                ValidationResult daysValidation = validateValidityDaysOfWeek(request.validityDaysOfWeek());
                if (!daysValidation.isValid()) {
                    return daysValidation;
                }
            }

            // 4. Validate validity hours per day if provided
            if (request.validityHoursPerDay() != null) {
                ValidationResult hoursValidation = validateValidityHoursPerDay(request.validityHoursPerDay());
                if (!hoursValidation.isValid()) {
                    return hoursValidation;
                }
            }

            log.debug("Validation setting validation successful for ruleId: {}", request.validationRuleId());
            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Validation setting validation failed for ruleId: {}", request.validationRuleId(), e);
            return ValidationResult.failure(
                "VALIDATION_SETTING_PROCESSING_ERROR",
                "Failed to process validation setting: " + e.getMessage()
            );
        }
    }

    private ValidationResult validateRule(String validationRuleId, String ruleType) {
        try {
            // Check if rule exists and is active
            if (!ruleService.ruleExists(validationRuleId)) {
                return ValidationResult.failure(
                    "VALIDATION_RULE_NOT_FOUND",
                    "Validation rule not found: " + validationRuleId
                );
            }

            if (!ruleService.isRuleActive(validationRuleId)) {
                return ValidationResult.failure(
                    "VALIDATION_RULE_INACTIVE",
                    "Validation rule is inactive: " + validationRuleId
                );
            }

            // Validate rule type compatibility
            if (ruleType != null && !isRuleTypeCompatible(validationRuleId, ruleType)) {
                return ValidationResult.failure(
                    "VALIDATION_RULE_INVALID_TYPE",
                    "Rule type mismatch for rule: " + validationRuleId + ", expected: " + ruleType
                );
            }

            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Rule validation failed for ruleId: {}", validationRuleId, e);
            return ValidationResult.failure(
                "VALIDATION_RULE_ERROR",
                "Rule validation error: " + e.getMessage()
            );
        }
    }

    private ValidationResult validateTimeframe(Object timeframe) {
        try {
            // Parse timeframe object
            if (!(timeframe instanceof Map)) {
                return ValidationResult.failure(
                    "TIMEFRAME_INVALID_FORMAT",
                    "Timeframe must be an object"
                );
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> timeframeMap = (Map<String, Object>) timeframe;

            // Validate required timeframe fields
            Object validityTimeframe = timeframeMap.get("validityTimeframe");
            if (validityTimeframe instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> validityMap = (Map<String, Object>) validityTimeframe;

                // Validate start and end dates
                String startDate = (String) validityMap.get("startDate");
                String expirationDate = (String) validityMap.get("expirationDate");

                if (startDate != null && expirationDate != null) {
                    ValidationResult dateValidation = validateDateRange(startDate, expirationDate);
                    if (!dateValidation.isValid()) {
                        return dateValidation;
                    }
                }

                // Validate duration and interval
                String duration = (String) validityMap.get("duration");
                String interval = (String) validityMap.get("interval");

                if (duration != null && !isValidDuration(duration)) {
                    return ValidationResult.failure(
                        "TIMEFRAME_INVALID_DURATION",
                        "Invalid duration format: " + duration
                    );
                }

                if (interval != null && !isValidInterval(interval)) {
                    return ValidationResult.failure(
                        "TIMEFRAME_INVALID_INTERVAL",
                        "Invalid interval format: " + interval
                    );
                }
            }

            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Timeframe validation failed", e);
            return ValidationResult.failure(
                "TIMEFRAME_VALIDATION_ERROR",
                "Timeframe validation error: " + e.getMessage()
            );
        }
    }

    private ValidationResult validateDateRange(String startDate, String expirationDate) {
        try {
            OffsetDateTime start = OffsetDateTime.parse(startDate);
            OffsetDateTime end = OffsetDateTime.parse(expirationDate);
            OffsetDateTime now = OffsetDateTime.now();

            if (start.isAfter(end)) {
                return ValidationResult.failure(
                    "TIMEFRAME_END_DATE_BEFORE_START",
                    "End date must be after start date"
                );
            }

            if (start.isBefore(now)) {
                return ValidationResult.failure(
                    "TIMEFRAME_START_DATE_IN_PAST",
                    "Start date cannot be in the past"
                );
            }

            return ValidationResult.success();

        } catch (Exception e) {
            return ValidationResult.failure(
                "TIMEFRAME_INVALID_DATE_RANGE",
                "Invalid date format or range: " + e.getMessage()
            );
        }
    }

    private ValidationResult validateValidityDaysOfWeek(java.util.List<Integer> validityDaysOfWeek) {
        for (Integer day : validityDaysOfWeek) {
            if (day < 1 || day > 7) {
                return ValidationResult.failure(
                    "TIMEFRAME_INVALID_DAY_OF_WEEK",
                    "Day of week must be between 1 and 7, got: " + day
                );
            }
        }
        return ValidationResult.success();
    }

    private ValidationResult validateValidityHoursPerDay(java.util.List<Object> validityHoursPerDay) {
        for (Object hourObj : validityHoursPerDay) {
            if (hourObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> hourMap = (Map<String, Object>) hourObj;

                String dayOfWeek = (String) hourMap.get("dayOfWeek");
                String startTime = (String) hourMap.get("startTime");
                String expirationTime = (String) hourMap.get("expirationTime");

                if (dayOfWeek == null || startTime == null || expirationTime == null) {
                    return ValidationResult.failure(
                        "TIMEFRAME_INVALID_HOURS",
                        "Missing required fields in validity hours"
                    );
                }

                // Basic time format validation
                if (!isValidTimeFormat(startTime) || !isValidTimeFormat(expirationTime)) {
                    return ValidationResult.failure(
                        "TIMEFRAME_INVALID_HOURS",
                        "Invalid time format in validity hours"
                    );
                }
            }
        }
        return ValidationResult.success();
    }

    private boolean isRuleTypeCompatible(String ruleId, String ruleType) {
        // Basic rule type validation - could be enhanced with actual rule metadata lookup
        return ruleType.matches("^[A-Z_]+$");
    }

    private boolean isValidDuration(String duration) {
        // Validate ISO 8601 duration format (e.g., PT8H, P1D)
        return duration.matches("^P(\\d+Y)?(\\d+M)?(\\d+D)?(T(\\d+H)?(\\d+M)?(\\d+S)?)?$");
    }

    private boolean isValidInterval(String interval) {
        // Validate ISO 8601 duration format for intervals
        return interval.matches("^P(\\d+Y)?(\\d+M)?(\\d+D)?(T(\\d+H)?(\\d+M)?(\\d+S)?)?$");
    }

    private boolean isValidTimeFormat(String time) {
        // Validate time format (e.g., "09:00+07:00")
        return time.matches("^\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}$");
    }
}