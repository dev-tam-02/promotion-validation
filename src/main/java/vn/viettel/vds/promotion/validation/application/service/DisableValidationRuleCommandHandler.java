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
import vn.viettel.vds.promotion.validation.command.DisableValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.DisableValidationRuleCommand.DisableValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationException;

import java.time.Instant;
import java.util.List;

/**
 * Service xử lý DisableValidationRuleCommand để disable validation rule assignments.
 *
 * Chức năng chính:
 * - Validate command từ Kafka
 * - Tìm và disable (set active = false) assignments theo campaignId + validationRuleId
 * - Undeploy rules khỏi validation-engine
 * - Publish events thông báo kết quả
 *
 * Lưu ý: Disable khác với Delete - Disable có thể Enable lại, Delete là soft delete vĩnh viễn
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Service
@Transactional
public class DisableValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(DisableValidationRuleCommandHandler.class);

    private final AssignmentJpaRepository assignmentRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;

    public DisableValidationRuleCommandHandler(
            AssignmentJpaRepository assignmentRepository,
            ValidationRuleJpaRepository validationRuleRepository,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService) {
        this.assignmentRepository = assignmentRepository;
        this.validationRuleRepository = validationRuleRepository;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Xử lý disable command từ campaign service.
     *
     * Logic flow:
     * 1. Kiểm tra idempotency - nếu đã xử lý thì return true ngay
     * 2. Validate command payload
     * 3. Tìm assignment theo campaignId và validationRuleId
     * 4. Disable assignment (set active = false)
     * 5. Undeploy rule khỏi validation-engine
     * 6. Mark command as processed
     * 7. Publish success event
     *
     * @param command Disable command chứa campaignId và validationRuleId
     * @return true nếu disable thành công, false nếu thất bại
     * @throws BusinessException nếu validation fails (non-retryable, gửi DLQ ngay)
     */
    @SuppressWarnings("java:S2139") // Exception được log đầy đủ trước khi rethrow
    public boolean handleCommand(DisableValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing DisableValidationRuleCommand: commandId={}", commandId);

            // Bước 1: Kiểm tra idempotency
            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Disable command already processed (idempotent check): commandId={}", commandId);
                return true;
            }

            // Bước 2: Validate command
            validateCommand(command);

            // Extract payload
            DisableValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Disable command payload is null: commandId={}", commandId);
                publishDisableErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String campaignId = payload.getCampaignId();
            String validationRuleId = payload.getValidationRuleId();
            String disableReason = payload.getDisableReason();

            logger.info("Disable request: campaignId={}, validationRuleId={}, reason={}",
                    campaignId, validationRuleId, disableReason);

            // Bước 3 & 4 & 5: Execute disable logic
            boolean success = executeDisable(campaignId, validationRuleId);

            if (!success) {
                String errorCode = "DISABLE_FAILED";
                String errorMessage = "Failed to disable validation rule assignment";
                publishDisableErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process DisableValidationRuleCommand: commandId={}, campaignId={}, errorCode={}",
                        commandId, campaignId, errorCode);
                throw ExceptionFactory.createValidationException(errorCode, errorMessage);
            }

            // Bước 6: Mark as processed
            idempotencyService.markAsProcessed(commandId, "Disable completed successfully");

            // Bước 7: Publish success event
            publishDisableSuccessEvent(commandId, campaignId, validationRuleId);

            logger.info("Successfully processed DisableValidationRuleCommand: commandId={}, campaignId={}",
                    commandId, campaignId);
            return true;

        } catch (BusinessException e) {
            logger.error("Validation failed for DisableValidationRuleCommand: commandId={}, error={}",
                commandId, e.getMessage(), e);
            throw e; // NOSONAR - Exception được log đầy đủ trước khi rethrow
        } catch (Exception e) {
            logger.error("Unexpected error processing DisableValidationRuleCommand: commandId={}", commandId, e);
            String campaignId = command.getPayload() != null ? command.getPayload().getCampaignId() : null;
            publishDisableErrorEvent(commandId, campaignId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate command.
     *
     * @param command Command cần validate
     * @throws BusinessException nếu validation fails
     */
    private void validateCommand(DisableValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw ExceptionFactory.createValidationException(
                "INVALID_COMMAND",
                "Command or payload is null"
            );
        }

        // Validate required fields
        DisableValidationRuleCommandPayload payload = command.getPayload();
        if (payload.getCampaignId() == null || payload.getCampaignId().isBlank()) {
            throw ExceptionFactory.createValidationException("INVALID_CAMPAIGN_ID", "campaignId is required");
        }

        if (payload.getValidationRuleId() == null || payload.getValidationRuleId().isBlank()) {
            throw ExceptionFactory.createValidationException("INVALID_VALIDATION_RULE_ID", "validationRuleId is required");
        }

        logger.debug("DisableValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    /**
     * Thực thi logic disable assignment.
     *
     * @param campaignId Campaign ID
     * @param validationRuleId Assignment ID cần disable
     * @return true nếu thành công, false nếu thất bại
     */
    private boolean executeDisable(String campaignId, String validationRuleId) {
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

            logger.info("Found assignment to disable: assignmentId={}, ruleId={}, currentActive={}",
                    assignment.getId(), assignment.getRuleId(), assignment.getActive());

            // Disable assignment
            assignment.setActive(false);
            assignment.setUpdatedAt(Instant.now());
            assignmentRepository.save(assignment);

            logger.info("Disabled assignment: assignmentId={}", assignment.getId());

            // Undeploy rule khỏi validation-engine
            undeployRuleFromEngine(assignment);

            return true;

        } catch (Exception e) {
            logger.error("Error executing disable for campaign: campaignId={}, validationRuleId={}",
                    campaignId, validationRuleId, e);
            return false;
        }
    }

    /**
     * Undeploy rule khỏi validation-engine.
     *
     * @param assignment Assignment chứa rule cần undeploy
     */
    private void undeployRuleFromEngine(AssignmentEntity assignment) {
        try {
            String ruleId = assignment.getRuleId();

            // Kiểm tra rule có tồn tại không
            if (ruleId == null || ruleId.isBlank()) {
                logger.info("Skipping undeployment - assignment has no ruleId: assignmentId={}", assignment.getId());
                return;
            }

            var ruleOpt = validationRuleRepository.findById(ruleId);
            if (ruleOpt.isEmpty()) {
                logger.warn("Validation rule not found for undeployment: ruleId={}", ruleId);
                return;
            }

            // Note: removeRule() is deprecated và không làm gì
            // Bundle management hiện tại là automatic trong validation-engine
            logger.info("Rule removal requested (automatic management): ruleId={}, assignmentId={}",
                    ruleId, assignment.getId());

        } catch (Exception e) {
            logger.error("Error undeploying rule from validation-engine: assignmentId={}",
                    assignment.getId(), e);
            // Không fail toàn bộ disable vì undeployment issues
        }
    }

    /**
     * Publish disable success event.
     */
    private void publishDisableSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        try {
            eventPublisher.publishDisableSuccessEvent(commandId, campaignId, validationRuleId);
        } catch (Exception e) {
            logger.error("Failed to publish disable success event: commandId={}", commandId, e);
        }
    }

    /**
     * Publish disable error event.
     */
    private void publishDisableErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishDisableErrorEvent(commandId, campaignId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish disable error event: commandId={}", commandId, e);
        }
    }

    /**
     * Xử lý dead letter command từ DLQ topic.
     *
     * @param command Failed disable command từ DLQ
     */
    public void handleDeadLetterCommand(DisableValidationRuleCommand command) {
        logger.error("Processing dead letter DisableValidationRuleCommand: commandId={}, campaignId={}, validationRuleId={}, reason={}",
                command.getId(),
                command.getPayload() != null ? command.getPayload().getCampaignId() : "unknown",
                command.getPayload() != null ? command.getPayload().getValidationRuleId() : "unknown",
                command.getPayload() != null ? command.getPayload().getDisableReason() : "unknown");

        publishDisableErrorEvent(
                command.getId(),
                command.getPayload() != null ? command.getPayload().getCampaignId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue after multiple retries"
        );
    }
}
