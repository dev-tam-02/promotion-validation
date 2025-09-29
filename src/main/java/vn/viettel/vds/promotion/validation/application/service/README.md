# Application Usecase Package

## Description
The application.usecase package contains implementations of the application's use cases. These classes orchestrate the flow of data to and from the domain entities and implement the business rules specific to the application.

## Purpose
The purpose of this package is to encapsulate the application's business logic and coordinate the interactions between the domain entities and the outside world through the port interfaces.

## Usage
This package typically contains:
- Use case implementations that implement the interfaces defined in the application.port.in package
- Service classes that orchestrate domain entities to fulfill business requirements
- Command handlers and query handlers if following the CQRS pattern
- Transaction management for operations that span multiple domain entities

## Examples

### Command Use Case Implementation Example
```java
// CreateCampaignService.java
@Service
@RequiredArgsConstructor
public class CreateCampaignService implements CreateCampaignUseCase {

    private final CampaignRepository campaignRepository;
    private final CampaignValidator campaignValidator;
    private final CampaignEventPublisher eventPublisher;

    @Override
    @Transactional
    public CampaignId createCampaign(CreateCampaignCommand command) {
        // Validate the command
        campaignValidator.validateCreateCampaignCommand(command);

        // Create a DateRange value object
        DateRange validityPeriod = new DateRange(command.getStartDate(), command.getEndDate());

        // Generate a new campaign ID
        CampaignId campaignId = new CampaignId();

        // Create the campaign entity
        Campaign campaign = new Campaign(
            campaignId,
            command.getName(),
            validityPeriod
        );

        // Set additional properties
        campaign.setDescription(command.getDescription());
        campaign.setTargetAudience(command.getTargetAudience());
        campaign.setGeographicRestrictions(command.getGeographicRestrictions());
        campaign.setMaxUsagePerCustomer(command.getMaxUsagePerCustomer());

        // Save the campaign using the output port
        campaignRepository.save(campaign);

        // Publish an event
        eventPublisher.publishCampaignCreatedEvent(campaign);

        // Return the ID of the created campaign
        return campaignId;
    }
}
```

### Query Use Case Implementation Example
```java
// GetCampaignService.java
@Service
@RequiredArgsConstructor
public class GetCampaignService implements GetCampaignQuery {

    private final CampaignRepository campaignRepository;
    private final CampaignCache campaignCache;

    @Override
    @Transactional(readOnly = true)
    public CampaignDto getCampaignById(CampaignId campaignId) {
        // Try to get from cache first
        Campaign campaign = campaignCache.getCampaign(campaignId)
            .orElseGet(() -> campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId)));

        // Update the cache if it was retrieved from the repository
        if (!campaignCache.getCampaign(campaignId).isPresent()) {
            campaignCache.putCampaign(campaign, 3600); // Cache for 1 hour
        }

        // Map the domain entity to a DTO
        return mapToDto(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignDto> getActiveCampaigns() {
        List<Campaign> activeCampaigns = campaignRepository.findByStatus(CampaignStatus.ACTIVE);
        return activeCampaigns.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignDto> getCampaignsByStatus(CampaignStatus status) {
        List<Campaign> campaigns = campaignRepository.findByStatus(status);
        return campaigns.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private CampaignDto mapToDto(Campaign campaign) {
        return new CampaignDto(
            campaign.getId().getValue(),
            campaign.getName(),
            campaign.getDescription(),
            campaign.getStatus().name(),
            campaign.getValidityPeriod().getStartDate(),
            campaign.getValidityPeriod().getEndDate(),
            campaign.getTargetAudience().name(),
            campaign.getGeographicRestrictions().stream()
                .map(GeographicArea::getName)
                .collect(Collectors.toSet()),
            campaign.getMaxUsagePerCustomer(),
            campaign.getPromotions().stream()
                .map(this::mapToPromotionDto)
                .collect(Collectors.toList())
        );
    }

    private PromotionDto mapToPromotionDto(Promotion promotion) {
        // Mapping logic omitted for brevity
        return new PromotionDto(
            // ... map promotion properties to DTO
        );
    }
}
```

### Complex Use Case with Domain Service Example
```java
// ApplyCampaignDiscountService.java
@Service
@RequiredArgsConstructor
public class ApplyCampaignDiscountService implements ApplyCampaignDiscountUseCase {

    private final CampaignRepository campaignRepository;
    private final CustomerRepository customerRepository;
    private final DiscountCalculationService discountCalculationService;
    private final CampaignEligibilityService campaignEligibilityService;
    private final CampaignUsageTracker campaignUsageTracker;

    @Override
    @Transactional
    public DiscountResult calculateDiscount(CalculateDiscountCommand command) {
        // Get the customer
        Customer customer = customerRepository.findById(command.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.getCustomerId()));

        // Get the order from the command
        Order order = mapToOrder(command);

        // Find applicable campaigns
        List<Campaign> activeCampaigns = campaignRepository.findByStatus(CampaignStatus.ACTIVE);

        // Find the best discount
        DiscountResult bestDiscount = DiscountResult.noDiscount();
        Campaign bestCampaign = null;
        Promotion bestPromotion = null;

        for (Campaign campaign : activeCampaigns) {
            // Check if customer is eligible for this campaign
            if (!campaignEligibilityService.isCustomerEligibleForCampaign(customer, campaign)) {
                continue;
            }

            // Check each promotion in the campaign
            for (Promotion promotion : campaign.getPromotions()) {
                // Check if the promotion applies to the order
                if (!isPromotionApplicableToOrder(promotion, order)) {
                    continue;
                }

                // Calculate the discount
                Money discountedPrice = discountCalculationService.calculateDiscountedPrice(order, promotion);
                Money discountAmount = order.calculateTotal().subtract(discountedPrice);

                // Check if this is the best discount so far
                if (discountAmount.getAmount().compareTo(bestDiscount.getDiscountAmount().getAmount()) > 0) {
                    bestDiscount = new DiscountResult(
                        campaign.getId(),
                        promotion.getId(),
                        discountedPrice,
                        discountAmount,
                        promotion.getDiscountMethod()
                    );
                    bestCampaign = campaign;
                    bestPromotion = promotion;
                }
            }
        }

        // If a discount was found, track the usage
        if (bestCampaign != null && bestPromotion != null) {
            campaignUsageTracker.trackUsage(customer.getId(), bestCampaign.getId(), bestPromotion.getId());
        }

        return bestDiscount;
    }

    private Order mapToOrder(CalculateDiscountCommand command) {
        // Mapping logic omitted for brevity
        return new Order(
            // ... map command properties to Order
        );
    }

    private boolean isPromotionApplicableToOrder(Promotion promotion, Order order) {
        // Check if the promotion applies to any of the products in the order
        return order.getItems().stream()
            .anyMatch(item -> 
                promotion.getApplicableProducts().contains(item.getProductId()) ||
                promotion.getApplicableCategories().stream()
                    .anyMatch(categoryId -> item.getCategories().contains(categoryId))
            );
    }
}
```

## Rules
1. Use case implementations should implement the interfaces defined in the application.port.in package
2. Use cases should depend on domain entities and output port interfaces, not on their implementations
3. Business logic that spans multiple domain entities should be implemented here
4. Use cases should be focused on a single responsibility or a closely related group of responsibilities
5. Use cases should not contain infrastructure concerns (database access, HTTP requests, etc.)
6. Input validation should be performed before modifying domain entities
7. Transactions should be managed at this level, not in the domain or adapter layers
8. Use cases should be testable without requiring external dependencies
9. Error handling should be consistent across all use cases
10. Use cases should be designed to be resilient to failures in external systems
