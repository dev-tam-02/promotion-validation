# Application Port Out Package

## Description

The application.port.out package contains output port interfaces that define the operations that the application needs
from the outside world. These interfaces represent the dependencies of the application on external systems.

## Purpose

The purpose of this package is to define clear contracts for what the application needs from external systems, allowing
the application core to remain isolated from the implementation details of these systems.

## Usage

This package typically contains:

- Repository interfaces for data persistence
- Client interfaces for external services
- Messaging interfaces for sending messages to external systems
- Notification interfaces for sending notifications
- File system interfaces for file operations
- Cache interfaces for caching operations

## Examples

### Repository Interface Example

```java
// CampaignRepository.java
public interface CampaignRepository {

    /**
     * Saves a campaign to the persistent storage
     * 
     * @param campaign The campaign to save
     */
    void save(Campaign campaign);

    /**
     * Finds a campaign by its ID
     * 
     * @param campaignId The ID of the campaign to find
     * @return The campaign if found, empty otherwise
     */
    Optional<Campaign> findById(CampaignId campaignId);

    /**
     * Finds all campaigns with the specified status
     * 
     * @param status The status to filter by
     * @return A list of campaigns with the specified status
     */
    List<Campaign> findByStatus(CampaignStatus status);

    /**
     * Finds all campaigns that are valid on the specified date
     * 
     * @param date The date to check validity for
     * @return A list of campaigns valid on the specified date
     */
    List<Campaign> findValidOnDate(LocalDate date);

    /**
     * Deletes a campaign
     * 
     * @param campaignId The ID of the campaign to delete
     * @return true if the campaign was deleted, false if it was not found
     */
    boolean delete(CampaignId campaignId);
}
```

### External Service Client Example

```java
// ProductCatalogClient.java
public interface ProductCatalogClient {

    /**
     * Gets product details from the product catalog
     * 
     * @param productId The ID of the product to retrieve
     * @return The product details if found, empty otherwise
     */
    Optional<Product> getProduct(ProductId productId);

    /**
     * Gets multiple products by their IDs
     * 
     * @param productIds The IDs of the products to retrieve
     * @return A map of product IDs to products
     */
    Map<ProductId, Product> getProducts(Set<ProductId> productIds);

    /**
     * Gets all products in a category
     * 
     * @param categoryId The ID of the category
     * @return A list of products in the category
     */
    List<Product> getProductsByCategory(CategoryId categoryId);

    /**
     * Checks if a product is in stock
     * 
     * @param productId The ID of the product to check
     * @return true if the product is in stock, false otherwise
     */
    boolean isProductInStock(ProductId productId);
}
```

### Messaging Interface Example

```java
// CampaignEventPublisher.java
public interface CampaignEventPublisher {

    /**
     * Publishes an event when a campaign is created
     * 
     * @param campaign The created campaign
     */
    void publishCampaignCreatedEvent(Campaign campaign);

    /**
     * Publishes an event when a campaign is activated
     * 
     * @param campaign The activated campaign
     */
    void publishCampaignActivatedEvent(Campaign campaign);

    /**
     * Publishes an event when a campaign is completed
     * 
     * @param campaign The completed campaign
     */
    void publishCampaignCompletedEvent(Campaign campaign);

    /**
     * Publishes an event when a promotion is added to a campaign
     * 
     * @param campaign The campaign
     * @param promotion The added promotion
     */
    void publishPromotionAddedEvent(Campaign campaign, Promotion promotion);
}
```

### Cache Interface Example

```java
// CampaignCache.java
public interface CampaignCache {

    /**
     * Gets a campaign from the cache
     * 
     * @param campaignId The ID of the campaign to retrieve
     * @return The campaign if found in the cache, empty otherwise
     */
    Optional<Campaign> getCampaign(CampaignId campaignId);

    /**
     * Puts a campaign in the cache
     * 
     * @param campaign The campaign to cache
     * @param timeToLive The time to live for the cache entry in seconds
     */
    void putCampaign(Campaign campaign, int timeToLive);

    /**
     * Removes a campaign from the cache
     * 
     * @param campaignId The ID of the campaign to remove
     */
    void removeCampaign(CampaignId campaignId);

    /**
     * Clears all campaigns from the cache
     */
    void clearAllCampaigns();
}
```

### Usage in Application Service

```java
// ActivateCampaignService.java
@Service
@RequiredArgsConstructor
public class ActivateCampaignService implements ActivateCampaignUseCase {

    private final CampaignRepository campaignRepository;
    private final CampaignEventPublisher eventPublisher;
    private final CampaignCache campaignCache;

    @Override
    @Transactional
    public void activateCampaign(CampaignId campaignId) {
        // Try to get from cache first
        Campaign campaign = campaignCache.getCampaign(campaignId)
            .orElseGet(() -> campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId)));

        // Activate the campaign
        campaign.activate();

        // Save the updated campaign
        campaignRepository.save(campaign);

        // Update the cache
        campaignCache.putCampaign(campaign, 3600); // Cache for 1 hour

        // Publish an event
        eventPublisher.publishCampaignActivatedEvent(campaign);
    }
}
```

## Rules

1. Output port interfaces should be defined in terms of the application's domain model
2. Each interface should represent a single responsibility or a closely related group of responsibilities
3. Method names should clearly describe the operation being performed
4. Interfaces should be designed to be easily mockable for testing
5. Output ports should not expose implementation details of external systems
6. Documentation should clearly describe the purpose and expected behavior of each method
7. Output ports should follow the Interface Segregation Principle (ISP)
8. Return types should be well-defined and consistent with the domain model
9. Error handling should be clearly defined (exceptions, return types, etc.)
10. Output ports should be designed to be resilient to external system failures
