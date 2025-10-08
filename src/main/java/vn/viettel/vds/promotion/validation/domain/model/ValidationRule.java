package vn.viettel.vds.promotion.validation.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Legacy validation rule model.
 *
 * @deprecated Use {@link Rule} instead. This class is kept for backward compatibility only.
 * Will be removed in version 2.0.0.
 *
 * Migration path:
 * - Use {@link RuleMigrationMapper} to convert between ValidationRule and Rule
 * - Update all new code to use Rule directly
 * - Existing code should be gradually migrated to use Rule
 */
@Deprecated(since = "1.0.0", forRemoval = true)
public class ValidationRule {
    private String id;
    private String ruleId;
    private String code;
    private String ruleCode;
    private String name;
    private String description;
    private String expression;
    private String type;
    private int priority;
    private Map<String, Object> configuration;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Set<String> targetSegments = new HashSet<>();
    private String state;
    private Integer version;
    private String logic;
    private UsageLimits limits;
    private java.util.List<RuleNode> nodes;
    private Instant publishedAt;
    private String publishedBy;
    private String createdBy;

    public ValidationRule() {
    }

    public ValidationRule(String ruleId, String ruleCode, String name, String description, String expression,
                          String type, int priority, Map<String, Object> configuration,
                          Instant createdAt, Instant updatedAt, Instant effectiveFrom, Instant effectiveTo) {
        this.ruleId = ruleId;
        this.ruleCode = ruleCode;
        this.name = name;
        this.description = description;
        this.expression = expression;
        this.type = type;
        this.priority = priority;
        this.configuration = configuration;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.targetSegments = new HashSet<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isActive() {
        return "published".equalsIgnoreCase(state);
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Instant getEffectiveUntil() {
        return effectiveTo;
    }

    public void setEffectiveUntil(Instant effectiveUntil) {
        this.effectiveTo = effectiveUntil;
    }

    public Set<String> getTargetSegments() {
        return targetSegments != null ? targetSegments : new HashSet<>();
    }

    public void setTargetSegments(Set<String> targetSegments) {
        this.targetSegments = targetSegments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    public UsageLimits getLimits() {
        return limits;
    }

    public void setLimits(UsageLimits limits) {
        this.limits = limits;
    }

    public java.util.List<RuleNode> getNodes() {
        return nodes;
    }

    public void setNodes(java.util.List<RuleNode> nodes) {
        this.nodes = nodes;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Builder toBuilder() {
        return new Builder()
                .ruleId(this.ruleId)
                .ruleCode(this.ruleCode)
                .name(this.name)
                .description(this.description)
                .expression(this.expression)
                .type(this.type)
                .priority(this.priority)
                .configuration(this.configuration)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .effectiveFrom(this.effectiveFrom)
                .effectiveTo(this.effectiveTo)
                .targetSegments(this.targetSegments);
    }

    public boolean appliesTo(String segment) {
        return targetSegments == null || targetSegments.isEmpty() || targetSegments.contains(segment);
    }

    public RuleType getRuleType() {
        if (type == null) return RuleType.CUSTOM;
        try {
            return RuleType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RuleType.CUSTOM;
        }
    }

    public boolean isEffective(Instant checkTime) {
        if (!"published".equalsIgnoreCase(state)) {
            return false;
        }

        if (checkTime == null) {
            checkTime = Instant.now();
        }

        if (effectiveFrom != null && checkTime.isBefore(effectiveFrom)) {
            return false;
        }

        if (effectiveTo != null && checkTime.isAfter(effectiveTo)) {
            return false;
        }

        return true;
    }

    public ValidationResult evaluate(ValidationRequest request) {
        if (!isEffective(request.getTimestamp())) {
            return ValidationResult.builder()
                    .validationId(request.getTransactionId())
                    .ruleId(this.ruleId)
                    .ruleCode(this.ruleCode)
                    .decision(ValidationResult.Decision.ALLOW)
                    .message("Rule not effective at request time")
                    .timestamp(Instant.now())
                    .processingTimeMs(0)
                    .build();
        }

        try {
            // Simple expression evaluation - in real implementation, this would use
            // a proper expression language like SpEL or JEXL
            boolean result = evaluateExpression(request);

            return ValidationResult.builder()
                    .validationId(request.getTransactionId())
                    .ruleId(this.ruleId)
                    .ruleCode(this.ruleCode)
                    .decision(result ? ValidationResult.Decision.ALLOW : ValidationResult.Decision.DENY)
                    .message(result ? "Rule passed" : "Rule failed: " + this.description)
                    .timestamp(Instant.now())
                    .processingTimeMs(0)
                    .build();
        } catch (Exception e) {
            return ValidationResult.builder()
                    .validationId(request.getTransactionId())
                    .ruleId(this.ruleId)
                    .ruleCode(this.ruleCode)
                    .decision(ValidationResult.Decision.ERROR)
                    .message("Rule evaluation error: " + e.getMessage())
                    .timestamp(Instant.now())
                    .processingTimeMs(0)
                    .build();
        }
    }

    private boolean evaluateExpression(ValidationRequest request) {
        // Placeholder implementation - would need proper expression evaluation
        if (expression == null || expression.isEmpty()) {
            return true;
        }

        // Simple mock evaluation based on rule type
        switch (type != null ? type.toUpperCase() : "") {
            case "BLACKLIST":
                return !isCustomerBlacklisted(request);
            case "RANGE":
                return isWithinRange(request);
            case "FORMAT":
                return hasValidFormat(request);
            default:
                return true;
        }
    }

    private boolean isCustomerBlacklisted(ValidationRequest request) {
        // Mock implementation
        return false;
    }

    private boolean isWithinRange(ValidationRequest request) {
        // Mock implementation
        return request.getOrderValue() != null &&
                request.getOrderValue().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasValidFormat(ValidationRequest request) {
        // Mock implementation
        return request.getCustomerId() != null &&
                !request.getCustomerId().isEmpty();
    }

    public enum RuleType {
        REQUIRED,
        FORMAT,
        RANGE,
        PATTERN,
        CUSTOM,
        BUSINESS_RULE,
        BLACKLIST
    }

    public static class Builder {
        private String ruleId;
        private String ruleCode;
        private String name;
        private String description;
        private String expression;
        private String type;
        private int priority;
        private String state;
        private Map<String, Object> configuration;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant effectiveFrom;
        private Instant effectiveTo;
        private Set<String> targetSegments = new HashSet<>();

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder ruleCode(String ruleCode) {
            this.ruleCode = ruleCode;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder configuration(Map<String, Object> configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder effectiveFrom(Instant effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(Instant effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public Builder effectiveUntil(Instant effectiveUntil) {
            this.effectiveTo = effectiveUntil;
            return this;
        }

        public Builder targetSegments(Set<String> targetSegments) {
            this.targetSegments = targetSegments;
            return this;
        }

        public Builder type(RuleType type) {
            this.type = type != null ? type.name() : null;
            return this;
        }

        public ValidationRule build() {
            ValidationRule rule = new ValidationRule(ruleId, ruleCode, name, description, expression, type,
                    priority, configuration, createdAt, updatedAt, effectiveFrom, effectiveTo);
            if (targetSegments != null) {
                rule.setTargetSegments(targetSegments);
            }
            if (state != null) {
                rule.setState(state);
            }
            return rule;
        }
    }

    public static class UsageLimits {
        private Integer perCodeTotal;
        private Integer perCustomer;
        private Integer perDay;

        public UsageLimits() {
        }

        public UsageLimits(Integer perCodeTotal, Integer perCustomer, Integer perDay) {
            this.perCodeTotal = perCodeTotal;
            this.perCustomer = perCustomer;
            this.perDay = perDay;
        }

        public Integer getPerCodeTotal() {
            return perCodeTotal;
        }

        public void setPerCodeTotal(Integer perCodeTotal) {
            this.perCodeTotal = perCodeTotal;
        }

        public Integer getPerCustomer() {
            return perCustomer;
        }

        public void setPerCustomer(Integer perCustomer) {
            this.perCustomer = perCustomer;
        }

        public Integer getPerDay() {
            return perDay;
        }

        public void setPerDay(Integer perDay) {
            this.perDay = perDay;
        }
    }
}