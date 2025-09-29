# Adapter In Web Package

## Description
The adapter.in.web package contains inbound adapters that handle incoming HTTP requests to the application. These adapters convert external HTTP requests into calls to the application's use cases and transform the results back into HTTP responses.

## Purpose
The purpose of this package is to provide RESTful API endpoints and other web interfaces to the application, allowing external clients to interact with the application while keeping the application core independent of these external interfaces.

## Usage
This package typically contains:
- REST controllers
- GraphQL resolvers
- Request/response mappers
- Exception handlers
- API documentation (e.g., Swagger annotations)
- Authentication and authorization filters

Each adapter implements one or more input port interfaces defined in the application.port.in package.

## Examples

### REST Controller Example
```java
// CampaignController.java
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Validated
public class CampaignController {

    private final CreateCampaignUseCase createCampaignUseCase;
    private final GetCampaignQuery getCampaignQuery;
    private final ActivateCampaignUseCase activateCampaignUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Create a new campaign", notes = "Creates a new marketing campaign with the provided details")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Campaign created successfully"),
        @ApiResponse(code = 400, message = "Invalid request data"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden")
    })
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        // Map the request to a command
        CreateCampaignCommand command = new CreateCampaignCommand(
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

        // Execute the use case
        CampaignId campaignId = createCampaignUseCase.createCampaign(command);

        // Get the created campaign
        CampaignDto campaignDto = getCampaignQuery.getCampaignById(campaignId);

        // Map to response and return
        CampaignResponse response = mapToResponse(campaignDto);

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
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
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

### GraphQL Resolver Example
```java
// CampaignQueryResolver.java
@Component
@RequiredArgsConstructor
public class CampaignQueryResolver implements GraphQLQueryResolver {

    private final GetCampaignQuery getCampaignQuery;
    private final SearchCampaignsQuery searchCampaignsQuery;

    public CampaignResponse getCampaign(String id) {
        try {
            CampaignId campaignId = new CampaignId(id);
            CampaignDto campaignDto = getCampaignQuery.getCampaignById(campaignId);
            return mapToResponse(campaignDto);
        } catch (CampaignNotFoundException e) {
            throw new GraphQLException("Campaign not found with ID: " + id);
        }
    }

    public List<CampaignResponse> searchCampaigns(String keyword, String status, String targetAudience) {
        SearchCampaignsQuery.SearchCriteria criteria = new SearchCampaignsQuery.SearchCriteria(
            keyword,
            status != null ? CampaignStatus.valueOf(status) : null,
            targetAudience != null ? TargetAudience.valueOf(targetAudience) : null
        );

        List<CampaignDto> campaignDtos = searchCampaignsQuery.searchCampaigns(criteria);
        
        return campaignDtos.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    private CampaignResponse mapToResponse(CampaignDto dto) {
        // Mapping logic omitted for brevity
        return new CampaignResponse(
            // ... map DTO properties to response
        );
    }
}
```

## Rules
1. Web adapters should implement the interfaces defined in the application.port.in package
2. Adapters should handle the conversion between the HTTP request/response format and the application's input/output model
3. Input validation should be performed before calling the application use cases
4. Error handling should be consistent across all controllers
5. Authentication and authorization checks should be performed here
6. Adapters should not contain business logic
7. Each adapter should be focused on a specific web interface technology
8. API documentation (e.g., Swagger annotations) should be added to controllers
9. HTTP status codes should be used appropriately
10. Response formats should be consistent across the API