package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseCode;
import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleBindingRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleBindingResponse;
import vn.viettel.vds.promotion.validation.application.service.RuleBindingService;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Rule Binding operations.
 * <p>
 * This controller provides a unified API for managing rule bindings, replacing:
 * - AssignmentController
 * - TemporalPolicyController (binding-related endpoints)
 * - ApplicabilityRuleController
 */
@Slf4j
@RestController
@Validated
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/api/v1/rule-bindings")
@RequiredArgsConstructor
@Tag(name = "Rule Bindings", description = "Unified API for managing rule bindings to targets")
public class RuleBindingController {

    private final RuleBindingService bindingService;

    // ========== Create Operations ==========

    @Operation(summary = "Create a new rule binding",
            description = "Bind a validation rule to a target (campaign, discount, voucher, etc.) with time and product constraints")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Binding created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "409", description = "Binding already exists")
    })
    @PostMapping
    @ResponseCode(code = "RULE_BINDING_CREATED")
    public RuleBindingResponse createBinding(
            @Valid @RequestBody RuleBindingRequest request,
            @Parameter(description = "User making the request")
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        log.info("Creating rule binding: targetType={}, targetId={}, ruleId={}",
                request.getTargetType(), request.getTargetId(), request.getRuleId());

        RuleBinding binding = mapRequestToDomain(request);
        RuleBinding created = bindingService.createBinding(binding, userId);

        return mapDomainToResponse(created);
    }

    // ========== Read Operations ==========

    @Operation(summary = "Get binding by ID",
            description = "Retrieve a specific rule binding by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Binding found"),
            @ApiResponse(responseCode = "404", description = "Binding not found")
    })
    @GetMapping("/{id}")
    public RuleBindingResponse getBinding(
            @Parameter(description = "Binding ID") @PathVariable String id) {

        log.info("Getting rule binding: id={}", id);
        RuleBinding binding = bindingService.getBindingById(id);
        return mapDomainToResponse(binding);
    }

    @Operation(summary = "Get bindings by target",
            description = "Retrieve all rule bindings for a specific target")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bindings")
    })
    @GetMapping
    public List<RuleBindingResponse> getBindingsByTarget(
            @Parameter(description = "Target type (CAMPAIGN, DISCOUNT, VOUCHER, etc.)", required = true)
            @RequestParam String targetType,
            @Parameter(description = "Target ID", required = true)
            @RequestParam String targetId,
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean active) {

        log.info("Getting bindings for target: type={}, id={}, active={}", targetType, targetId, active);

        List<RuleBinding> bindings;
        if (Boolean.TRUE.equals(active)) {
            bindings = bindingService.getActiveBindingsByTarget(targetType, targetId);
        } else {
            bindings = bindingService.getBindingsByTarget(targetType, targetId);
        }

        return bindings.stream()
                .map(this::mapDomainToResponse)
                .toList();
    }

    @Operation(summary = "Get bindings for multiple targets (batch)",
            description = "Retrieve rule bindings for multiple targets at once")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bindings")
    })
    @GetMapping("/batch")
    public Map<String, List<RuleBindingResponse>> getBindingsBatch(
            @Parameter(description = "Target type", required = true)
            @RequestParam String targetType,
            @Parameter(description = "Comma-separated list of target IDs", required = true)
            @RequestParam String targetIds) {

        List<String> ids = Arrays.stream(targetIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        log.info("Getting bindings for {} targets of type {}", ids.size(), targetType);

        Map<String, List<RuleBinding>> bindingsMap = bindingService.getBindingsByTargets(targetType, ids);

        return bindingsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().map(this::mapDomainToResponse).toList()
                ));
    }

    @Operation(summary = "Search bindings",
            description = "Search rule bindings with filters and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results")
    })
    @GetMapping("/search")
    public Page<RuleBindingResponse> searchBindings(
            @Parameter(description = "Target type")
            @RequestParam(required = false) String targetType,
            @Parameter(description = "Target ID")
            @RequestParam(required = false) String targetId,
            @Parameter(description = "Rule ID")
            @RequestParam(required = false) String ruleId,
            @Parameter(description = "Active status")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)")
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Searching bindings: targetType={}, targetId={}, ruleId={}, active={}",
                targetType, targetId, ruleId, active);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return bindingService.searchBindings(targetType, targetId, ruleId, active, pageable)
                .map(this::mapDomainToResponse);
    }

    @Operation(summary = "Find target IDs by time range",
            description = "Find targets that have rule bindings within a specific time range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved target IDs")
    })
    @GetMapping("/search-targets")
    public List<String> findTargetIdsByTimeRange(
            @Parameter(description = "Target type", required = true)
            @RequestParam String targetType,
            @Parameter(description = "Start timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startFrom,
            @Parameter(description = "End timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTo) {

        log.info("Finding target IDs: type={}, startFrom={}, endTo={}", targetType, startFrom, endTo);

        return bindingService.findTargetIdsByTimeRange(
                targetType,
                startFrom != null ? startFrom.toInstant() : null,
                endTo != null ? endTo.toInstant() : null
        );
    }

    // ========== Update Operations ==========

    @Operation(summary = "Update a rule binding",
            description = "Update an existing rule binding")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Binding updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Binding not found")
    })
    @PutMapping("/{id}")
    @ResponseCode(code = "RULE_BINDING_UPDATED")
    public RuleBindingResponse updateBinding(
            @Parameter(description = "Binding ID") @PathVariable String id,
            @Valid @RequestBody RuleBindingRequest request,
            @Parameter(description = "User making the request")
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        log.info("Updating rule binding: id={}", id);

        RuleBinding updates = mapRequestToDomain(request);
        RuleBinding updated = bindingService.updateBinding(id, updates, userId);

        return mapDomainToResponse(updated);
    }

    // ========== Delete Operations ==========

    @Operation(summary = "Delete a rule binding",
            description = "Delete a rule binding by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Binding deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Binding not found")
    })
    @DeleteMapping("/{id}")
    @ResponseCode(code = "RULE_BINDING_DELETED")
    public void deleteBinding(
            @Parameter(description = "Binding ID") @PathVariable String id,
            @Parameter(description = "User making the request")
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        log.info("Deleting rule binding: id={}, userId={}", id, userId);
        bindingService.deleteBinding(id, userId);
    }

    @Operation(summary = "Delete binding by target and rule",
            description = "Delete a specific binding for a target and rule combination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Binding deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Binding not found")
    })
    @DeleteMapping("/by-target-rule")
    @ResponseCode(code = "RULE_BINDING_DELETED")
    public void deleteBindingByTargetAndRule(
            @Parameter(description = "Target type", required = true)
            @RequestParam String targetType,
            @Parameter(description = "Target ID", required = true)
            @RequestParam String targetId,
            @Parameter(description = "Rule ID", required = true)
            @RequestParam String ruleId,
            @Parameter(description = "User making the request")
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        log.info("Deleting binding: targetType={}, targetId={}, ruleId={}, userId={}",
                targetType, targetId, ruleId, userId);
        bindingService.deleteBindingByTargetAndRule(targetType, targetId, ruleId, userId);
    }

    @Operation(summary = "Deactivate a rule binding",
            description = "Soft delete: deactivate a binding without removing it")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Binding deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Binding not found")
    })
    @PostMapping("/{id}/deactivate")
    @ResponseCode(code = "RULE_BINDING_DEACTIVATED")
    public void deactivateBinding(
            @Parameter(description = "Binding ID") @PathVariable String id,
            @Parameter(description = "User making the request")
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        log.info("Deactivating rule binding: id={}, userId={}", id, userId);
        bindingService.deactivateBinding(id, userId);
    }

    // ========== Mapping Methods ==========

    private RuleBinding mapRequestToDomain(RuleBindingRequest request) {
        return RuleBinding.builder()
                .ruleId(request.getRuleId())
                .ruleVersionPinned(request.getRuleVersionPinned())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .priority(request.getPriority())
                .active(request.getActive())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .timezone(request.getTimezone())
                .rrule(request.getRrule())
                .timeWindows(request.getTimeWindows() != null ?
                        request.getTimeWindows().stream()
                                .map(tw -> RuleBinding.TimeWindow.builder()
                                        .start(tw.getStart())
                                        .end(tw.getEnd())
                                        .build())
                                .toList() : null)
                .excludedDates(request.getExcludedDates())
                .includedAll(request.getIncludedAll())
                .includedProducts(request.getIncludedProducts())
                .excludedProducts(request.getExcludedProducts())
                .includedCategories(request.getIncludedCategories())
                .excludedCategories(request.getExcludedCategories())
                .includedBrands(request.getIncludedBrands())
                .excludedBrands(request.getExcludedBrands())
                .trafficPercent(request.getTrafficPercent())
                .stickyKeyStrategy(request.getStickyKeyStrategy() != null ?
                        RuleBinding.StickyKeyStrategy.valueOf(request.getStickyKeyStrategy()) : null)
                .build();
    }

    private RuleBindingResponse mapDomainToResponse(RuleBinding binding) {
        return RuleBindingResponse.builder()
                .id(binding.getId())
                .ruleId(binding.getRuleId())
                .ruleVersionPinned(binding.getRuleVersionPinned())
                .targetType(binding.getTargetType())
                .targetId(binding.getTargetId())
                .priority(binding.getPriority())
                .active(binding.getActive())
                .validFrom(binding.getValidFrom())
                .validTo(binding.getValidTo())
                .timezone(binding.getTimezone())
                .rrule(binding.getRrule())
                .timeWindows(binding.getTimeWindows() != null ?
                        binding.getTimeWindows().stream()
                                .map(tw -> RuleBindingResponse.TimeWindowDto.builder()
                                        .start(tw.getStart())
                                        .end(tw.getEnd())
                                        .build())
                                .toList() : null)
                .excludedDates(binding.getExcludedDates())
                .includedAll(binding.getIncludedAll())
                .includedProducts(binding.getIncludedProducts())
                .excludedProducts(binding.getExcludedProducts())
                .includedCategories(binding.getIncludedCategories())
                .excludedCategories(binding.getExcludedCategories())
                .includedBrands(binding.getIncludedBrands())
                .excludedBrands(binding.getExcludedBrands())
                .trafficPercent(binding.getTrafficPercent())
                .stickyKeyStrategy(binding.getStickyKeyStrategy() != null ?
                        binding.getStickyKeyStrategy().name() : null)
                .bundleHash(binding.getBundleHash())
                .createdAt(binding.getCreatedAt())
                .updatedAt(binding.getUpdatedAt())
                .createdBy(binding.getCreatedBy())
                .updatedBy(binding.getUpdatedBy())
                .version(binding.getVersion())
                .build();
    }
}
