package vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.UpdateValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.command.UpdateValidationRuleCommand;

/**
 * Mapper to convert UpdateValidationRuleCommand to UpdateValidationRuleCommandDTO for validation.
 * Extracts only the fields that require validation in validation service.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UpdateValidationRuleCommandDTOMapper {

    /**
     * Convert command to DTO for validation.
     *
     * @param command the command from Kafka
     * @return DTO with fields to be validated
     */
    public UpdateValidationRuleCommandDTO toDTO(UpdateValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            log.warn("Received null command or payload, returning null DTO");
            return null;
        }

        var payload = command.getPayload();

        UpdateValidationRuleCommandDTO dto = UpdateValidationRuleCommandDTO.builder()
            // Map envelope fields
            .id(command.getId())
            .type(command.getType())
            .source(command.getSource())
            .subject(command.getSubject())
            // Map payload fields requiring validation
            .assignmentId(payload.getAssignmentId())
            .ruleId(payload.getRuleId())
            .objectType(payload.getObjectType())
            .objectId(payload.getObjectId())
            .active(payload.getActive())
            .trafficPercent(payload.getTrafficPercent())
            .priority(payload.getPriority())
            .notes(payload.getNotes())
            .updatedBy(payload.getUpdatedBy())
            .reason(payload.getReason())
            .build();

        log.debug("Mapped UpdateValidationRuleCommand to DTO: commandId={}, assignmentId={}, updatedBy={}",
            command.getId(), payload.getAssignmentId(), payload.getUpdatedBy());

        return dto;
    }
}
