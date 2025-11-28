package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.application.service.SettingValidationRuleCommandHandler;
import vn.viettel.vds.promotion.validation.command.SettingValidationRuleCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST API Controller to directly invoke SettingValidationRuleCommand processing.
 * This controller allows manual triggering of validation command processing via HTTP.
 *
 * Uses SettingValidationRuleCommandHandler directly instead of going through Kafka consumer.
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/validation/commands")
public class SettingValidationCommandController {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationCommandController.class);
    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String COMMAND_ID_KEY = "commandId";

    private final SettingValidationRuleCommandHandler commandHandler;

    public SettingValidationCommandController(SettingValidationRuleCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    /**
     * Process SettingValidationRuleCommand via REST API.
     * This endpoint directly calls the command handler without going through Kafka.
     *
     * @param command The SettingValidationRuleCommand to process
     * @return Response indicating success or failure of command processing
     */
    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> processSettingValidationCommand(
            @RequestBody SettingValidationRuleCommand command) {

        String commandId = command.getId() != null ? command.getId() : UUID.randomUUID().toString();

        logger.info("REST API: Received SettingValidationRuleCommand for processing: commandId={}, type={}, source={}",
                commandId, command.getType(), command.getSource());

        Map<String, Object> response = new HashMap<>();

        try {
            // Call the handler directly
            boolean success = commandHandler.handleCommand(command);

            if (success) {
                response.put(STATUS_KEY, "SUCCESS");
                response.put(MESSAGE_KEY, "Command processed successfully");
                response.put(COMMAND_ID_KEY, commandId);
                response.put("commandType", command.getType());

                logger.info("REST API: Successfully processed SettingValidationRuleCommand: commandId={}", commandId);
                return ResponseEntity.ok(response);
            } else {
                response.put(STATUS_KEY, "FAILED");
                response.put(MESSAGE_KEY, "Command processing failed");
                response.put(COMMAND_ID_KEY, commandId);
                response.put("commandType", command.getType());

                logger.error("REST API: Failed to process SettingValidationRuleCommand: commandId={}", commandId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("REST API: Error processing SettingValidationRuleCommand: commandId={}, error={}",
                    commandId, e.getMessage(), e);

            response.put(STATUS_KEY, "ERROR");
            response.put(MESSAGE_KEY, "Error processing command: " + e.getMessage());
            response.put(COMMAND_ID_KEY, commandId);
            response.put("errorDetails", e.toString());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health check endpoint for the command processing API.
     */
    @GetMapping("/settings/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put(STATUS_KEY, "UP");
        health.put("service", "SettingValidationCommandController");
        health.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(health);
    }
}
