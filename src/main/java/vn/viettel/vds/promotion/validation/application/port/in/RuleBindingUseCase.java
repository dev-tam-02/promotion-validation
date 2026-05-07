package vn.viettel.vds.promotion.validation.application.port.in;

import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.List;

/**
 * Use case for binding / unbinding validation rules to resources.
 */
public interface RuleBindingUseCase {

    /**
     * Bind a rule to a resource.
     *
     * @param ruleId       rule id
     * @param resourceType CAMPAIGN | COUPON_CONFIG | COUPON_CODE
     * @param resourceId   resource identifier
     * @param activeFrom   optional start time (null = immediately)
     * @param activeTo     optional end time (null = indefinite)
     * @param priority     binding priority (higher = evaluated first)
     * @param createdBy    user id
     * @return created binding
     */
    RuleBinding bindRuleToResource(String ruleId, String resourceType, String resourceId,
                                   Instant activeFrom, Instant activeTo,
                                   int priority, String createdBy);

    /**
     * Remove a binding.
     *
     * @param bindingId binding id
     * @param deletedBy user id
     */
    void unbind(String bindingId, String deletedBy);

    /**
     * List all active bindings for a resource.
     *
     * @param resourceType CAMPAIGN | COUPON_CONFIG | COUPON_CODE
     * @param resourceId   resource identifier
     * @return list of bindings ordered by priority desc
     */
    List<RuleBinding> listBindings(String resourceType, String resourceId);
}
