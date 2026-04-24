package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder.RuleCategoriesResponse;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder.RuleOptionsResponse;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.OperatorResponse;
import vn.viettel.vds.promotion.validation.application.port.in.RuleCatalogUseCase;
import vn.viettel.vds.promotion.validation.application.service.RuleBuilderService;
import vn.viettel.vds.promotion.validation.domain.model.Operator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Rule Builder API.
 * Provides endpoints for UI to build validation rules with categories, rules, and options.
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/rule-builder")
public class RuleBuilderController {

    private static final Logger logger = LoggerFactory.getLogger(RuleBuilderController.class);
    private static final String DEFAULT_TENANT = "default";

    private final RuleBuilderService ruleBuilderService;
    private final RuleCatalogUseCase ruleCatalogUseCase;

    public RuleBuilderController(RuleBuilderService ruleBuilderService,
                                 RuleCatalogUseCase ruleCatalogUseCase) {
        this.ruleBuilderService = ruleBuilderService;
        this.ruleCatalogUseCase = ruleCatalogUseCase;
    }

    /**
     * Get all rule categories with their rules.
     * Returns static categories (Audience, Products, etc.) and dynamic metadata categories.
     *
     * @param tenantId optional tenant ID (defaults to "default")
     * @return all categories with rules
     */
    @GetMapping("/categories")
    public RuleCategoriesResponse getAllCategories(
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = DEFAULT_TENANT) String tenantId) {
        logger.debug("Getting all rule categories for tenant: {}", tenantId);
        return ruleBuilderService.getAllCategories(tenantId);
    }

    /**
     * Get options for a specific rule.
     * This endpoint fetches available options from external services (segment, product, etc.)
     * or returns predefined options for the rule.
     *
     * @param ruleId   the rule ID
     * @param search   optional search query
     * @param page     page number (default: 0)
     * @param size     page size (default: 20)
     * @param tenantId optional tenant ID
     * @return paginated options for the rule
     */
    @GetMapping("/options/{ruleId}")
    public RuleOptionsResponse getRuleOptions(
            @PathVariable String ruleId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestHeader(value = "X-Tenant-ID", required = false, defaultValue = DEFAULT_TENANT) String tenantId) {
        logger.debug("Getting options for rule: {} (search: {}, page: {}, size: {}, tenant: {})",
                ruleId, search, page, size, tenantId);
        return ruleBuilderService.getRuleOptions(ruleId, search, page, size, tenantId);
    }

    /**
     * GET /v1/rule-builder/operators?categoryId= (Task 06)
     * List active operators, optionally filtered by category id.
     *
     * @param categoryId optional category id to filter; null returns all
     * @return list of operator responses
     */
    @GetMapping("/operators")
    public List<OperatorResponse> listOperators(
            @RequestParam(required = false) String categoryId) {
        logger.debug("listOperators: categoryId={}", categoryId);
        List<Operator> operators = ruleCatalogUseCase.listOperators(categoryId);
        return operators.stream()
                .map(op -> {
                    OperatorResponse resp = new OperatorResponse();
                    resp.setOperatorId(op.getId());
                    resp.setName(op.getName());
                    resp.setCompilerId(op.getCompilerId());
                    resp.setStatus(op.getStatus() != null ? op.getStatus().name() : null);
                    resp.setContext(op.getContext());
                    resp.setJsonSchema(op.getJsonSchema());
                    resp.setCreatedAt(op.getCreatedAt());
                    resp.setUpdatedAt(op.getUpdatedAt());
                    return resp;
                })
                .collect(Collectors.toList());
    }
}
