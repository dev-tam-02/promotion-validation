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
 * JPA entity for validation rules mapped to validation_rules table.
 *
 * Schema columns (from 001-create-validation-rule-engine-schema.yaml):
 * - id: varchar(36) - Primary key
 * - code: varchar(100) - Rule code identifier
 * - name: varchar(200) - Rule name
 * - state: varchar(20) - Rule state (draft, published, archived)
 * - rule_version: bigint - Rule version number
 * - logic: varchar(50) - Root logic for rule evaluation
 * - dsl: text - DSL snapshot in JSON format
 * - published_at: timestamp - When rule was published
 * - published_by: varchar(50) - Who published the rule
 * - created_at: timestamp - Creation timestamp
 * - updated_at: timestamp - Last update timestamp
 * - created_by: varchar(100) - Creator
 * - updated_by: varchar(100) - Last updater
 *
 * Related tables (ElementCollection):
 * - rule_configuration: Stores configuration key-value pairs
 * - rule_target_segments: Stores target segments list
 */
@Entity
@Table(name = "validation_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleJpaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "state", length = 20, nullable = false)
    private String state;

    @Column(name = "rule_version", nullable = false)
    private Long ruleVersion;

    @Column(name = "logic", length = 50)
    private String logic;

    @Column(name = "dsl", columnDefinition = "TEXT")
    private String dsl;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by", length = 50)
    private String publishedBy;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (ruleVersion == null) {
            ruleVersion = 1L;
        }
        if (state == null) {
            state = "DRAFT";
        }
    }
}