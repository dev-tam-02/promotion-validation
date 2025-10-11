package vn.viettel.vds.promotion.validation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidateObjectValidityRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResult;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Service for validating object validity.
 * Handles validation of objects (campaigns, promotions) against their validity timeframes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectValidityService {

    private final AssignmentService assignmentService;

    /**
     * Validate object validity within its timeframe.
     * Checks if an object is currently valid based on stored validation settings.
     *
     * @param request Object validity request
     * @return ValidationResult with validity status
     */
    public ValidationResult validateObjectValidity(ValidateObjectValidityRequest request) {
        log.debug("Validating object validity for objectType: {}, objectId: {}",
                request.objectType(), request.objectId());

        try {
            // 1. Check if object exists in our validation system
            if (!objectExists(request.objectType(), request.objectId())) {
                return ValidationResult.failure(
                        "OBJECT_NOT_FOUND",
                        String.format("Object not found: %s with ID %s", request.objectType(), request.objectId())
                );
            }

            // 2. Get validation settings for the object
            Map<String, Object> validationSettings = getObjectValidationSettings(
                    request.objectType(),
                    request.objectId()
            );

            if (validationSettings == null || validationSettings.isEmpty()) {
                // No validation settings configured - consider as valid
                log.debug("No validation settings found for object {}:{}, considering as valid",
                        request.objectType(), request.objectId());
                return ValidationResult.success();
            }

            // 3. Check if object is currently within its validity timeframe
            ValidationResult timeframeResult = validateTimeframeValidity(
                    validationSettings,
                    request.currentDateTime()
            );
            if (!timeframeResult.isValid()) {
                return timeframeResult;
            }

            // 4. Check if object is active/enabled
            ValidationResult statusResult = validateObjectStatus(
                    request.objectType(),
                    request.objectId()
            );
            if (!statusResult.isValid()) {
                return statusResult;
            }

            log.debug("Object validity validation successful for objectId: {}", request.objectId());
            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Object validity validation failed for objectId: {}", request.objectId(), e);
            return ValidationResult.failure(
                    "OBJECT_VALIDITY_PROCESSING_ERROR",
                    "Failed to process object validity check: " + e.getMessage()
            );
        }
    }

    private boolean objectExists(String objectType, String objectId) {
        try {
            // Check if object is registered in validation system
            // This could query a database table storing object registrations
            return assignmentService.hasValidationAssignment(objectType, objectId);
        } catch (Exception e) {
            log.error("Error checking object existence for {}:{}", objectType, objectId, e);
            return false;
        }
    }

    private Map<String, Object> getObjectValidationSettings(String objectType, String objectId) {
        try {
            // Retrieve validation settings for the object
            // This could include timeframe, rules, and other validation metadata
            return assignmentService.getValidationSettings(objectType, objectId);
        } catch (Exception e) {
            log.error("Error retrieving validation settings for {}:{}", objectType, objectId, e);
            return null;
        }
    }

    private ValidationResult validateTimeframeValidity(
            Map<String, Object> validationSettings,
            OffsetDateTime currentDateTime) {

        try {
            // Extract timeframe configuration from validation settings
            @SuppressWarnings("unchecked")
            Map<String, Object> timeframe = (Map<String, Object>) validationSettings.get("timeframe");

            if (timeframe == null) {
                // No timeframe restriction
                return ValidationResult.success();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> validityTimeframe = (Map<String, Object>) timeframe.get("validityTimeframe");

            if (validityTimeframe != null) {
                // Check if current time is within start and end dates
                String startDateStr = (String) validityTimeframe.get("startDate");
                String endDateStr = (String) validityTimeframe.get("expirationDate");

                if (startDateStr != null && endDateStr != null) {
                    OffsetDateTime startDate = OffsetDateTime.parse(startDateStr);
                    OffsetDateTime endDate = OffsetDateTime.parse(endDateStr);

                    if (currentDateTime.isBefore(startDate)) {
                        return ValidationResult.failure(
                                "OBJECT_NOT_STARTED",
                                "Object is not yet active. Start date: " + startDateStr
                        );
                    }

                    if (currentDateTime.isAfter(endDate)) {
                        return ValidationResult.failure(
                                "OBJECT_EXPIRED",
                                "Object has expired. End date: " + endDateStr
                        );
                    }
                }
            }

            // Check validity days of week
            @SuppressWarnings("unchecked")
            java.util.List<Integer> validityDaysOfWeek =
                    (java.util.List<Integer>) validationSettings.get("validityDaysOfWeek");

            if (validityDaysOfWeek != null && !validityDaysOfWeek.isEmpty()) {
                int currentDayOfWeek = currentDateTime.getDayOfWeek().getValue(); // 1-7 (Monday-Sunday)
                if (!validityDaysOfWeek.contains(currentDayOfWeek)) {
                    return ValidationResult.failure(
                            "OBJECT_INVALID_DAY",
                            String.format("Object is not valid on day %d. Valid days: %s",
                                    currentDayOfWeek, validityDaysOfWeek)
                    );
                }
            }

            // Check validity hours per day
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> validityHoursPerDay =
                    (java.util.List<Map<String, Object>>) validationSettings.get("validityHoursPerDay");

            if (validityHoursPerDay != null && !validityHoursPerDay.isEmpty()) {
                String currentDayName = currentDateTime.getDayOfWeek().name();

                ValidationResult hoursResult = validateCurrentTimeInValidHours(
                        validityHoursPerDay,
                        currentDayName,
                        currentDateTime
                );
                if (!hoursResult.isValid()) {
                    return hoursResult;
                }
            }

            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Timeframe validity validation failed", e);
            return ValidationResult.failure(
                    "TIMEFRAME_VALIDITY_ERROR",
                    "Timeframe validity check failed: " + e.getMessage()
            );
        }
    }

    private ValidationResult validateCurrentTimeInValidHours(
            java.util.List<Map<String, Object>> validityHoursPerDay,
            String currentDayName,
            OffsetDateTime currentDateTime) {

        // Find hours configuration for current day
        Map<String, Object> dayHours = validityHoursPerDay.stream()
                .filter(hours -> currentDayName.equals(hours.get("dayOfWeek")))
                .findFirst()
                .orElse(null);

        if (dayHours == null) {
            // No specific hours configured for this day - consider invalid
            return ValidationResult.failure(
                    "OBJECT_INVALID_TIME",
                    String.format("No valid hours configured for day %s", currentDayName)
            );
        }

        try {
            String startTimeStr = (String) dayHours.get("startTime");
            String endTimeStr = (String) dayHours.get("expirationTime");

            if (startTimeStr != null && endTimeStr != null) {
                // Parse time components (assuming format like "09:00:00+07:00")
                OffsetDateTime startTime = OffsetDateTime.parse(
                        currentDateTime.toLocalDate() + "T" + startTimeStr
                );
                OffsetDateTime endTime = OffsetDateTime.parse(
                        currentDateTime.toLocalDate() + "T" + endTimeStr
                );

                if (currentDateTime.isBefore(startTime) || currentDateTime.isAfter(endTime)) {
                    return ValidationResult.failure(
                            "OBJECT_INVALID_TIME",
                            String.format("Current time %s is outside valid hours %s - %s",
                                    currentDateTime.toLocalTime(), startTimeStr, endTimeStr)
                    );
                }
            }

            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Error validating current time in valid hours", e);
            return ValidationResult.failure(
                    "OBJECT_INVALID_TIME",
                    "Failed to validate time range: " + e.getMessage()
            );
        }
    }

    private ValidationResult validateObjectStatus(String objectType, String objectId) {
        try {
            // Check if object is in active status
            // This could query object-specific status from external services

            if ("CAMPAIGN".equals(objectType)) {
                // For campaigns, we might check campaign service
                Boolean isActive = checkCampaignStatus(objectId);

                // TODO: Implement actual campaign status check with campaign service
                // Currently returns null for unknown status, true for active, false for inactive

                // Only fail validation if we definitively know the campaign is inactive
                // If status is unknown (null) or active (true), we allow validation to pass
                if (isActive != null && !isActive) {
                    return ValidationResult.failure(
                            "OBJECT_INACTIVE",
                            String.format("Campaign %s is not in active status", objectId)
                    );
                }
            }

            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Object status validation failed for {}:{}", objectType, objectId, e);
            return ValidationResult.failure(
                    "OBJECT_STATUS_ERROR",
                    "Failed to validate object status: " + e.getMessage()
            );
        }
    }

    private Boolean checkCampaignStatus(String campaignId) {
        // Placeholder for campaign status check
        // In real implementation, this would call campaign service
        // Returns: true if active, false if inactive
        //
        // Note: This method currently returns true to indicate unknown campaign status
        // is treated as "active/allow" until campaign service integration is complete.
        // When integrated with campaign service, this should return the actual status.
        log.debug("Campaign status check not yet implemented for campaignId: {}, assuming active", campaignId);
        return Boolean.TRUE;
    }
}