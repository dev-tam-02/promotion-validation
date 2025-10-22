package vn.viettel.vds.promotion.validation.adapter.in.web;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidateCompatibilityRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.ValidateCompatibilityResponse;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.RuleNodeEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.ValidationRuleEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for pre-flight validation rule compatibility checks
 * Allows campaign service to validate rule compatibility before starting saga
 */
@RestController
@RequestMapping("${spring.application.context-path}/api/v1/validation-rules")
public class ValidationRuleCompatibilityController {

    private static final Logger logger = LoggerFactory.getLogger(ValidationRuleCompatibilityController.class);

    private final ValidationRuleJpaRepository validationRuleRepository;

    public ValidationRuleCompatibilityController(ValidationRuleJpaRepository validationRuleRepository) {
        this.validationRuleRepository = validationRuleRepository;
    }

    /**
     * Pre-flight validation endpoint
     * Checks if a rule is compatible with the given campaign type
     *
     * @param request Contains ruleId and campaignType
     * @return Validation result with success/failure status
     */
    @PostMapping("/validate-compatibility")
    public ResponseEntity<ValidateCompatibilityResponse> validateCompatibility(
            @Valid @RequestBody ValidateCompatibilityRequest request) {

        if (logger.isInfoEnabled()) {
            logger.info("Pre-flight validation check: ruleId={}, campaignType={}", request.ruleId(), request.campaignType());
        }

        try {
            // 1. Check if rule exists
            Optional<ValidationRuleEntity> ruleOpt = validationRuleRepository.findById(request.ruleId());
            if (ruleOpt.isEmpty()) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Rule not found: ruleId={}", request.ruleId());
                }
                return ResponseEntity.ok(ValidateCompatibilityResponse.failure(
                        request.ruleId(),
                        "RULE_NOT_FOUND",
                        "Validation rule not found: " + request.ruleId()
                ));
            }

            ValidationRuleEntity rule = ruleOpt.get();

            // 2. Check if rule is published (not draft or archived)
            if (!"published".equalsIgnoreCase(rule.getState())) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Rule is not published: ruleId={}, state={}", request.ruleId(), rule.getState());
                }
                return ResponseEntity.ok(ValidateCompatibilityResponse.failure(
                        request.ruleId(),
                        "RULE_NOT_PUBLISHED",
                        "Validation rule is not published. State: " + rule.getState()
                ));
            }

            // 3. Validate rule compatibility with campaign type
            // Check if rule contains required nodes based on campaign type
            if (!isRuleCompatibleWithCampaignType(rule, request.campaignType())) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Rule is not compatible with campaign type: ruleId={}, campaignType={}", request.ruleId(), request.campaignType());
                }
                return ResponseEntity.ok(ValidateCompatibilityResponse.failure(
                        request.ruleId(),
                        "RULE_INCOMPATIBLE_WITH_CAMPAIGN_TYPE",
                        "Validation rule is not compatible with campaign type: " + request.campaignType()
                ));
            }

            // 4. Success - rule is valid and compatible
            if (logger.isInfoEnabled()) {
                logger.info("Pre-flight validation passed: ruleId={}, campaignType={}", request.ruleId(), request.campaignType());
            }

            return ResponseEntity.ok(ValidateCompatibilityResponse.success(
                    rule.getId(),
                    rule.getName(),
                    rule.getState()
            ));

        } catch (Exception e) {
            logger.error("Error during pre-flight validation: ruleId={}", request.ruleId(), e);
            return ResponseEntity.internalServerError().body(ValidateCompatibilityResponse.failure(
                    request.ruleId(),
                    "VALIDATION_ERROR",
                    "Unexpected error during validation: " + e.getMessage()
            ));
        }
    }

    /**
     * Check if rule is compatible with the given campaign type
     * This can be extended with more sophisticated logic as needed
     *
     * @param rule         The validation rule entity
     * @param campaignType The type of campaign (e.g., "CASHBACK", "DISCOUNT", "VOUCHER")
     * @return true if compatible, false otherwise
     */
    private boolean isRuleCompatibleWithCampaignType(ValidationRuleEntity rule, String campaignType) {
        // Basic check: ensure rule has nodes
        if (rule.getNodes() == null || rule.getNodes().isEmpty()) {
            if (logger.isWarnEnabled()) {
                logger.warn("Rule has no nodes: ruleId={}", rule.getId());
            }
            return false;
        }

        // For CASHBACK and DISCOUNT campaigns, check for product applicability node
        // This ensures the rule can validate product-based criteria
        if ("CASHBACK".equalsIgnoreCase(campaignType) || "DISCOUNT".equalsIgnoreCase(campaignType)) {
            boolean hasProductApplicability = hasProductApplicabilityNode(rule.getNodes());
            if (!hasProductApplicability && logger.isDebugEnabled()) {
                logger.debug("Rule does not have product applicability node for campaign type: {}",
                        campaignType);
            }
            // This is not necessarily a failure - some rules may be campaign-level only
            // For now, we'll allow it but log the information
        }

        // Additional compatibility checks can be added here based on campaign type
        // For example:
        // - Check for time-based nodes for time-limited campaigns
        // - Check for customer segment nodes for segmented campaigns
        // - Check for minimum order value nodes for threshold-based campaigns

        // Default: rule is compatible if it's well-formed
        return true;
    }

    /**
     * Check if rule contains product applicability node (product.applicability.in operator)
     *
     * @param nodes The rule nodes to search
     * @return true if product applicability node found, false otherwise
     */
    private boolean hasProductApplicabilityNode(List<RuleNodeEntity> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }

        for (RuleNodeEntity node : nodes) {
            if ("COND".equals(node.getType()) &&
                    "product.applicability.in".equals(node.getOperatorName())) {
                return true;
            }
        }

        return false;
    }
}
