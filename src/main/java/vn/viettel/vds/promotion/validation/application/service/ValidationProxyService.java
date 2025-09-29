package vn.viettel.vds.promotion.validation.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationProxyFeignClient;

import java.util.Map;

/**
 * Service that proxies validation requests to validation-engine.
 * Acts as a middleware layer ensuring all validation goes through the validation service.
 */
@Service
@Slf4j
public class ValidationProxyService {

    private final ValidationProxyFeignClient validationProxyFeignClient;

    public ValidationProxyService(ValidationProxyFeignClient validationProxyFeignClient) {
        this.validationProxyFeignClient = validationProxyFeignClient;
    }

    /**
     * Proxy fast-check request to validation-engine
     */
    public Map<String, Object> performFastCheck(Map<String, Object> request) {
        log.debug("Proxying fast-check request to validation-engine via Feign");

        try {
            Map<String, Object> result = validationProxyFeignClient.performFastCheck(request);
            log.debug("Fast-check response from validation-engine: {}", result.get("decision"));
            return result;
        } catch (Exception e) {
            log.error("Failed to call validation-engine fast-check via Feign", e);
            // Fallback will handle the error response
            throw e;
        }
    }

    /**
     * Proxy execution request to validation-engine
     */
    public Map<String, Object> performExecution(Map<String, Object> request) {
        log.debug("Proxying execution request to validation-engine via Feign");

        try {
            Map<String, Object> result = validationProxyFeignClient.performExecution(request);
            log.debug("Execution response from validation-engine: {}", result.get("decision"));
            return result;
        } catch (Exception e) {
            log.error("Failed to call validation-engine execute via Feign", e);
            // Fallback will handle the error response
            throw e;
        }
    }

    /**
     * Check if validation-engine is healthy by attempting to get supported operators
     */
    public boolean isValidationEngineHealthy() {
        try {
            // Use a simple operation to check health - attempting fast-check with minimal data
            Map<String, Object> healthCheckRequest = Map.of("healthCheck", true);
            validationProxyFeignClient.performFastCheck(healthCheckRequest);
            log.debug("Validation-engine health check: HEALTHY");
            return true;
        } catch (Exception e) {
            log.warn("Validation-engine health check failed", e);
            return false;
        }
    }
}