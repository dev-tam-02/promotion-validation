package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TranslationEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.TranslationJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorLabelPort;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA adapter for {@link OperatorLabelPort}. Loads the {@code CONDITION_OPERATOR}
 * rows from the generic {@code translations} table (both {@code label} and
 * {@code label_date} fields) in two queries and assembles the
 * {@code entity_key -> field -> locale -> value} index the catalog resolves from.
 */
@Component
@ConditionalOnPromixJpa
public class OperatorLabelJpaAdapter implements OperatorLabelPort {

    /** Entity type discriminator used in the generic translations table. */
    public static final String ENTITY_TYPE = "CONDITION_OPERATOR";
    /** Default-wording translatable field. */
    public static final String FIELD_LABEL = "label";
    /** Override wording for DATE/DATETIME fields. */
    public static final String FIELD_LABEL_DATE = "label_date";

    private final TranslationJpaRepository translationRepository;

    public OperatorLabelJpaAdapter(TranslationJpaRepository translationRepository) {
        this.translationRepository = translationRepository;
    }

    @Override
    public Map<String, Map<String, Map<String, String>>> loadOperatorLabels() {
        Map<String, Map<String, Map<String, String>>> byKey = new HashMap<>();
        index(byKey, FIELD_LABEL);
        index(byKey, FIELD_LABEL_DATE);
        return byKey;
    }

    private void index(Map<String, Map<String, Map<String, String>>> byKey, String field) {
        for (TranslationEntity t : translationRepository.findByEntityTypeAndField(ENTITY_TYPE, field)) {
            byKey
                    .computeIfAbsent(t.getEntityKey(), k -> new HashMap<>())
                    .computeIfAbsent(field, f -> new HashMap<>())
                    .put(t.getLocale(), t.getValue());
        }
    }
}
