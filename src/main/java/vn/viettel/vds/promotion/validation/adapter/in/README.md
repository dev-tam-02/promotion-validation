# Adapter In Package

## Description
The adapter.in package contains inbound adapters that handle incoming requests to the application. These adapters convert external requests into calls to the application's use cases.

## Purpose
The purpose of this package is to provide entry points to the application from the outside world, such as REST APIs, message consumers, or UI controllers, while keeping the application core independent of these external interfaces.

## Usage
This package typically contains:
- REST controllers
- GraphQL resolvers
- Message consumers (Kafka, RabbitMQ, etc.)
- Scheduled job triggers
- Command-line interfaces
- WebSocket handlers

Each adapter implements one or more input port interfaces defined in the application.port.in package.

## Examples

### REST Controller Example
```java
// CampaignController.java in adapter.in.web package
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Validated
public class CampaignController {

    private final CreateCampaignUseCase createCampaignUseCase;
    private final GetCampaignQuery getCampaignQuery;
    private final ActivateCampaignUseCase activateCampaignUseCase;
    private final UpdateCampaignUseCase updateCampaignUseCase;
    private final DeleteCampaignUseCase deleteCampaignUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Create a new campaign", notes = "Creates a new marketing campaign with the provided details")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Campaign created successfully"),
        @ApiResponse(code = 400, message = "Invalid request data"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden")
    })
    public ResponseEntity<CampaignResponse> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Received request to create campaign: {} with correlation ID: {}", request, correlationId);

        // Map the request to a command
        CreateCampaignCommand command = mapToCommand(request);

        // Execute the use case
        CampaignId campaignId = createCampaignUseCase.createCampaign(command);

        // Get the created campaign
        CampaignDto campaignDto = getCampaignQuery.getCampaignById(campaignId);

        // Map to response and return
        CampaignResponse response = mapToResponse(campaignDto);

        log.info("Campaign created successfully with ID: {}", campaignId.getValue());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Get campaign by ID", notes = "Retrieves a campaign by its unique identifier")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Campaign retrieved successfully"),
        @ApiResponse(code = 404, message = "Campaign not found"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden")
    })
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable("id") String id) {
        try {
            // Map the ID string to a domain ID
            CampaignId campaignId = new CampaignId(id);

            // Execute the query
            CampaignDto campaignDto = getCampaignQuery.getCampaignById(campaignId);

            // Map to response and return
            return ResponseEntity.ok(mapToResponse(campaignDto));
        } catch (CampaignNotFoundException e) {
            log.warn("Campaign not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Update campaign", notes = "Updates an existing campaign with new details")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Campaign updated successfully"),
        @ApiResponse(code = 400, message = "Invalid request data"),
        @ApiResponse(code = 404, message = "Campaign not found"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden")
    })
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateCampaignRequest request) {

        try {
            // Map the ID string to a domain ID
            CampaignId campaignId = new CampaignId(id);

            // Map the request to a command
            UpdateCampaignCommand command = new UpdateCampaignCommand(
                campaignId,
                request.getName(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getTargetAudience() != null ? TargetAudience.valueOf(request.getTargetAudience()) : null,
                request.getGeographicRestrictions() != null ? 
                    request.getGeographicRestrictions().stream()
                        .map(GeographicArea::new)
                        .collect(Collectors.toSet()) : null,
                request.getMaxUsagePerCustomer()
            );

            // Execute the use case
            updateCampaignUseCase.updateCampaign(command);

            // Get the updated campaign
            CampaignDto campaignDto = getCampaignQuery.getCampaignById(campaignId);

            // Map to response and return
            return ResponseEntity.ok(mapToResponse(campaignDto));
        } catch (CampaignNotFoundException e) {
            log.warn("Campaign not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/activate")
    @ApiOperation(value = "Activate campaign", notes = "Activates a campaign that has been approved")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Campaign activated successfully"),
        @ApiResponse(code = 404, message = "Campaign not found"),
        @ApiResponse(code = 400, message = "Campaign cannot be activated"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden")
    })
    public ResponseEntity<Void> activateCampaign(@PathVariable("id") String id) {
        try {
            // Map the ID string to a domain ID
            CampaignId campaignId = new CampaignId(id);

            // Execute the use case
            activateCampaignUseCase.activateCampaign(campaignId);

            return ResponseEntity.ok().build();
        } catch (CampaignNotFoundException e) {
            log.warn("Campaign not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot activate campaign with ID: {}, reason: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete campaign", notes = "Deletes a campaign by its unique identifier")
    @ApiResponses({
        @ApiResponse(code = 204, message = "Campaign deleted successfully"),
        @ApiResponse(code = 404, message = "Campaign not found"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden")
    })
    public ResponseEntity<Void> deleteCampaign(@PathVariable("id") String id) {
        try {
            // Map the ID string to a domain ID
            CampaignId campaignId = new CampaignId(id);

            // Execute the use case
            deleteCampaignUseCase.deleteCampaign(campaignId);

            return ResponseEntity.noContent().build();
        } catch (CampaignNotFoundException e) {
            log.warn("Campaign not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // Helper methods for mapping between DTOs and domain objects
    private CreateCampaignCommand mapToCommand(CreateCampaignRequest request) {
        return new CreateCampaignCommand(
            request.getName(),
            request.getDescription(),
            request.getStartDate(),
            request.getEndDate(),
            TargetAudience.valueOf(request.getTargetAudience()),
            request.getGeographicRestrictions().stream()
                .map(GeographicArea::new)
                .collect(Collectors.toSet()),
            request.getMaxUsagePerCustomer()
        );
    }

    private CampaignResponse mapToResponse(CampaignDto dto) {
        return new CampaignResponse(
            dto.getId(),
            dto.getName(),
            dto.getDescription(),
            dto.getStatus(),
            dto.getStartDate(),
            dto.getEndDate(),
            dto.getTargetAudience(),
            dto.getGeographicRestrictions(),
            dto.getMaxUsagePerCustomer(),
            dto.getPromotions().stream()
                .map(this::mapToPromotionResponse)
                .collect(Collectors.toList())
        );
    }

    private PromotionResponse mapToPromotionResponse(PromotionDto dto) {
        // Mapping logic omitted for brevity
        return new PromotionResponse(
            // ... map DTO properties to response
        );
    }
}
```

### Kafka Consumer Example
```java
// CampaignEventConsumer.java in adapter.in.messaging package
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

### Scheduled Job Example
```java
// CampaignScheduler.java in adapter.in.scheduler package
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final ActivateScheduledCampaignsUseCase activateScheduledCampaignsUseCase;
    private final CompleteCampaignsUseCase completeCampaignsUseCase;

    @Scheduled(cron = "${scheduler.activate-campaigns.cron}")
    public void activateScheduledCampaigns() {
        log.info("Starting scheduled job to activate campaigns");
        try {
            int activatedCount = activateScheduledCampaignsUseCase.activateScheduledCampaigns();
            log.info("Activated {} campaigns", activatedCount);
        } catch (Exception e) {
            log.error("Error activating scheduled campaigns", e);
        }
    }

    @Scheduled(cron = "${scheduler.complete-campaigns.cron}")
    public void completeCampaigns() {
        log.info("Starting scheduled job to complete expired campaigns");
        try {
            int completedCount = completeCampaignsUseCase.completeExpiredCampaigns();
            log.info("Completed {} expired campaigns", completedCount);
        } catch (Exception e) {
            log.error("Error completing expired campaigns", e);
        }
    }
}
```

## Rules
1. Inbound adapters should implement the interfaces defined in the application.port.in package
2. Adapters should handle the conversion between the external request format and the application's input model
3. Input validation should be performed here before calling the application use cases
4. Error handling specific to the external interface should be implemented here
5. Authentication and authorization checks should be performed here
6. Adapters should not contain business logic
7. Each adapter should be focused on a specific external interface technology
8. Documentation (e.g., Swagger annotations) should be added to API controllers
