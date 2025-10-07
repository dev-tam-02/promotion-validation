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
import vn.viettel.vds.promotion.validation.adapter.in.web.mapper.OperatorResponseMapper;
import vn.viettel.vds.promotion.validation.application.service.OperatorService;
import vn.viettel.vds.promotion.validation.domain.model.Operator;

import java.util.List;

@RestController
@RequestMapping("/v1/operators")
@ResponseWrapper
@Tag(name = "Operators", description = "Operator registry management API")
public class OperatorController {

    private static final Logger logger = LoggerFactory.getLogger(OperatorController.class);

    private final OperatorService operatorService;
    private final OperatorResponseMapper operatorMapper;

    public OperatorController(OperatorService operatorService, OperatorResponseMapper operatorMapper) {
        this.operatorService = operatorService;
        this.operatorMapper = operatorMapper;
    }

    @Operation(summary = "Get supported operators from validation engine",
            description = "Returns list of operators supported by the validation engine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved supported operators"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/supported")
    public List<String> getSupportedOperators() {
        logger.info("GET /v1/operators/supported - Retrieving supported operators from validation engine");

        List<String> supportedOperators = operatorService.getSupportedOperatorNames();
        logger.info("Successfully retrieved {} supported operators", supportedOperators.size());
        return supportedOperators;
    }

    @Operation(summary = "Check operator support",
            description = "Check if specific operator is supported by validation engine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/supported/{operatorName}")
    public Boolean isOperatorSupported(
            @Parameter(description = "Operator name") @PathVariable String operatorName,
            @Parameter(description = "Operator version (optional)") @RequestParam(required = false) Integer version) {

        logger.info("GET /v1/operators/supported/{} - Checking operator support (version: {})",
                operatorName, version);

        boolean supported = operatorService.isOperatorSupportedByEngine(operatorName, version);
        logger.info("Operator {} version {} is {}", operatorName, version,
                supported ? "supported" : "not supported");
        return supported;
    }

    @Operation(summary = "Create a new operator", description = "Register a new validation operator")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Operator created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Operator already exists")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperatorResponse createOperator(
            @Valid @RequestBody CreateOperatorRequest request) {

        logger.info("Creating operator: tenant={}, name={}, version={}",
                request.getTenantId(), request.getName(), request.getVersion().intValue());

        Operator operator = operatorService.createOperator(
                request.getTenantId(),
                request.getName(),
                request.getVersion(),
                request.getContext(),
                request.getJsonSchema(),
                request.getCompilerId()
        );

        return operatorMapper.toOperatorResponse(operator);
    }

    @Operation(summary = "Get operator by name and version", description = "Retrieve a specific operator")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operator found"),
            @ApiResponse(responseCode = "404", description = "Operator not found")
    })
    @GetMapping("/{name}@{version}")
    public OperatorResponse getOperator(
            @Parameter(description = "Operator name") @PathVariable String name,
            @Parameter(description = "Operator version") @PathVariable Integer version,
            @Parameter(description = "Tenant ID") @RequestParam(required = false) String tenantId) {

        logger.info("Getting operator: name={}, version={}, tenant={}", name, version, tenantId);

        Operator operator = operatorService.getOperator(tenantId, name, version);
        return operatorMapper.toOperatorResponse(operator);
    }

    @Operation(summary = "List operators", description = "List operators with optional filtering and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operators retrieved successfully")
    })
    @GetMapping
    public PageResponse<OperatorResponse> listOperators(
            @Parameter(description = "Tenant ID (null for global operators)") @RequestParam(required = false) String tenantId,
            @Parameter(description = "Filter by context") @RequestParam(required = false) String context,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String direction) {

        logger.info("Listing operators: tenant={}, context={}, page={}, size={}",
                tenantId, context, page, size);

        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Operator.OperatorStatus statusEnum = status != null ?
                Operator.OperatorStatus.valueOf(status.toUpperCase()) : null;

        Page<Operator> operators = operatorService.findOperators(tenantId, context, statusEnum, pageable);
        Page<OperatorResponse> responses = operators.map(operatorMapper::toOperatorResponse);

        return PageResponse.from(responses);
    }

    @Operation(summary = "Update operator status", description = "Update operator status (active/deprecated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operator updated successfully"),
            @ApiResponse(responseCode = "404", description = "Operator not found")
    })
    @PatchMapping("/{name}@{version}")
    public OperatorResponse updateOperatorStatus(
            @Parameter(description = "Operator name") @PathVariable String name,
            @Parameter(description = "Operator version") @PathVariable Integer version,
            @Parameter(description = "Tenant ID") @RequestParam(required = false) String tenantId,
            @Valid @RequestBody UpdateOperatorStatusRequest request) {

        logger.info("Updating operator status: name={}, version={}, status={}",
                name, version, request.getStatus());

        Operator.OperatorStatus status = Operator.OperatorStatus.valueOf(request.getStatus().toUpperCase());

        Operator operator = operatorService.updateOperatorStatus(tenantId, name, version, status);
        return operatorMapper.toOperatorResponse(operator);
    }

    @Operation(summary = "Validate operator parameters", description = "Validate parameters against operator JSON schema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation completed"),
            @ApiResponse(responseCode = "404", description = "Operator not found")
    })
    @PostMapping(":lint-params")
    public ValidationResponse lintOperatorParams(
            @Valid @RequestBody LintOperatorParamsRequest request) {

        logger.info("Linting operator params: operator={}@{}", request.getName(), request.getVersion().intValue());

        OperatorService.ValidationResult result = operatorService.validateOperatorParams(
                request.getTenantId(),
                request.getName(),
                request.getVersion(),
                request.getParams()
        );

        return new ValidationResponse(
                result.isValid(),
                result.getIssues().stream()
                        .map(issue -> new ValidationIssue(
                                issue.getPath(), issue.getMessage(), issue.getOperator()))
                        .toList()
        );
    }

    @Operation(summary = "Get operator versions", description = "Get all versions of an operator")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Versions retrieved successfully")
    })
    @GetMapping("/{name}/versions")
    public List<OperatorResponse> getOperatorVersions(
            @Parameter(description = "Operator name") @PathVariable String name,
            @Parameter(description = "Tenant ID") @RequestParam(required = false) String tenantId) {

        logger.info("Getting operator versions: name={}, tenant={}", name, tenantId);

        List<Operator> operators = operatorService.getOperatorVersions(tenantId, name);
        return operators.stream()
                .map(operatorMapper::toOperatorResponse)
                .toList();
    }
}