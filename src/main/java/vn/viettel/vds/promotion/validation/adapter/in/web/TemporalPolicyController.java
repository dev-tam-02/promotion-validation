package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.TemporalPolicyResponse;
import vn.viettel.vds.promotion.validation.application.service.TemporalPolicyService;
import vn.viettel.vds.promotion.validation.domain.model.TemporalPolicy;
import vn.viettel.vds.promotion.validation.domain.model.TimeOfDayWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${spring.application.context-path}/v1/temporal-policies")
@ResponseWrapper
@Tag(name = "Temporal Policies", description = "Temporal policy query API")
public class TemporalPolicyController {

    private static final Logger logger = LoggerFactory.getLogger(TemporalPolicyController.class);

    private final TemporalPolicyService temporalPolicyService;

    public TemporalPolicyController(TemporalPolicyService temporalPolicyService) {
        this.temporalPolicyService = temporalPolicyService;
    }

    @Operation(
            summary = "Get temporal policies by object type and ID",
            description = "Retrieves all temporal policies associated with a specific object/entity through assignments. " +
                    "The objectType represents the entity type (e.g., CAMPAIGN, DISCOUNT), " +
                    "and objectId is the unique identifier of that entity."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved temporal policies"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public List<TemporalPolicyResponse> getTemporalPoliciesByObject(
            @Parameter(description = "Type of the object/entity (e.g., CAMPAIGN, DISCOUNT)", required = true)
            @RequestParam String objectType,
            @Parameter(description = "ID of the object/entity", required = true)
            @RequestParam String objectId) {

        logger.info("GET /v1/temporal-policies - objectType={}, objectId={}", objectType, objectId);

        List<TemporalPolicyService.TemporalPolicyWithMode> policiesWithMode =
                temporalPolicyService.getTemporalPoliciesByObjectTypeAndId(objectType, objectId);

        List<TemporalPolicyResponse> response = policiesWithMode.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        logger.info("Successfully retrieved {} temporal policies for objectType={}, objectId={}",
                response.size(), objectType, objectId);

        return response;
    }

    /**
     * Maps domain model to response DTO
     */
    private TemporalPolicyResponse mapToResponse(TemporalPolicyService.TemporalPolicyWithMode policyWithMode) {
        TemporalPolicy policy = policyWithMode.getTemporalPolicy();
        TemporalPolicyResponse response = new TemporalPolicyResponse();

        response.setId(policy.getId());
        response.setName(policy.getName());
        response.setTimezone(policy.getTz());
        response.setStartTs(policy.getStartTs());
        response.setEndTs(policy.getEndTs());
        response.setRrule(policy.getRrule());
        response.setRdate(policy.getRdate());
        response.setExrule(policy.getExrule());
        response.setExdate(policy.getExdate());
        response.setMetadata(policy.getMetadata());
        response.setMode(policyWithMode.getMode());
        response.setCreatedAt(policy.getCreatedAt());
        response.setUpdatedAt(policy.getUpdatedAt());
        response.setVersion(policy.getVersion());

        // Map time of day windows
        if (policy.getTimeOfDayWindows() != null) {
            List<TemporalPolicyResponse.TimeOfDayWindowDto> windowDtos = new ArrayList<>();
            for (TimeOfDayWindow window : policy.getTimeOfDayWindows()) {
                TemporalPolicyResponse.TimeOfDayWindowDto dto = new TemporalPolicyResponse.TimeOfDayWindowDto();
                dto.setStart(window.getStart());
                dto.setEnd(window.getEnd());
                windowDtos.add(dto);
            }
            response.setTimeOfDayWindows(windowDtos);
        }

        return response;
    }
}
