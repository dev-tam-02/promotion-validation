package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity for the service-wide {@code translations} table.
 *
 * <p>Generic, polymorphic i18n store: one row per
 * {@code (entity_type, entity_key, field, locale)}. Localizes any entity in the
 * service (rule_contexts, operator_categories, reason_codes, ...) without
 * per-entity translation tables or inline {@code *_vi} columns.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "translations")
@IdClass(TranslationId.class)
public class TranslationEntity {

    @Id
    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Id
    @Column(name = "entity_key", length = 100, nullable = false)
    private String entityKey;

    @Id
    @Column(name = "field", length = 50, nullable = false)
    private String field;

    @Id
    @Column(name = "locale", length = 10, nullable = false)
    private String locale;

    @Column(name = "value", columnDefinition = "TEXT", nullable = false)
    private String value;
}
