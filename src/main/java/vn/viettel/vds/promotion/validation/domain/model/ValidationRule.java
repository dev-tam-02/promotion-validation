package vn.viettel.vds.promotion.validation.domain.model;

import java.time.Instant;
import java.util.Map;

public class ValidationRule {
    private String ruleId;
    private String name;
    private String description;
    private String expression;
    private String type;
    private boolean active;
    private int priority;
    private Map<String, Object> configuration;
    private Instant createdAt;
    private Instant updatedAt;

    public ValidationRule() {
    }

    public ValidationRule(String ruleId, String name, String description, String expression,
                         String type, boolean active, int priority, Map<String, Object> configuration,
                         Instant createdAt, Instant updatedAt) {
        this.ruleId = ruleId;
        this.name = name;
        this.description = description;
        this.expression = expression;
        this.type = type;
        this.active = active;
        this.priority = priority;
        this.configuration = configuration;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public static class Builder {
        private String ruleId;
        private String name;
        private String description;
        private String expression;
        private String type;
        private boolean active;
        private int priority;
        private Map<String, Object> configuration;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
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

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
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

        public ValidationRule build() {
            return new ValidationRule(ruleId, name, description, expression, type, active,
                                     priority, configuration, createdAt, updatedAt);
        }
    }
}