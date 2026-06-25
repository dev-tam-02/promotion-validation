package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TranslationEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.TranslationJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorOptionLabelPort;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA adapter for {@link OperatorOptionLabelPort}. Loads the {@code OPERATOR_OPTION}
 * rows from the generic {@code translations} table in one query and assembles the
 * {@code option_id -> field -> locale -> value} index the rule-builder catalog
 * resolves operator-option display text from (changelog 082 migrated the inline
 * {@code operator_options} i18n columns into this table).
 */
@Component
@ConditionalOnPromixJpa
public class OperatorOptionLabelJpaAdapter implements OperatorOptionLabelPort {

    /** Entity type discriminator used in the generic translations table. */
    public static final String ENTITY_TYPE = "OPERATOR_OPTION";
    /** Display-name field (maps to former {@code name}/{@code name_vi}). */
    public static final String FIELD_NAME = "name";
    /** Description field (maps to former {@code description}/{@code description_vi}). */
    public static final String FIELD_DESCRIPTION = "description";
    /** Input-label field (maps to former {@code label_en}/{@code label_vi}). */
    public static final String FIELD_LABEL = "label";
    /** Input-placeholder field (maps to former {@code placeholder_en}/{@code placeholder_vi}). */
    public static final String FIELD_PLACEHOLDER = "placeholder";

    private final TranslationJpaRepository translationRepository;

    public OperatorOptionLabelJpaAdapter(TranslationJpaRepository translationRepository) {
        this.translationRepository = translationRepository;
    }

    @Override
    public Map<String, Map<String, Map<String, String>>> loadOptionLabels() {
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
