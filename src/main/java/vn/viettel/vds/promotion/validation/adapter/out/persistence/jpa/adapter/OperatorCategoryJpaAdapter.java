package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promix.platform.data.jpa.autoconfigure.condition.ConditionalOnPromixJpa;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorCategoryEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper.OperatorCategoryMapper;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.OperatorCategoryJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.OperatorOptionJpaRepository;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorCategoryLabelPort;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorCategoryPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorOptionLabelPort;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter.OperatorOptionLabelJpaAdapter.FIELD_DESCRIPTION;
import static vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter.OperatorOptionLabelJpaAdapter.FIELD_LABEL;
import static vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter.OperatorOptionLabelJpaAdapter.FIELD_NAME;
import static vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter.OperatorOptionLabelJpaAdapter.FIELD_PLACEHOLDER;

/**
 * JPA adapter implementation for OperatorCategory persistence.
 *
 * <p>Category and operator-option display text (name / description, plus the
 * option's input label / placeholder) no longer lives on the
 * {@code operator_categories} / {@code operator_options} columns — changelogs 082
 * (options) and 083 (categories) moved it into the generic {@code translations}
 * table. Every read path here overlays the localized values back onto the mapped
 * domain models via {@link OperatorOptionLabelPort} / {@link OperatorCategoryLabelPort},
 * keeping the domain model and downstream response shape identical to before.
 */
@Component
@ConditionalOnPromixJpa
public class OperatorCategoryJpaAdapter implements OperatorCategoryPersistencePort {

    private static final String EN = "en";
    private static final String VI = "vi";

    private final OperatorCategoryJpaRepository repository;
    private final OperatorOptionJpaRepository optionRepository;
    private final OperatorCategoryMapper mapper;
    private final OperatorOptionLabelPort operatorOptionLabelPort;
    private final OperatorCategoryLabelPort operatorCategoryLabelPort;

    public OperatorCategoryJpaAdapter(OperatorCategoryJpaRepository repository,
                                      OperatorOptionJpaRepository optionRepository,
                                      OperatorCategoryMapper mapper,
                                      OperatorOptionLabelPort operatorOptionLabelPort,
                                      OperatorCategoryLabelPort operatorCategoryLabelPort) {
        this.repository = repository;
        this.optionRepository = optionRepository;
        this.mapper = mapper;
        this.operatorOptionLabelPort = operatorOptionLabelPort;
        this.operatorCategoryLabelPort = operatorCategoryLabelPort;
    }

    /** Both translation indexes loaded once per request (option labels + category labels). */
    private record CatalogLabels(
            Map<String, Map<String, Map<String, String>>> options,
            Map<String, Map<String, Map<String, String>>> categories) {
    }

    private CatalogLabels loadLabels() {
        return new CatalogLabels(
                operatorOptionLabelPort.loadOptionLabels(),
                operatorCategoryLabelPort.loadCategoryLabels());
    }

    @Override
    public List<OperatorCategory> findAllActiveWithOptions() {
        CatalogLabels labels = loadLabels();
        return repository.findAllWithOptions()
                .stream()
                .map(mapper::toDomain)
                .map(category -> applyTranslations(category, labels))
                .toList();
    }

    @Override
    public Optional<OperatorOption> findOptionForResolution(String ruleId) {
        // id (PK) → code → operatorName, all active-agnostic so disabled rules
        // referenced by older persisted conditions still resolve their values.
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Map<String, Map<String, String>>> optionLabels = operatorOptionLabelPort.loadOptionLabels();
        return optionRepository.findById(ruleId)
                .or(() -> optionRepository.findFirstByCodeOrderByDisplayOrderAsc(ruleId))
                .or(() -> optionRepository.findFirstByOperatorNameOrderByDisplayOrderAsc(ruleId))
                .map(entity -> mapper.mapOption(entity, objectMapper))
                .map(option -> applyTranslations(option, optionLabels));
    }

    @Override
    public List<OperatorCategory> findAll() {
        CatalogLabels labels = loadLabels();
        return repository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(mapper::toDomain)
                .map(category -> applyTranslations(category, labels))
                .toList();
    }

    @Override
    public Optional<OperatorCategory> findById(String id) {
        CatalogLabels labels = loadLabels();
        return repository.findById(id).map(mapper::toDomain).map(category -> applyTranslations(category, labels));
    }

    @Override
    public Optional<OperatorCategory> findByCode(String code) {
        CatalogLabels labels = loadLabels();
        return repository.findByCode(code).map(mapper::toDomain).map(category -> applyTranslations(category, labels));
    }

    @Override
    public Optional<OperatorCategory> findByCodeWithOptions(String code) {
        CatalogLabels labels = loadLabels();
        return repository.findByCode(code)
                .map(entity -> {
                    // Force load options
                    entity.getOptions().forEach(opt -> { /* trigger lazy load */ });
                    return mapper.toDomain(entity);
                })
                .map(category -> applyTranslations(category, labels));
    }

    @Override
    public List<OperatorCategory> findMetadataCategories() {
        CatalogLabels labels = loadLabels();
        return repository.findByMetadataCategoryTrueAndActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(mapper::toDomain)
                .map(category -> applyTranslations(category, labels))
                .toList();
    }

    @Override
    public List<OperatorCategory> findNonMetadataCategories() {
        CatalogLabels labels = loadLabels();
        return repository.findByMetadataCategoryFalseAndActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(mapper::toDomain)
                .map(category -> applyTranslations(category, labels))
                .toList();
    }

    @Override
    public OperatorCategory save(OperatorCategory category) {
        OperatorCategoryEntity entity = mapper.toEntity(category);
        OperatorCategoryEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    /**
     * Overlay translations onto a category: its own name / description, plus every
     * option's name / description / input label / placeholder. Fields with no
     * translation row are left as mapped (null after the columns were dropped).
     */
    private OperatorCategory applyTranslations(OperatorCategory category, CatalogLabels labels) {
        if (category == null) {
            return null;
        }
        OperatorCategory.OperatorCategoryBuilder builder = category.toBuilder();

        if (category.getOptions() != null && !category.getOptions().isEmpty()) {
            builder.options(category.getOptions().stream()
                    .map(option -> applyTranslations(option, labels.options()))
                    .toList());
        }

        Map<String, Map<String, String>> catFields = labels.categories().get(category.getId());
        if (catFields != null) {
            builder
                    .name(locale(catFields, FIELD_NAME, EN))
                    .nameVi(locale(catFields, FIELD_NAME, VI))
                    .description(locale(catFields, FIELD_DESCRIPTION, EN))
                    .descriptionVi(locale(catFields, FIELD_DESCRIPTION, VI));
        }
        return builder.build();
    }

    /**
     * Replace an option's i18n fields (name / description / input label / placeholder,
     * each en + vi) with values from the {@code translations} table. Options with no
     * translation rows pass through unchanged.
     */
    private OperatorOption applyTranslations(OperatorOption option,
                                             Map<String, Map<String, Map<String, String>>> labels) {
        if (option == null) {
            return null;
        }
        Map<String, Map<String, String>> byField = labels.get(option.getId());
        if (byField == null) {
            return option;
        }
        return option.toBuilder()
                .name(locale(byField, FIELD_NAME, EN))
                .nameVi(locale(byField, FIELD_NAME, VI))
                .description(locale(byField, FIELD_DESCRIPTION, EN))
                .descriptionVi(locale(byField, FIELD_DESCRIPTION, VI))
                .labelEn(locale(byField, FIELD_LABEL, EN))
                .labelVi(locale(byField, FIELD_LABEL, VI))
                .placeholderEn(locale(byField, FIELD_PLACEHOLDER, EN))
                .placeholderVi(locale(byField, FIELD_PLACEHOLDER, VI))
                .build();
    }

    /** Read one (field, locale) value from a per-entity translation index, or null. */
    private String locale(Map<String, Map<String, String>> byField, String field, String locale) {
        Map<String, String> byLocale = byField.get(field);
        return byLocale == null ? null : byLocale.get(locale);
    }
}
