package vn.viettel.vds.promotion.validation.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidateObjectValidityRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResult;
import vn.viettel.vds.promotion.validation.domain.model.TemporalPolicy;

import java.time.Instant;
import java.util.List;

/**
 * Service for validating object validity.
 * Handles validation of objects (campaigns, promotions) against their validity timeframes.
 * <p>
 * Purpose: Check if an object is currently within its active time (start_ts to end_ts).
 * If no temporal policy is configured for the object, it is considered VALID (no time restriction).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectValidityService {

    private final TemporalPolicyService temporalPolicyService;

    /**
     * Validate object validity within its timeframe.
     * Checks if an object is currently valid based on its temporal policies (start_ts, end_ts).
     * <p>
     * Logic:
     * - If no temporal policy found for object -> PASS (no time restriction)
     * - If temporal policy exists -> check if currentDateTime is within start_ts and end_ts
     *
     * @param request Object validity request containing objectType (e.g., "CASHBACK"), objectId, and currentDateTime
     * @return ValidationResult with validity status
     */
    public ValidationResult validateObjectValidity(ValidateObjectValidityRequest request) {
        log.info("Validating active time for objectType: {}, objectId: {}",
                request.objectType(), request.objectId());

        try {
            // Get temporal policies for this object
            List<TemporalPolicyService.TemporalPolicyWithMode> temporalPolicies =
                    temporalPolicyService.getTemporalPoliciesByObjectTypeAndId(
                            request.objectType(),
                            request.objectId()
                    );

            // If no temporal policy configured -> PASS (no time restriction)
            if (temporalPolicies.isEmpty()) {
                log.info("No temporal policy found for {}:{}, considering as VALID (no time restriction)",
                        request.objectType(), request.objectId());
                return ValidationResult.success();
            }

            // Check each temporal policy
            Instant currentInstant = request.currentDateTime() != null
                    ? request.currentDateTime().toInstant()
                    : Instant.now();

            for (TemporalPolicyService.TemporalPolicyWithMode policyWithMode : temporalPolicies) {
                TemporalPolicy policy = policyWithMode.getTemporalPolicy();
                String mode = policyWithMode.getMode(); // "ALLOW" or "DENY"

                ValidationResult result = validateAgainstTemporalPolicy(policy, currentInstant, mode);
                if (!result.isValid()) {
                    return result;
                }
            }

            log.info("Object validity validation PASSED for objectType: {}, objectId: {}",
                    request.objectType(), request.objectId());
            return ValidationResult.success();

        } catch (Exception e) {
            log.error("Object validity validation failed for objectType: {}, objectId: {}",
                    request.objectType(), request.objectId(), e);
            return ValidationResult.failure(
                    "OBJECT_VALIDITY_PROCESSING_ERROR",
                    "Failed to process object validity check: " + e.getMessage()
            );
        }
    }

    /**
     * Validate current time against a temporal policy's start_ts and end_ts.
     *
     * @param policy         The temporal policy containing start_ts and end_ts
     * @param currentInstant The current time to validate
     * @param mode           The mode of the policy ("ALLOW" means time must be within range)
     * @return ValidationResult
     */
    private ValidationResult validateAgainstTemporalPolicy(TemporalPolicy policy, Instant currentInstant, String mode) {
        Instant startTs = policy.getStartTs();
        Instant endTs = policy.getEndTs();

        log.debug("Checking temporal policy: id={}, startTs={}, endTs={}, mode={}, currentTime={}",
                policy.getId(), startTs, endTs, mode, currentInstant);

        // If no start/end time defined, consider as no restriction
        if (startTs == null && endTs == null) {
            log.debug("No start_ts/end_ts defined for policy {}, considering as VALID", policy.getId());
            return ValidationResult.success();
        }

        // Validate based on mode
        if ("ALLOW".equalsIgnoreCase(mode)) {
            return validateAllowMode(startTs, endTs, currentInstant);
        }

        if ("DENY".equalsIgnoreCase(mode)) {
            return validateDenyMode(startTs, endTs, currentInstant);
        }

        return ValidationResult.success();
    }

    /**
     * Validate for ALLOW mode: current time must be within [startTs, endTs].
     */
    private ValidationResult validateAllowMode(Instant startTs, Instant endTs, Instant currentInstant) {
        // Check if not yet started
        if (startTs != null && currentInstant.isBefore(startTs)) {
            String message = String.format("Object is not yet active. Start time: %s", startTs);
            log.info("Validation FAILED - not started yet: {}", message);
            return ValidationResult.failure("OBJECT_NOT_STARTED", message);
        }

        // Check if already expired
        if (endTs != null && currentInstant.isAfter(endTs)) {
            String message = String.format("Object has expired. End time: %s", endTs);
            log.info("Validation FAILED - expired: {}", message);
            return ValidationResult.failure("OBJECT_EXPIRED", message);
        }

        return ValidationResult.success();
    }

    /**
     * Validate for DENY mode: current time must NOT be within [startTs, endTs].
     * This is for blackout periods.
     */
    private ValidationResult validateDenyMode(Instant startTs, Instant endTs, Instant currentInstant) {
        boolean withinDenyPeriod = isWithinPeriod(startTs, endTs, currentInstant);

        if (withinDenyPeriod) {
            String message = String.format("Object is in blackout period: %s to %s", startTs, endTs);
            log.info("Validation FAILED - in blackout period: {}", message);
            return ValidationResult.failure("OBJECT_IN_BLACKOUT", message);
        }

        return ValidationResult.success();
    }

    /**
     * Check if current time is within the period [startTs, endTs].
     */
    private boolean isWithinPeriod(Instant startTs, Instant endTs, Instant currentInstant) {
        if (startTs != null && currentInstant.isBefore(startTs)) {
            return false;
        }
        return endTs == null || !currentInstant.isAfter(endTs);
    }
}