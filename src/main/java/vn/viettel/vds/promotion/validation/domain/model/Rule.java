package vn.viettel.vds.promotion.validation.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple domain model for Rule persistence operations.
 * This is used by application services and ports for CRUD operations.
 *
 * Note: This is different from RuleAggregate which is a DDD aggregate root with value objects.
 */
@Data
@Builder(toBuilder = true)
public class Rule {
    private String id;
    private String tenantId;
    private String code;
    private String name;
    private String description;
    private String state;
    private Long ruleVersion;
    private String logic;
    private Map<String, Object> dsl;
    private Instant publishedAt;
    private String publishedBy;

    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;

    // Additional fields from RuleJpaEntity
    private String ruleCode;
    private String notes;
    private String expression;
    private String type;
    private Boolean active;
    private Integer priority;
    private Integer latestVersion;
    private Map<String, String> configuration;
    private List<String> targetSegments;
    private String campaignId;
    private String ruleSetId;
    private List<RuleNode> nodes;
    private Map<String, Object> limits;

    /**
     * Rule state enum
     */
    public enum RuleState {
        DRAFT,
        PUBLISHED,
        ARCHIVED,
        DEPRECATED
    }

    /**
     * Logic type enum
     */
    public enum LogicType {
        ALL,  // AND
        ANY,  // OR
        NONE  // NOT
    }

    public Rule() {
        this.dsl = new HashMap<>();
        this.configuration = new HashMap<>();
        this.targetSegments = new ArrayList<>();
    }

    public Rule(String id, String tenantId, String code, String name, String description, String state,
                Long ruleVersion, String logic, Map<String, Object> dsl, Instant publishedAt, String publishedBy,
                Instant createdAt, Instant updatedAt, String createdBy, String updatedBy, Long version,
                String ruleCode, String notes, String expression, String type, Boolean active, Integer priority,
                Integer latestVersion, Map<String, String> configuration, List<String> targetSegments,
                String campaignId, String ruleSetId) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.state = state;
        this.ruleVersion = ruleVersion;
        this.logic = logic;
        this.dsl = dsl != null ? dsl : new HashMap<>();
        this.publishedAt = publishedAt;
        this.publishedBy = publishedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;
        this.ruleCode = ruleCode;
        this.notes = notes;
        this.expression = expression;
        this.type = type;
        this.active = active;
        this.priority = priority;
        this.latestVersion = latestVersion;
        this.configuration = configuration != null ? configuration : new HashMap<>();
        this.targetSegments = targetSegments != null ? targetSegments : new ArrayList<>();
        this.campaignId = campaignId;
        this.ruleSetId = ruleSetId;
    }
}
