package vn.viettel.vds.promotion.validation.adapter.out.integration;

import com.promix.platform.web.template.ResponseTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ValidationEngineClientFallback implements ValidationEngineClient {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEngineClientFallback.class);
    private static final String SERVICE_UNAVAILABLE_MESSAGE = "Validation engine service is unavailable";

    @Override
    public ResponseTemplate<CompileResponse> compile(CompileRequest request) {
        logger.error("Fallback: Rule compilation failed for ruleId={}, version={}",
                request.getRuleId(), request.getVersion());

        CompileResponse fallbackResponse = new CompileResponse();
        fallbackResponse.setOk(false);
        fallbackResponse.setErrors(List.of(SERVICE_UNAVAILABLE_MESSAGE));
        return createErrorResponse(fallbackResponse);
    }

    @Override
    public ResponseTemplate<ExecuteResponse> execute(ExecuteRequest request) {
        if (logger.isErrorEnabled()) {
            logger.error("Fallback: Rule execution failed for bundleHash={}, customerId={}",
                    request.getBundleHash(), request.getCustomer().id());
        }

        ExecuteResponse fallbackResponse = new ExecuteResponse();
        fallbackResponse.setOk(false);
        fallbackResponse.setDecision("DENY");
        fallbackResponse.setReasonCodes(List.of("SERVICE_UNAVAILABLE"));
        fallbackResponse.setExplain(List.of(SERVICE_UNAVAILABLE_MESSAGE));
        return createErrorResponse(fallbackResponse);
    }

    @Override
    public ResponseTemplate<List<ExecuteResponse>> executeBatch(List<ExecuteRequest> requests) {
        logger.error("Fallback: Batch execution failed for {} requests", requests.size());

        List<ExecuteResponse> fallbackResponses = requests.stream()
                .map(request -> {
                    ExecuteResponse fallbackResponse = new ExecuteResponse();
                    fallbackResponse.setOk(false);
                    fallbackResponse.setDecision("DENY");
                    fallbackResponse.setReasonCodes(List.of("SERVICE_UNAVAILABLE"));
                    fallbackResponse.setExplain(List.of(SERVICE_UNAVAILABLE_MESSAGE));
                    return fallbackResponse;
                })
                .toList();
        return createErrorResponse(fallbackResponses);
    }

    @Override
    public ResponseTemplate<WarmupResponse> warmup(WarmupRequest request) {
        logger.error("Fallback: Bundle warmup failed for bundleHash={}", request.getBundleHash());
        WarmupResponse fallbackResponse = new WarmupResponse();
        fallbackResponse.setOk(false);
        fallbackResponse.setErrors(List.of(SERVICE_UNAVAILABLE_MESSAGE));
        return createErrorResponse(fallbackResponse);
    }

    @Override
    public ResponseTemplate<BundleStatusResponse> getBundleStatus(String bundleHash) {
        logger.error("Fallback: Bundle status check failed for bundleHash={}", bundleHash);

        BundleStatusResponse fallbackResponse = new BundleStatusResponse();
        fallbackResponse.setBundleHash(bundleHash);
        fallbackResponse.setLoaded(false);
        fallbackResponse.setHealth("UNKNOWN");
        fallbackResponse.setInfo(SERVICE_UNAVAILABLE_MESSAGE);
        return createErrorResponse(fallbackResponse);
    }

    @Override
    public ResponseTemplate<DeployResponse> deployRuleSet(String ruleSetId, Map<String, Object> compiledRules) {
        logger.error("Fallback: Rule deployment failed for ruleSetId={}", ruleSetId);
        DeployResponse fallbackResponse = new DeployResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setMessage(SERVICE_UNAVAILABLE_MESSAGE);
        return createErrorResponse(fallbackResponse);
    }

    @Override
    public ResponseTemplate<List<String>> getSupportedOperators() {
        logger.error("Fallback: Get supported operators failed");
        return createErrorResponse(List.of());
    }

    @Override
    public org.springframework.http.ResponseEntity<String> getHealth() {
        logger.error("Fallback: Health check failed");
        return org.springframework.http.ResponseEntity.status(503).body(SERVICE_UNAVAILABLE_MESSAGE);
    }

    /**
     * Helper method to create error ResponseTemplate
     */
    private <T> ResponseTemplate<T> createErrorResponse(T data) {
        ResponseTemplate<T> errorResponse = new ResponseTemplate<>();
        errorResponse.setStatus(503);
        errorResponse.setCode("SERVICE_UNAVAILABLE");
        errorResponse.setSuccess(false);
        errorResponse.setMessage(SERVICE_UNAVAILABLE_MESSAGE);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setData(data);
        return errorResponse;
    }
}