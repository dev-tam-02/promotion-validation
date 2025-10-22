package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationRequestDto;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidationResponseDto;
import vn.viettel.vds.promotion.validation.adapter.in.web.mapper.ValidationWebMapper;
import vn.viettel.vds.promotion.validation.application.port.in.ValidateDataUseCase;
import vn.viettel.vds.promotion.validation.domain.model.ValidationRequest;
import vn.viettel.vds.promotion.validation.domain.model.ValidationResult;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for validation endpoints
 * Thin adapter that delegates to use cases
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/api/v1/validation")
@RequiredArgsConstructor
@Slf4j
public class ValidationController {

    private final ValidateDataUseCase validateDataUseCase;
    private final ValidationWebMapper mapper;

    /**
     * Validate data endpoint
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponseDto> validate(
            @RequestBody ValidationRequestDto requestDto) {

        log.info("Received validation request for transaction: {}",
                requestDto.getTransactionId());

        // Map DTO to domain model
        ValidationRequest request = mapper.toDomain(requestDto);

        // Execute validation use case
        ValidationResult result = validateDataUseCase.validate(request);

        // Map result to DTO
        ValidationResponseDto response = mapper.toDto(result);

        log.info("Validation completed for transaction: {} with decision: {}",
                requestDto.getTransactionId(), result.getDecision());

        return ResponseEntity.ok(response);
    }

    /**
     * Fast check endpoint for quick validation
     */
    @PostMapping("/fast-check")
    public ResponseEntity<ValidationResponseDto> fastCheck(
            @RequestBody ValidationRequestDto requestDto) {

        log.info("Received fast-check request for promotion: {}",
                requestDto.getPromotionId());

        // Map DTO to domain model
        ValidationRequest request = mapper.toDomain(requestDto);

        // Execute fast check
        ValidationResult result = validateDataUseCase.performFastCheck(request);

        // Map result to DTO
        ValidationResponseDto response = mapper.toDto(result);

        log.debug("Fast-check completed with decision: {}", result.getDecision());

        return ResponseEntity.ok(response);
    }

    /**
     * Full validation endpoint with all rules
     */
    @PostMapping("/full-validation")
    public ResponseEntity<ValidationResponseDto> fullValidation(
            @RequestBody ValidationRequestDto requestDto) {

        log.info("Received full validation request for transaction: {}",
                requestDto.getTransactionId());

        // Map DTO to domain model
        ValidationRequest request = mapper.toDomain(requestDto);

        // Execute full validation
        ValidationResult result = validateDataUseCase.executeFullValidation(request);

        // Map result to DTO
        ValidationResponseDto response = mapper.toDto(result);

        log.info("Full validation completed with {} rules evaluated",
                result.getReasonCodes() != null ? result.getReasonCodes().size() : 0);

        return ResponseEntity.ok(response);
    }

    /**
     * Validate with specific rule set
     */
    @PostMapping("/validate-with-ruleset/{ruleSetId}")
    public ResponseEntity<ValidationResponseDto> validateWithRuleSet(
            @PathVariable String ruleSetId,
            @RequestBody ValidationRequestDto requestDto) {

        log.info("Validating with rule set: {} for transaction: {}",
                ruleSetId, requestDto.getTransactionId());

        // Map DTO to domain model
        ValidationRequest request = mapper.toDomain(requestDto);

        // Execute validation with rule set
        ValidationResult result =
                validateDataUseCase.validateWithRuleSet(request, ruleSetId);

        // Map result to DTO
        ValidationResponseDto response = mapper.toDto(result);

        return ResponseEntity.ok(response);
    }

    /**
     * Batch validation endpoint
     */
    @PostMapping("/batch-validate")
    public ResponseEntity<Map<String, ValidationResponseDto>> batchValidate(
            @RequestBody Map<String, ValidationRequestDto> requests) {

        log.info("Received batch validation request for {} items", requests.size());

        // Map DTOs to domain models
        Map<String, ValidationRequest> domainRequests = new HashMap<>();
        for (Map.Entry<String, ValidationRequestDto> entry : requests.entrySet()) {
            domainRequests.put(entry.getKey(), mapper.toDomain(entry.getValue()));
        }

        // Execute batch validation
        Map<String, ValidationResult> results =
                validateDataUseCase.validateBatch(domainRequests);

        // Map results to DTOs
        Map<String, ValidationResponseDto> responses = new HashMap<>();
        for (Map.Entry<String, ValidationResult> entry : results.entrySet()) {
            responses.put(entry.getKey(), mapper.toDto(entry.getValue()));
        }

        log.info("Batch validation completed for {} items", responses.size());

        return ResponseEntity.ok(responses);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "validation");
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }
}