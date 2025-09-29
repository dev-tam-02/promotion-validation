# Adapter In Web DTO Package

## Description
The adapter.in.web.dto package contains Data Transfer Objects (DTOs) used by web adapters to convert between HTTP request/response formats and the application's domain model. These DTOs represent the structure of incoming requests and outgoing responses, providing a clear boundary between the web interface and the application core.

## Purpose
The purpose of this package is to isolate the structure of HTTP requests and responses from the domain model, allowing the application to evolve independently of the API contracts. DTOs also provide a place to implement validation and conversion logic specific to web interfaces.

## Usage
This package typically contains:
- Request DTOs that represent the structure of incoming HTTP requests
- Response DTOs that represent the structure of outgoing HTTP responses
- Mappers that convert between DTOs and domain objects or commands
- Validation annotations and custom validators for request payloads
- Documentation annotations (e.g., Swagger/OpenAPI) for API documentation

## Examples

### Request DTO Example
```java
// CreateCampaignRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {
    
    @NotNull
    @Size(min = 3, max = 100)
    @ApiModelProperty(value = "Campaign name", example = "Summer Sale 2023", required = true)
    private String name;
    
    @Size(max = 500)
    @ApiModelProperty(value = "Campaign description", example = "Special discounts for summer products")
    private String description;
    
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "Campaign start date", example = "2023-06-01", required = true)
    private LocalDate startDate;
    
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "Campaign end date", example = "2023-08-31", required = true)
    private LocalDate endDate;
    
    @NotNull
    @ApiModelProperty(value = "Target audience", example = "ALL_CUSTOMERS", required = true, 
                     allowableValues = "ALL_CUSTOMERS, NEW_CUSTOMERS, PREMIUM_CUSTOMERS, RETURNING_CUSTOMERS")
    private String targetAudience;
    
    @ApiModelProperty(value = "Geographic restrictions", example = "[\"NORTH\", \"CENTRAL\", \"SOUTH\"]")
    private Set<String> geographicRestrictions = new HashSet<>();
    
    @Min(1)
    @ApiModelProperty(value = "Maximum usage per customer", example = "3", required = true)
    private int maxUsagePerCustomer = 1;
    
    // Validation method
    public void validate() {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        try {
            TargetAudience.valueOf(targetAudience);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid target audience: " + targetAudience);
        }
    }
}
```

### Response DTO Example
```java
// CampaignResponse.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignResponse {
    
    @ApiModelProperty(value = "Campaign ID", example = "c123e4567-e89b-12d3-a456-426614174000")
    private String id;
    
    @ApiModelProperty(value = "Campaign name", example = "Summer Sale 2023")
    private String name;
    
    @ApiModelProperty(value = "Campaign description", example = "Special discounts for summer products")
    private String description;
    
    @ApiModelProperty(value = "Campaign status", example = "ACTIVE", 
                     allowableValues = "DRAFT, PENDING_APPROVAL, APPROVED, ACTIVE, PAUSED, COMPLETED, CANCELLED, REJECTED")
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "Campaign start date", example = "2023-06-01")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "Campaign end date", example = "2023-08-31")
    private LocalDate endDate;
    
    @ApiModelProperty(value = "Target audience", example = "ALL_CUSTOMERS")
    private String targetAudience;
    
    @ApiModelProperty(value = "Geographic restrictions", example = "[\"NORTH\", \"CENTRAL\", \"SOUTH\"]")
    private Set<String> geographicRestrictions;
    
    @ApiModelProperty(value = "Maximum usage per customer", example = "3")
    private int maxUsagePerCustomer;
    
    @ApiModelProperty(value = "Promotions in this campaign")
    private List<PromotionResponse> promotions;
}
```

### Nested Response DTO Example
```java
// PromotionResponse.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromotionResponse {
    
    @ApiModelProperty(value = "Promotion ID", example = "p123e4567-e89b-12d3-a456-426614174000")
    private String id;
    
    @ApiModelProperty(value = "Promotion name", example = "Buy 2 Get 1 Free")
    private String name;
    
    @ApiModelProperty(value = "Promotion description", example = "Buy 2 items and get 1 free")
    private String description;
    
    @ApiModelProperty(value = "Discount method", example = "BUY_X_GET_Y", 
                     allowableValues = "PERCENTAGE, FIXED_AMOUNT, BUY_X_GET_Y, BUNDLE_PRICE")
    private String discountMethod;
    
    @ApiModelProperty(value = "Discount value (for PERCENTAGE or FIXED_AMOUNT)", example = "20.0")
    private Double discountValue;
    
    @ApiModelProperty(value = "Bundle price (for BUNDLE_PRICE)", example = "99.99 USD")
    private String bundlePrice;
    
    @ApiModelProperty(value = "Minimum quantity (for BUY_X_GET_Y)", example = "2")
    private Integer minQuantity;
    
    @ApiModelProperty(value = "Free quantity (for BUY_X_GET_Y)", example = "1")
    private Integer freeQuantity;
    
    @ApiModelProperty(value = "Applicable product IDs", example = "[\"prod-123\", \"prod-456\"]")
    private Set<String> applicableProducts;
    
    @ApiModelProperty(value = "Applicable category IDs", example = "[\"cat-789\", \"cat-012\"]")
    private Set<String> applicableCategories;
}
```

### DTO Mapper Example
```java
// CampaignDtoMapper.java
@Component
public class CampaignDtoMapper {
    
    /**
     * Maps a create campaign request to a create campaign command
     */
    public CreateCampaignCommand toCommand(CreateCampaignRequest request) {
        // Validate the request
        request.validate();
        
        // Create and return the command
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
    
    /**
     * Maps a campaign DTO to a campaign response
     */
    public CampaignResponse toResponse(CampaignDto dto) {
        if (dto == null) {
            return null;
        }
        
        List<PromotionResponse> promotionResponses = dto.getPromotions().stream()
            .map(this::toPromotionResponse)
            .collect(Collectors.toList());
        
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
            promotionResponses
        );
    }
    
    private PromotionResponse toPromotionResponse(PromotionDto dto) {
        if (dto == null) {
            return null;
        }
        
        String bundlePrice = null;
        if (dto.getBundlePrice() != null) {
            bundlePrice = dto.getBundlePrice().getAmount() + " " + dto.getBundlePrice().getCurrency().getCurrencyCode();
        }
        
        return new PromotionResponse(
            dto.getId(),
            dto.getName(),
            dto.getDescription(),
            dto.getDiscountMethod(),
            dto.getDiscountValue(),
            bundlePrice,
            dto.getMinQuantity(),
            dto.getFreeQuantity(),
            dto.getApplicableProducts(),
            dto.getApplicableCategories()
        );
    }
}
```

## Rules
1. DTOs should be simple data containers with minimal logic
2. DTOs should include validation annotations and/or validation methods
3. DTOs should be designed to match the structure of HTTP requests and responses
4. Mappers should handle the conversion between DTOs and domain objects or commands
5. DTOs should not expose domain model details to API clients
6. DTOs should be immutable where possible
7. DTOs should use appropriate JSON annotations for serialization/deserialization
8. API documentation annotations should be added to all DTOs
9. Naming conventions should clearly indicate the purpose of each DTO (e.g., Request, Response)
10. Error responses should follow a consistent format