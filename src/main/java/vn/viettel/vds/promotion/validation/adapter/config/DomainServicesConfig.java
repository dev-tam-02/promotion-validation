package vn.viettel.vds.promotion.validation.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineClient;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RulePersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort;
import vn.viettel.vds.promotion.validation.config.TenantProperties;
import vn.viettel.vds.promotion.validation.application.service.RuleDeploymentPipelineService;
import vn.viettel.vds.promotion.validation.application.service.RulePublishingService;
import vn.viettel.vds.promotion.validation.application.service.RuleVersioningService;
import vn.viettel.vds.promotion.validation.domain.service.StackableDiscountValidationService;
import vn.viettel.vds.promotion.validation.domain.service.StackableRuleEvaluator;

/**
 * Configuration for domain services.
 * <p>
 * This configuration class wires domain services that have been refactored
 * to remove Spring annotations. It follows the hexagonal architecture principle
 * of separating infrastructure concerns from domain logic.
 * </p>
 * <p>
 * NOTE: RulePublishingService, RuleVersioningService, and RuleDeploymentPipelineService
 * are currently in domain/service/ but have dependencies on adapters and ports.
 * They should ideally be moved to application/service/ in a future refactoring.
 * For now, they are wired here as infrastructure configuration.
 * </p>
 */
@Configuration
@EnableTransactionManagement
public class DomainServicesConfig {

    /**
     * Pure domain service for stackable discount validation.
     * No infrastructure dependencies.
     */
    @Bean
    public StackableDiscountValidationService stackableDiscountValidationService() {
        return new StackableDiscountValidationService();
    }

    /**
     * Pure domain service for rule evaluation.
     * No infrastructure dependencies.
     */
    @Bean
    public StackableRuleEvaluator stackableRuleEvaluator() {
        return new StackableRuleEvaluator();
    }

    /**
     * Service for rule publishing operations.
     * Has dependencies on validation engine client and persistence ports.
     * <p>
     * NOTE: This service should ideally be in application/service/ layer
     * but is kept in domain/service/ for backward compatibility.
     * Transaction management is handled via Spring's @Transactional at use case level.
     * </p>
     */
    @Bean
    public RulePublishingService rulePublishingService(
            ValidationEngineClient validationEngineClient,
            RulePersistencePort rulePersistencePort,
            TenantProperties tenantProperties,
            RuleBindingPersistencePort ruleBindingPersistencePort,
            ObjectMapper objectMapper) {
        return new RulePublishingService(
                validationEngineClient,
                rulePersistencePort,
                tenantProperties,
                ruleBindingPersistencePort,
                objectMapper
        );
    }

    /**
     * Service for rule versioning operations.
     * Has dependencies on persistence ports and publishing service.
     * <p>
     * NOTE: This service should ideally be in application/service/ layer
     * but is kept in domain/service/ for backward compatibility.
     * Transaction management is handled via Spring's @Transactional at use case level.
     * </p>
     */
    @Bean
    public RuleVersioningService ruleVersioningService(
            RulePersistencePort rulePersistencePort,
            RuleVersionPersistencePort ruleVersionPersistencePort,
            RulePublishingService rulePublishingService) {
        return new RuleVersioningService(
                rulePersistencePort,
                ruleVersionPersistencePort,
                rulePublishingService
        );
    }

    /**
     * Service for rule deployment pipeline operations.
     * Has dependencies on publishing service and persistence ports.
     * <p>
     * NOTE: This service should ideally be in application/service/ layer
     * but is kept in domain/service/ for backward compatibility.
     * Transaction management is handled via Spring's @Transactional at use case level.
     * </p>
     */
    @Bean
    public RuleDeploymentPipelineService ruleDeploymentPipelineService(
            RulePublishingService rulePublishingService,
            RulePersistencePort rulePersistencePort) {
        return new RuleDeploymentPipelineService(
                rulePublishingService,
                rulePersistencePort
        );
    }
}
