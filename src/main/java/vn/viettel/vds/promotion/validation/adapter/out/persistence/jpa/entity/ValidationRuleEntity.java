package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.MapStringObjectConverter;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
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
public class ValidationRuleEntity extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "state", nullable = false, length = 20)
    private String state; // "draft" | "published" | "archived"

    @Column(name = "rule_version", nullable = false)
    private Long ruleVersion;

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

    // One-to-one relationship with usage limits
    @OneToOne(mappedBy = "validationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RuleUsageLimitsEntity limits;

    // One-to-many relationship with rule nodes
    @OneToMany(mappedBy = "validationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SuppressWarnings("java:S1948") // JPA managed relationship, not serialized directly
    private List<RuleNodeEntity> nodes = new ArrayList<>();

    // One-to-many relationship with rule assignments
    @OneToMany(mappedBy = "validationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SuppressWarnings("java:S1948") // JPA managed relationship, not serialized directly
    private List<RuleAssignmentEntity> assignments = new ArrayList<>();

    // One-to-many relationship with rule bundles
    @OneToMany(mappedBy = "validationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SuppressWarnings("java:S1948") // JPA managed relationship, not serialized directly
    private List<RuleBundleEntity> bundles = new ArrayList<>();

    // One-to-many relationship with rule temporal links
    @OneToMany(mappedBy = "validationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SuppressWarnings("java:S1948") // JPA managed relationship, not serialized directly
    private List<RuleTemporalLinkEntity> temporalLinks = new ArrayList<>();

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