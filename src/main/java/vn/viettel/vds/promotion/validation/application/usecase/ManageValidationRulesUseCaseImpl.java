package vn.viettel.vds.promotion.validation.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.in.ManageValidationRulesUseCase;
import vn.viettel.vds.promotion.validation.application.port.in.command.CreateRuleCommand;
import vn.viettel.vds.promotion.validation.application.port.in.command.DeployRulesCommand;
import vn.viettel.vds.promotion.validation.application.port.in.command.UpdateRuleCommand;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationEnginePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;
import vn.viettel.vds.promotion.validation.domain.service.ValidationDomainService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ManageValidationRulesUseCase
 * Handles rule management operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ManageValidationRulesUseCaseImpl implements ManageValidationRulesUseCase {

    private final ValidationRuleRepositoryPort ruleRepository;
    private final ValidationEnginePort validationEngine;
    private final ValidationDomainService domainService;

    @Override
    public Rule createRule(CreateRuleCommand command) {
        log.info("Creating new rule with code: {}", command.getRuleCode());

        // Validate rule syntax
        if (!validateRuleSyntax(command.getExpression())) {
            throw new IllegalArgumentException("Invalid rule expression syntax");
        }

        // Build domain model
        Rule rule = Rule.builder()
                .id(UUID.randomUUID().toString())
                .ruleCode(command.getRuleCode())
                .name(command.getName())
                .description(command.getDescription())
                .expression(command.getExpression())
                .type(command.getType().name())
                .state(command.isActive() ? Rule.RuleState.PUBLISHED : Rule.RuleState.DRAFT)
                .priority(command.getPriority())
                .configuration(convertConfiguration(command.getConfiguration()))
                .targetSegments(command.getTargetSegments())
                .effectiveFrom(command.getEffectiveFrom())
                .effectiveTo(command.getEffectiveUntil())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Save to repository
        Rule savedRule = ruleRepository.save(rule);

        log.info("Rule created successfully: {}", savedRule.getId());
        return savedRule;
    }

    @Override
    public Rule updateRule(String ruleId, UpdateRuleCommand command) {
        log.info("Updating rule: {}", ruleId);

        // Get existing rule
        Rule existingRule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Rule not found: " + ruleId));

        // Update fields if provided
        Rule.RuleBuilder updatedRule = existingRule.toBuilder()
                .updatedAt(Instant.now());

        if (command.getName() != null) {
            updatedRule.name(command.getName());
        }
        if (command.getDescription() != null) {
            updatedRule.description(command.getDescription());
        }
        if (command.getExpression() != null) {
            // Validate new expression
            if (!validateRuleSyntax(command.getExpression())) {
                throw new IllegalArgumentException("Invalid rule expression syntax");
            }
            updatedRule.expression(command.getExpression());
        }
        if (command.getType() != null) {
            updatedRule.type(command.getType().name());
        }
        if (command.getActive() != null) {
            updatedRule.state(command.getActive() ? Rule.RuleState.PUBLISHED : Rule.RuleState.DRAFT);
        }
        if (command.getPriority() != null) {
            updatedRule.priority(command.getPriority());
        }
        if (command.getConfiguration() != null) {
            updatedRule.configuration(convertConfiguration(command.getConfiguration()));
        }
        if (command.getTargetSegments() != null) {
            updatedRule.targetSegments(command.getTargetSegments());
        }
        if (command.getEffectiveFrom() != null) {
            updatedRule.effectiveFrom(command.getEffectiveFrom());
        }
        if (command.getEffectiveUntil() != null) {
            updatedRule.effectiveTo(command.getEffectiveUntil());
        }

        Rule updated = updatedRule.build();

        // Save updated rule
        Rule savedRule = ruleRepository.save(updated);

        log.info("Rule updated successfully: {}", ruleId);
        return savedRule;
    }

    @Override
    public void deleteRule(String ruleId) {
        log.info("Deleting rule: {}", ruleId);

        if (!ruleRepository.existsById(ruleId)) {
            throw new NoSuchElementException("Rule not found: " + ruleId);
        }

        ruleRepository.deleteById(ruleId);
        log.info("Rule deleted successfully: {}", ruleId);
    }

    @Override
    public Optional<Rule> getRule(String ruleId) {
        return ruleRepository.findById(ruleId);
    }

    @Override
    public List<Rule> getAllRules() {
        return ruleRepository.findActiveRules();
    }

    @Override
    public List<Rule> getActiveRules() {
        return ruleRepository.findActiveRules()
                .stream()
                .filter(Rule::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public Rule activateRule(String ruleId) {
        log.info("Activating rule: {}", ruleId);

        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Rule not found: " + ruleId));

        Rule activated = rule.toBuilder()
                .state(Rule.RuleState.PUBLISHED)
                .updatedAt(Instant.now())
                .build();

        return ruleRepository.save(activated);
    }

    @Override
    public Rule deactivateRule(String ruleId) {
        log.info("Deactivating rule: {}", ruleId);

        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Rule not found: " + ruleId));

        Rule deactivated = rule.toBuilder()
                .state(Rule.RuleState.ARCHIVED)
                .updatedAt(Instant.now())
                .build();

        return ruleRepository.save(deactivated);
    }

    @Override
    public boolean deployRules(DeployRulesCommand command) {
        log.info("Deploying {} rules to rule set: {}",
                command.getRuleIds().size(), command.getRuleSetId());

        try {
            // Load rules
            List<Rule> rules = command.getRuleIds().stream()
                    .map(ruleRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            if (rules.isEmpty()) {
                log.warn("No rules found to deploy");
                return false;
            }

            // Build deployment payload
            Map<String, Object> deploymentPayload = new HashMap<>();
            for (Rule rule : rules) {
                deploymentPayload.put(rule.getRuleCode(), rule.getExpression());
            }

            // Deploy to engine
            boolean deployed = validationEngine.deployRules(
                    command.getRuleSetId(),
                    deploymentPayload
            );

            // Warm up if requested
            if (deployed && command.isWarmUp()) {
                validationEngine.warmUp(command.getRuleSetId());
            }

            log.info("Rules deployed successfully: {}", deployed);
            return deployed;

        } catch (Exception e) {
            log.error("Failed to deploy rules", e);
            return false;
        }
    }

    @Override
    public boolean validateRuleSyntax(String ruleExpression) {
        try {
            Map<String, Object> result = validationEngine.validateRuleSyntax(ruleExpression);
            return Boolean.TRUE.equals(result.get("valid"));
        } catch (Exception e) {
            log.error("Failed to validate rule syntax", e);
            return false;
        }
    }

    @Override
    public Rule cloneRule(String ruleId, String newRuleCode) {
        log.info("Cloning rule {} with new code: {}", ruleId, newRuleCode);

        Rule originalRule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Rule not found: " + ruleId));

        Rule clonedRule = originalRule.toBuilder()
                .id(UUID.randomUUID().toString())
                .ruleCode(newRuleCode)
                .name(originalRule.getName() + " (Copy)")
                .state(Rule.RuleState.DRAFT) // Start as draft
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return ruleRepository.save(clonedRule);
    }

    @Override
    public List<Rule> getRulesByPromotion(String promotionId) {
        return ruleRepository.findByPromotionId(promotionId);
    }

    @Override
    public List<Rule> getRulesByType(String ruleType) {
        try {
            Rule.RuleType type = Rule.RuleType.valueOf(ruleType.toUpperCase());
            return ruleRepository.findByType(type);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid rule type: {}", ruleType);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean testRule(String ruleId, Map<String, Object> testData) {
        log.info("Testing rule {} with test data", ruleId);

        try {
            Rule rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new NoSuchElementException("Rule not found: " + ruleId));

            // Create test request
            ValidationRequest testRequest = ValidationRequest.builder()
                    .transactionId("TEST_" + UUID.randomUUID())
                    .context(testData)
                    .timestamp(Instant.now())
                    .build();

            // Test with single rule
            ValidationResult result = domainService.validate(testRequest, List.of(rule));

            return result.isAllowed();

        } catch (Exception e) {
            log.error("Failed to test rule", e);
            return false;
        }
    }

    @Override
    public List<Rule> bulkCreateRules(List<CreateRuleCommand> commands) {
        log.info("Bulk creating {} rules", commands.size());

        List<Rule> createdRules = new ArrayList<>();

        for (CreateRuleCommand command : commands) {
            try {
                Rule rule = createRule(command);
                createdRules.add(rule);
            } catch (Exception e) {
                log.error("Failed to create rule: {}", command.getRuleCode(), e);
                // Continue with other rules
            }
        }

        log.info("Bulk created {} rules successfully", createdRules.size());
        return createdRules;
    }

    @Override
    public List<Rule> bulkUpdateRules(Map<String, UpdateRuleCommand> updates) {
        log.info("Bulk updating {} rules", updates.size());

        List<Rule> updatedRules = new ArrayList<>();

        for (Map.Entry<String, UpdateRuleCommand> entry : updates.entrySet()) {
            try {
                Rule rule = updateRule(entry.getKey(), entry.getValue());
                updatedRules.add(rule);
            } catch (Exception e) {
                log.error("Failed to update rule: {}", entry.getKey(), e);
                // Continue with other rules
            }
        }

        log.info("Bulk updated {} rules successfully", updatedRules.size());
        return updatedRules;
    }

    @Override
    public Map<String, Object> exportRules(List<String> ruleIds) {
        log.info("Exporting {} rules", ruleIds.size());

        Map<String, Object> export = new HashMap<>();
        export.put("version", "1.0");
        export.put("exportDate", Instant.now());

        List<Map<String, Object>> rulesExport = new ArrayList<>();

        for (String ruleId : ruleIds) {
            ruleRepository.findById(ruleId).ifPresent(rule -> {
                Map<String, Object> ruleExport = new HashMap<>();
                ruleExport.put("code", rule.getRuleCode());
                ruleExport.put("name", rule.getName());
                ruleExport.put("description", rule.getDescription());
                ruleExport.put("expression", rule.getExpression());
                ruleExport.put("type", rule.getType().toString());
                ruleExport.put("priority", rule.getPriority());
                ruleExport.put("configuration", rule.getConfiguration());
                ruleExport.put("targetSegments", rule.getTargetSegments());
                rulesExport.add(ruleExport);
            });
        }

        export.put("rules", rulesExport);
        return export;
    }

    @Override
    public List<Rule> importRules(Map<String, Object> configuration) {
        log.info("Importing rules from configuration");

        List<Rule> importedRules = new ArrayList<>();

        if (configuration.get("rules") instanceof List) {
            List<Map<String, Object>> rulesConfig = (List<Map<String, Object>>) configuration.get("rules");

            for (Map<String, Object> ruleConfig : rulesConfig) {
                try {
                    CreateRuleCommand command = CreateRuleCommand.builder()
                            .ruleCode((String) ruleConfig.get("code"))
                            .name((String) ruleConfig.get("name"))
                            .description((String) ruleConfig.get("description"))
                            .expression((String) ruleConfig.get("expression"))
                            .type(Rule.RuleType.valueOf((String) ruleConfig.get("type")))
                            .priority((Integer) ruleConfig.getOrDefault("priority", 100))
                            .configuration((Map<String, Object>) ruleConfig.get("configuration"))
                            .targetSegments((Set<String>) ruleConfig.get("targetSegments"))
                            .active(false) // Start as inactive
                            .build();

                    Rule rule = createRule(command);
                    importedRules.add(rule);

                } catch (Exception e) {
                    log.error("Failed to import rule", e);
                    // Continue with other rules
                }
            }
        }

        log.info("Imported {} rules successfully", importedRules.size());
        return importedRules;
    }

    /**
     * Convert Map<String, Object> to Map<String, String>
     */
    private Map<String, String> convertConfiguration(Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        Map<String, String> converted = new HashMap<>();
        config.forEach((key, value) -> {
            if (value != null) {
                converted.put(key, value.toString());
            }
        });
        return converted;
    }
}