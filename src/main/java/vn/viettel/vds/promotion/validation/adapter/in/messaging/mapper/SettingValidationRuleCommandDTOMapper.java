package vn.viettel.vds.promotion.validation.adapter.in.messaging.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.SettingValidationRuleCommandDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.TimeFrameDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.ValidityHoursPerDayDTO;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.dto.ValidityTimeframeDTO;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.TimeFrame;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ValidityHoursPerDay;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand.ValidityTimeframe;

import java.util.List;

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
                // Map timeframe fields (cascading validation)
                .timeframe(mapTimeFrame(payload.getTimeframe()))
                .build();

        log.debug("Mapped SettingValidationRuleCommand to DTO: commandId={}, ruleId={}, objectType={}, objectId={}, hasTimeframe={}",
                command.getId(), payload.getRuleId(), payload.getObjectType(), payload.getObjectId(),
                payload.getTimeframe() != null);

        return dto;
    }

    /**
     * Map TimeFrame from command to TimeFrameDTO.
     *
     * @param timeFrame the timeframe from command payload
     * @return TimeFrameDTO or null if timeFrame is null
     */
    private TimeFrameDTO mapTimeFrame(TimeFrame timeFrame) {
        if (timeFrame == null) {
            return null;
        }

        return TimeFrameDTO.builder()
                .validityTimeframe(mapValidityTimeframe(timeFrame.getValidityTimeframe()))
                .validityDaysOfWeek(timeFrame.getValidityDaysOfWeek())
                .validityHoursPerDay(mapValidityHoursPerDay(timeFrame.getValidityHoursPerDay()))
                .timeFrameId(timeFrame.getTimeFrameId())
                .mode(timeFrame.getMode() != null ? timeFrame.getMode().name() : null)
                .timezone(timeFrame.getTimezone())
                .build();
    }

    /**
     * Map ValidityTimeframe from command to ValidityTimeframeDTO.
     *
     * @param validityTimeframe the validity timeframe from command
     * @return ValidityTimeframeDTO or null if validityTimeframe is null
     */
    private ValidityTimeframeDTO mapValidityTimeframe(ValidityTimeframe validityTimeframe) {
        if (validityTimeframe == null) {
            return null;
        }

        return ValidityTimeframeDTO.builder()
                .startDate(validityTimeframe.getStartDate())
                .expirationDate(validityTimeframe.getExpirationDate())
                .interval(validityTimeframe.getInterval())
                .duration(validityTimeframe.getDuration())
                .activityDurationAfterPublishing(validityTimeframe.getActivityDurationAfterPublishing())
                .build();
    }

    /**
     * Map list of ValidityHoursPerDay from command to list of ValidityHoursPerDayDTO.
     *
     * @param validityHoursPerDayList the list from command
     * @return list of DTOs or null if input is null
     */
    private List<ValidityHoursPerDayDTO> mapValidityHoursPerDay(List<ValidityHoursPerDay> validityHoursPerDayList) {
        if (validityHoursPerDayList == null || validityHoursPerDayList.isEmpty()) {
            return List.of();
        }

        return validityHoursPerDayList.stream()
                .map(this::mapSingleValidityHoursPerDay)
                .toList();
    }

    /**
     * Map single ValidityHoursPerDay to ValidityHoursPerDayDTO.
     *
     * @param validityHoursPerDay the single entry from command
     * @return ValidityHoursPerDayDTO
     */
    private ValidityHoursPerDayDTO mapSingleValidityHoursPerDay(ValidityHoursPerDay validityHoursPerDay) {
        if (validityHoursPerDay == null) {
            return null;
        }

        return ValidityHoursPerDayDTO.builder()
                .dayOfWeek(validityHoursPerDay.getDayOfWeek())
                .startTime(validityHoursPerDay.getStartTime())
                .expirationTime(validityHoursPerDay.getExpirationTime())
                .build();
    }
}
