package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.validation.annotations.Id;
import com.promix.platform.web.annotation.ResponseWrapper;
import com.promix.platform.web.mvc.model.PageableRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.mapper.RuleResponseMapper;
import vn.viettel.vds.promotion.validation.application.port.in.RuleBindingUseCase;
import vn.viettel.vds.promotion.validation.application.service.RuleLinter;
import vn.viettel.vds.promotion.validation.application.service.RuleService;
import vn.viettel.vds.promotion.validation.application.service.RuleSimulationService;
import vn.viettel.vds.promotion.validation.application.service.RuleValidationService;
import vn.viettel.vds.promotion.validation.domain.enums.RuleContextType;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.LintReportDto;
import vn.viettel.vds.promotion.validation.domain.model.LintIssue;
import vn.viettel.vds.promotion.validation.domain.model.LintReport;
import vn.viettel.vds.promotion.validation.domain.model.Rule;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;
import vn.viettel.vds.promotion.validation.domain.exception.BindingNotFoundException;

import java.util.*;

@RestController
@Validated
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/rules")
@Tag(name = "Rules", description = "Rule management API")
public class RuleController {

    private static final Logger logger = LoggerFactory.getLogger(RuleController.class);

    /**
     * Allowed sort fields for rules list endpoint.
     * These fields map to columns in the validation_rules table.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "code", "name", "state", "ruleVersion",
            "logic", "publishedAt", "publishedBy",
            "createdAt", "updatedAt", "createdBy", "updatedBy"
    );

    private final RuleService ruleService;
    private final RuleResponseMapper ruleMapper;
    private final RuleValidationService ruleValidationService;
    private final RuleSimulationService ruleSimulationService;
    private final RuleBindingUseCase ruleBindingUseCase;
    private final RuleLinter ruleLinter;

    public RuleController(RuleService ruleService, RuleResponseMapper ruleMapper,
                          RuleValidationService ruleValidationService,
                          RuleSimulationService ruleSimulationService,
                          RuleBindingUseCase ruleBindingUseCase,
                          RuleLinter ruleLinter) {
        this.ruleService = ruleService;
        this.ruleMapper = ruleMapper;
        this.ruleValidationService = ruleValidationService;
        this.ruleSimulationService = ruleSimulationService;
        this.ruleBindingUseCase = ruleBindingUseCase;
        this.ruleLinter = ruleLinter;
    }

    @Operation(summary = "Create a new rule", description = "Create a new validation rule in draft state")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rule created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Rule code already exists")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse createRule(
            @Valid @RequestBody CreateRuleRequest request,
            @Parameter(description = "User making the request") @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        logger.info("Creating rule: code={}", request.getCode());

        List<RuleNode> nodes = ruleMapper.toRuleNodes(request.getNodes());

        Rule rule = ruleService.createRule(
                request.getCode(),
                request.getName(),
                Rule.LogicType.valueOf(request.getLogic()),
                nodes,
                request.getContext(),
                request.getDescription(),
                request.getFallbackErrorMessage(),
                userId
        );

        RuleResponse response = ruleMapper.toRuleResponse(rule);
        LintReport lintReport = ruleLinter.lint(rule, nodes);
        if (!lintReport.isClean()) {
            response.setLint(toLintReportDto(lintReport));
        }
        return response;
    }

    @Operation(summary = "Get rule by ID", description = "Retrieve a specific rule by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule found"),
            @ApiResponse(responseCode = "400", description = "Invalid rule ID format"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @GetMapping("/{ruleId}")
    public RuleResponse getRuleById(
            @Parameter(description = "Rule ID (UUID format, max 36 characters, alphanumeric and hyphens only)")
            @PathVariable
            @NotBlank(message = "VALIDATION_RULE_ID_REQUIRED")
            @Size(max = 36, message = "VALIDATION_RULE_ID_LENGTH_EXCEEDED")
            @Id(errorCode = "VALIDATION_RULE_ID_INVALID", description = "ID quy tắc không đúng định dạng UUID")
            String ruleId) {

        // Trim whitespace from rule ID
        String trimmedRuleId = ruleId.trim();

        logger.info("Getting rule: id={}", trimmedRuleId);

        Rule rule = ruleService.getRuleById(trimmedRuleId);
        return ruleMapper.toRuleResponse(rule);
    }

    @Operation(summary = "List rules", description = "List rules with optional filtering and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rules retrieved successfully")
    })
    @GetMapping
    public PageResponse<RuleListItemResponse> listRules(
            @Parameter(description = "Filter by state") @RequestParam(required = false) String state,
            @Parameter(description = "Filter by code pattern") @RequestParam(required = false) String code,
            @Parameter(description = "Filter by name pattern") @RequestParam(required = false) String name,
            @Parameter(description = "Search query") @RequestParam(required = false) String q,
            @ParameterObject PageableRequest pageableRequest) {

        logger.info("Listing rules: page={}, size={}", pageableRequest.getPage(), pageableRequest.getSize());

        // Set default sort if not provided
        if (pageableRequest.getSort() == null || pageableRequest.getSort().isEmpty()) {
            pageableRequest.setSort(List.of("updatedAt,desc"));
        }

        // Validate pagination parameters including sort field validation
        pageableRequest.validate(ALLOWED_SORT_FIELDS);

        Pageable pageable = pageableRequest.toPageable();

        Rule.RuleState stateEnum = state != null ? Rule.RuleState.valueOf(state.toUpperCase()) : null;

        Page<Rule> rules = ruleService.findRules(stateEnum, code, name, pageable);
        Page<RuleListItemResponse> responses = rules.map(ruleMapper::toListItemResponse);

        return PageResponse.from(responses);
    }

    @Operation(summary = "Update rule", description = "Update an existing rule (only draft rules can be updated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule updated successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "409", description = "Rule is not in draft state")
    })
    @PatchMapping("/{ruleId}")
    public RuleResponse updateRule(
            @Parameter(description = "Rule ID") @PathVariable String ruleId,
            @Valid @RequestBody UpdateRuleRequest request,
            @Parameter(description = "User making the request") @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        logger.info("Updating rule: id={}", ruleId);

        Rule.LogicType logic = request.getLogic() != null ? Rule.LogicType.valueOf(request.getLogic()) : null;
        List<RuleNode> nodes = request.getNodes() != null ? ruleMapper.toRuleNodes(request.getNodes()) : null;

        Rule rule = ruleService.updateRule(
                ruleId,
                request.getName(),
                logic,
                nodes,
                request.getContext(),
                request.getDescription(),
                request.getFallbackErrorMessage(),
                userId
        );

        RuleResponse response = ruleMapper.toRuleResponse(rule);
        List<RuleNode> lintNodes = nodes != null ? nodes : rule.getNodes();
        if (lintNodes != null) {
            LintReport lintReport = ruleLinter.lint(rule, lintNodes);
            if (!lintReport.isClean()) {
                response.setLint(toLintReportDto(lintReport));
            }
        }
        return response;
    }

    @Operation(summary = "Clone rule", description = "Clone an existing rule with new code and name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rule cloned successfully"),
            @ApiResponse(responseCode = "404", description = "Source rule not found"),
            @ApiResponse(responseCode = "409", description = "New rule code already exists")
    })
    @PostMapping("/{ruleId}:clone")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse cloneRule(
            @Parameter(description = "Source rule ID") @PathVariable String ruleId,
            @Valid @RequestBody CloneRuleRequest request,
            @Parameter(description = "User making the request") @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        logger.info("Cloning rule: sourceId={}, newCode={}", ruleId, request.getNewCode());

        Rule rule = ruleService.cloneRule(ruleId, request.getNewCode(), request.getNewName(), userId);
        return ruleMapper.toRuleResponse(rule);
    }

    @Operation(summary = "Activate rule", description = "Activate a rule from DRAFT state (ready for publishing)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule activated successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "400", description = "Rule is not in DRAFT state")
    })
    @PostMapping("/{ruleId}:activate")
    public RuleResponse activateRule(
            @Parameter(description = "Rule ID") @PathVariable String ruleId,
            @Parameter(description = "User making the request") @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        logger.info("Activating rule: id={}", ruleId);

        Rule rule = ruleService.activateRule(ruleId, userId);
        return ruleMapper.toRuleResponse(rule);
    }

    @Operation(summary = "Get available contexts", description = "Return list of available validation rule contexts for dropdown")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contexts retrieved successfully")
    })
    @GetMapping("/contexts")
    public List<ContextOptionResponse> getContexts() {
        logger.info("Getting rule context options");
        return Arrays.stream(RuleContextType.values())
                .map(ctx -> new ContextOptionResponse(ctx.name(), ctx.getLabel()))
                .toList();
    }

    @Operation(summary = "Delete rule", description = "Permanently delete a rule (hard delete). Rule must not be assigned to any campaigns.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict (CONFLICTED)"),
            @ApiResponse(responseCode = "400", description = "Rule has active bindings (RULE_HAS_BINDINGS)")
    })
    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteRule(
            @Parameter(description = "Rule ID") @PathVariable String ruleId,
            @Parameter(description = "Current version for optimistic locking", required = true)
            @RequestParam long version,
            @Parameter(description = "User making the request") @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        logger.info("Deleting rule: id={}, version={}, userId={}", ruleId, version, userId);
        ruleService.deleteRule(ruleId, version);
    }

    @Operation(summary = "Archive rule", description = "Archive a rule (mark as inactive)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule archived successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PostMapping("/{ruleId}:archive")
    public RuleResponse archiveRule(
            @Parameter(description = "Rule ID") @PathVariable String ruleId,
            @Parameter(description = "User making the request") @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        logger.info("Archiving rule: id={}", ruleId);

        Rule rule = ruleService.archiveRule(ruleId, userId);
        return ruleMapper.toRuleResponse(rule);
    }

    @Operation(summary = "Bind rule to resource (Task 06)",
            description = "Create a rule binding for a resource (CAMPAIGN, COUPON_CONFIG, COUPON_CODE)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Binding created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PostMapping("/{ruleId}/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleBindingResponse bindRuleToResource(
            @Parameter(description = "Rule ID") @PathVariable String ruleId,
            @Valid @RequestBody RuleBindingRequest request,
            @Parameter(description = "User making the request") @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        logger.info("bindRuleToResource: ruleId={} resourceType={} resourceId={}",
                ruleId, request.getObjectType(), request.getObjectId());

        RuleBinding binding = ruleBindingUseCase.bindRuleToResource(
                ruleId,
                request.getObjectType(),
                request.getObjectId(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getPriority() != null ? request.getPriority() : 100,
                userId);

        return RuleBindingResponse.builder()
                .id(binding.getId())
                .ruleId(binding.getRuleId())
                .objectType(binding.getObjectType())
                .objectId(binding.getObjectId())
                .active(binding.getActive())
                .priority(binding.getPriority())
                .validFrom(binding.getValidFrom())
                .validTo(binding.getValidTo())
                .createdAt(binding.getCreatedAt())
                .createdBy(binding.getCreatedBy())
                .build();
    }

    @Operation(summary = "Lint rules", description = "Validate rule structure and parameters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping(":lint")
    public LintResponse lintRules(
            @Valid @RequestBody LintRulesRequest request) {

        logger.info("Linting rules: nodeCount={}", request.getNodes().size());

        RuleValidationService.LintResult result = ruleValidationService.lintRule(
                ruleMapper.toRuleNodes(request.getNodes())
        );

        LintResponse response = new LintResponse();
        response.setOk(result.isValid());
        response.setIssues(result.getIssues().stream()
                .map(issue -> new LintResponse.LintIssue(issue.getPath(), issue.getMessage(), issue.getOperator()))
                .toList());

        return response;
    }

    @Operation(summary = "Batch simulate rule", description = "Run multiple test cases against a rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch simulation completed"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PostMapping("/{ruleId}/simulate:batch")
    public BatchSimulationResponse batchSimulateRule(
            @Parameter(description = "Rule ID") @PathVariable String ruleId,
            @Valid @RequestBody BatchSimulateRequest request) {

        logger.info("Batch simulating rule: id={}, version={}, caseCount={}",
                ruleId, request.getVersion(), request.getCases().size());

        List<RuleSimulationService.SimulationCase> cases = request.getCases().stream()
                .map(this::mapToSimulationCase)
                .toList();

        RuleSimulationService.BatchSimulationResult result = ruleSimulationService.simulateBatch(
                ruleId, request.getVersion(), cases);

        BatchSimulationResponse response = new BatchSimulationResponse();
        response.setStats(new BatchSimulationResponse.Stats(
                result.getStats().getPass(), result.getStats().getFail()));
        response.setResults(result.getResults().stream()
                .map(this::mapToCaseResult)
                .toList());

        return response;
    }

    private RuleSimulationService.SimulationContext mapToSimulationContext(Map<String, Object> contextData) {
        RuleSimulationService.SimulationContext context = new RuleSimulationService.SimulationContext();

        if (contextData.containsKey("now")) {
            context.setNow(java.time.Instant.parse((String) contextData.get("now")));
        }
        if (contextData.containsKey("tz")) {
            context.setTimezone((String) contextData.get("tz"));
        }

        // Map order data
        @SuppressWarnings("unchecked")
        Map<String, Object> order = (Map<String, Object>) contextData.get("order");
        if (order != null) {
            if (order.containsKey("total")) {
                context.setOrderTotal((Number) order.get("total"));
            }
            if (order.containsKey("currency")) {
                context.setOrderCurrency((String) order.get("currency"));
            }
        }

        // Map customer data
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) contextData.get("customer");
        if (customer != null) {
            if (customer.containsKey("id")) {
                context.setCustomerId((String) customer.get("id"));
            }
            if (customer.containsKey("segments")) {
                @SuppressWarnings("unchecked")
                List<String> segments = (List<String>) customer.get("segments");
                context.setCustomerSegments(segments);
            }
            if (customer.containsKey("region")) {
                context.setCustomerRegion((String) customer.get("region"));
            }
        }

        // Map metadata
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) contextData.get("metadata");
        if (metadata != null) {
            context.setMetadata(metadata);
        }

        return context;
    }

    private RuleSimulationService.ExplainLevel mapToExplainLevel(String explain) {
        if (explain == null) return RuleSimulationService.ExplainLevel.NONE;
        return switch (explain.toUpperCase()) {
            case "FULL" -> RuleSimulationService.ExplainLevel.FULL;
            case "FAIL_ONLY" -> RuleSimulationService.ExplainLevel.FAIL_ONLY;
            default -> RuleSimulationService.ExplainLevel.NONE;
        };
    }

    private RuleSimulationService.SimulationCase mapToSimulationCase(BatchSimulateRequest.TestCase testCase) {
        RuleSimulationService.SimulationCase simulationCase = new RuleSimulationService.SimulationCase();
        simulationCase.setName(testCase.getName());
        simulationCase.setContext(mapToSimulationContext(testCase.getContext()));

        if (testCase.getExpect() != null) {
            RuleSimulationService.ExpectedResult expected = new RuleSimulationService.ExpectedResult();
            expected.setDecision(RuleSimulationService.Decision.valueOf(testCase.getExpect().getDecision().toUpperCase()));
            expected.setReasonCodes(testCase.getExpect().getReasonCodes());
            simulationCase.setExpected(expected);
        }

        return simulationCase;
    }

    private BatchSimulationResponse.CaseResult mapToCaseResult(RuleSimulationService.CaseResult result) {
        BatchSimulationResponse.CaseResult caseResult = new BatchSimulationResponse.CaseResult();
        caseResult.setName(result.getName());
        caseResult.setDecision(result.getDecision().name().toLowerCase());
        caseResult.setReasonCodes(result.getReasonCodes());
        caseResult.setOk(result.isOk());
        caseResult.setExplain(result.getExplain());
        return caseResult;
    }

    @Operation(summary = "Get rule by object", description = "Retrieve validation rule with binding details for a specific object")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule found"),
            @ApiResponse(responseCode = "404", description = "No rule assigned to this object")
    })
    @GetMapping("/by-object")
    public RuleWithBindingResponse getRuleByObject(
            @Parameter(description = "Object type (e.g., CAMPAIGN, PROMOTION)") @RequestParam String objectType,
            @Parameter(description = "Object ID") @RequestParam String objectId) {

        logger.info("Getting rule by object: type={}, id={}", objectType, objectId);

        // Get binding details
        Optional<RuleBinding> binding = ruleService.getBindingForObject(objectType, objectId);

        if (binding.isEmpty()) {
            throw new BindingNotFoundException(objectType, objectId);
        }

        RuleBinding b = binding.get();

        // Get the rule
        Rule rule = ruleService.getRuleById(b.getRuleId());

        // Build response with rule and binding details
        return RuleWithBindingResponse.builder()
                .rule(ruleMapper.toRuleResponse(rule))
                .binding(mapToBindingDetails(b))
                .build();
    }

    @Operation(summary = "Get all rules by object", description = "Retrieve all validation rules (active and inactive) assigned to a specific object")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rules found"),
            @ApiResponse(responseCode = "404", description = "No rules assigned to this object")
    })
    @GetMapping("/by-object/all")
    public List<RuleWithBindingResponse> getAllRulesByObject(
            @Parameter(description = "Object type (e.g., CAMPAIGN, PROMOTION)") @RequestParam String objectType,
            @Parameter(description = "Object ID") @RequestParam String objectId) {

        logger.info("Getting all rules by object: type={}, id={}", objectType, objectId);

        // Get all bindings for this object
        List<RuleBinding> bindings = ruleService.getAllBindingsForObject(objectType, objectId);

        if (bindings.isEmpty()) {
            throw new BindingNotFoundException(objectType, objectId);
        }

        // Build response list with rule and binding details
        return bindings.stream()
                .map(binding -> {
                    try {
                        Rule rule = ruleService.getRuleById(binding.getRuleId());

                        return RuleWithBindingResponse.builder()
                                .rule(ruleMapper.toRuleResponse(rule))
                                .binding(mapToBindingDetails(binding))
                                .build();
                    } catch (Exception e) {
                        logger.warn("Failed to get rule {}: {}", binding.getRuleId(), e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Operation(summary = "Get bundle hash for object",
            description = "Retrieve the compiled bundle hash for a specific object (campaign, voucher, etc.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bundle hash found"),
            @ApiResponse(responseCode = "404", description = "No bundle found for this object")
    })
    @GetMapping("/bundle-hash")
    public BundleHashResponse getBundleHashForObject(
            @Parameter(description = "Object type (campaign, voucher, tier, reward)", example = "campaign")
            @RequestParam String objectType,
            @Parameter(description = "Object identifier/key", example = "CAMPAIGN-001")
            @RequestParam String objectId) {

        logger.info("Received bundle hash request for objectType: {}, objectId: {}", objectType, objectId);

        BundleHashResponse response = ruleService.getBundleHashForObject(objectType, objectId);

        logger.debug("Bundle hash retrieved for objectId: {} - hash: {}",
                objectId, response.bundleHash());

        return response;
    }

    /**
     * Map RuleBinding to BindingDetails DTO
     */
    private RuleWithBindingResponse.BindingDetails mapToBindingDetails(RuleBinding binding) {
        return RuleWithBindingResponse.BindingDetails.builder()
                .bindingId(binding.getId())
                .objectType(binding.getObjectType())
                .objectId(binding.getObjectId())
                .ruleId(binding.getRuleId())
                .ruleVersionPinned(binding.getRuleVersionPinned())
                .active(binding.getActive())
                .priority(binding.getPriority())
                .validFrom(binding.getValidFrom())
                .validTo(binding.getValidTo())
                .timezone(binding.getTimezone())
                .rrule(binding.getRrule())
                .timeWindows(binding.getTimeWindows() != null ?
                        binding.getTimeWindows().stream()
                                .map(tw -> RuleWithBindingResponse.TimeWindowDto.builder()
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
                .build();
    }

    // -------------------------------------------------------------------------
    // Lint helpers
    // -------------------------------------------------------------------------

    private LintReportDto toLintReportDto(LintReport report) {
        var warnings = report.warnings().stream().map(this::toIssueDto).toList();
        var errors = report.errors().stream().map(this::toIssueDto).toList();
        return new LintReportDto(warnings, errors);
    }

    private LintReportDto.LintIssueDto toIssueDto(LintIssue issue) {
        return new LintReportDto.LintIssueDto(issue.severity(), issue.code(), issue.message(), issue.nodeId());
    }
}