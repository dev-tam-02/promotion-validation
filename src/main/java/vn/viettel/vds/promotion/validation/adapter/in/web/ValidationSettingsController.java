package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidateValidationSettingRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResult;
import vn.viettel.vds.promotion.validation.application.service.ValidationSettingsService;

/**
 * Controller for validation settings validation.
 * Handles validation of validation rule configurations and timeframe settings.
 */
@RestController
@RequestMapping("/api/v1/validation-settings")
@ResponseWrapper
@RequiredArgsConstructor
@Slf4j
public class ValidationSettingsController {

    private final ValidationSettingsService validationSettingsService;

    /**
     * Validate validation settings including timeframe and validation rule.
     * This combines validation of both timeframe configuration and validation rule assignment.
     *
     * @param request Validation setting request containing rule ID, type, timeframe config
     * @return ValidationResult with validation outcome
     */
    @PostMapping("/validate")
    public ValidationResult validateValidationSetting(
            @RequestBody ValidateValidationSettingRequest request) {

        log.info("Received validation setting validation request for ruleId: {}, ruleType: {}",
                request.validationRuleId(), request.ruleType());

        try {
            ValidationResult result = validationSettingsService.validateValidationSetting(request);

            log.debug("Validation setting validation completed with result: {}", result.isValid());
            return result;

        } catch (Exception e) {
            log.error("Validation setting validation failed for ruleId: {}",
                    request.validationRuleId(), e);

            return ValidationResult.failure(
                    "VALIDATION_SETTING_ERROR",
                    "Validation setting validation failed: " + e.getMessage()
            );
        }
    }
}