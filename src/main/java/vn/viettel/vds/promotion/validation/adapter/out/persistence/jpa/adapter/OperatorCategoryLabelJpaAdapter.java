package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TranslationEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.TranslationJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorCategoryLabelPort;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA adapter for {@link OperatorCategoryLabelPort}. Loads the {@code OPERATOR_CATEGORY}
 * rows from the generic {@code translations} table in one query and assembles the
 * {@code category_id -> field -> locale -> value} index the rule-builder catalog
 * resolves category display text from (changelog 083 migrated the inline
 * {@code operator_categories} i18n columns into this table).
 */
@Component
@ConditionalOnPromixJpa
public class OperatorCategoryLabelJpaAdapter implements OperatorCategoryLabelPort {

    /** Entity type discriminator used in the generic translations table. */
    public static final String ENTITY_TYPE = "OPERATOR_CATEGORY";

    private final TranslationJpaRepository translationRepository;

    public OperatorCategoryLabelJpaAdapter(TranslationJpaRepository translationRepository) {
        this.translationRepository = translationRepository;
    }

    @Override
    public Map<String, Map<String, Map<String, String>>> loadCategoryLabels() {
        Map<String, Map<String, Map<String, String>>> byId = new HashMap<>();
        for (TranslationEntity t : translationRepository.findByEntityType(ENTITY_TYPE)) {
            byId
                    .computeIfAbsent(t.getEntityKey(), k -> new HashMap<>())
                    .computeIfAbsent(t.getField(), f -> new HashMap<>())
                    .put(t.getLocale(), t.getValue());
        }
        return byId;
    }
}
