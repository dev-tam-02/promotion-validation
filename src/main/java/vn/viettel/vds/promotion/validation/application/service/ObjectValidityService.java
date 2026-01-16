package vn.viettel.vds.promotion.validation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidateObjectValidityRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResult;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.List;

/**
 * Service for validating object validity.
 * Handles validation of objects (campaigns, promotions) against their validity timeframes.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectValidityService {

    private final RuleBindingPersistencePort ruleBindingPort;

    /**
     * Validate object validity within its timeframe.
     * Checks if an object is currently valid based on its RuleBinding temporal constraints.
     * <p>
     * Logic:
     * - If no RuleBinding found for object -> PASS (no time restriction)
     * - If RuleBinding exists -> check if currentDateTime is within validFrom and validTo
     *
     * @param request Object validity request containing objectType, objectId, and currentDateTime
     * @return ValidationResult with validity status
     */
    public ValidationResult validateObjectValidity(ValidateObjectValidityRequest request) {
        log.info("Validating active time for objectType: {}, objectId: {}",
                request.objectType(), request.objectId());

        try {
            // Get rule bindings for this object
            List<RuleBinding> bindings = ruleBindingPort.findByObject(
                    request.objectType(),
                    request.objectId()
            );

            // If no binding configured -> PASS (no time restriction)
            if (bindings.isEmpty()) {
                log.info("No rule binding found for {}:{}, considering as VALID (no time restriction)",
                        request.objectType(), request.objectId());
                return ValidationResult.success();
            }

            // Check each binding's temporal constraints
            Instant currentInstant = request.currentDateTime() != null
                    ? request.currentDateTime().toInstant()
                    : Instant.now();

            for (RuleBinding binding : bindings) {
                // Skip if binding has no temporal constraints
                if (!binding.hasTemporalConstraints()) {
                    continue;
                }

                // Check if within time range
                if (binding.getValidFrom() != null && currentInstant.isBefore(binding.getValidFrom())) {
                    log.info("Object {}:{} is not yet active (starts at {})",
                            request.objectType(), request.objectId(), binding.getValidFrom());
                    return ValidationResult.failure(
                            "TIME_VALIDATION_FAILED",
                            String.format("Object %s:%s is not yet active (starts at %s)",
                                    request.objectType(), request.objectId(), binding.getValidFrom())
                    );
                }

                if (binding.getValidTo() != null && currentInstant.isAfter(binding.getValidTo())) {
                    log.info("Object {}:{} has expired (ended at {})",
                            request.objectType(), request.objectId(), binding.getValidTo());
                    return ValidationResult.failure(
                            "TIME_VALIDATION_FAILED",
                            String.format("Object %s:%s has expired (ended at %s)",
                                    request.objectType(), request.objectId(), binding.getValidTo())
                    );
                }

                // Check if within daily time windows
                if (binding.getTimeWindows() != null && !binding.getTimeWindows().isEmpty()) {
                    if (!isWithinTimeWindows(currentInstant, binding)) {
                        log.info("Object {}:{} is outside active time windows",
                                request.objectType(), request.objectId());
                        return ValidationResult.failure(
                                "TIME_VALIDATION_FAILED",
                                String.format("Object %s:%s is outside active time windows",
                                        request.objectType(), request.objectId())
                        );
                    }
                }
            }

            log.info("Object {}:{} passed time validation", request.objectType(), request.objectId());
            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Error validating object validity for {}:{}: {}",
                    request.objectType(), request.objectId(), e.getMessage(), e);
            return ValidationResult.failure("VALIDATION_ERROR", "Error validating object validity: " + e.getMessage());
        }
    }

    private boolean isWithinTimeWindows(Instant currentInstant, RuleBinding binding) {
        // Simple implementation - check if current time of day is within any window
        // For full implementation, would need to handle timezone and day of week
        String timezone = binding.getTimezone() != null ? binding.getTimezone() : "Asia/Ho_Chi_Minh";

        java.time.ZonedDateTime zdt = currentInstant.atZone(java.time.ZoneId.of(timezone));
        java.time.LocalTime currentTime = zdt.toLocalTime();

        for (RuleBinding.TimeWindow window : binding.getTimeWindows()) {
            try {
                java.time.LocalTime startTime = java.time.LocalTime.parse(window.getStart());
                java.time.LocalTime endTime = java.time.LocalTime.parse(window.getEnd());

                if (!currentTime.isBefore(startTime) && !currentTime.isAfter(endTime)) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("Invalid time window format: start={}, end={}", window.getStart(), window.getEnd());
            }
        }

        return false;
    }
}
