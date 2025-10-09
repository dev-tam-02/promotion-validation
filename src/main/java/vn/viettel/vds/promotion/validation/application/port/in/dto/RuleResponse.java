package vn.viettel.vds.promotion.validation.application.port.in.dto;

import vn.viettel.vds.promotion.validation.domain.model.LogicType;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleStatus;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for a validation rule
 */
public class RuleResponse {

    private final String id;
    private final String code;
    private final String name;
    private final String description;
    private final LogicType logicType;
    private final List<RuleNodeResponse> nodes;
    private final RuleStatus status;
    private final String version;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String createdBy;
    private final String updatedBy;
    private final Instant publishedAt;
    private final String publishedBy;

    private RuleResponse(Builder builder) {
        this.id = builder.id;
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.logicType = builder.logicType;
        this.nodes = builder.nodes;
        this.status = builder.status;
        this.version = builder.version;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
        this.publishedAt = builder.publishedAt;
        this.publishedBy = builder.publishedBy;
    }

    public static RuleResponse from(Rule rule) {
        return builder()
                .id(rule.getId())
                .code(rule.getCode())
                .name(rule.getName())
                .description(rule.getDescription())
                .logicType(rule.getLogic() != null ? LogicType.valueOf(rule.getLogic().name()) : null)
                .nodes(rule.getNodes() != null ? rule.getNodes().stream()
                        .map(RuleNodeResponse::from)
                        .collect(Collectors.toList()) : List.of())
                .status(rule.getState() != null ? RuleStatus.valueOf(rule.getState().name()) : null)
                .version(rule.getVersion() != null ? rule.getVersion().toString() : null)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .createdBy(rule.getCreatedBy())
                .updatedBy(rule.getUpdatedBy())
                .publishedAt(rule.getPublishedAt())
                .publishedBy(rule.getPublishedBy())
                .build();
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LogicType getLogicType() {
        return logicType;
    }

    public List<RuleNodeResponse> getNodes() {
        return nodes;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public String getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public static class Builder {
        private String id;
        private String code;
        private String name;
        private String description;
        private LogicType logicType;
        private List<RuleNodeResponse> nodes;
        private RuleStatus status;
        private String version;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
        private Instant publishedAt;
        private String publishedBy;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
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

        public Builder logicType(LogicType logicType) {
            this.logicType = logicType;
            return this;
        }

        public Builder nodes(List<RuleNodeResponse> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder status(RuleStatus status) {
            this.status = status;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
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

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public Builder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder publishedBy(String publishedBy) {
            this.publishedBy = publishedBy;
            return this;
        }

        public RuleResponse build() {
            return new RuleResponse(this);
        }
    }
}