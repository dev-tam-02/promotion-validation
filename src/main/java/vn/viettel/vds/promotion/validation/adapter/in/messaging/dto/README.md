# Adapter In Messaging DTO Package

## Description
The adapter.in.messaging.dto package contains Data Transfer Objects (DTOs) used by messaging adapters to convert between external message formats and the application's domain model. These DTOs represent the structure of incoming messages and provide a clear boundary between external systems and the application core.

## Purpose
The purpose of this package is to isolate the structure of external messages from the domain model, allowing the application to evolve independently of the message formats used by external systems. DTOs also provide a place to implement validation and conversion logic specific to messaging interfaces.

## Usage
This package typically contains:
- Message DTOs that represent the structure of incoming messages
- Event DTOs that represent domain events received from external systems
- Command DTOs that represent commands received from external systems
- Mappers that convert between DTOs and domain objects or commands
- Validation annotations and custom validators for message payloads

## Examples

### Message DTO Example
```java
// ExternalCampaignEvent.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalCampaignEvent {
    
    @NotNull
    private String externalId;
    
    @NotNull
    @Size(min = 3, max = 100)
    private String name;
    
    @Size(max = 500)
    private String description;
    
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    @NotNull
    private String targetAudience;
    
    private List<ExternalDiscountDto> discounts;
    
    // Validation method
    public void validate() {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        if (discounts == null || discounts.isEmpty()) {
            throw new IllegalArgumentException("At least one discount must be provided");
        }
    }
}
```

### Nested DTO Example
```java
// ExternalDiscountDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDiscountDto {
    
    @NotNull
    @Size(min = 3, max = 100)
    private String name;
    
    @NotNull
    private String discountMethod;
    
    private Double discountValue;
    
    private BigDecimal bundlePrice;
    
    private String currency;
    
    private Integer minQuantity;
    
    private Integer freeQuantity;
    
    @NotNull
    private List<String> applicableProductIds;
    
    private List<String> applicableCategoryIds;
    
    // Validation method
    public void validate() {
        if ("PERCENTAGE".equals(discountMethod)) {
            if (discountValue == null || discountValue <= 0 || discountValue > 100) {
                throw new IllegalArgumentException("Percentage discount must be between 0 and 100");
            }
        } else if ("FIXED_AMOUNT".equals(discountMethod)) {
            if (discountValue == null || discountValue <= 0) {
                throw new IllegalArgumentException("Fixed amount discount must be greater than 0");
            }
            if (currency == null) {
                throw new IllegalArgumentException("Currency is required for fixed amount discounts");
            }
        } else if ("BUY_X_GET_Y".equals(discountMethod)) {
            if (minQuantity == null || minQuantity <= 0) {
                throw new IllegalArgumentException("Minimum quantity must be greater than 0");
            }
            if (freeQuantity == null || freeQuantity <= 0) {
                throw new IllegalArgumentException("Free quantity must be greater than 0");
            }
        } else if ("BUNDLE_PRICE".equals(discountMethod)) {
            if (bundlePrice == null || bundlePrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Bundle price must be greater than 0");
            }
            if (currency == null) {
                throw new IllegalArgumentException("Currency is required for bundle price discounts");
            }
        }
        
        if (applicableProductIds == null || applicableProductIds.isEmpty()) {
            if (applicableCategoryIds == null || applicableCategoryIds.isEmpty()) {
                throw new IllegalArgumentException("At least one applicable product or category must be specified");
            }
        }
    }
}
```

### DTO Mapper Example
```java
// CampaignEventMapper.java
@Component
public class CampaignEventMapper {
    
    /**
     * Maps an external campaign event to a process external campaign command
     */
    public ProcessExternalCampaignCommand toCommand(ExternalCampaignEvent event) {
        // Validate the event
        event.validate();
        
        // Map discounts
        List<ProcessExternalCampaignCommand.DiscountDto> discountDtos = event.getDiscounts().stream()
            .map(this::toDiscountDto)
            .collect(Collectors.toList());
        
        // Create and return the command
        return new ProcessExternalCampaignCommand(
            event.getExternalId(),
            event.getName(),
            event.getDescription(),
            event.getStartDate(),
            event.getEndDate(),
            TargetAudience.valueOf(event.getTargetAudience()),
            discountDtos
        );
    }
    
    private ProcessExternalCampaignCommand.DiscountDto toDiscountDto(ExternalDiscountDto dto) {
        // Validate the discount DTO
        dto.validate();
        
        // Map to command's discount DTO
        return new ProcessExternalCampaignCommand.DiscountDto(
            dto.getName(),
            DiscountMethod.valueOf(dto.getDiscountMethod()),
            dto.getDiscountValue(),
            dto.getBundlePrice() != null ? new Money(dto.getBundlePrice(), Currency.getInstance(dto.getCurrency())) : null,
            dto.getMinQuantity(),
            dto.getFreeQuantity(),
            dto.getApplicableProductIds().stream()
                .map(ProductId::new)
                .collect(Collectors.toSet()),
            dto.getApplicableCategoryIds() != null ? 
                dto.getApplicableCategoryIds().stream()
                    .map(CategoryId::new)
                    .collect(Collectors.toSet()) : 
                Collections.emptySet()
        );
    }
}
```

## Rules
1. DTOs should be simple data containers with minimal logic
2. DTOs should include validation annotations and/or validation methods
3. DTOs should be designed to match the structure of incoming messages
4. Mappers should handle the conversion between DTOs and domain objects or commands
5. DTOs should not expose domain model details to external systems
6. DTOs should be immutable where possible
7. DTOs should use appropriate JSON annotations for serialization/deserialization
8. Naming conventions should clearly indicate the purpose of each DTO
9. Documentation should be provided for complex DTOs
10. DTOs should not contain business logic