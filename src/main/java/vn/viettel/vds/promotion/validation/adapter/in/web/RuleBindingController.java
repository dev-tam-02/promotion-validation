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
import vn.viettel.vds.promotion.validation.application.service.RuleBindingRedeployService;
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
@Tag(name = "Rule Bindings", description = "Unified API for managing rule bindings to objects (campaigns, discounts, vouchers, etc.)")
public class RuleBindingController {

    private final RuleBindingService bindingService;
    private final RuleBindingRedeployService redeployService;

    // ========== Create Operations ==========

    @Operation(summary = "Create a new rule binding",
            description = "Bind a validation rule to an object (campaign, discount, voucher, etc.) with time and product constraints")
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

        log.info("Creating rule binding: objectType={}, objectId={}, ruleId={}",
                request.getObjectType(), request.getObjectId(), request.getRuleId());

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

    @Operation(summary = "Get bindings by object",
            description = "Retrieve all rule bindings for a specific object")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bindings")
    })
    @GetMapping
    public List<RuleBindingResponse> getBindingsByObject(
            @Parameter(description = "Object type (CAMPAIGN, DISCOUNT, VOUCHER, CASHBACK)", required = true)
            @RequestParam String objectType,
            @Parameter(description = "Object ID", required = true)
            @RequestParam String objectId,
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean active) {

        log.info("Getting bindings for object: type={}, id={}, active={}", objectType, objectId, active);

        List<RuleBinding> bindings;
        if (Boolean.TRUE.equals(active)) {
            bindings = bindingService.getActiveBindingsByObject(objectType, objectId);
        } else {
            bindings = bindingService.getBindingsByObject(objectType, objectId);
        }

        return bindings.stream()
                .map(this::mapDomainToResponse)
                .toList();
    }

    @Operation(summary = "Get bindings for multiple objects (batch)",
            description = "Retrieve rule bindings for multiple objects at once")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bindings")
    })
    @GetMapping("/batch")
    public Map<String, List<RuleBindingResponse>> getBindingsBatch(
            @Parameter(description = "Object type", required = true)
            @RequestParam String objectType,
            @Parameter(description = "Comma-separated list of object IDs", required = true)
            @RequestParam String objectIds) {

        List<String> ids = Arrays.stream(objectIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        log.info("Getting bindings for {} objects of type {}", ids.size(), objectType);

        Map<String, List<RuleBinding>> bindingsMap = bindingService.getBindingsByObjects(objectType, ids);

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
            @Parameter(description = "Object type")
            @RequestParam(required = false) String objectType,
            @Parameter(description = "Object ID")
            @RequestParam(required = false) String objectId,
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

        log.info("Searching bindings: objectType={}, objectId={}, ruleId={}, active={}",
                objectType, objectId, ruleId, active);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return bindingService.searchBindings(objectType, objectId, ruleId, active, pageable)
                .map(this::mapDomainToResponse);
    }

    @Operation(summary = "Find object IDs by time range",
            description = "Find objects that have rule bindings within a specific time range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved object IDs")
    })
    @GetMapping("/search-objects")
    public List<String> findObjectIdsByTimeRange(
            @Parameter(description = "Object type", required = true)
            @RequestParam String objectType,
            @Parameter(description = "Start timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startFrom,
            @Parameter(description = "End timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTo) {

        log.info("Finding object IDs: type={}, startFrom={}, endTo={}", objectType, startFrom, endTo);

        return bindingService.findObjectIdsByTimeRange(
                objectType,
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

    @Operation(summary = "Redeploy a rule binding's bundle",
            description = "PROM-1437: biên dịch lại bundle từ trạng thái hiện tại của binding và "
                    + "đồng bộ bundleHash mới sang pp-rule-engine. Dùng để sửa các binding tạo trước "
                    + "bản vá — chúng đang pin bundle 24/7 nên khung thời gian không được áp dụng, và "
                    + "không sửa được qua CMS vì chiến dịch đã ở trạng thái RUNNING.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Binding redeployed successfully"),
            @ApiResponse(responseCode = "404", description = "Binding not found"),
            @ApiResponse(responseCode = "422", description = "Binding has nothing to compile or is inactive")
    })
    @PostMapping("/{id}/redeploy")
    @ResponseCode(code = "RULE_BINDING_REDEPLOYED")
    public RuleBindingResponse redeployBinding(
            @Parameter(description = "Binding ID") @PathVariable String id) {

        log.info("Redeploying rule binding: id={}", id);

        return mapDomainToResponse(redeployService.redeploy(id));
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

    @Operation(summary = "Delete binding by object and rule",
            description = "Delete a specific binding for an object and rule combination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Binding deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Binding not found")
    })
    @DeleteMapping("/by-object-rule")
    @ResponseCode(code = "RULE_BINDING_DELETED")
    public void deleteBindingByObjectAndRule(
            @Parameter(description = "Object type", required = true)
            @RequestParam String objectType,
            @Parameter(description = "Object ID", required = true)
            @RequestParam String objectId,
            @Parameter(description = "Rule ID", required = true)
            @RequestParam String ruleId,
            @Parameter(description = "User making the request")
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        log.info("Deleting binding: objectType={}, objectId={}, ruleId={}, userId={}",
                objectType, objectId, ruleId, userId);
        bindingService.deleteBindingByObjectAndRule(objectType, objectId, ruleId, userId);
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
                .objectType(request.getObjectType())
                .objectId(request.getObjectId())
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
                                        .daysOfWeek(tw.getDaysOfWeek())
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
                .objectType(binding.getObjectType())
                .objectId(binding.getObjectId())
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
                                        .daysOfWeek(tw.getDaysOfWeek())
                                        .build())
                                .toList() : null)
                .duration(binding.getDuration())
                .activityDurationAfterPublishing(binding.getActivityDurationAfterPublishing())
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
