# Application Port In Package

## Description
The application.port.in package contains input port interfaces that define the operations that can be performed on the application. These interfaces represent the entry points to the application's use cases, following the Hexagonal Architecture (also known as Ports and Adapters) pattern.

## Purpose
The purpose of this package is to define a clear API for the application's use cases, allowing external components to interact with the application without knowing the implementation details. This separation enables better testability, maintainability, and flexibility in changing implementations.

## Package Structure
This package is organized into the following structure:

```
application.port.in/
├── *UseCase.java        # Use case interfaces
├── dto/                 # Data Transfer Objects
│   ├── *Command.java    # Command objects for input
│   └── *Result.java     # Result objects for output
└── mapper/              # Mappers between DTOs
    └── *DtoMapper.java  # MapStruct mappers
```

## Usage
This package typically contains:
- Use case interfaces that define methods representing user actions or system events
- Command and query interfaces following the Command Query Responsibility Segregation (CQRS) pattern
- DTOs (Data Transfer Objects) that represent the input and output parameters for use cases
- Mappers that convert between different DTO representations

## Examples

### Command Use Case Example
```java
// CreatePromotionUseCase.java
public interface CreatePromotionUseCase {

    /**
     * Creates a new promotion and adds it to the specified campaign
     * 
     * @param command The details of the promotion to create
     * @return The ID of the created promotion
     * @throws CampaignNotFoundException If the campaign does not exist
     * @throws InvalidPromotionException If the promotion details are invalid
     */
    PromotionId createPromotion(CreatePromotionCommand command);

    /**
     * Command for creating a promotion
     */
    @Value
    class CreatePromotionCommand {
        @NotNull CampaignId campaignId;
        @NotNull @Size(min = 3, max = 100) String name;
        @Size(max = 500) String description;
        @NotNull DiscountMethod discountMethod;
        Double discountValue;
        Money bundlePrice;
        Integer minQuantity;
        Integer freeQuantity;
        LocalDate startDate;
        LocalDate endDate;
        Set<ProductId> applicableProducts;
        Set<CategoryId> applicableCategories;
        Integer maxUsagePerCustomer;

        // Validation method that can be used by the implementation
        public void validate() {
            if (discountMethod.requiresDiscountValue() && (discountValue == null || discountValue <= 0)) {
                throw new InvalidPromotionException("Discount value is required for " + discountMethod);
            }

            if (discountMethod == DiscountMethod.PERCENTAGE && discountValue > 100) {
                throw new InvalidPromotionException("Percentage discount cannot exceed 100%");
            }

            if (discountMethod == DiscountMethod.BUY_X_GET_Y && (minQuantity == null || freeQuantity == null)) {
                throw new InvalidPromotionException("Min quantity and free quantity are required for Buy X Get Y promotions");
            }

            if (discountMethod == DiscountMethod.BUNDLE_PRICE && bundlePrice == null) {
                throw new InvalidPromotionException("Bundle price is required for Bundle Price promotions");
            }

            if (applicableProducts.isEmpty() && applicableCategories.isEmpty()) {
                throw new InvalidPromotionException("Promotion must apply to at least one product or category");
            }
        }
    }
}
```

### Query Use Case Example
```java
// GetCampaignQuery.java
public interface GetCampaignQuery {

    /**
     * Gets a campaign by its ID
     * 
     * @param campaignId The ID of the campaign to retrieve
     * @return The campaign details
     * @throws CampaignNotFoundException If the campaign does not exist
     */
    CampaignDto getCampaignById(CampaignId campaignId);

    /**
     * Gets all active campaigns
     * 
     * @return A list of active campaigns
     */
    List<CampaignDto> getActiveCampaigns();

    /**
     * Gets campaigns by status
     * 
     * @param status The status to filter by
     * @return A list of campaigns with the specified status
     */
    List<CampaignDto> getCampaignsByStatus(CampaignStatus status);

    /**
     * DTO for campaign details
     */
    @Value
    class CampaignDto {
        String id;
        String name;
        String description;
        String status;
        LocalDate startDate;
        LocalDate endDate;
        String targetAudience;
        Set<String> geographicRestrictions;
        int maxUsagePerCustomer;
        List<PromotionDto> promotions;
    }

    /**
     * DTO for promotion details
     */
    @Value
    class PromotionDto {
        String id;
        String name;
        String description;
        String discountMethod;
        Double discountValue;
        String bundlePrice;
        Integer minQuantity;
        Integer freeQuantity;
        LocalDate startDate;
        LocalDate endDate;
        Set<String> applicableProducts;
        Set<String> applicableCategories;
        Integer maxUsagePerCustomer;
    }
}
```

### CQRS Pattern Example
```java
// Command interface
public interface ActivateCampaignUseCase {

    /**
     * Activates a campaign that has been approved
     * 
     * @param campaignId The ID of the campaign to activate
     * @throws CampaignNotFoundException If the campaign does not exist
     * @throws IllegalStateException If the campaign is not in the APPROVED state
     */
    void activateCampaign(CampaignId campaignId);
}

// Query interface
public interface GetCampaignStatusQuery {

    /**
     * Gets the current status of a campaign
     * 
     * @param campaignId The ID of the campaign
     * @return The current status of the campaign
     * @throws CampaignNotFoundException If the campaign does not exist
     */
    CampaignStatus getCampaignStatus(CampaignId campaignId);
}

// Usage in an adapter
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final ActivateCampaignUseCase activateCampaignUseCase;
    private final GetCampaignStatusQuery getCampaignStatusQuery;

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateCampaign(@PathVariable("id") String id) {
        CampaignId campaignId = new CampaignId(id);
        activateCampaignUseCase.activateCampaign(campaignId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> getCampaignStatus(@PathVariable("id") String id) {
        CampaignId campaignId = new CampaignId(id);
        CampaignStatus status = getCampaignStatusQuery.getCampaignStatus(campaignId);
        return ResponseEntity.ok(status.name());
    }
}
```

## Rules
1. Input port interfaces should be defined in terms of the application's domain model
2. Each interface should represent a single use case or a closely related group of use cases
3. Method names should clearly describe the action being performed
4. Input parameters should be well-defined and validated by the use case implementation
5. DTOs should be immutable and contain only the data needed for the use case
6. Interfaces should be designed with testability in mind
7. Input ports should not expose implementation details of the application
8. Documentation should clearly describe the purpose and expected behavior of each method
9. Input ports should follow the Interface Segregation Principle (ISP)

## Naming Conventions

### Use Case Interfaces
- Name: `[Action][Entity]UseCase`
- Examples: `CreateCampaignUseCase`, `UpdateCampaignUseCase`, `DeleteCampaignUseCase`
- Method name: `[action][Entity]` (camelCase)
- Examples: `createCampaign`, `updateCampaign`, `deleteCampaign`

### DTOs
- Command objects: `[Action][Entity]Command`
- Result objects: `[Action][Entity]Result`
- Examples: `CreateCampaignCommand`, `UpdateCampaignResult`
- Use Java records for immutability and conciseness

### Mappers
- Name: `[Entity]DtoMapper`
- Examples: `CampaignDtoMapper`
- Methods: `to[Target]` (camelCase)
- Examples: `toCreateCampaignRequest`, `toCreateCampaignCommand`

## Java Records for DTOs

This project uses Java records for DTOs, which were introduced in Java 14. Records provide a concise way to create immutable data classes:

```java
/**
 * Command for creating a new campaign.
 */
public record CreateCampaignCommand(
    String name,
    OffsetDateTime expirationDate,
    String type,
    String campaignType,
    Integer vouchersCount,
    Map<String, Object> metadata,
    String activityDurationAfterPublishing,
    ValidityTimeframe validityTimeframe,
    List<String> validityDayOfWeek,
    Boolean joinOnce,
    Boolean useVoucherMetadataSchema,
    AccessSettings accessSettings,
    String object,
    Voucher voucher
) {
    // Nested records for complex structures
    public record ValidityTimeframe(
        String interval,
        String duration
    ) {}
    
    // More nested records...
}
```

Benefits of using records:
- Automatic generation of constructors, getters, equals, hashCode, and toString
- Immutability by default
- Concise syntax
- Clear intent (records are meant to be data carriers)

## MapStruct for DTO Mapping

This project uses MapStruct for mapping between different DTO representations:

```java
@Mapper(componentModel = "spring")
public interface CampaignDtoMapper {
    @Mapping(source = "name", target = "name")
    @Mapping(source = "expirationDate", target = "expiration_date")
    // More mappings...
    CreateCampaignRequest toCreateCampaignRequest(CreateCampaignCommand command);
    
    // More mapping methods...
}
```

Benefits of using MapStruct:
- Automatic generation of mapping code
- Type-safe mapping
- Integration with Spring
- Support for complex mappings
- Reduced boilerplate code

## Guidelines for Creating Similar Classes

When creating new classes in this package, follow these guidelines:

1. **Use Case Interfaces**:
   - Create a new interface for each distinct use case
   - Follow the naming convention `[Action][Entity]UseCase`
   - Define a single method that takes a command and returns a result
   - Document the method with JavaDoc, including parameters, return values, and exceptions

2. **Command Objects**:
   - Create a new record for each command
   - Follow the naming convention `[Action][Entity]Command`
   - Include all necessary fields for the use case
   - Use nested records for complex structures
   - Add validation logic if needed

3. **Result Objects**:
   - Create a new record for each result
   - Follow the naming convention `[Action][Entity]Result`
   - Include only the fields needed by the caller
   - Use nested records for complex structures

4. **Mappers**:
   - Create or update a mapper interface for each entity
   - Follow the naming convention `[Entity]DtoMapper`
   - Use MapStruct annotations for mapping
   - Include mappings in both directions (command/request, result/response)
   - Add helper methods for collections and pagination if needed
