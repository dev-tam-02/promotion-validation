package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.util.Map;

/**
 * Inbound port for data validation use case
 * Defines the contract for validation operations
 */
public interface ValidateDataUseCase {

    /**
     * Validate data against configured rules
     *
     * @param request the validation request
     * @return validation result
     */
    ValidationResult validate(ValidationRequest request);

    /**
     * Perform fast check validation.
     * Pre-validates request locally, then delegates to Rule-Engine service for
     * actual fast-check logic (time constraints, order constraints, blacklist, rate limiting).
     *
     * @param request the validation request
     * @return validation result from Rule-Engine
     */
    ValidationResult performFastCheck(ValidationRequest request);

    /**
     * Execute full validation with all rules
     *
     * @param request the validation request
     * @return validation result
     */
    ValidationResult executeFullValidation(ValidationRequest request);

    /**
     * Validate with specific rule set
     *
     * @param request   the validation request
     * @param ruleSetId ID of the rule set to use
     * @return validation result
     */
    ValidationResult validateWithRuleSet(ValidationRequest request, String ruleSetId);

    /**
     * Batch validation for multiple requests
     *
     * @param requests map of request ID to validation request
     * @return map of request ID to validation result
     */
    Map<String, ValidationResult> validateBatch(Map<String, ValidationRequest> requests);
}