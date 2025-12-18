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
 * Provides endpoints to query applicability rules by assignment's entity type and entity ID.
 * <p>
 * Flow:
 * 1. Input: objectType (assignment's entityType), objectId (assignment's entityId)
 * 2. Find assignment(s) matching entityType and entityId
 * 3. Return all applicability rules (included/excluded products, collections, SKUs) from those assignments
 */
@RestController
@Validated
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/api/v1/applicability-rules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Applicability Rules", description = "API for querying applicability rules by assignment")
public class ApplicabilityRuleController {

    private final ApplicabilityRuleService applicabilityRuleService;

    /**
     * Get applicability rules by assignment's entity type and entity ID.
     * <p>
     * Flow:
     * 1. Find assignment(s) by entityType (objectType) and entityId (objectId)
     * 2. Return all applicability rules from those assignments
     *
     * @param objectType the assignment's entity type (e.g., CAMPAIGN, PROMOTION, VOUCHER)
     * @param objectId   the assignment's entity identifier (e.g., campaign ID)
     * @return list of applicability rules (included/excluded products, collections, SKUs)
     */
    @Operation(
            summary = "Get applicability rules by assignment",
            description = "Retrieve all applicability rules (included/excluded products, collections, SKUs) " +
                    "for assignments matching the given entity type and entity ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved applicability rules"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    @GetMapping
    public List<ApplicabilityRuleResponse> getApplicabilityRules(
            @Parameter(description = "Assignment's entity type (e.g., CAMPAIGN, PROMOTION, VOUCHER)",
                    example = "CAMPAIGN", required = true)
            @RequestParam
            @NotBlank(message = "objectType is required")
            String objectType,

            @Parameter(description = "Assignment's entity identifier (e.g., campaign ID)",
                    example = "CAMP-001", required = true)
            @RequestParam
            @NotBlank(message = "objectId is required")
            String objectId) {

        log.info("Getting applicability rules for assignment: entityType={}, entityId={}", objectType, objectId);

        List<ApplicabilityRuleResponse> rules = applicabilityRuleService
                .findByObjectTypeAndObjectId(objectType.toUpperCase(), objectId);

        log.info("Found {} applicability rules for assignment: entityType={}, entityId={}",
                rules.size(), objectType, objectId);

        return rules;
    }
}
