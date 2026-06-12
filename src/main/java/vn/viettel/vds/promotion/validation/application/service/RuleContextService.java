package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.application.port.in.GetRuleContextsUseCase;
import vn.viettel.vds.promotion.validation.application.port.out.RuleContextPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleContextOption;

import java.util.List;

/**
 * Implementation of {@link GetRuleContextsUseCase}. Read-only listing of validation
 * rule contexts for the FE rule-builder dropdown and CMS list filter.
 */
@Service
public class RuleContextService implements GetRuleContextsUseCase {

    private static final Logger log = LoggerFactory.getLogger(RuleContextService.class);

    private final RuleContextPersistencePort ruleContextPort;

    public RuleContextService(RuleContextPersistencePort ruleContextPort) {
        this.ruleContextPort = ruleContextPort;
    }

    @Override
    public List<RuleContextOption> listContexts() {
        List<RuleContextOption> contexts = ruleContextPort.findActiveContexts();
        log.info("Loaded {} active rule contexts", contexts.size());
        return contexts;
    }

    @Override
    public boolean isActiveContext(String code) {
        return ruleContextPort.isActiveContext(code);
    }
}
