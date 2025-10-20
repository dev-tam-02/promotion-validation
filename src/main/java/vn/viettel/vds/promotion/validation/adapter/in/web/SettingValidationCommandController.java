package vn.viettel.vds.promotion.validation.adapter.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.domain.dto.command.SettingValidationRuleCommand;
import vn.viettel.vds.promotion.validation.adapter.in.messaging.SettingValidationRuleCommandConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST API Controller to directly invoke SettingValidationRuleCommand consumer
 * This controller allows manual triggering of validation command processing via HTTP
 */
@RestController
@RequestMapping("/v1/validation/commands")
public class SettingValidationCommandController {

    private static final Logger logger = LoggerFactory.getLogger(SettingValidationCommandController.class);
    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String COMMAND_ID_KEY = "commandId";

    private SettingValidationRuleCommandConsumer commandConsumer;

    public SettingValidationCommandController() {
        // Default constructor for Spring
    }

    // Optional setter injection to avoid circular dependency
    @Autowired(required = false)
    public void setCommandConsumer(SettingValidationRuleCommandConsumer commandConsumer) {
        this.commandConsumer = commandConsumer;
    }

    /**
     * Process SettingValidationRuleCommand via REST API
     * This endpoint directly calls the Kafka consumer method without going through Kafka
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

        // Check if consumer is available
        if (commandConsumer == null) {
            response.put(STATUS_KEY, "ERROR");
            response.put(MESSAGE_KEY, "SettingValidationRuleCommandConsumer not available");
            response.put(COMMAND_ID_KEY, commandId);

            logger.error("REST API: SettingValidationRuleCommandConsumer is null, cannot process command");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            // Create a mock acknowledgment for the consumer (since we're not using Kafka)
            MockAcknowledgment acknowledgment = new MockAcknowledgment();

            // Call the consumer directly with mock Kafka headers
            commandConsumer.handleSettingValidationRuleCommand(
                    command,
                    "api-direct-call",  // Mock topic name
                    0,                   // Mock partition
                    0L,                  // Mock offset
                    commandId,           // Use command ID as key
                    acknowledgment
            );

            // Check if the command was acknowledged (successfully processed)
            if (acknowledgment.isAcknowledged()) {
                response.put(STATUS_KEY, "SUCCESS");
                response.put(MESSAGE_KEY, "Command processed successfully");
                response.put(COMMAND_ID_KEY, commandId);
                response.put("commandType", command.getType());

                logger.info("REST API: Successfully processed SettingValidationRuleCommand: commandId={}", commandId);
                return ResponseEntity.ok(response);
            } else {
                response.put(STATUS_KEY, "FAILED");
                response.put(MESSAGE_KEY, "Command processing failed - not acknowledged");
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
     * Health check endpoint for the command processing API
     */
    @GetMapping("/settings/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put(STATUS_KEY, "UP");
        health.put("service", "SettingValidationCommandController");
        health.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(health);
    }

    /**
     * Mock implementation of Kafka Acknowledgment for direct API calls
     */
    private static class MockAcknowledgment implements Acknowledgment {
        private boolean acknowledged = false;

        @Override
        public void acknowledge() {
            this.acknowledged = true;
            logger.debug("Mock acknowledgment: Command has been acknowledged");
        }

        public void nack(long sleep) {
            this.acknowledged = false;
            logger.debug("Mock acknowledgment: Command has been negatively acknowledged with sleep: {}", sleep);
        }

        public void nack(int index, long sleep) {
            this.acknowledged = false;
            logger.debug("Mock acknowledgment: Command has been negatively acknowledged at index {} with sleep: {}", index, sleep);
        }

        public boolean isAcknowledged() {
            return acknowledged;
        }
    }
}
