package vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.SettingValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;

/**
 * Mapper to convert SettingValidationRuleCommand to SettingValidationRuleCommandDTO for validation.
 * Extracts only the fields that require validation in validation service.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SettingValidationRuleCommandDTOMapper {

    /**
     * Convert command to DTO for validation.
     *
     * @param command the command from Kafka
     * @return DTO with fields to be validated
     */
    public SettingValidationRuleCommandDTO toDTO(SettingValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            log.warn("Received null command or payload, returning null DTO");
            return null;
        }

        var payload = command.getPayload();

        SettingValidationRuleCommandDTO dto = SettingValidationRuleCommandDTO.builder()
            // Map envelope fields
            .id(command.getId())
            .type(command.getType())
            .source(command.getSource())
            .subject(command.getSubject())
            // Map payload fields requiring validation
            .ruleId(payload.getRuleId())
            .objectType(payload.getObjectType())
            .objectId(payload.getObjectId())
            .active(payload.getActive())
            .trafficPercent(payload.getTrafficPercent())
            .priority(payload.getPriority())
            // Map optional fields (no validation)
            .notes(payload.getNotes())
            .build();

        log.debug("Mapped SettingValidationRuleCommand to DTO: commandId={}, ruleId={}, objectType={}, objectId={}",
            command.getId(), payload.getRuleId(), payload.getObjectType(), payload.getObjectId());

        return dto;
    }
}
