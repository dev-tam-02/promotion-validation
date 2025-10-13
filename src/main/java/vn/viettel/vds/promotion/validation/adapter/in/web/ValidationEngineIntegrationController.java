package vn.viettel.vds.promotion.validation.adapter.in.web;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.application.service.ValidationEngineIntegrationService;
import vn.viettel.vds.promotion.validation.application.service.SynchronizationStatus;

@RestController
@RequestMapping("/api/v1/integration/validation-engine")
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