package vn.viettel.vds.promotion.validation.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.in.CreateRuleUseCase;
import vn.viettel.vds.promotion.validation.application.port.in.command.CreateRuleCommand;
import vn.viettel.vds.promotion.validation.application.port.in.dto.RuleResponse;
import vn.viettel.vds.promotion.validation.application.port.out.EventPublisherPort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.domain.event.RuleCreatedEvent;
import vn.viettel.vds.promotion.validation.domain.exception.RuleAlreadyExistsException;
import vn.viettel.vds.promotion.validation.domain.factory.RuleFactory;
import vn.viettel.vds.promotion.validation.domain.model.LogicType;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleCode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of CreateRuleUseCase
 */
@Service
@Transactional
public class CreateRuleUseCaseImpl implements CreateRuleUseCase {

    private final RulePersistencePort rulePersistencePort;
    private final EventPublisherPort eventPublisherPort;
    private final RuleFactory ruleFactory;

    public CreateRuleUseCaseImpl(
            RulePersistencePort rulePersistencePort,
            EventPublisherPort eventPublisherPort,
            RuleFactory ruleFactory
    ) {
        this.rulePersistencePort = rulePersistencePort;
        this.eventPublisherPort = eventPublisherPort;
        this.ruleFactory = ruleFactory;
    }

    @Override
    public RuleResponse execute(CreateRuleCommand command) {
        // Note: This use case is not currently in use - uses incompatible domain model
        // Minimal implementation to allow compilation
        throw new UnsupportedOperationException("This use case is deprecated and not currently supported");
    }
}