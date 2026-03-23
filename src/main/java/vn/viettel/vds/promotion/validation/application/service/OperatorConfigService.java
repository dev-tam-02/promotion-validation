package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.MetadataSchemaPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorCategoryPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.MetadataSchema;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing operator configuration for UI rule builder.
 * Provides methods to query categories, options, and metadata schemas.
 */
@Service
@Transactional(readOnly = true)
public class OperatorConfigService {

    private static final Logger logger = LoggerFactory.getLogger(OperatorConfigService.class);
    private static final String OP_EQUALS = "equals";

    private final OperatorCategoryPersistencePort categoryPort;
    private final MetadataSchemaPersistencePort metadataSchemaPort;

    public OperatorConfigService(
            OperatorCategoryPersistencePort categoryPort,
            MetadataSchemaPersistencePort metadataSchemaPort) {
        this.categoryPort = categoryPort;
        this.metadataSchemaPort = metadataSchemaPort;
    }

    /**
     * Get all active categories with their options for UI.
     * For metadata categories, options are dynamically generated from schema.
     *
     * @param tenantId the tenant identifier
     * @return list of categories with options
     */
    public List<OperatorCategory> getAllCategoriesWithOptions(String tenantId) {
        logger.debug("Getting all categories with options for tenant: {}", tenantId);

        List<OperatorCategory> categories = categoryPort.findAllActiveWithOptions();

        // Enhance metadata categories with dynamic options from schema
        return categories.stream()
                .map(category -> {
                    if (category.isMetadataCategory()) {
                        return enrichMetadataCategory(category, tenantId);
                    }
                    return category;
                })
                .toList();
    }

    /**
     * Get category by code with its options.
     *
     * @param code     the category code
     * @param tenantId the tenant identifier
     * @return optional category
     */
    public Optional<OperatorCategory> getCategoryByCode(String code, String tenantId) {
        logger.debug("Getting category by code: {} for tenant: {}", code, tenantId);

        return categoryPort.findByCodeWithOptions(code)
                .map(category -> {
                    if (category.isMetadataCategory()) {
                        return enrichMetadataCategory(category, tenantId);
                    }
                    return category;
                });
    }

    /**
     * Get options for a specific category.
     *
     * @param categoryCode the category code
     * @param tenantId     the tenant identifier
     * @return list of options
     */
    public List<OperatorOption> getOptionsForCategory(String categoryCode, String tenantId) {
        logger.debug("Getting options for category: {} tenant: {}", categoryCode, tenantId);

        return getCategoryByCode(categoryCode, tenantId)
                .map(OperatorCategory::getOptions)
                .orElse(Collections.emptyList());
    }

    /**
     * Get metadata schema fields for a schema type.
     *
     * @param tenantId   the tenant identifier
     * @param schemaType the schema type (customer, order, redemption, custom_event)
     * @return list of metadata schema fields
     */
    public List<MetadataSchema> getMetadataSchema(String tenantId, String schemaType) {
        logger.debug("Getting metadata schema for tenant: {}, type: {}", tenantId, schemaType);
        return metadataSchemaPort.findActiveByTenantIdAndSchemaType(tenantId, schemaType);
    }

    /**
     * Check if metadata schema has any fields for the given type.
     *
     * @param tenantId   the tenant identifier
     * @param schemaType the schema type
     * @return true if schema has fields
     */
    public boolean hasMetadataFields(String tenantId, String schemaType) {
        return metadataSchemaPort.countByTenantIdAndSchemaType(tenantId, schemaType) > 0;
    }

    /**
     * Enrich metadata category with dynamic options from schema.
     * If no schema fields exist, the category will have empty options
     * (UI should show "Add field" button).
     */
    private OperatorCategory enrichMetadataCategory(OperatorCategory category, String tenantId) {
        if (category.getMetadataSchemaType() == null) {
            return category;
        }

        List<MetadataSchema> schemaFields = metadataSchemaPort.findActiveByTenantIdAndSchemaType(
                tenantId, category.getMetadataSchemaType());

        if (schemaFields.isEmpty()) {
            // No schema fields - category will be shown with "Add field" button
            return category.toBuilder()
                    .options(Collections.emptyList())
                    .build();
        }

        // Convert schema fields to operator options
        List<OperatorOption> dynamicOptions = schemaFields.stream()
                .map(field -> createOptionFromSchemaField(category, field))
                .toList();

        return category.toBuilder()
                .options(dynamicOptions)
                .build();
    }

    /**
     * Create an operator option from a metadata schema field.
     */
    private OperatorOption createOptionFromSchemaField(OperatorCategory category, MetadataSchema field) {
        String operatorName = buildMetadataOperatorName(category.getMetadataSchemaType(), field.getFieldKey());

        OperatorOption.ComparisonType comparisonType = switch (field.getFieldType()) {
            case NUMBER -> OperatorOption.ComparisonType.RANGE;
            case BOOLEAN -> OperatorOption.ComparisonType.BOOLEAN;
            default -> OperatorOption.ComparisonType.SINGLE;
        };

        List<String> comparators = switch (field.getFieldType()) {
            case NUMBER -> List.of(OP_EQUALS, "not_equals", "gte", "lte", "between");
            case BOOLEAN -> List.of(OP_EQUALS);
            default -> List.of(OP_EQUALS, "not_equals", "in", "not_in");
        };

        OperatorOption.ValueType valueType = switch (field.getFieldType()) {
            case NUMBER -> OperatorOption.ValueType.NUMBER;
            case BOOLEAN -> OperatorOption.ValueType.BOOLEAN;
            case DATE -> OperatorOption.ValueType.DATE;
            default -> OperatorOption.ValueType.STRING;
        };

        OperatorOption.ValueSource valueSource = (field.getAvailableValues() != null && !field.getAvailableValues().isEmpty())
                ? OperatorOption.ValueSource.SELECT
                : OperatorOption.ValueSource.INPUT;

        List<OperatorOption.ValueOption> valueOptions = (field.getAvailableValues() != null)
                ? field.getAvailableValues().stream()
                        .map(v -> OperatorOption.ValueOption.builder().value(v).label(v).build())
                        .toList()
                : Collections.emptyList();

        return OperatorOption.builder()
                .id(field.getId())
                .categoryId(category.getId())
                .code(field.getFieldKey())
                .name(field.getFieldName())
                .displayOrder(field.getDisplayOrder())
                .description("Metadata field: " + field.getFieldName())
                .operatorName(operatorName)
                .operatorVersion(1)
                .comparisonType(comparisonType)
                .availableComparators(comparators)
                .defaultComparator(comparators.get(0))
                .valueType(valueType)
                .valueSource(valueSource)
                .valueOptions(valueOptions)
                .active(true)
                .createdAt(field.getCreatedAt())
                .updatedAt(field.getUpdatedAt())
                .build();
    }

    /**
     * Build operator name for metadata field.
     * Format: {schemaType}.metadata.{fieldKey}.equals
     */
    private String buildMetadataOperatorName(String schemaType, String fieldKey) {
        return schemaType + ".metadata." + fieldKey + ".equals";
    }
}
