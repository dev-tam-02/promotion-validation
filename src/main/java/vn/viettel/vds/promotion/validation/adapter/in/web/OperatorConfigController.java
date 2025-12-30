package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.*;
import vn.viettel.vds.promotion.validation.application.service.MetadataSchemaService;
import vn.viettel.vds.promotion.validation.application.service.OperatorConfigService;
import vn.viettel.vds.promotion.validation.domain.model.MetadataSchema;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.List;

/**
 * Controller for operator configuration API.
 * Provides endpoints for UI to configure validation rules.
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/operator-config")
public class OperatorConfigController {

    private static final Logger logger = LoggerFactory.getLogger(OperatorConfigController.class);
    private static final String DEFAULT_TENANT = "default";

    private final OperatorConfigService operatorConfigService;
    private final MetadataSchemaService metadataSchemaService;

    public OperatorConfigController(
            OperatorConfigService operatorConfigService,
            MetadataSchemaService metadataSchemaService) {
        this.operatorConfigService = operatorConfigService;
        this.metadataSchemaService = metadataSchemaService;
    }

    /**
     * Get all categories with their options for UI rule builder.
     * For metadata categories, options are dynamically generated from schema.
     *
     * @param tenantId optional tenant ID (defaults to "default")
     * @return list of categories with options
     */
    @GetMapping("/categories")
    public List<OperatorCategoryResponse> getAllCategories(
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = DEFAULT_TENANT) String tenantId) {
        logger.debug("Getting all categories for tenant: {}", tenantId);

        List<OperatorCategory> categories = operatorConfigService.getAllCategoriesWithOptions(tenantId);
        return categories.stream()
                .map(OperatorCategoryResponse::from)
                .toList();
    }

    /**
     * Get options for a specific category.
     *
     * @param categoryCode the category code
     * @param tenantId     optional tenant ID
     * @return list of options
     */
    @GetMapping("/categories/{categoryCode}/options")
    public List<OperatorOptionResponse> getOptionsForCategory(
            @PathVariable String categoryCode,
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = DEFAULT_TENANT) String tenantId) {
        logger.debug("Getting options for category: {} tenant: {}", categoryCode, tenantId);

        List<OperatorOption> options = operatorConfigService.getOptionsForCategory(categoryCode, tenantId);
        return options.stream()
                .map(OperatorOptionResponse::from)
                .toList();
    }

    /**
     * Get metadata schema for a schema type.
     *
     * @param schemaType the schema type (customer, order, redemption, custom_event)
     * @param tenantId   optional tenant ID
     * @return list of schema fields
     */
    @GetMapping("/metadata-schema/{schemaType}")
    public List<MetadataSchemaResponse> getMetadataSchema(
            @PathVariable String schemaType,
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = DEFAULT_TENANT) String tenantId) {
        logger.debug("Getting metadata schema for type: {} tenant: {}", schemaType, tenantId);

        List<MetadataSchema> schemas = metadataSchemaService.getSchemaFields(tenantId, schemaType);
        return schemas.stream()
                .map(MetadataSchemaResponse::from)
                .toList();
    }

    /**
     * Create a new metadata schema field.
     *
     * @param schemaType the schema type
     * @param request    the create request
     * @param tenantId   optional tenant ID
     * @return created schema field
     */
    @PostMapping("/metadata-schema/{schemaType}")
    @ResponseStatus(HttpStatus.CREATED)
    public MetadataSchemaResponse createMetadataSchemaField(
            @PathVariable String schemaType,
            @Valid @RequestBody CreateMetadataSchemaRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = DEFAULT_TENANT) String tenantId) {
        logger.info("Creating metadata schema field for type: {} tenant: {}", schemaType, tenantId);

        try {
            MetadataSchemaService.CreateSchemaFieldRequest serviceRequest =
                    new MetadataSchemaService.CreateSchemaFieldRequest(
                            request.fieldKey(),
                            request.fieldName(),
                            mapFieldType(request.fieldType()),
                            request.availableValues(),
                            request.required()
                    );

            MetadataSchema created = metadataSchemaService.createSchemaField(tenantId, schemaType, serviceRequest);
            return MetadataSchemaResponse.from(created);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Update a metadata schema field.
     *
     * @param schemaType the schema type
     * @param fieldKey   the field key
     * @param request    the update request
     * @param tenantId   optional tenant ID
     * @return updated schema field
     */
    @PutMapping("/metadata-schema/{schemaType}/{fieldKey}")
    public MetadataSchemaResponse updateMetadataSchemaField(
            @PathVariable String schemaType,
            @PathVariable String fieldKey,
            @Valid @RequestBody UpdateMetadataSchemaRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = DEFAULT_TENANT) String tenantId) {
        logger.info("Updating metadata schema field: {}.{} tenant: {}", schemaType, fieldKey, tenantId);

        try {
            MetadataSchemaService.UpdateSchemaFieldRequest serviceRequest =
                    new MetadataSchemaService.UpdateSchemaFieldRequest(
                            request.fieldName(),
                            request.fieldType() != null ? mapFieldType(request.fieldType()) : null,
                            request.availableValues(),
                            request.required(),
                            request.displayOrder()
                    );

            MetadataSchema updated = metadataSchemaService.updateSchemaField(tenantId, schemaType, fieldKey, serviceRequest);
            return MetadataSchemaResponse.from(updated);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Delete a metadata schema field.
     *
     * @param schemaType the schema type
     * @param fieldKey   the field key
     * @param tenantId   optional tenant ID
     */
    @DeleteMapping("/metadata-schema/{schemaType}/{fieldKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMetadataSchemaField(
            @PathVariable String schemaType,
            @PathVariable String fieldKey,
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = DEFAULT_TENANT) String tenantId) {
        logger.info("Deleting metadata schema field: {}.{} tenant: {}", schemaType, fieldKey, tenantId);

        try {
            metadataSchemaService.deleteSchemaField(tenantId, schemaType, fieldKey);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Toggle active status of a metadata schema field.
     *
     * @param schemaType the schema type
     * @param fieldKey   the field key
     * @param active     new active status
     * @param tenantId   optional tenant ID
     * @return updated schema field
     */
    @PatchMapping("/metadata-schema/{schemaType}/{fieldKey}/active")
    public MetadataSchemaResponse toggleMetadataSchemaFieldActive(
            @PathVariable String schemaType,
            @PathVariable String fieldKey,
            @RequestParam boolean active,
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = DEFAULT_TENANT) String tenantId) {
        logger.info("Toggling metadata schema field active: {}.{} = {} tenant: {}", schemaType, fieldKey, active, tenantId);

        try {
            MetadataSchema updated = metadataSchemaService.toggleActive(tenantId, schemaType, fieldKey, active);
            return MetadataSchemaResponse.from(updated);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private MetadataSchema.FieldType mapFieldType(CreateMetadataSchemaRequest.FieldType type) {
        return MetadataSchema.FieldType.valueOf(type.name());
    }

    private MetadataSchema.FieldType mapFieldType(UpdateMetadataSchemaRequest.FieldType type) {
        return MetadataSchema.FieldType.valueOf(type.name());
    }
}
