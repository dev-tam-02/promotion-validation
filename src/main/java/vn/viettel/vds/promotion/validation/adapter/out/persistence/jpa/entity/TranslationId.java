package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for {@link TranslationEntity}:
 * {@code (entity_type, entity_key, field, locale)}.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class TranslationId implements Serializable {

    private String entityType;
    private String entityKey;
    private String field;
    private String locale;
}
