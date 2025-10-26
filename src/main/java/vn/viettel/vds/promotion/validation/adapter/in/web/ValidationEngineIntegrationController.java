package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.application.service.SynchronizationStatus;
import vn.viettel.vds.promotion.validation.application.service.ValidationEngineIntegrationService;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/api/v1/integration/validation-engine")
public class ValidationEngineIntegrationController {

    private final ValidationEngineIntegrationService integrationService;

    public ValidationEngineIntegrationController(ValidationEngineIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping("/synchronize")
    public ResponseEntity<Void> synchronizeAllRules() {
        integrationService.synchronizeAllRules();
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/status")
    public ResponseEntity<SynchronizationStatus> getSynchronizationStatus() {
        return ResponseEntity.ok(integrationService.getSynchronizationStatus());
    }
}