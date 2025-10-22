package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidateObjectValidityRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResult;
import vn.viettel.vds.promotion.validation.application.service.ObjectValidityService;

/**
 * Controller for object validity validation.
 * Handles validation of objects (campaigns, promotions) against their validity timeframes.
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/api/v1/objects")
@RequiredArgsConstructor
@Slf4j
public class ObjectValidityController {

    private final ObjectValidityService objectValidityService;

    /**
     * Validate object validity within its timeframe.
     * Checks if an object (e.g., campaign) is currently valid based on stored
     * validation settings and current time.
     *
     * @param request Object validity request with object type, ID, code, and current time
     * @return ValidationResult with validity status
     */
    @PostMapping("/validity")
    public ValidationResult validateObjectValidity(
            @RequestBody ValidateObjectValidityRequest request) {

        log.info("Received object validity validation request for objectType: {}, objectId: {}",
                request.objectType(), request.objectId());

        try {
            ValidationResult result = objectValidityService.validateObjectValidity(request);

            log.debug("Object validity validation completed for objectId: {} with result: {}",
                    request.objectId(), result.isValid());
            return result;

        } catch (Exception e) {
            log.error("Object validity validation failed for objectId: {}",
                    request.objectId(), e);

            return ValidationResult.failure(
                    "OBJECT_VALIDITY_ERROR",
                    "Object validity validation failed: " + e.getMessage()
            );
        }
    }
}