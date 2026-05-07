package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.MapStringObjectConverter;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "validation_rules", indexes = {
        @Index(name = "idx_validation_rules_state_version", columnList = "state, rule_version"),
        @Index(name = "idx_validation_rules_code", columnList = "code", unique = true)
})
@EntityListeners(IdGenerationListener.class)
@EqualsAndHashCode(callSuper = true)
public class ValidationRuleEntity extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "state", nullable = false, length = 20)
    private String state; // "draft" | "published" | "archived"

    @Column(name = "rule_version", nullable = false)
    private Long ruleVersion;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    @Column(name = "logic", length = 50)
    private String logic; // root logic for implicit top-level

    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "dsl", columnDefinition = "TEXT")
    @SuppressWarnings("java:S1948") // Map content is converted to JSON by MapStringObjectConverter
    private Map<String, Object> dsl; // optional raw DSL snapshot for audit

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by", length = 50)
    private String publishedBy;

    @Column(name = "bundle_hash", length = 200)
    private String bundleHash;

    // One-to-one relationship with usage limits
    @OneToOne(mappedBy = "validationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RuleUsageLimitsEntity limits;

    // One-to-many relationship with rule nodes
    @OneToMany(mappedBy = "validationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SuppressWarnings("java:S1948") // JPA managed relationship, not serialized directly
    private List<RuleNodeEntity> nodes = new ArrayList<>();

    // REMOVED: temporalLinks relationship
    // After migration 013, rule_temporal_links now links to assignments, not validation_rules
    // Temporal policy relationships are assignment-specific, not rule-specific
    // If you need to find temporal policies for a rule, query via assignments:
    // assignment -> rule (FK) AND assignment -> temporal_policy (via rule_temporal_links)

    // One-to-many relationship with rule time frames
    @OneToMany(mappedBy = "validationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SuppressWarnings("java:S1948") // JPA managed relationship, not serialized directly
    private List<RuleTimeFrameEntity> timeFrames = new ArrayList<>();

    public ValidationRuleEntity() {
        super();
    }

    public ValidationRuleEntity(String code, String name, String state, Long ruleVersion) {
        super();
        this.code = code;
        this.name = name;
        this.state = state;
        this.ruleVersion = ruleVersion;
    }
}