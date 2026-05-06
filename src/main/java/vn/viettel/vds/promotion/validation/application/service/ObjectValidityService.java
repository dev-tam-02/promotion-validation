package vn.viettel.vds.promotion.validation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidateObjectValidityRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResult;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    private static final String TIME_VALIDATION_FAILED = "TIME_VALIDATION_FAILED";
    private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

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
            List<RuleBinding> bindings = ruleBindingPort.findByObject(
                    request.objectType(),
                    request.objectId()
            );

            if (bindings.isEmpty()) {
                log.info("No rule binding found for {}:{}, considering as VALID (no time restriction)",
                        request.objectType(), request.objectId());
                return ValidationResult.success();
            }

            Instant currentInstant = request.currentDateTime() != null
                    ? request.currentDateTime().toInstant()
                    : Instant.now();

            for (RuleBinding binding : bindings) {
                ValidationResult result = checkTemporalConstraint(binding, currentInstant, request);
                if (!result.isValid()) {
                    return result;
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

    private ValidationResult checkTemporalConstraint(RuleBinding binding, Instant currentInstant,
                                                     ValidateObjectValidityRequest request) {
        if (!binding.hasTemporalConstraints()) {
            return ValidationResult.success();
        }

        if (binding.getValidFrom() != null && currentInstant.isBefore(binding.getValidFrom())) {
            log.info("Object {}:{} is not yet active (starts at {})",
                    request.objectType(), request.objectId(), binding.getValidFrom());
            return ValidationResult.failure(
                    TIME_VALIDATION_FAILED,
                    String.format("Object %s:%s is not yet active (starts at %s)",
                            request.objectType(), request.objectId(), binding.getValidFrom())
            );
        }

        if (binding.getValidTo() != null && currentInstant.isAfter(binding.getValidTo())) {
            log.info("Object {}:{} has expired (ended at {})",
                    request.objectType(), request.objectId(), binding.getValidTo());
            return ValidationResult.failure(
                    TIME_VALIDATION_FAILED,
                    String.format("Object %s:%s has expired (ended at %s)",
                            request.objectType(), request.objectId(), binding.getValidTo())
            );
        }

        if (binding.getTimeWindows() != null && !binding.getTimeWindows().isEmpty()
                && !isWithinTimeWindows(currentInstant, binding)) {
            log.info("Object {}:{} is outside active time windows",
                    request.objectType(), request.objectId());
            return ValidationResult.failure(
                    TIME_VALIDATION_FAILED,
                    String.format("Object %s:%s is outside active time windows",
                            request.objectType(), request.objectId())
            );
        }

        return ValidationResult.success();
    }

    private boolean isWithinTimeWindows(Instant currentInstant, RuleBinding binding) {
        String timezone = binding.getTimezone() != null ? binding.getTimezone() : DEFAULT_TIMEZONE;

        ZonedDateTime zdt = currentInstant.atZone(ZoneId.of(timezone));
        LocalTime currentTime = zdt.toLocalTime();

        for (RuleBinding.TimeWindow window : binding.getTimeWindows()) {
            try {
                LocalTime startTime = LocalTime.parse(window.getStart());
                LocalTime endTime = LocalTime.parse(window.getEnd());

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
