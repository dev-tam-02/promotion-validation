package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JPA entity for validation rules mapped to the {@code validation_rules} table.
 *
 * <p>Extends {@link SoftDeleteEntity} so the rule participates in the promix-starter
 * soft-delete convention: deleting a rule archives it to the {@code validation_rules_deleted}
 * shadow table instead of a hard {@code DELETE}. The actual shadow move is performed by
 * {@code RuleJpaAdapter.softDelete()} (explicit INSERT...SELECT) rather than the generic
 * reflection-based {@code BaseRepository.softDelete()}, because the two {@code @ElementCollection}
 * fields below ({@code configuration}, {@code targetSegments}) are not columns of the main
 * table and would corrupt the reflection-derived column list.</p>
 *
 * <p>{@code id}, {@code version} and the audit columns ({@code created_at/by},
 * {@code updated_at/by}) are inherited from {@link com.promix.platform.jpa.entity.BaseEntity}.</p>
 *
 * <p>Related tables (ElementCollection):</p>
 * <ul>
 *   <li>{@code rule_configuration} — configuration key-value pairs</li>
 *   <li>{@code rule_target_segments} — target segments list</li>
 * </ul>
 */
@Entity
@Table(name = "validation_rules")
@Getter
@Setter
@NoArgsConstructor
public class RuleJpaEntity extends SoftDeleteEntity implements Persistable<String> {

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "rule_version", nullable = false)
    private Long ruleVersion;

    @Column(name = "logic", length = 50)
    private String logic;

    @Column(name = "dsl", columnDefinition = "TEXT")
    private String dsl;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by", length = 100)
    private String publishedBy;

    @Column(name = "bundle_hash", length = 200)
    private String bundleHash;

    @Column(name = "context", length = 100)
    private String context;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "fallback_error_message", length = 500)
    private String fallbackErrorMessage;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @ElementCollection
    @CollectionTable(
            name = "rule_configuration",
            joinColumns = @JoinColumn(name = "rule_id")
    )
    @MapKeyColumn(name = "config_key")
    @Column(name = "config_value", columnDefinition = "TEXT")
    private Map<String, String> configuration = new HashMap<>();

    @ElementCollection
    @CollectionTable(
            name = "rule_target_segments",
            joinColumns = @JoinColumn(name = "rule_id")
    )
    @Column(name = "segment")
    private List<String> targetSegments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (getId() == null) {
            setId(java.util.UUID.randomUUID().toString());
        }
        if (ruleVersion == null) {
            ruleVersion = 1L;
        }
    }

    /**
     * BUG-024 fix: Spring Data JPA's default {@code SimpleJpaRepository.save()} chooses
     * between {@code persist()} and {@code merge()} via {@link #isNew()}. Without this
     * override, an entity assembled with a non-null UUIDv7 {@code id} and a default
     * {@code version} is treated as <i>existing</i>, so {@code save()} issues an UPDATE
     * that matches zero rows → {@code StaleObjectStateException}.
     * <p>
     * We treat the entity as new whenever the {@code @Version} field is null. The save
     * adapters leave {@code version} null for freshly auto-generated rules, so Spring
     * Data routes them through {@code persist()} (INSERT). After persist the version is
     * assigned by Hibernate; subsequent updates load the entity with version != null, so
     * {@code isNew()} returns false and the optimistic lock check still applies.
     */
    @Override
    @Transient
    public boolean isNew() {
        return getVersion() == null;
    }
}
