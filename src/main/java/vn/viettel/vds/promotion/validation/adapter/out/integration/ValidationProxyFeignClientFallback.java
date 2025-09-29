package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ValidationProxyFeignClientFallback implements ValidationProxyFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(ValidationProxyFeignClientFallback.class);

    @Override
    public Map<String, Object> performFastCheck(Map<String, Object> request) {
        logger.error("Fallback: Fast-check request failed for validation-engine");
        return createErrorResponse("FAST_CHECK_ERROR", "Validation-engine service unavailable");
    }

    @Override
    public Map<String, Object> performExecution(Map<String, Object> request) {
        logger.error("Fallback: Execution request failed for validation-engine");
        return createExecutionErrorResponse("EXECUTION_ERROR", "Validation-engine service unavailable");
    }

    private Map<String, Object> createErrorResponse(String reasonCode, String explanation) {
        Map<String, Object> response = new HashMap<>();
        response.put("decision", "DENY");
        response.put("reasonCode", reasonCode);
        response.put("explanation", explanation);
        return response;
    }

    private Map<String, Object> createExecutionErrorResponse(String reasonCode, String explanation) {
        Map<String, Object> response = new HashMap<>();
        response.put("ok", false);
        response.put("decision", "DENY");
        response.put("reasonCodes", java.util.List.of(reasonCode));
        response.put("explain", java.util.List.of(explanation));
        return response;
    }
}