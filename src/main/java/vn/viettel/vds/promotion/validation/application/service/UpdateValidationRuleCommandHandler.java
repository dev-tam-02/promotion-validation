package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ErrorDetail;
import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.factory.ExceptionFactory;
import com.promix.platform.core.util.IdGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.UpdateValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper.UpdateValidationRuleCommandDTOMapper;
import vn.viettel.vds.promotion.validation.application.port.out.RuleBindingPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.ValidationRuleRepositoryPort;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.ApplicabilityScope;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand.UpdateValidationRuleCommandPayload;
import vn.viettel.vds.promotion.validation.domain.model.RuleBinding;
import vn.viettel.vds.promotion.validation.event.ValidationSettingUpdateResultEvent;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to handle UpdateValidationRuleCommand processing.
 * <p>
 * Refactored to use unified RuleBinding model.
 */
@Service
@Transactional
public class UpdateValidationRuleCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpdateValidationRuleCommandHandler.class);
    private static final String SOURCE = "validation-service";

    private final RuleBindingPersistencePort ruleBindingPort;
    private final ValidationRuleRepositoryPort validationRulePort;
    private final IdempotencyService idempotencyService;
    private final RulePublishingService rulePublishingService;
    private final Validator validator;
    private final UpdateValidationRuleCommandDTOMapper dtoMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.validation-event:promotion_validation_event}")
    private String validationEventTopic;

    public UpdateValidationRuleCommandHandler(
            RuleBindingPersistencePort ruleBindingPort,
            ValidationRuleRepositoryPort validationRulePort,
            IdempotencyService idempotencyService,
            RulePublishingService rulePublishingService,
            Validator validator,
            UpdateValidationRuleCommandDTOMapper dtoMapper,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.ruleBindingPort = ruleBindingPort;
        this.validationRulePort = validationRulePort;
        this.idempotencyService = idempotencyService;
        this.rulePublishingService = rulePublishingService;
        this.validator = validator;
        this.dtoMapper = dtoMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @SuppressWarnings("java:S2139")
    public boolean handleCommand(UpdateValidationRuleCommand command) {
        String commandId = command.getId();

        try {
            logger.info("Processing UpdateValidationRuleCommand: commandId={}", commandId);

            if (idempotencyService.isProcessed(commandId)) {
                logger.info("Update command already processed: commandId={}", commandId);
                return true;
            }

            validateCommand(command);

            UpdateValidationRuleCommandPayload payload = command.getPayload();
            if (payload == null) {
                logger.error("Update command payload is null: commandId={}", commandId);
                publishErrorEvent(commandId, null, "INVALID_PAYLOAD", "Command payload is missing");
                return false;
            }

            String objectType = payload.getObjectType();
            String objectId = payload.getObjectId();
            String assignmentId = payload.getAssignmentId();

            logger.info("Update request: objectType={}, objectId={}, assignmentId={}", objectType, objectId, assignmentId);

            // Find existing binding
            RuleBinding existingBinding = findExistingBinding(assignmentId, objectType, objectId);
            if (existingBinding == null) {
                String errorMessage = "Binding not found: assignmentId=" + assignmentId;
                logger.error(errorMessage);
                publishErrorEvent(commandId, objectId, "BINDING_NOT_FOUND", errorMessage);
                throw ExceptionFactory.createValidationException("BINDING_NOT_FOUND", errorMessage);
            }

            // Update binding
            RuleBinding updatedBinding = updateBinding(existingBinding, payload);

            // Deploy to validation-engine
            deployRuleToEngine(updatedBinding, payload.getApplicableTo());

            // Mark as processed
            idempotencyService.markAsProcessed(commandId, "Update completed successfully");

            // Publish success event
            publishSuccessEvent(commandId, objectId, updatedBinding);

            logger.info("Successfully processed UpdateValidationRuleCommand: commandId={}", commandId);
            return true;

        } catch (BusinessException e) {
            logger.error("Validation failed: commandId={}, error={}", commandId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: commandId={}", commandId, e);
            String objectId = command.getPayload() != null ? command.getPayload().getObjectId() : null;
            publishErrorEvent(commandId, objectId, "PROCESSING_ERROR", "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private void validateCommand(UpdateValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            logger.error("Received null command or null payload");
            throw ExceptionFactory.createValidationException("INVALID_COMMAND", "Command or payload is null");
        }

        UpdateValidationRuleCommandDTO dto = dtoMapper.toDTO(command);
        if (dto == null) {
            logger.error("Failed to convert command to DTO");
            throw ExceptionFactory.createValidationException("INVALID_COMMAND", "Failed to convert command to DTO");
        }

        Set<ConstraintViolation<UpdateValidationRuleCommandDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            List<ErrorDetail> errorDetails = violations.stream()
                    .map(violation -> ErrorDetail.of(
                            violation.getPropertyPath().toString(),
                            violation.getMessage(),
                            String.format("Invalid value: %s", violation.getInvalidValue()),
                            violation.getInvalidValue()
                    ))
                    .toList();

            String errorMessage = String.format("Validation failed with %d error(s)", violations.size());

            logger.error("UpdateValidationRuleCommand validation failed: commandId={}, errors={}",
                    command.getId(), errorDetails);

            throw ExceptionFactory.createValidationException(
                    "METHOD_ARGUMENT_NOT_VALID",
                    errorMessage,
                    errorDetails.toArray(new ErrorDetail[0])
            );
        }

        logger.debug("UpdateValidationRuleCommand validation passed: commandId={}", command.getId());
    }

    private RuleBinding findExistingBinding(String assignmentId, String objectType, String objectId) {
        // Try to find by ID first
        if (assignmentId != null && !assignmentId.isBlank()) {
            Optional<RuleBinding> binding = ruleBindingPort.findById(assignmentId);
            if (binding.isPresent()) {
                return binding.get();
            }
        }

        // Search by target
        List<RuleBinding> bindings = ruleBindingPort.findByTarget(objectType, objectId);
        return bindings.isEmpty() ? null : bindings.get(0);
    }

    private RuleBinding updateBinding(RuleBinding existing, UpdateValidationRuleCommandPayload payload) {
        RuleBinding.RuleBindingBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now())
                .updatedBy(payload.getUpdatedBy() != null ? payload.getUpdatedBy() : "system");

        // Update rule ID if provided
        if (payload.getRuleId() != null && !payload.getRuleId().isBlank()) {
            builder.ruleId(payload.getRuleId());
        }

        // Update active status if provided
        if (payload.getActive() != null) {
            builder.active(payload.getActive());
        }

        // Update traffic percent if provided
        if (payload.getTrafficPercent() != null) {
            builder.trafficPercent(payload.getTrafficPercent());
        }

        // Update priority if provided
        if (payload.getPriority() != null) {
            builder.priority(payload.getPriority());
        }

        // Update applicability data
        ApplicabilityScope applicableToData = payload.getApplicableTo();
        if (applicableToData != null) {
            builder.includedAll(Boolean.TRUE.equals(applicableToData.getIncludedAll()));

            if (applicableToData.getIncluded() != null) {
                List<String> includedIds = applicableToData.getIncluded().stream()
                        .map(UpdateValidationRuleCommand.ApplicabilityRule::getId)
                        .collect(Collectors.toList());
                builder.includedProducts(includedIds);
            }

            if (applicableToData.getExcluded() != null) {
                List<String> excludedIds = applicableToData.getExcluded().stream()
                        .map(UpdateValidationRuleCommand.ApplicabilityRule::getId)
                        .collect(Collectors.toList());
                builder.excludedProducts(excludedIds);
            }
        }

        // Update timeframe data
        TimeFrame timeframeData = payload.getTimeframe();
        if (timeframeData != null) {
            if (timeframeData.getTimezone() != null) {
                builder.timezone(timeframeData.getTimezone());
            }

            if (timeframeData.getValidityTimeframe() != null) {
                var validity = timeframeData.getValidityTimeframe();
                builder.validFrom(validity.getStartDate());
                builder.validTo(validity.getExpirationDate());
            }

            if (timeframeData.getValidityDaysOfWeek() != null && !timeframeData.getValidityDaysOfWeek().isEmpty()) {
                String rrule = buildRRuleFromDaysOfWeek(timeframeData.getValidityDaysOfWeek());
                builder.rrule(rrule);
            }

            if (timeframeData.getValidityHoursPerDay() != null && !timeframeData.getValidityHoursPerDay().isEmpty()) {
                List<RuleBinding.TimeWindow> windows = timeframeData.getValidityHoursPerDay().stream()
                        .map(hours -> RuleBinding.TimeWindow.builder()
                                .start(extractTimeOnly(hours.getStartTime()))
                                .end(extractTimeOnly(hours.getExpirationTime()))
                                .build())
                        .collect(Collectors.toList());
                builder.timeWindows(windows);
            }
        }

        RuleBinding updated = builder.build();
        return ruleBindingPort.save(updated);
    }

    private String buildRRuleFromDaysOfWeek(List<Integer> daysOfWeek) {
        String[] dayCodes = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};
        String byDay = daysOfWeek.stream()
                .filter(day -> day >= 1 && day <= 7)
                .map(day -> dayCodes[day - 1])
                .collect(Collectors.joining(","));
        return "FREQ=WEEKLY;BYDAY=" + byDay;
    }

    private String extractTimeOnly(String timeString) {
        if (timeString == null) {
            return null;
        }
        String time = timeString.split("\\+")[0].split("-")[0];
        String[] parts = time.split(":");
        if (parts.length >= 2) {
            return parts[0] + ":" + parts[1];
        }
        return time;
    }

    private void deployRuleToEngine(RuleBinding binding, ApplicabilityScope applicableToData) {
        try {
            if (!Boolean.TRUE.equals(binding.getActive())) {
                logger.info("Skipping deployment - binding is not active: bindingId={}", binding.getId());
                return;
            }

            String ruleId = binding.getRuleId();
            if (ruleId == null || ruleId.isBlank()) {
                logger.info("Skipping deployment - no ruleId: bindingId={}", binding.getId());
                return;
            }

            if (!validationRulePort.existsById(ruleId)) {
                logger.warn("Rule not found for deployment: ruleId={}", ruleId);
                return;
            }

            // Convert applicability scope for publishing
            var publishResult = rulePublishingService.publishRule(ruleId, binding.getId(),
                    convertApplicabilityScope(applicableToData));

            if (publishResult.isSuccess()) {
                RuleBinding updated = binding.toBuilder()
                        .bundleHash(publishResult.getBundleHash())
                        .build();
                ruleBindingPort.save(updated);
                logger.info("Rule deployed successfully: ruleId={}, bundleHash={}", ruleId, publishResult.getBundleHash());
            } else {
                logger.error("Failed to deploy rule: ruleId={}, error={}", ruleId, publishResult.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Error deploying rule to engine: bindingId={}", binding.getId(), e);
        }
    }

    private vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope convertApplicabilityScope(
            ApplicabilityScope source) {
        if (source == null) {
            return null;
        }

        return vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityScope.builder()
                .includedAll(source.getIncludedAll())
                .included(source.getIncluded() != null ? source.getIncluded().stream()
                        .map(r -> vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityRule.builder()
                                .id(r.getId())
                                .object(r.getObject() != null ?
                                        vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ObjectType.valueOf(r.getObject().name()) : null)
                                .build())
                        .toList() : null)
                .excluded(source.getExcluded() != null ? source.getExcluded().stream()
                        .map(r -> vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ApplicabilityRule.builder()
                                .id(r.getId())
                                .object(r.getObject() != null ?
                                        vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ObjectType.valueOf(r.getObject().name()) : null)
                                .build())
                        .toList() : null)
                .build();
    }

    private void publishSuccessEvent(String commandId, String objectId, RuleBinding binding) {
        try {
            ValidationSettingUpdateResultEvent event = ValidationSettingUpdateResultEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate("Validation")
                    .type("ValidationSettingUpdateResultEvent")
                    .source(SOURCE)
                    .subject(objectId)
                    .occurredAt(Instant.now())
                    .version(1)
                    .assignmentId(binding.getId())
                    .ruleId(binding.getRuleId())
                    .payload(ValidationSettingUpdateResultEvent.ValidationSettingUpdatePayload.builder()
                            .commandId(commandId)
                            .isSuccess(true)
                            .processedAt(Instant.now())
                            .updateResult(ValidationSettingUpdateResultEvent.UpdateResult.builder()
                                    .assignmentId(binding.getId())
                                    .ruleId(binding.getRuleId())
                                    .active(binding.getActive())
                                    .build())
                            .build())
                    .metadata(new HashMap<>())
                    .build();

            kafkaTemplate.send(validationEventTopic, objectId, event);

            logger.info("Published ValidationSettingUpdateResultEvent: commandId={}", commandId);

        } catch (Exception e) {
            logger.error("Failed to publish success event: commandId={}", commandId, e);
        }
    }

    private void publishErrorEvent(String commandId, String objectId, String errorCode, String errorMessage) {
        try {
            ValidationSettingUpdateResultEvent event = ValidationSettingUpdateResultEvent.builder()
                    .id(IdGenerator.generateId())
                    .aggregate("Validation")
                    .type("ValidationSettingUpdateResultEvent")
                    .source(SOURCE)
                    .subject(objectId != null ? objectId : commandId)
                    .occurredAt(Instant.now())
                    .version(1)
                    .errorMessage(errorMessage)
                    .error(errorCode)
                    .payload(ValidationSettingUpdateResultEvent.ValidationSettingUpdatePayload.builder()
                            .commandId(commandId)
                            .isSuccess(false)
                            .errorMessage(errorMessage)
                            .processedAt(Instant.now())
                            .build())
                    .metadata(new HashMap<>())
                    .build();

            kafkaTemplate.send(validationEventTopic, objectId != null ? objectId : commandId, event);

            logger.info("Published error event: commandId={}, errorCode={}", commandId, errorCode);

        } catch (Exception e) {
            logger.error("Failed to publish error event: commandId={}", commandId, e);
        }
    }

    public void handleDeadLetterCommand(UpdateValidationRuleCommand command) {
        logger.error("Processing dead letter UpdateValidationRuleCommand: commandId={}", command.getId());

        publishErrorEvent(
                command.getId(),
                command.getPayload() != null ? command.getPayload().getObjectId() : null,
                "DLQ_PROCESSING",
                "Command moved to dead letter queue"
        );
    }
}
