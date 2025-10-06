package vn.viettel.vds.promotion.validation.application.port.out;

import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRule;

/**
 * Outbound port for publishing domain events
 */
public interface EventPublisherPort {

    /**
     * Publish validation completed event
     */
    void publishValidationCompleted(ValidationResult result);

    /**
     * Publish validation failed event
     */
    void publishValidationFailed(String validationId, String error);

    /**
     * Publish rule created event
     */
    void publishRuleCreated(ValidationRule rule);

    /**
     * Publish rule updated event
     */
    void publishRuleUpdated(ValidationRule rule);

    /**
     * Publish rule deleted event
     */
    void publishRuleDeleted(String ruleId);

    /**
     * Publish rule deployed event
     */
    void publishRuleDeployed(String ruleSetId, boolean success);

    /**
     * Publish high risk validation alert
     */
    void publishHighRiskValidation(ValidationResult result);

    /**
     * Publish event asynchronously
     */
    void publishAsync(Object event);
}