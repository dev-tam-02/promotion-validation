package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TranslationEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TranslationId;

import java.util.List;

/**
 * JPA repository for the service-wide {@code translations} table.
 */
@ConditionalOnPromixJpa
@Repository
public interface TranslationJpaRepository extends JpaRepository<TranslationEntity, TranslationId> {

    /**
     * Find every translation for an entity type + field across all keys and locales.
     * Used to bulk-load display names for a lookup list in one query.
     */
    List<TranslationEntity> findByEntityTypeAndField(String entityType, String field);

    /**
     * Find every translation for an entity type across all keys, fields and locales.
     * Used to bulk-load a multi-field entity's i18n (e.g. operator_options: name,
     * description, label, placeholder) for a whole catalog request in one query.
     */
    List<TranslationEntity> findByEntityType(String entityType);
}
