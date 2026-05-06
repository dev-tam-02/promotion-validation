package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.in.RuleBindingUseCase;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link RuleBindingUseCase}.
 * Manages the binding between validation rules and campaign/coupon resources.
 */
@Service
public class RuleBindingManagementService implements RuleBindingUseCase {

    private static final Logger log = LoggerFactory.getLogger(RuleBindingManagementService.class);

    private final RuleBindingPersistencePort bindingPort;
    private final RuleValidator ruleValidator;

    public RuleBindingManagementService(RuleBindingPersistencePort bindingPort,
                                        RuleValidator ruleValidator) {
        this.bindingPort = bindingPort;
        this.ruleValidator = ruleValidator;
    }

    @Override
    @Transactional
    public RuleBinding bindRuleToResource(String ruleId, String resourceType, String resourceId,
                                          Instant activeFrom, Instant activeTo,
                                          int priority, String createdBy) {
        log.info("bindRuleToResource: ruleId={} resourceType={} resourceId={} by={}",
                ruleId, resourceType, resourceId, createdBy);

        // V3: validate scope fields (null here — no structured scopes via this entry point)
        ruleValidator.checkBindingScopeSchema(null, null, null);

        RuleBinding binding = RuleBinding.builder()
                .id(UUID.randomUUID().toString())
                .ruleId(ruleId)
                .objectType(resourceType)
                .objectId(resourceId)
                .validFrom(activeFrom != null ? activeFrom : null)
                .validTo(activeTo != null ? activeTo : null)
                .active(true)
                .priority(priority)
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .version(0L)
                .build();

        RuleBinding saved = bindingPort.save(binding);
        log.info("bindRuleToResource: created binding id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void unbind(String bindingId, String deletedBy) {
        log.info("unbind: bindingId={} by={}", bindingId, deletedBy);
        bindingPort.deleteById(bindingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleBinding> listBindings(String resourceType, String resourceId) {
        log.debug("listBindings: resourceType={} resourceId={}", resourceType, resourceId);
        return bindingPort.findActiveByObject(resourceType, resourceId);
    }
}
