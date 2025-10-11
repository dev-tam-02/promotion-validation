package vn.viettel.vds.promotion.validation.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.in.PublishRuleUseCase;
import vn.viettel.vds.promotion.validation.application.port.in.command.PublishRuleCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.RuleResponse;
import vn.viettel.vds.promotion.validation.application.port.out.EventPublisherPort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;

/**
 * Implementation of PublishRuleUseCase
 */
@Service
@Transactional
public class PublishRuleUseCaseImpl implements PublishRuleUseCase {

    private final RulePersistencePort rulePersistencePort;
    private final EventPublisherPort eventPublisherPort;

    public PublishRuleUseCaseImpl(
            RulePersistencePort rulePersistencePort,
            EventPublisherPort eventPublisherPort
    ) {
        this.rulePersistencePort = rulePersistencePort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public RuleResponse execute(PublishRuleCommand command) {
        // Note: This use case is not currently in use - uses incompatible domain model
        // Minimal implementation to allow compilation
        throw new UnsupportedOperationException("This use case is deprecated and not currently supported");
    }
}