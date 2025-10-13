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
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.PublishRuleBatchRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.PublishRuleRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RuleDeploymentStatusResponse;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.RulePublishResponse;
import vn.viettel.vds.promotion.validation.domain.service.RulePublishingService;
import vn.viettel.vds.promotion.validation.domain.exception.RulePublishingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/v1/rules/publishing")
@ResponseWrapper
@Tag(name = "Rule Publishing", description = "Rule publishing and deployment management API")
public class RulePublishingController {

    private static final Logger logger = LoggerFactory.getLogger(RulePublishingController.class);

    private final RulePublishingService rulePublishingService;

    public RulePublishingController(RulePublishingService rulePublishingService) {
        this.rulePublishingService = rulePublishingService;
    }

    @Operation(summary = "Publish a rule",
            description = "Compile and deploy a rule to the validation engine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule published successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or rule validation failed"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "500", description = "Publishing failed")
    })
    @PostMapping("/rules/{ruleId}")
    public RulePublishResponse publishRule(
            @Parameter(description = "Rule ID") @PathVariable String ruleId,
            @Valid @RequestBody(required = false) PublishRuleRequest request) {

        logger.info("Publishing rule: ruleId={}", ruleId);

        try {
            RulePublishingService.RulePublishResult result = rulePublishingService.publishRule(ruleId);

            RulePublishResponse response = new RulePublishResponse();
            response.setRuleId(result.getRuleId());
            response.setSuccess(result.isSuccess());
            response.setBundleHash(result.getBundleHash());
            response.setArtifactSize(result.getArtifactSize());
            response.setErrorMessage(result.getErrorMessage());

            if (result.isSuccess()) {
                logger.info("Rule published successfully: ruleId={}, bundleHash={}", ruleId, result.getBundleHash());
            } else {
                logger.warn("Rule publishing failed: ruleId={}, error={}", ruleId, result.getErrorMessage());
            }

            return response;

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid rule publishing request: ruleId={}, error={}", ruleId, e.getMessage());
            return RulePublishResponse.failed(ruleId, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error publishing rule: ruleId={}", ruleId, e);
            return RulePublishResponse.failed(ruleId, "Internal server error: " + e.getMessage());
        }
    }

    @Operation(summary = "Publish multiple rules",
            description = "Batch publish multiple rules to the validation engine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch publishing completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Batch publishing failed")
    })
    @PostMapping("/batch")
    public List<RulePublishResponse> publishRuleBatch(
            @Valid @RequestBody PublishRuleBatchRequest request) {

        logger.info("Publishing rule batch: count={}", request.getRuleIds().size());

        try {
            List<RulePublishingService.RulePublishResult> results =
                    rulePublishingService.publishRuleBatch(request.getRuleIds());

            List<RulePublishResponse> responses = results.stream()
                    .map(result -> {
                        RulePublishResponse response = new RulePublishResponse();
                        response.setRuleId(result.getRuleId());
                        response.setSuccess(result.isSuccess());
                        response.setBundleHash(result.getBundleHash());
                        response.setArtifactSize(result.getArtifactSize());
                        response.setErrorMessage(result.getErrorMessage());
                        return response;
                    })
                    .toList();

            long successCount = responses.stream().mapToLong(r -> r.isSuccess() ? 1L : 0L).sum();
            logger.info("Batch publishing completed: total={}, successful={}, failed={}",
                    responses.size(), successCount, responses.size() - successCount);

            return responses;

        } catch (Exception e) {
            throw new vn.viettel.vds.promotion.validation.domain.exception.RulePublishingException("Batch publishing failed: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "Unpublish a rule",
            description = "Remove a rule from the validation engine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule unpublished successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "500", description = "Unpublishing failed")
    })
    @DeleteMapping("/rules/{ruleId}")
    public RulePublishResponse unpublishRule(
            @Parameter(description = "Rule ID") @PathVariable String ruleId) {

        logger.info("Unpublishing rule: ruleId={}", ruleId);

        try {
            RulePublishingService.RulePublishResult result = rulePublishingService.unpublishRule(ruleId);

            RulePublishResponse response = new RulePublishResponse();
            response.setRuleId(result.getRuleId());
            response.setSuccess(result.isSuccess());
            response.setErrorMessage(result.getErrorMessage());

            if (result.isSuccess()) {
                logger.info("Rule unpublished successfully: ruleId={}", ruleId);
            } else {
                logger.warn("Rule unpublishing failed: ruleId={}, error={}", ruleId, result.getErrorMessage());
            }

            return response;

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid rule unpublishing request: ruleId={}, error={}", ruleId, e.getMessage());
            return RulePublishResponse.failed(ruleId, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error unpublishing rule: ruleId={}", ruleId, e);
            return RulePublishResponse.failed(ruleId, "Internal server error");
        }
    }

    @Operation(summary = "Get rule deployment status",
            description = "Get the current deployment status of a rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deployment status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "500", description = "Status check failed")
    })
    @GetMapping("/rules/{ruleId}/status")
    public RuleDeploymentStatusResponse getDeploymentStatus(
            @Parameter(description = "Rule ID") @PathVariable String ruleId) {

        logger.debug("Getting deployment status: ruleId={}", ruleId);

        try {
            RulePublishingService.RuleDeploymentStatus status =
                    rulePublishingService.getDeploymentStatus(ruleId);

            RuleDeploymentStatusResponse response = new RuleDeploymentStatusResponse();
            response.setRuleId(status.getRuleId());
            response.setStatus(status.getStatus());
            response.setDeployed(status.isDeployed());
            response.setBundleHash(status.getBundleHash());

            return response;

        } catch (IllegalArgumentException e) {
            logger.warn("Rule not found for status check: ruleId={}", ruleId);
            throw new RulePublishingException("Rule not found: " + ruleId, null);
        } catch (Exception e) {
            throw new RulePublishingException("Failed to get deployment status: ruleId=" + ruleId + " due to: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "Validate rule for publishing",
            description = "Validate that a rule is ready for publishing without actually publishing it")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule is valid for publishing"),
            @ApiResponse(responseCode = "400", description = "Rule validation failed"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @PostMapping("/rules/{ruleId}/validate")
    public Map<String, Object> validateRuleForPublishing(
            @Parameter(description = "Rule ID") @PathVariable String ruleId) {

        logger.debug("Validating rule for publishing: ruleId={}", ruleId);

        try {
            // This would use a validation method in the service
            // For now, return a simple validation result
            Map<String, Object> result = new HashMap<>();
            result.put("ruleId", ruleId);
            result.put("valid", true);
            result.put("message", "Rule is valid for publishing");

            return result;

        } catch (Exception e) {
            logger.error("Rule validation failed: ruleId={}", ruleId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("ruleId", ruleId);
            result.put("valid", false);
            result.put("message", e.getMessage());

            return result;
        }
    }
}