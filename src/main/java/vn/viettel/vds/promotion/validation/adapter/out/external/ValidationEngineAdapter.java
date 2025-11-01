package vn.viettel.vds.promotion.validation.adapter.out.external;

import com.promix.platform.web.template.ResponseTemplate;
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

    private static final String SUCCESS_KEY = "success";
    private static final String ERROR_KEY = "error";
    private static final String NULL_RESPONSE = "null response";

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
            ResponseTemplate<ExecuteResponse> responseTemplate = validationEngineClient.execute(executeRequest);

            // Unwrap response from ResponseTemplate
            if (responseTemplate == null || !responseTemplate.isSuccess() || responseTemplate.getData() == null) {
                log.error("Validation engine returned error response: {}",
                        responseTemplate != null ? responseTemplate.getMessage() : NULL_RESPONSE);
                return ValidationResult.error(
                        request.getTransactionId(),
                        responseTemplate != null ? responseTemplate.getMessage() : "No response from engine"
                );
            }

            ExecuteResponse response = responseTemplate.getData();

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
            errorResult.put(SUCCESS_KEY, false);
            return errorResult;

        } catch (Exception e) {
            log.error("Failed to compile rules", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put(SUCCESS_KEY, false);
            errorResult.put(ERROR_KEY, "This method is deprecated. Use RulePublishingService.publishRule() instead.");
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
            ResponseTemplate<DeployResponse> responseTemplate = validationEngineClient.deployRuleSet(ruleSetId, deployRequest);

            // Unwrap and check deployment status
            if (responseTemplate == null || !responseTemplate.isSuccess() || responseTemplate.getData() == null) {
                log.error("Deploy rule set failed: {}", responseTemplate != null ? responseTemplate.getMessage() : NULL_RESPONSE);
                return false;
            }

            return responseTemplate.getData().isDeployed();

        } catch (Exception e) {
            log.error("Failed to deploy rules", e);
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Health check via getting supported operators
            ResponseTemplate<List<String>> responseTemplate = validationEngineClient.getSupportedOperators();
            if (responseTemplate == null || !responseTemplate.isSuccess() || responseTemplate.getData() == null) {
                return false;
            }
            List<String> operators = responseTemplate.getData();
            return !operators.isEmpty();
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
            ResponseTemplate<WarmupResponse> responseTemplate = validationEngineClient.warmup(warmupRequest);

            // Unwrap and check warmup status
            if (responseTemplate == null || !responseTemplate.isSuccess() || responseTemplate.getData() == null) {
                log.error("Warmup failed: {}", responseTemplate != null ? responseTemplate.getMessage() : NULL_RESPONSE);
                return false;
            }

            return responseTemplate.getData().isOk();

        } catch (Exception e) {
            log.error("Failed to warm up rule set", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getSupportedOperators() {
        try {
            ResponseTemplate<List<String>> responseTemplate = validationEngineClient.getSupportedOperators();
            if (responseTemplate == null || !responseTemplate.isSuccess() || responseTemplate.getData() == null) {
                log.error("Failed to get supported operators: {}", responseTemplate != null ? responseTemplate.getMessage() : NULL_RESPONSE);
                return new HashMap<>();
            }

            List<String> operators = responseTemplate.getData();
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

            result.put("valid", Boolean.TRUE.equals(compileResult.get(SUCCESS_KEY)));
            result.put("errors", compileResult.get("errors"));

            return result;

        } catch (Exception e) {
            result.put("valid", false);
            result.put(ERROR_KEY, "Syntax validation failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Build execute request from domain model
     */
    private ExecuteRequest buildExecuteRequest(ValidationRequest request) {
        // Prepare customer data
        String customerId = request.getCustomerId();
        List<String> segments = new ArrayList<>();
        Integer tier = null;
        Map<String, Object> customerMetadata = new HashMap<>();

        if (request.getValidationContext() != null && request.getValidationContext().getCustomer() != null) {
            if (request.getValidationContext().getCustomer().getSegment() != null) {
                segments.add(request.getValidationContext().getCustomer().getSegment());
            }
            String tierStr = request.getValidationContext().getCustomer().getTier();
            if (tierStr != null) {
                try {
                    tier = Integer.valueOf(tierStr);
                } catch (NumberFormatException e) {
                    // Ignore if tier is not a valid integer
                }
            }
        }

        // Build customer DTO using record constructor
        CustomerDto customerDto = new CustomerDto(
                customerId,
                segments,
                null,  // region
                tier,
                customerMetadata
        );

        // Prepare order data
        Map<String, Object> orderMetadata = new HashMap<>();
        if (request.getValidationContext() != null && request.getValidationContext().getOrder() != null) {
            orderMetadata.put("channel", request.getValidationContext().getOrder().getChannel());
            orderMetadata.put("itemCount", request.getValidationContext().getOrder().getItemCount());
        }

        // Build order DTO using record constructor
        OrderDto orderDto = new OrderDto(
                "order-" + customerId,  // order ID
                request.getOrderValue(),
                "VND",  // currency
                null,   // items
                orderMetadata
        );

        // Build candidate DTO using record constructor
        CandidateDto candidateDto = new CandidateDto(
                request.getPromotionId(),
                "promotion",  // type
                null  // metadata
        );

        // Build execution context (this is a class, not record)
        ExecutionContextDto executionContext = new ExecutionContextDto();
        executionContext.setNow(request.getTimestamp());
        executionContext.setTimezone("Asia/Bangkok");
        executionContext.setSessionId(request.getSessionId());

        // Build variables map for additional context
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", request.getCustomerId());
        variables.put("orderValue", request.getOrderValue());
        executionContext.setVariables(variables);

        // Build execute request (this is a class, not record)
        ExecuteRequest executeRequest = new ExecuteRequest();
        executeRequest.setBundleHash(request.getPromotionId());
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