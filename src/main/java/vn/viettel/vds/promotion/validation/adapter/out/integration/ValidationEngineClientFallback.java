package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.*;

import java.util.List;
import java.util.Map;

@Component
public class ValidationEngineClientFallback implements ValidationEngineClient {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEngineClientFallback.class);

    @Override
    public CompileResponse compile(CompileRequest request) {
        logger.error("Fallback: Rule compilation failed for ruleId={}, version={}",
                request.getRuleId(), request.getVersion().intValue());

        CompileResponse fallbackResponse = new CompileResponse();
        fallbackResponse.setOk(false);
        fallbackResponse.setErrors(List.of("Validation engine service is unavailable"));
        return fallbackResponse;
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        logger.error("Fallback: Rule execution failed for bundleHash={}, customerId={}",
                request.getBundleHash(), request.getCustomer().id());

        ExecuteResponse fallbackResponse = new ExecuteResponse();
        fallbackResponse.setOk(false);
        fallbackResponse.setDecision("DENY");
        fallbackResponse.setReasonCodes(List.of("SERVICE_UNAVAILABLE"));
        fallbackResponse.setExplain(List.of("Validation engine service is unavailable"));
        return fallbackResponse;
    }

    @Override
    public List<ExecuteResponse> executeBatch(List<ExecuteRequest> requests) {
        logger.error("Fallback: Batch execution failed for {} requests", requests.size());

        return requests.stream()
                .map(request -> {
                    ExecuteResponse fallbackResponse = new ExecuteResponse();
                    fallbackResponse.setOk(false);
                    fallbackResponse.setDecision("DENY");
                    fallbackResponse.setReasonCodes(List.of("SERVICE_UNAVAILABLE"));
                    fallbackResponse.setExplain(List.of("Validation engine service is unavailable"));
                    return fallbackResponse;
                })
                .toList();
    }

    @Override
    public WarmupResponse warmup(WarmupRequest request) {
        logger.error("Fallback: Bundle warmup failed for bundleHash={}", request.getBundleHash());
        WarmupResponse fallbackResponse = new WarmupResponse();
        fallbackResponse.setOk(false);
        fallbackResponse.setErrors(List.of("Validation engine service is unavailable"));
        return fallbackResponse;
    }

    @Override
    public BundleStatusResponse getBundleStatus(String bundleHash) {
        logger.error("Fallback: Bundle status check failed for bundleHash={}", bundleHash);

        BundleStatusResponse fallbackResponse = new BundleStatusResponse();
        fallbackResponse.setBundleHash(bundleHash);
        fallbackResponse.setLoaded(false);
        fallbackResponse.setHealth("UNKNOWN");
        fallbackResponse.setInfo("Validation engine service is unavailable");
        return fallbackResponse;
    }

    @Override
    public DeployResponse deployRuleSet(String ruleSetId, Map<String, Object> compiledRules) {
        logger.error("Fallback: Rule deployment failed for ruleSetId={}", ruleSetId);
        DeployResponse fallbackResponse = new DeployResponse();
        fallbackResponse.setDeployed(false);
        fallbackResponse.setMessage("Validation engine service is unavailable");
        return fallbackResponse;
    }

    @Override
    public List<String> getSupportedOperators() {
        logger.error("Fallback: Get supported operators failed");
        return List.of();
    }

    @Override
    public org.springframework.http.ResponseEntity<String> getHealth() {
        logger.error("Fallback: Health check failed");
        return org.springframework.http.ResponseEntity.status(503).body("Validation engine service is unavailable");
    }
}