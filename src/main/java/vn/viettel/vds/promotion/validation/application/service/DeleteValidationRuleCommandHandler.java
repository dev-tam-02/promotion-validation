package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.factory.ExceptionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineDeploymentService;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.command.DeleteValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.DeleteValidationRuleCommand.DeleteValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.exception.ValidationException;

import java.time.Instant;
import java.util.List;

/**
 * Service xử lý DeleteValidationRuleCommand để soft delete validation rule assignments.
 * <p>
 * Chức năng chính:
 * - Validate command từ Kafka
 * - Tìm và soft delete (set active = false) các assignments theo campaignId
 * - Undeploy rules khỏi validation-engine
 * - Publish events thông báo kết quả
 * <p>
 * Lưu ý: Đây là soft delete, không xóa vật lý khỏi database
 *
 * @author Validation Team
 * @since 1.0.0
 */
@Service
@Transactional
public class DeleteValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeleteValidationRuleCommandHandler.class);

    private final AssignmentJpaRepository assignmentRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    @SuppressWarnings("unused") // Reserved for future use
    private final ValidationEngineDeploymentService validationEngineClient;
    private final SettingValidationRuleEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;

    public DeleteValidationRuleCommandHandler(
            AssignmentJpaRepository assignmentRepository,
            ValidationRuleJpaRepository validationRuleRepository,
            ValidationEngineDeploymentService validationEngineClient,
            SettingValidationRuleEventPublisher eventPublisher,
            IdempotencyService idempotencyService) {
        this.assignmentRepository = assignmentRepository;
        this.validationRuleRepository = validationRuleRepository;
        this.validationEngineClient = validationEngineClient;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Xử lý delete command từ campaign service.
     * <p>
     * Logic flow:
     * 1. Kiểm tra idempotency - nếu đã xử lý thì return true ngay
     * 2. Validate command payload
     * 3. Tìm assignments theo campaignId và validationRuleId (nếu có)
     * 4. Soft delete từng assignment (set active = false)
     * 5. Undeploy rules khỏi validation-engine
     * 6. Mark command as processed
     * 7. Publish success event
     *
     * @param command Delete command chứa campaignId và optional validationRuleId
     * @return true nếu delete thành công, false nếu thất bại
     * @throws BusinessException nếu validation fails (non-retryable, gửi DLQ ngay)
     */
    @SuppressWarnings("java:S2139") // Exception được log đầy đủ trước khi rethrow
    public boolean handleCommand(DeleteValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing DeleteValidationRuleCommand: commandId={}", commandId);

            // Bước 1: Kiểm tra idempotency - nếu đã xử lý rồi thì return success ngay
            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Delete command already processed (idempotent check): commandId={}", commandId);
                return true;
            }

            // Bước 2: Validate command payload
            validateCommand(command);

            // Extract command payload
            DeleteValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Delete command payload is null: commandId={}", commandId);
                publishDeleteErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String campaignId = payload.getCampaignId();
            String validationRuleId = payload.getValidationRuleId() != null ?
                    payload.getValidationRuleId() : null;
            boolean deleteAll = Boolean.TRUE.equals(payload.getDeleteAll());

            logger.info("Delete request: campaignId={}, validationRuleId={}, deleteAll={}",
                    campaignId, validationRuleId, deleteAll);

            // Bước 3 & 4: Execute delete logic
            boolean success = executeDelete(campaignId, validationRuleId, deleteAll);

            // Xử lý thất bại - throw BusinessException với error code cụ thể
            if (!success) {
                String errorCode = "DELETE_FAILED";
                String errorMessage = "Failed to delete validation rule assignment";
                publishDeleteErrorEvent(commandId, campaignId, errorCode, errorMessage);
                logger.error("Failed to process DeleteValidationRuleCommand: commandId={}, campaignId={}, errorCode={}",
                        commandId, campaignId, errorCode);
                throw ExceptionFactory.createValidationException(errorCode, errorMessage);
            }

            // Bước 5: Mark as processed sau khi delete thành công
            idempotencyService.markAsProcessed(commandId, "Delete completed successfully");

            // Bước 6: Publish success event
            publishDeleteSuccessEvent(commandId, campaignId, validationRuleId);

            logger.info("Successfully processed DeleteValidationRuleCommand: commandId={}, campaignId={}",
                    commandId, campaignId);
            return true;

        } catch (BusinessException e) {
            // Re-throw BusinessException (validation errors) để promix-messaging xử lý
            // BusinessException với BAD_REQUEST → DLQ ngay lập tức (non-retryable)
            logger.error("Validation failed for DeleteValidationRuleCommand: commandId={}, error={}",
                    commandId, e.getMessage(), e);
            throw e; // NOSONAR - Exception được log đầy đủ trước khi rethrow
        } catch (Exception e) {
            logger.error("Unexpected error processing DeleteValidationRuleCommand: commandId={}", commandId, e);
            String campaignId = command.getPayload() != null ? command.getPayload().getCampaignId() : null;
            publishDeleteErrorEvent(commandId, campaignId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate command sử dụng Bean Validation annotations.
     * <p>
     * Validation flow:
     * 1. Kiểm tra command và payload not null
     * 2. Run Bean Validation
     * 3. Throw ValidationException nếu có lỗi
     *
     * @param command Command cần validate
     * @throws BusinessException nếu validation fails với error codes cụ thể
     */
    private void validateCommand(DeleteValidationRuleCommand command) {
        // Bước 1: Null check
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw ExceptionFactory.createValidationException(
                    "INVALID_COMMAND",
                    "Command or payload is null"
            );
        }

        // Bước 2: Bean Validation (có thể thêm DTO validation nếu cần)
        // Hiện tại skip vì chưa có DTO mapper, validate trực tiếp payload

        // Validate campaignId
        if (command.getPayload().getCampaignId() == null || command.getPayload().getCampaignId().isBlank()) {
            logger.error("DeleteValidationRuleCommand validation failed: campaignId is required");
            throw ExceptionFactory.createValidationException(
                    "INVALID_CAMPAIGN_ID",
                    "campaignId is required"
            );
        }

        logger.debug("DeleteValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    /**
     * Thực thi logic delete assignments.
     *
     * @param campaignId       Campaign ID để tìm assignments
     * @param validationRuleId Assignment ID cụ thể để delete (optional)
     * @param deleteAll        Nếu true, delete tất cả assignments của campaign
     * @return true nếu thành công, false nếu thất bại
     */
    private boolean executeDelete(String campaignId, String validationRuleId, boolean deleteAll) {
        try {
            // Tìm assignments theo campaign ID
            List<AssignmentEntity> assignments = findAssignmentsByCampaignId(campaignId);

            if (assignments.isEmpty()) {
                logger.warn("No assignments found for campaign: campaignId={}", campaignId);
                // Không phải error - assignment có thể chưa được tạo
                return true;
            }

            // Filter assignments nếu có validationRuleId cụ thể
            if (!deleteAll && validationRuleId != null) {
                assignments = assignments.stream()
                        .filter(a -> validationRuleId.equals(a.getId()))
                        .toList();

                if (assignments.isEmpty()) {
                    logger.warn("No assignment found with specific ID: validationRuleId={}", validationRuleId);
                    return true;
                }
            }

            logger.info("Found {} assignment(s) to delete for campaign: campaignId={}", assignments.size(), campaignId);

            // Delete từng assignment (soft delete)
            for (AssignmentEntity assignment : assignments) {
                deleteAssignment(assignment);
            }

            return true;

        } catch (Exception e) {
            logger.error("Error executing delete for campaign: campaignId={}", campaignId, e);
            return false;
        }
    }

    /**
     * Soft delete một assignment.
     * Đánh dấu assignment là INACTIVE và undeploy khỏi validation-engine.
     *
     * @param assignment Assignment cần delete
     */
    private void deleteAssignment(AssignmentEntity assignment) {
        try {
            logger.info("Deleting assignment: assignmentId={}, ruleId={}, campaignId={}",
                    assignment.getId(), assignment.getRuleId(), assignment.getEntityId());

            // Soft delete: Set active = false thay vì xóa vật lý
            assignment.setActive(false);
            assignment.setUpdatedAt(Instant.now());
            assignmentRepository.save(assignment);

            logger.info("Marked assignment as inactive: assignmentId={}", assignment.getId());

            // Undeploy rule khỏi validation-engine
            undeployRuleFromEngine(assignment);

            logger.info("Successfully deleted assignment: assignmentId={}", assignment.getId());

        } catch (Exception e) {
            throw new ValidationException("Failed to delete assignment: " + assignment.getId() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Tìm assignments theo campaign ID.
     *
     * @param campaignId Campaign ID để search
     * @return List assignments của campaign đó
     */
    private List<AssignmentEntity> findAssignmentsByCampaignId(String campaignId) {
        // Sử dụng optimized query method thay vì findAll() + filter in memory
        return assignmentRepository.findByEntityTypeAndEntityId("campaign", campaignId);
    }

    /**
     * Undeploy rule khỏi validation-engine.
     *
     * @param assignment Assignment chứa rule cần undeploy
     */
    private void undeployRuleFromEngine(AssignmentEntity assignment) {
        try {
            String ruleId = assignment.getRuleId();

            // Kiểm tra validation rule có tồn tại không
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
            // Không fail toàn bộ delete vì undeployment issues
        }
    }

    /**
     * Publish delete success event.
     */
    private void publishDeleteSuccessEvent(String commandId, String campaignId, String validationRuleId) {
        try {
            eventPublisher.publishDeleteSuccessEvent(commandId, campaignId, validationRuleId);
        } catch (Exception e) {
            logger.error("Failed to publish delete success event: commandId={}", commandId, e);
        }
    }

    /**
     * Publish delete error event.
     */
    private void publishDeleteErrorEvent(String commandId, String campaignId, String errorCode, String errorMessage) {
        try {
            eventPublisher.publishDeleteErrorEvent(commandId, campaignId, errorCode, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to publish delete error event: commandId={}", commandId, e);
        }
    }

    /**
     * Xử lý dead letter command từ DLQ topic.
     * Log failed command để manual investigation và alerting.
     *
     * @param command Failed delete command từ DLQ
     */
    public void handleDeadLetterCommand(DeleteValidationRuleCommand command) {
        logger.error("Processing dead letter DeleteValidationRuleCommand: commandId={}, campaignId={}, reason={}",
                command.getId(),
                command.getPayload() != null ? command.getPayload().getCampaignId() : "unknown",
                command.getPayload() != null ? command.getPayload().getDeleteReason() : "unknown");

        // Log detailed information để debugging
        if (command.getPayload() != null) {
            logger.error("Dead letter delete details: validationRuleId={}, deleteAll={}, correlationId={}",
                    command.getPayload().getValidationRuleId(),
                    command.getPayload().getDeleteAll(),
                    command.getPayload().getCorrelationId());
        }

        // Có thể trigger alerting system, lưu vào DB để manual processing, etc.
        // Hiện tại chỉ log và acknowledge
        publishDeleteErrorEvent(
                command.getId(),
                command.getPayload() != null ? command.getPayload().getCampaignId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue after multiple retries"
        );
    }
}
