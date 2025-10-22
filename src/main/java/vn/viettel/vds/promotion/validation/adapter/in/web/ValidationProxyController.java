package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.application.service.ValidationProxyService;

import java.util.Map;

/**
 * Controller that provides proxy endpoints for validation requests.
 * Acts as a gateway between redemption service and validation-engine.
 * This ensures all validation requests go through the validation service as rule admin.
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/api/validation")
@RequiredArgsConstructor
@Slf4j
public class ValidationProxyController {

    private static final String CAMPAIGN_ID_KEY = "campaignId";
    private static final String DECISION_KEY = "decision";
    private static final String BUNDLE_HASH_KEY = "bundleHash";

    private final ValidationProxyService validationProxyService;

    /**
     * Proxy endpoint for fast validation checks.
     * Forwards requests to validation-engine /v1/fast-check
     */
    @PostMapping("/fast-check")
    public Map<String, Object> fastCheck(@RequestBody Map<String, Object> request) {
        log.info("Received fast-check request for campaign: {}", request.get(CAMPAIGN_ID_KEY));

        try {
            Map<String, Object> result = validationProxyService.performFastCheck(request);
            log.debug("Fast-check completed for campaign: {} with decision: {}",
                    request.get(CAMPAIGN_ID_KEY), result.get(DECISION_KEY));
            return result;

        } catch (Exception e) {
            log.error("Fast-check failed for campaign: {}", request.get(CAMPAIGN_ID_KEY), e);

            // Return failure response
            return Map.of(
                    DECISION_KEY, "DENY",
                    "reasonCode", "FAST_CHECK_ERROR",
                    "explanation", "Fast check service unavailable: " + e.getMessage()
            );
        }
    }

    /**
     * Proxy endpoint for full validation execution.
     * Forwards requests to validation-engine /v1/execute
     */
    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody Map<String, Object> request) {
        log.info("Received execute request for bundle: {}", request.get(BUNDLE_HASH_KEY));

        try {
            Map<String, Object> result = validationProxyService.performExecution(request);
            log.debug("Execution completed for bundle: {} with decision: {}",
                    request.get(BUNDLE_HASH_KEY), result.get(DECISION_KEY));
            return result;

        } catch (Exception e) {
            log.error("Execution failed for bundle: {}", request.get(BUNDLE_HASH_KEY), e);

            // Return failure response
            return Map.of(
                    "ok", false,
                    DECISION_KEY, "DENY",
                    "reasonCodes", java.util.List.of("EXECUTION_ERROR"),
                    "explain", java.util.List.of("Validation execution service unavailable: " + e.getMessage())
            );
        }
    }

    /**
     * Health check endpoint for validation proxy
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        try {
            boolean isHealthy = validationProxyService.isValidationEngineHealthy();

            return Map.of(
                    "status", isHealthy ? "UP" : "DOWN",
                    "validation-engine", isHealthy ? "AVAILABLE" : "UNAVAILABLE",
                    "timestamp", java.time.Instant.now().toString()
            );

        } catch (Exception e) {
            log.error("Health check failed", e);

            return Map.of(
                    "status", "DOWN",
                    "validation-engine", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", java.time.Instant.now().toString()
            );
        }
    }
}
