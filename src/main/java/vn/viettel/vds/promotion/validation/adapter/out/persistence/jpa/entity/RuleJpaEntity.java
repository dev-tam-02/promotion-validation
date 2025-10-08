package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JPA entity for validation rules
 */
@Entity
@Table(name = "validation_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleJpaEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "rule_code", unique = true, nullable = false)
    private String ruleCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "expression", columnDefinition = "TEXT")
    private String expression;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "latest_version")
    private Integer latestVersion;

    @Column(name = "logic", length = 50)
    private String logic;

    @ElementCollection
    @CollectionTable(
            name = "rule_configuration",
            joinColumns = @JoinColumn(name = "rule_id")
    )
    @MapKeyColumn(name = "config_key")
    @Column(name = "config_value", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, String> configuration = new HashMap<>();

    @ElementCollection
    @CollectionTable(
            name = "rule_target_segments",
            joinColumns = @JoinColumn(name = "rule_id")
    )
    @Column(name = "segment")
    @Builder.Default
    private List<String> targetSegments = new ArrayList<>();

    @Column(name = "campaign_id")
    private String campaignId;

    @Column(name = "rule_set_id")
    private String ruleSetId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (priority == null) {
            priority = 100;
        }
    }
}