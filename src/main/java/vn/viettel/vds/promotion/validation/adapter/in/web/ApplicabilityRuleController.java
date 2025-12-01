package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ApplicabilityRuleResponse;
import vn.viettel.vds.promotion.validation.application.service.ApplicabilityRuleService;

import java.util.List;

/**
 * Controller for managing applicability rules.
 * Provides endpoints to query applicability rules by object type and object ID.
 */
@RestController
@Validated
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/api/v1/applicability-rules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Applicability Rules", description = "API for querying applicability rules")
public class ApplicabilityRuleController {

    private final ApplicabilityRuleService applicabilityRuleService;

    /**
     * Get applicability rules by object type and object ID.
     * Returns a list of applicability rules (included/excluded products, collections, SKUs)
     * that match the given object type and object ID.
     *
     * @param objectType the object type (COLLECTION, PRODUCT, SKU)
     * @param objectId   the object identifier
     * @return list of applicability rules
     */
    @Operation(
            summary = "Get applicability rules by object",
            description = "Retrieve all applicability rules (included/excluded) for a specific object type and ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved applicability rules"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    @GetMapping
    public List<ApplicabilityRuleResponse> getApplicabilityRules(
            @Parameter(description = "Object type (COLLECTION, PRODUCT, SKU)", example = "PRODUCT", required = true)
            @RequestParam
            @NotBlank(message = "objectType is required")
            String objectType,

            @Parameter(description = "Object identifier", example = "PROD-001", required = true)
            @RequestParam
            @NotBlank(message = "objectId is required")
            String objectId) {

        log.info("Getting applicability rules for objectType={}, objectId={}", objectType, objectId);

        List<ApplicabilityRuleResponse> rules = applicabilityRuleService
                .findByObjectTypeAndObjectId(objectType.toUpperCase(), objectId);

        log.info("Found {} applicability rules for objectType={}, objectId={}",
                rules.size(), objectType, objectId);

        return rules;
    }
}
