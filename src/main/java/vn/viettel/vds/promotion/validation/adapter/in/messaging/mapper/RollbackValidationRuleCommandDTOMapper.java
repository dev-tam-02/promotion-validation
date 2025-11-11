package vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.RollbackValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.command.RollbackValidationRuleCommand;

/**
 * Mapper to convert RollbackValidationRuleCommand to RollbackValidationRuleCommandDTO for validation.
 * Extracts only the fields that require validation in validation service.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RollbackValidationRuleCommandDTOMapper {

    /**
     * Convert command to DTO for validation.
     *
     * @param command the command from Kafka
     * @return DTO with fields to be validated
     */
    public RollbackValidationRuleCommandDTO toDTO(RollbackValidationRuleCommand command) {
        if (command == null || command.getPayload() == null) {
            log.warn("Received null command or payload, returning null DTO");
            return null;
        }

        var payload = command.getPayload();

        RollbackValidationRuleCommandDTO dto = RollbackValidationRuleCommandDTO.builder()
            // Map envelope fields
            .id(command.getId())
            .type(command.getType())
            .source(command.getSource())
            .subject(command.getSubject())
            // Map payload fields requiring validation
            .campaignId(payload.getCampaignId())
            .validationRuleId(payload.getValidationRuleId())
            .rollbackAll(payload.getRollbackAll())
            // Map optional fields (no validation)
            .rollbackReason(payload.getRollbackReason())
            .correlationId(payload.getCorrelationId())
            .build();

        log.debug("Mapped RollbackValidationRuleCommand to DTO: commandId={}, campaignId={}, validationRuleId={}",
            command.getId(), payload.getCampaignId(), payload.getValidationRuleId());

        return dto;
    }
}
