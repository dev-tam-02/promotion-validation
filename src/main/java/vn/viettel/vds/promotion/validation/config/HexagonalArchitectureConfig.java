package vn.viettel.vds.promotion.validation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import vn.viettel.vds.promotion.validation.application.port.in.ManageValidationRulesUseCase;
import vn.viettel.vds.promotion.validation.application.port.in.ValidateDataUseCase;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationEnginePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.application.usecase.ManageValidationRulesUseCaseImpl;
import vn.viettel.vds.promotion.validation.application.usecase.ValidateDataUseCaseImpl;
import vn.viettel.vds.promotion.validation.domain.service.ValidationDomainService;

/**
 * Configuration for hexagonal architecture dependency injection
 * Wires use cases with their dependencies
 */
@Configuration
public class HexagonalArchitectureConfig {

    /**
     * Configure ValidateDataUseCase with its dependencies
     */
    @Bean
    @Primary
    public ValidateDataUseCase validateDataUseCase(
            ValidationDomainService domainService,
            ValidationRuleRepositoryPort ruleRepository,
            ValidationEnginePort validationEngine) {

        return new ValidateDataUseCaseImpl(
                domainService,
                ruleRepository,
                validationEngine
        );
    }

    /**
     * Configure ManageValidationRulesUseCase with its dependencies
     */
    @Bean
    @Primary
    public ManageValidationRulesUseCase manageValidationRulesUseCase(
            ValidationRuleRepositoryPort ruleRepository,
            ValidationEnginePort validationEngine,
            ValidationDomainService domainService) {

        return new ManageValidationRulesUseCaseImpl(
                ruleRepository,
                validationEngine,
                domainService
        );
    }

    /**
     * Configure ValidationDomainService
     */
    @Bean
    public ValidationDomainService validationDomainService() {
        return new ValidationDomainService();
    }
}