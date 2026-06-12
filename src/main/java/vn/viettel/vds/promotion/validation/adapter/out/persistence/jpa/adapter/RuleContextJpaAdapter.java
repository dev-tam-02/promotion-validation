package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleContextEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.TranslationEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleContextJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.TranslationJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.RuleContextPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.RuleContextOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JPA adapter for {@link RuleContextPersistencePort}. Loads active contexts from
 * {@code rule_contexts} and attaches their localized names from {@code translations}
 * (entity_type = {@value #ENTITY_TYPE}, field = {@value #FIELD_NAME}) in a single
 * extra query, assembling {@link RuleContextOption} read models.
 */
@Component
@ConditionalOnPromixJpa
public class RuleContextJpaAdapter implements RuleContextPersistencePort {

    /** Entity type discriminator used in the generic translations table. */
    public static final String ENTITY_TYPE = "RULE_CONTEXT";
    /** Translatable field name. */
    public static final String FIELD_NAME = "name";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final RuleContextJpaRepository contextRepository;
    private final TranslationJpaRepository translationRepository;

    public RuleContextJpaAdapter(RuleContextJpaRepository contextRepository,
                                 TranslationJpaRepository translationRepository) {
        this.contextRepository = contextRepository;
        this.translationRepository = translationRepository;
    }

    @Override
    public List<RuleContextOption> findActiveContexts() {
        Map<String, Map<String, String>> namesByCode = loadNames();

        return contextRepository.findByStatusOrderByCodeAsc(STATUS_ACTIVE).stream()
                .map(entity -> toOption(entity, namesByCode))
                .toList();
    }

    @Override
    public boolean isActiveContext(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return contextRepository.existsByCodeIgnoreCaseAndStatus(code, STATUS_ACTIVE);
    }

    private Map<String, Map<String, String>> loadNames() {
        Map<String, Map<String, String>> namesByCode = new HashMap<>();
        for (TranslationEntity t : translationRepository.findByEntityTypeAndField(ENTITY_TYPE, FIELD_NAME)) {
            namesByCode
                    .computeIfAbsent(t.getEntityKey(), k -> new HashMap<>())
                    .put(t.getLocale(), t.getValue());
        }
        return namesByCode;
    }

    private RuleContextOption toOption(RuleContextEntity entity,
                                       Map<String, Map<String, String>> namesByCode) {
        return RuleContextOption.builder()
                .code(entity.getCode())
                .status(entity.getStatus())
                .names(namesByCode.getOrDefault(entity.getCode(), Map.of()))
                .build();
    }
}
