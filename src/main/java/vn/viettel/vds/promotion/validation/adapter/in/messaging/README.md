# Adapter In Messaging Package

## Description

The adapter.in.messaging package contains inbound adapters that handle incoming messages from message brokers such as
Kafka, RabbitMQ, or JMS. These adapters convert external messages into calls to the application's use cases.

## Purpose

The purpose of this package is to provide entry points to the application from messaging systems, allowing the
application to react to events and commands received through message queues while keeping the application core
independent of these external interfaces.

## Usage

This package typically contains:

- Message consumers (Kafka, RabbitMQ, JMS, etc.)
- Message handlers that process incoming messages
- Message deserializers that convert message payloads to domain objects
- Error handlers for message processing failures

Each adapter implements one or more input port interfaces defined in the application.port.in package.

## Examples

### Kafka Consumer Example

```java
// CampaignEventConsumer.java
@Service
@RequiredArgsConstructor
public class CampaignEventConsumer {

    private final ProcessExternalCampaignUseCase processExternalCampaignUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.external-campaigns}", groupId = "${kafka.consumer.group-id}")
    public void consumeExternalCampaignEvent(String message) {
        try {
            log.info("Received external campaign event: {}", message);

            // Deserialize the message
            ExternalCampaignEvent event = objectMapper.readValue(message, ExternalCampaignEvent.class);

            // Map to command
            ProcessExternalCampaignCommand command = new ProcessExternalCampaignCommand(
                event.getExternalId(),
                event.getName(),
                event.getDescription(),
                event.getStartDate(),
                event.getEndDate(),
                event.getTargetAudience(),
                event.getDiscounts()
            );

            // Process the command
            processExternalCampaignUseCase.processExternalCampaign(command);

            log.info("Successfully processed external campaign with ID: {}", event.getExternalId());
        } catch (Exception e) {
            log.error("Error processing external campaign event: {}", message, e);
            // Handle error (e.g., send to dead letter queue, retry, etc.)
        }
    }
}
```

### RabbitMQ Consumer Example

```java
// PromotionEventConsumer.java
@Service
@RequiredArgsConstructor
public class PromotionEventConsumer {

    private final CreatePromotionUseCase createPromotionUseCase;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queues.promotion-events}")
    public void consumePromotionEvent(String message) {
        try {
            log.info("Received promotion event: {}", message);

            // Deserialize the message
            PromotionEvent event = objectMapper.readValue(message, PromotionEvent.class);

            // Map to command
            CreatePromotionCommand command = new CreatePromotionCommand(
                new CampaignId(event.getCampaignId()),
                event.getName(),
                event.getDescription(),
                DiscountMethod.valueOf(event.getDiscountMethod()),
                event.getDiscountValue(),
                event.getBundlePrice() != null ? new Money(event.getBundlePrice(), Currency.getInstance(event.getCurrency())) : null,
                event.getMinQuantity(),
                event.getFreeQuantity(),
                event.getStartDate(),
                event.getEndDate(),
                event.getApplicableProducts().stream()
                    .map(ProductId::new)
                    .collect(Collectors.toSet()),
                event.getApplicableCategories().stream()
                    .map(CategoryId::new)
                    .collect(Collectors.toSet()),
                event.getMaxUsagePerCustomer()
            );

            // Process the command
            PromotionId promotionId = createPromotionUseCase.createPromotion(command);

            log.info("Successfully created promotion with ID: {}", promotionId.getValue());
        } catch (Exception e) {
            log.error("Error processing promotion event: {}", message, e);
            // Handle error
        }
    }
}
```

## Rules

1. Messaging adapters should implement the interfaces defined in the application.port.in package
2. Adapters should handle the conversion between the external message format and the application's input model
3. Input validation should be performed before calling the application use cases
4. Error handling should be robust, with clear strategies for handling failures (e.g., dead letter queues, retries)
5. Adapters should not contain business logic
6. Each adapter should be focused on a specific messaging technology
7. Message processing should be idempotent where possible
8. Logging should be comprehensive to aid in troubleshooting
9. Configuration should be externalized (e.g., queue names, topic names)
10. Security considerations (authentication, authorization) should be addressed where applicable