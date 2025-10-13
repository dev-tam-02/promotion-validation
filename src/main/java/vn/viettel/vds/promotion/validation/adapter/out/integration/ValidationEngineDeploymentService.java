package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for deploying validation rules to the validation engine
 * Updated to use the new compile-based approach instead of direct deployment
 */
@Service
public class ValidationEngineDeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEngineDeploymentService.class);

    private final ValidationEngineClient validationEngineClient;

    public ValidationEngineDeploymentService(ValidationEngineClient validationEngineClient) {
        this.validationEngineClient = validationEngineClient;
    }

    /**
     * Check if validation-engine is healthy
     */
    public boolean isHealthy() {
        try {
            ResponseEntity<String> response = validationEngineClient.getHealth();
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Validation-engine health check failed", e);
            return false;
        }
    }
}