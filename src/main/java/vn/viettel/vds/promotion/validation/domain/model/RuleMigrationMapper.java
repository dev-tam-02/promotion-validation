package vn.viettel.vds.promotion.validation.domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Mapper for migrating between ValidationRule (deprecated) and Rule (current).
 *
 * This mapper provides bidirectional conversion to support gradual migration
 * from ValidationRule to Rule model.
 *
 * Usage:
 * <pre>
 * // Convert ValidationRule to Rule
 * Rule rule = RuleMigrationMapper.toRule(validationRule);
 *
 * // Convert Rule to ValidationRule (for backward compatibility)
 * ValidationRule legacyRule = RuleMigrationMapper.toValidationRule(rule);
 * </pre>
 */
public class RuleMigrationMapper {

    /**
     * Convert ValidationRule to Rule
     */
    public static Rule toRule(ValidationRule validationRule) {
        if (validationRule == null) {
            return null;
        }

        Rule.RuleBuilder builder = Rule.builder()
            .id(validationRule.getId())
            .code(validationRule.getCode())
            .ruleCode(validationRule.getRuleCode())
            .name(validationRule.getName())
            .description(validationRule.getDescription())
            .expression(validationRule.getExpression())
            .type(validationRule.getType())
            .active(validationRule.isActive())
            .priority(validationRule.getPriority())
            .configuration(convertMapStringToMapString(validationRule.getConfiguration()))
            .effectiveFrom(validationRule.getEffectiveFrom())
            .effectiveTo(validationRule.getEffectiveTo())
            .createdAt(validationRule.getCreatedAt())
            .updatedAt(validationRule.getUpdatedAt())
            .createdBy(validationRule.getCreatedBy())
            .publishedAt(validationRule.getPublishedAt())
            .publishedBy(validationRule.getPublishedBy())
            .nodes(validationRule.getNodes());

        // Convert state
        if (validationRule.getState() != null) {
            try {
                builder.state(Rule.RuleState.valueOf(validationRule.getState()));
            } catch (IllegalArgumentException e) {
                builder.state(Rule.RuleState.DRAFT);
            }
        }

        // Convert logic
        if (validationRule.getLogic() != null) {
            try {
                builder.logic(Rule.LogicType.valueOf(validationRule.getLogic().toUpperCase()));
            } catch (IllegalArgumentException e) {
                builder.logic(Rule.LogicType.ALL);
            }
        }

        // Convert version
        if (validationRule.getVersion() != null) {
            builder.version(validationRule.getVersion().longValue());
        }

        // Convert target segments
        if (validationRule.getTargetSegments() != null) {
            builder.targetSegments(new HashSet<>(validationRule.getTargetSegments()));
        }

        // Convert usage limits
        if (validationRule.getLimits() != null) {
            ValidationRule.UsageLimits oldLimits = validationRule.getLimits();
            Rule.UsageLimits newLimits = Rule.UsageLimits.builder()
                .perCodeTotal(oldLimits.getPerCodeTotal())
                .perCustomer(oldLimits.getPerCustomer())
                .perDay(oldLimits.getPerDay())
                .build();
            builder.limits(newLimits);
        }

        return builder.build();
    }

    /**
     * Convert Rule to ValidationRule (for backward compatibility)
     */
    @SuppressWarnings("deprecation")
    public static ValidationRule toValidationRule(Rule rule) {
        if (rule == null) {
            return null;
        }

        ValidationRule validationRule = new ValidationRule();
        validationRule.setId(rule.getId());
        validationRule.setRuleId(rule.getId());
        validationRule.setCode(rule.getCode());
        validationRule.setRuleCode(rule.getRuleCode());
        validationRule.setName(rule.getName());
        validationRule.setDescription(rule.getDescription());
        validationRule.setExpression(rule.getExpression());
        validationRule.setType(rule.getType());
        validationRule.setActive(Boolean.TRUE.equals(rule.getActive()));
        validationRule.setPriority(rule.getPriority() != null ? rule.getPriority() : 0);
        validationRule.setConfiguration(convertMapStringToMapObject(rule.getConfiguration()));
        validationRule.setEffectiveFrom(rule.getEffectiveFrom());
        validationRule.setEffectiveTo(rule.getEffectiveTo());
        validationRule.setCreatedAt(rule.getCreatedAt());
        validationRule.setUpdatedAt(rule.getUpdatedAt());
        validationRule.setCreatedBy(rule.getCreatedBy());
        validationRule.setPublishedAt(rule.getPublishedAt());
        validationRule.setPublishedBy(rule.getPublishedBy());
        validationRule.setNodes(rule.getNodes());

        // Convert state
        if (rule.getState() != null) {
            validationRule.setState(rule.getState().name());
        }

        // Convert logic
        if (rule.getLogic() != null) {
            validationRule.setLogic(rule.getLogic().name());
        }

        // Convert version
        if (rule.getVersion() != null) {
            validationRule.setVersion(rule.getVersion().intValue());
        }

        // Convert target segments
        if (rule.getTargetSegments() != null) {
            validationRule.setTargetSegments(new HashSet<>(rule.getTargetSegments()));
        } else if (rule.getTargetSegmentsList() != null) {
            validationRule.setTargetSegments(new HashSet<>(rule.getTargetSegmentsList()));
        }

        // Convert usage limits
        if (rule.getLimits() != null) {
            Rule.UsageLimits newLimits = rule.getLimits();
            ValidationRule.UsageLimits oldLimits = new ValidationRule.UsageLimits(
                newLimits.getPerCodeTotal(),
                newLimits.getPerCustomer(),
                newLimits.getPerDay()
            );
            validationRule.setLimits(oldLimits);
        }

        return validationRule;
    }

    /**
     * Convert list of ValidationRule to list of Rule
     */
    public static List<Rule> toRules(List<ValidationRule> validationRules) {
        if (validationRules == null) {
            return null;
        }

        List<Rule> rules = new ArrayList<>(validationRules.size());
        for (ValidationRule validationRule : validationRules) {
            rules.add(toRule(validationRule));
        }
        return rules;
    }

    /**
     * Convert list of Rule to list of ValidationRule
     */
    public static List<ValidationRule> toValidationRules(List<Rule> rules) {
        if (rules == null) {
            return null;
        }

        List<ValidationRule> validationRules = new ArrayList<>(rules.size());
        for (Rule rule : rules) {
            validationRules.add(toValidationRule(rule));
        }
        return validationRules;
    }

    /**
     * Helper method to convert Map<String, Object> to Map<String, String>
     */
    private static java.util.Map<String, String> convertMapStringToMapString(java.util.Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        java.util.Map<String, String> result = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return result;
    }

    /**
     * Helper method to convert Map<String, String> to Map<String, Object>
     */
    private static java.util.Map<String, Object> convertMapStringToMapObject(java.util.Map<String, String> map) {
        if (map == null) {
            return null;
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.putAll(map);
        return result;
    }
}
