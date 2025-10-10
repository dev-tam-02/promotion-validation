package vn.viettel.vds.promotion.validation.adapter.out.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationProxyFeignClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationEnginePort;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter implementation for external validation engine integration
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationEngineAdapter implements ValidationEnginePort {

    private final ValidationEngineClient validationEngineClient;
    private final ValidationProxyFeignClient validationProxyClient;

    @Override
    public ValidationResult executeValidation(ValidationRequest request) {
        log.debug("Executing validation via engine for transaction: {}",
                request.getTransactionId());

        try {
            // Build execute request
            ExecuteRequest executeRequest = buildExecuteRequest(request);

            // Call validation engine
            ExecuteResponse response = validationEngineClient.execute(executeRequest);

            // Convert response to domain result
            return convertToValidationResult(response, request.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to execute validation via engine", e);
            return ValidationResult.error(
                    request.getTransactionId(),
                    "Engine execution failed: " + e.getMessage()
            );
        }
    }

    @Override
    public ValidationResult performFastCheck(String campaignId, Map<String, Object> context) {
        log.debug("Performing fast check for campaign: {}", campaignId);

        try {
            // Build fast check request
            Map<String, Object> fastCheckRequest = new HashMap<>();
            fastCheckRequest.put("campaignId", campaignId);
            fastCheckRequest.put("context", context);
            fastCheckRequest.put("timestamp", Instant.now().toString());

            // Call via proxy client
            Map<String, Object> response = validationProxyClient.performFastCheck(fastCheckRequest);

            // Convert response
            return convertMapToValidationResult(response, campaignId);

        } catch (Exception e) {
            log.error("Fast check failed", e);
            return ValidationResult.error(
                    campaignId,
                    "Fast check failed: " + e.getMessage()
            );
        }
    }

    @Override
    public Map<String, Object> compileRules(Map<String, String> rules) {
        log.debug("Compiling {} rules", rules.size());

        try {
            // Note: This method signature doesn't match the new compile API
            // which requires full rule structure with nodes, tenantId, etc.
            // This is a legacy method that needs to be deprecated or updated

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "This method is deprecated. Use RulePublishingService.publishRule() instead.");
            return errorResult;

        } catch (Exception e) {
            log.error("Failed to compile rules", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    @Override
    public boolean deployRules(String ruleSetId, Map<String, Object> rules) {
        log.info("Deploying rule set: {}", ruleSetId);

        try {
            // Build deployment request
            Map<String, Object> deployRequest = new HashMap<>();
            deployRequest.put("ruleSetId", ruleSetId);
            deployRequest.put("rules", rules);
            deployRequest.put("deploymentTime", Instant.now().toString());

            // Call engine deployment endpoint
            vn.viettel.vds.promotion.validation.adapter.out.integration.dto.DeployResponse response = validationEngineClient.deployRuleSet(ruleSetId, deployRequest);

            // Check deployment status
            return response.isDeployed();

        } catch (Exception e) {
            log.error("Failed to deploy rules", e);
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Health check via getting supported operators
            List<String> operators = validationEngineClient.getSupportedOperators();
            return operators != null && !operators.isEmpty();
        } catch (Exception e) {
            log.warn("Validation engine is not available", e);
            return false;
        }
    }

    @Override
    public boolean warmUp(String ruleSetId) {
        log.debug("Warming up rule set: {}", ruleSetId);

        try {
            // Build warmup request
            WarmupRequest warmupRequest = new WarmupRequest();
            warmupRequest.setBundleHash(ruleSetId);

            // Call warmup endpoint
            vn.viettel.vds.promotion.validation.adapter.out.integration.dto.WarmupResponse response = validationEngineClient.warmup(warmupRequest);

            // Check warmup status
            return response.isOk();

        } catch (Exception e) {
            log.error("Failed to warm up rule set", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getSupportedOperators() {
        try {
            List<String> operators = validationEngineClient.getSupportedOperators();
            Map<String, Object> result = new HashMap<>();
            result.put("operators", operators);
            result.put("count", operators.size());
            return result;
        } catch (Exception e) {
            log.error("Failed to get supported operators", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> validateRuleSyntax(String rule) {
        log.debug("Validating rule syntax");

        Map<String, Object> result = new HashMap<>();

        try {
            // Try to compile single rule
            Map<String, String> singleRule = new HashMap<>();
            singleRule.put("test_rule", rule);

            Map<String, Object> compileResult = compileRules(singleRule);

            result.put("valid", Boolean.TRUE.equals(compileResult.get("success")));
            result.put("errors", compileResult.get("errors"));

            return result;

        } catch (Exception e) {
            result.put("valid", false);
            result.put("error", "Syntax validation failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Build execute request from domain model
     */
    private ExecuteRequest buildExecuteRequest(ValidationRequest request) {
        ExecuteRequest executeRequest = new ExecuteRequest();
        executeRequest.setBundleHash(request.getPromotionId());

        // Build customer DTO
        vn.viettel.vds.promotion.validation.adapter.out.integration.dto.CustomerDto customerDto =
                new vn.viettel.vds.promotion.validation.adapter.out.integration.dto.CustomerDto();
        customerDto.setId(request.getCustomerId());

        // Build order DTO
        vn.viettel.vds.promotion.validation.adapter.out.integration.dto.OrderDto orderDto =
                new vn.viettel.vds.promotion.validation.adapter.out.integration.dto.OrderDto();
        orderDto.setTotal(request.getOrderValue());

        // Build candidate DTO
        vn.viettel.vds.promotion.validation.adapter.out.integration.dto.CandidateDto candidateDto =
                new vn.viettel.vds.promotion.validation.adapter.out.integration.dto.CandidateDto();
        candidateDto.setId(request.getPromotionId());

        // Build execution context
        vn.viettel.vds.promotion.validation.adapter.out.integration.dto.ExecutionContextDto executionContext =
                new vn.viettel.vds.promotion.validation.adapter.out.integration.dto.ExecutionContextDto();
        executionContext.setNow(request.getTimestamp());
        executionContext.setTimezone("Asia/Bangkok");
        executionContext.setSessionId(request.getSessionId());

        // Build variables map for additional context
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", request.getCustomerId());
        variables.put("orderValue", request.getOrderValue());

        // Add validation context if available
        if (request.getValidationContext() != null) {
            if (request.getValidationContext().getCustomer() != null) {
                java.util.List<String> segments = new java.util.ArrayList<>();
                if (request.getValidationContext().getCustomer().getSegment() != null) {
                    segments.add(request.getValidationContext().getCustomer().getSegment());
                }
                customerDto.setSegments(segments);
                String tierStr = request.getValidationContext().getCustomer().getTier();
                if (tierStr != null) {
                    try {
                        customerDto.setTier(Integer.valueOf(tierStr));
                    } catch (NumberFormatException e) {
                        // Ignore if tier is not a valid integer
                    }
                }
            }
            if (request.getValidationContext().getOrder() != null) {
                // Store channel and item count in metadata since OrderDto doesn't have these fields
                java.util.Map<String, Object> orderMetadata = new java.util.HashMap<>();
                orderMetadata.put("channel", request.getValidationContext().getOrder().getChannel());
                orderMetadata.put("itemCount", request.getValidationContext().getOrder().getItemCount());
                orderDto.setMetadata(orderMetadata);
            }
        }

        executionContext.setVariables(variables);

        executeRequest.setCustomer(customerDto);
        executeRequest.setOrder(orderDto);
        executeRequest.setCandidate(candidateDto);
        executeRequest.setExecutionContext(executionContext);

        return executeRequest;
    }

    /**
     * Convert engine response to ValidationResult
     */
    private ValidationResult convertToValidationResult(
            ExecuteResponse response,
            String validationId) {

        ValidationResult.Decision decision = mapDecision(response.getDecision());

        return ValidationResult.builder()
                .validationId(validationId)
                .decision(decision)
                .reasonCodes(response.getReasonCodes() != null ?
                        response.getReasonCodes() : new ArrayList<>())
                .explanations(response.getExplain() != null ?
                        response.getExplain() : new ArrayList<>())
                .metadata(response.getMetadata())
                .timestamp(Instant.now())
                .processingTimeMs(response.getEngine() != null && response.getEngine().getLatencyMs() != null ?
                        response.getEngine().getLatencyMs() : 0L)
                .build();
    }

    /**
     * Convert map response to ValidationResult
     */
    private ValidationResult convertMapToValidationResult(
            Map<String, Object> response,
            String validationId) {

        String decision = (String) response.get("decision");
        ValidationResult.Decision resultDecision = mapDecisionString(decision);

        List<String> reasonCodes = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        if (response.get("reasonCodes") instanceof List) {
            reasonCodes = (List<String>) response.get("reasonCodes");
        } else if (response.get("reasonCode") != null) {
            reasonCodes.add((String) response.get("reasonCode"));
        }

        if (response.get("explanations") instanceof List) {
            explanations = (List<String>) response.get("explanations");
        } else if (response.get("explanation") != null) {
            explanations.add((String) response.get("explanation"));
        }

        return ValidationResult.builder()
                .validationId(validationId)
                .decision(resultDecision)
                .reasonCodes(reasonCodes)
                .explanations(explanations)
                .metadata(response)
                .timestamp(Instant.now())
                .processingTimeMs(0L)
                .build();
    }

    /**
     * Map decision from engine response
     */
    private ValidationResult.Decision mapDecision(String decision) {
        if (decision == null) {
            return ValidationResult.Decision.ERROR;
        }

        return switch (decision.toUpperCase()) {
            case "ALLOW", "APPROVE", "PASS" -> ValidationResult.Decision.ALLOW;
            case "DENY", "REJECT", "FAIL" -> ValidationResult.Decision.DENY;
            case "PENDING", "REVIEW" -> ValidationResult.Decision.PENDING;
            default -> ValidationResult.Decision.ERROR;
        };
    }

    /**
     * Map decision string
     */
    private ValidationResult.Decision mapDecisionString(String decision) {
        return mapDecision(decision);
    }
}