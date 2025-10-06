package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.mapper.RuleMapper;
import vn.viettel.vds.promotion.validation.application.service.AssignmentService;
import vn.viettel.vds.promotion.validation.application.service.RuleService;
import vn.viettel.vds.promotion.validation.application.service.RuleSimulationService;
import vn.viettel.vds.promotion.validation.application.service.RuleValidationService;
import vn.viettel.vds.promotion.validation.domain.model.Rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@ResponseWrapper
@RequestMapping("/v1/rules")
@Tag(name = "Rules", description = "Rule management API")
public class RuleController {

    private static final Logger logger = LoggerFactory.getLogger(RuleController.class);

    private final RuleService ruleService;
    private final RuleMapper ruleMapper;
    private final RuleValidationService ruleValidationService;
    private final RuleSimulationService ruleSimulationService;
    private final AssignmentService assignmentService;

    public RuleController(RuleService ruleService, RuleMapper ruleMapper,
                          RuleValidationService ruleValidationService,
                          RuleSimulationService ruleSimulationService,
                          AssignmentService assignmentService) {
        this.ruleService = ruleService;
        this.ruleMapper = ruleMapper;
        this.ruleValidationService = ruleValidationService;
        this.ruleSimulationService = ruleSimulationService;
        this.assignmentService = assignmentService;
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

        logger.info("Creating rule: tenant={}, code={}", request.getTenantId(), request.getCode());

        Rule rule = ruleService.createRule(
                request.getTenantId(),
                request.getCode(),
                request.getName(),
                Rule.LogicType.valueOf(request.getLogic()),
                ruleMapper.toRuleNodes(request.getNodes()),
                userId
        );

        return ruleMapper.toRuleResponse(rule);
    }

    @Operation(summary = "Get rule by ID", description = "Retrieve a specific rule by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule found"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @GetMapping("/{ruleId}")
    public RuleResponse getRuleById(
            @Parameter(description = "Rule ID") @PathVariable String ruleId) {

        logger.info("Getting rule: id={}", ruleId);

        Rule rule = ruleService.getRuleById(ruleId);
        return ruleMapper.toRuleResponse(rule);
    }

    @Operation(summary = "List rules", description = "List rules with optional filtering and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rules retrieved successfully")
    })
    @GetMapping
    public PageResponse<RuleResponse> listRules(
            @Parameter(description = "Tenant ID") @RequestParam(required = false, defaultValue = "default") String tenantId,
            @Parameter(description = "Filter by state") @RequestParam(required = false) String state,
            @Parameter(description = "Filter by code pattern") @RequestParam(required = false) String code,
            @Parameter(description = "Filter by name pattern") @RequestParam(required = false) String name,
            @Parameter(description = "Search query") @RequestParam(required = false) String q,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "updatedAt") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction) {

        logger.info("Listing rules: tenant={}, page={}, size={}", tenantId, page, size);

        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Rule.RuleState stateEnum = state != null ? Rule.RuleState.valueOf(state.toUpperCase()) : null;

        Page<Rule> rules = ruleService.findRules(tenantId, stateEnum, code, name, pageable);
        Page<RuleResponse> responses = rules.map(ruleMapper::toRuleResponse);

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

        Rule rule = ruleService.updateRule(
                ruleId,
                request.getName(),
                logic,
                request.getNodes() != null ? ruleMapper.toRuleNodes(request.getNodes()) : null,
                userId
        );

        return ruleMapper.toRuleResponse(rule);
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

    @Operation(summary = "Lint rules", description = "Validate rule structure and parameters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping(":lint")
    public LintResponse lintRules(
            @Valid @RequestBody LintRulesRequest request) {

        logger.info("Linting rules: tenant={}, nodeCount={}",
                request.getTenantId(), request.getNodes().size());

        RuleValidationService.LintResult result = ruleValidationService.lintRule(
                request.getTenantId(),
                ruleMapper.toRuleNodes(request.getNodes())
        );

        LintResponse response = new LintResponse();
        response.setOk(result.isValid());
        response.setIssues(result.getIssues().stream()
                .map(issue -> new LintResponse.LintIssue(issue.getPath(), issue.getMessage(), issue.getOperator()))
                .toList());

        return response;
    }

    @Operation(summary = "Simulate rule execution", description = "Test rule against provided context without publishing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulation completed"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PostMapping("/{ruleId}/simulate")
    public SimulationResponse simulateRule(
            @Parameter(description = "Rule ID") @PathVariable String ruleId,
            @Valid @RequestBody SimulateRuleRequest request) {

        logger.info("Simulating rule: id={}, version={}", ruleId, request.getVersion());

        RuleSimulationService.SimulationResult result = ruleSimulationService.simulateRule(
                ruleId,
                request.getVersion(),
                mapToSimulationContext(request.getContext()),
                mapToExplainLevel(request.getExplain())
        );

        SimulationResponse response = new SimulationResponse();
        response.setDecision(result.getDecision().name().toLowerCase());
        response.setReasonCodes(result.getReasonCodes());
        response.setExplain(result.getExplain());

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

    @Operation(summary = "Get rule by object", description = "Retrieve validation rule with assignment details for a specific object")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule found"),
            @ApiResponse(responseCode = "404", description = "No rule assigned to this object")
    })
    @GetMapping("/by-object")
    public RuleWithAssignmentResponse getRuleByObject(
            @Parameter(description = "Object type (e.g., CAMPAIGN, PROMOTION)") @RequestParam String objectType,
            @Parameter(description = "Object ID") @RequestParam String objectId) {

        logger.info("Getting rule by object: type={}, id={}", objectType, objectId);

        // Get assignment details
        Optional<vn.viettel.vds.promotion.validation.domain.entity.Assignment> assignment =
                assignmentService.findBySubjectTypeAndKey(objectType, objectId);

        if (assignment.isEmpty()) {
            throw new com.promix.platform.core.exception.ResourceNotFoundException();
        }

        // Get the rule
        Rule rule = ruleService.getRuleById(assignment.get().getRuleId());

        // Build response with rule and assignment details
        RuleWithAssignmentResponse response = new RuleWithAssignmentResponse();
        response.setRule(ruleMapper.toRuleResponse(rule));

        RuleWithAssignmentResponse.AssignmentDetails assignmentDetails = new RuleWithAssignmentResponse.AssignmentDetails();
        assignmentDetails.setAssignmentId(assignment.get().getId());
        assignmentDetails.setObjectType(assignment.get().getSubject().getType());
        assignmentDetails.setObjectId(assignment.get().getSubject().getKey());
        assignmentDetails.setActive(assignment.get().getActive());
        assignmentDetails.setValidFrom(assignment.get().getValidFrom());
        assignmentDetails.setValidTo(assignment.get().getValidTo());
        assignmentDetails.setTrafficPercent(assignment.get().getTrafficPercent());
        assignmentDetails.setStickyKeyStrategy(assignment.get().getStickyKeyStrategy() != null ?
                assignment.get().getStickyKeyStrategy().name() : null);
        assignmentDetails.setRuleVersionPinned(assignment.get().getRuleVersionPinned());
        assignmentDetails.setAssignmentVersion(assignment.get().getAssignmentVersion());
        assignmentDetails.setCreatedAt(assignment.get().getCreatedAt());
        assignmentDetails.setUpdatedAt(assignment.get().getUpdatedAt());

        response.setAssignment(assignmentDetails);

        return response;
    }

    @Operation(summary = "Get all rules by object", description = "Retrieve all validation rules (active and inactive) assigned to a specific object")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rules found"),
            @ApiResponse(responseCode = "404", description = "No rules assigned to this object")
    })
    @GetMapping("/by-object/all")
    public List<RuleWithAssignmentResponse> getAllRulesByObject(
            @Parameter(description = "Object type (e.g., CAMPAIGN, PROMOTION)") @RequestParam String objectType,
            @Parameter(description = "Object ID") @RequestParam String objectId) {

        logger.info("Getting all rules by object: type={}, id={}", objectType, objectId);

        // Get all assignments for this object
        List<vn.viettel.vds.promotion.validation.domain.entity.Assignment> assignments =
                assignmentService.findAllBySubjectTypeAndKey(objectType, objectId);

        if (assignments.isEmpty()) {
            throw new com.promix.platform.core.exception.ResourceNotFoundException();
        }

        // Build response list with rule and assignment details
        return assignments.stream()
                .map(assignment -> {
                    try {
                        Rule rule = ruleService.getRuleById(assignment.getRuleId());

                        RuleWithAssignmentResponse response = new RuleWithAssignmentResponse();
                        response.setRule(ruleMapper.toRuleResponse(rule));

                        RuleWithAssignmentResponse.AssignmentDetails assignmentDetails = new RuleWithAssignmentResponse.AssignmentDetails();
                        assignmentDetails.setAssignmentId(assignment.getId());
                        assignmentDetails.setObjectType(assignment.getSubject().getType());
                        assignmentDetails.setObjectId(assignment.getSubject().getKey());
                        assignmentDetails.setActive(assignment.getActive());
                        assignmentDetails.setValidFrom(assignment.getValidFrom());
                        assignmentDetails.setValidTo(assignment.getValidTo());
                        assignmentDetails.setTrafficPercent(assignment.getTrafficPercent());
                        assignmentDetails.setStickyKeyStrategy(assignment.getStickyKeyStrategy() != null ?
                                assignment.getStickyKeyStrategy().name() : null);
                        assignmentDetails.setRuleVersionPinned(assignment.getRuleVersionPinned());
                        assignmentDetails.setAssignmentVersion(assignment.getAssignmentVersion());
                        assignmentDetails.setCreatedAt(assignment.getCreatedAt());
                        assignmentDetails.setUpdatedAt(assignment.getUpdatedAt());

                        response.setAssignment(assignmentDetails);

                        return response;
                    } catch (Exception e) {
                        logger.warn("Failed to get rule {}: {}", assignment.getRuleId(), e.getMessage());
                        return null;
                    }
                })
                .filter(response -> response != null)
                .toList();
    }
}