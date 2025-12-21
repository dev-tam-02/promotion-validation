package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.factory.ExceptionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.command.EnableValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.EnableValidationRuleCommand.EnableValidationRuleCommandPayload;

import java.time.Instant;
import java.util.List;

/**
 * Service xử lý EnableValidationRuleCommand để enable validation rule assignments.
 * <p>
 * Chức năng chính:
 * - Validate command từ Kafka
 * - Tìm và enable (set active = true) assignments theo campaignId + validationRuleId
 * - Deploy rules lên validation-engine
 * - Publish events thông báo kết quả
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Service
@Transactional
public class EnableValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(EnableValidationRuleCommandHandler.class);

    private final AssignmentJpaRepository assignmentRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final vn.viettel.vds.promotion.validation.domain.service.RulePublishingService rulePublishingService;

    public EnableValidationRuleCommandHandler(
            AssignmentJpaRepository assignmentRepository,
            ValidationRuleJpaRepository validationRuleRepository,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            vn.viettel.vds.promotion.validation.domain.service.RulePublishingService rulePublishingService) {
        this.assignmentRepository = assignmentRepository;
        this.validationRuleRepository = validationRuleRepository;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.rulePublishingService = rulePublishingService;
    }

    /**
     * Xử lý enable command từ campaign service.
     * <p>
     * Logic flow:
     * 1. Kiểm tra idempotency - nếu đã xử lý thì return true ngay
     * 2. Validate command payload
     * 3. Tìm assignment theo campaignId và validationRuleId
     * 4. Enable assignment (set active = true)
     * 5. Deploy rule lên validation-engine
     * 6. Mark command as processed
     * 7. Publish success event
     *
     * @param command Enable command chứa campaignId và validationRuleId
     * @return true nếu enable thành công, false nếu thất bại
     * @throws BusinessException nếu validation fails (non-retryable, gửi DLQ ngay)
     */
    @SuppressWarnings("java:S2139") // Exception được log đầy đủ trước khi rethrow
    public boolean handleCommand(EnableValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing EnableValidationRuleCommand: commandId={}", commandId);

            // Bước 1: Kiểm tra idempotency
            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Enable command already processed (idempotent check): commandId={}", commandId);
                return true;
            }

            // Bước 2: Validate command
            validateCommand(command);

            // Extract payload
            EnableValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Enable command payload is null: commandId={}", commandId);
                publishEnableErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String campaignId = payload.getCampaignId();
            String validationRuleId = payload.getValidationRuleId();

            logger.info("Enable request: campaignId={}, validationRuleId={}", campaignId, validationRuleId);

            // Bước 3 & 4 & 5: Execute enable logic
            boolean success = executeEnable(campaignId, validationRuleId);

            if (!success) {
                String errorCode = "ENABLE_FAILED";
                String errorMessage = "Failed to enable validation rule assignment";
                publishEnableErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process EnableValidationRuleCommand: commandId={}, campaignId={}, errorCode={}",
                        commandId, campaignId, errorCode);
                throw ExceptionFactory.createValidationException(errorCode, errorMessage);
            }

            // Bước 6: Mark as processed
            idempotencyService.markAsProcessed(commandId, "Enable completed successfully");

            // Bước 7: Publish success event
            publishEnableSuccessEvent(commandId, campaignId, validationRuleId);

            logger.info("Successfully processed EnableValidationRuleCommand: commandId={}, campaignId={}",
                    commandId, campaignId);
            return true;

        } catch (BusinessException e) {
            logger.error("Validation failed for EnableValidationRuleCommand: commandId={}, error={}",
                    commandId, e.getMessage(), e);
            throw e; // NOSONAR - Exception được log đầy đủ trước khi rethrow
        } catch (Exception e) {
            logger.error("Unexpected error processing EnableValidationRuleCommand: commandId={}", commandId, e);
            String campaignId = command.getPayload() != null ? command.getPayload().getCampaignId() : null;
            publishEnableErrorEvent(commandId, campaignId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate command.
     *
     * @param command Command cần validate
     * @throws BusinessException nếu validation fails
     */
    private void validateCommand(EnableValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw ExceptionFactory.createValidationException(
                    "INVALID_COMMAND",
                    "Command or payload is null"
            );
        }

        // Validate required fields
        EnableValidationRuleCommandPayload payload = command.getPayload();
        if (payload.getCampaignId() == null || payload.getCampaignId().isBlank()) {
            throw ExceptionFactory.createValidationException("INVALID_CAMPAIGN_ID", "campaignId is required");
        }

        if (payload.getValidationRuleId() == null || payload.getValidationRuleId().isBlank()) {
            throw ExceptionFactory.createValidationException("INVALID_VALIDATION_RULE_ID", "validationRuleId is required");
        }

        logger.debug("EnableValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    /**
     * Thực thi logic enable assignment.
     *
     * @param campaignId       Campaign ID
     * @param validationRuleId Assignment ID cần enable
     * @return true nếu thành công, false nếu thất bại
     */
    private boolean executeEnable(String campaignId, String validationRuleId) {
        try {
            // Tìm assignment theo validationRuleId (assignment ID)
            AssignmentEntity assignment = assignmentRepository.findById(validationRuleId)
                    .orElse(null);

            // Nếu không tìm thấy theo ID, thử tìm theo campaignId
            if (assignment == null) {
                List<AssignmentEntity> assignments = assignmentRepository.findByEntityTypeAndEntityId("campaign", campaignId);
                if (assignments.isEmpty()) {
                    logger.warn("No assignment found for campaign: campaignId={}", campaignId);
                    return false;
                }
                // Lấy assignment đầu tiên nếu có nhiều
                assignment = assignments.get(0);
            }

            logger.info("Found assignment to enable: assignmentId={}, ruleId={}, currentActive={}",
                    assignment.getId(), assignment.getRuleId(), assignment.getActive());

            // Enable assignment
            assignment.setActive(true);
            assignment.setUpdatedAt(Instant.now());
            assignmentRepository.save(assignment);

            logger.info("Enabled assignment: assignmentId={}", assignment.getId());

            // Deploy rule lên validation-engine nếu assignment có ruleId
            if (assignment.getRuleId() != null && !assignment.getRuleId().isBlank()) {
                deployRuleToEngine(assignment);
            } else {
                logger.info("Skipping deployment - assignment has no ruleId: assignmentId={}", assignment.getId());
            }

            return true;

        } catch (Exception e) {
            logger.error("Error executing enable for campaign: campaignId={}, validationRuleId={}",
                    campaignId, validationRuleId, e);
            return false;
        }
    }

    /**
     * Deploy rule lên validation-engine.
     *
     * @param assignment Assignment chứa rule cần deploy
     */
    private void deployRuleToEngine(AssignmentEntity assignment) {
        try {
            String ruleId = assignment.getRuleId();
            String assignmentId = assignment.getId();

            logger.info("Deploying rule to validation-engine: ruleId={}, assignmentId={}", ruleId, assignmentId);

            // Kiểm tra rule có tồn tại không
            var ruleOpt = validationRuleRepository.findById(ruleId);
            if (ruleOpt.isEmpty()) {
                logger.warn("Validation rule not found for deployment: ruleId={}", ruleId);
                return;
            }

            // Deploy rule sử dụng RulePublishingService
            // ApplicabilityScope null vì enable không thay đổi applicability
            var publishResult = rulePublishingService.publishRule(ruleId, assignmentId, null);

            if (publishResult.isSuccess()) {
                assignment.setTemporalBundleHash(publishResult.getBundleHash());
                // Explicit save to persist temporalBundleHash - entity was saved before this method was called
                assignmentRepository.save(assignment);
                logger.info("Rule deployed successfully: ruleId={}, assignmentId={}, bundleHash={}",
                        ruleId, assignmentId, publishResult.getBundleHash());
            } else {
                logger.error("Failed to deploy rule: ruleId={}, assignmentId={}, error={}",
                        ruleId, assignmentId, publishResult.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Error deploying rule to validation-engine: assignmentId={}",
                    assignment.getId(), e);
            // Không fail toàn bộ enable vì deployment issues
        }
    }

    /**
     * Publish enable success event.
     */
    private void publishEnableSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        try {
            eventPublisher.publishEnableSuccessEvent(commandId, campaignId, validationRuleId);
        } catch (Exception e) {
            logger.error("Failed to publish enable success event: commandId={}", commandId, e);
        }
    }

    /**
     * Publish enable error event.
     */
    private void publishEnableErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishEnableErrorEvent(commandId, campaignId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish enable error event: commandId={}", commandId, e);
        }
    }

    /**
     * Xử lý dead letter command từ DLQ topic.
     *
     * @param command Failed enable command từ DLQ
     */
    public void handleDeadLetterCommand(EnableValidationRuleCommand command) {
        logger.error("Processing dead letter EnableValidationRuleCommand: commandId={}, campaignId={}, validationRuleId={}",
                command.getId(),
                command.getPayload() != null ? command.getPayload().getCampaignId() : "unknown",
                command.getPayload() != null ? command.getPayload().getValidationRuleId() : "unknown");

        publishEnableErrorEvent(
                command.getId(),
                command.getPayload() != null ? command.getPayload().getCampaignId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue after multiple retries"
        );
    }
}
