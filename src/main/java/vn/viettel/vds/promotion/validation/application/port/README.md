# Application Port Package

## Description
The application.port package contains interfaces that define the boundaries of the application. These interfaces act as contracts between the application core and the outside world.

## Purpose
The purpose of this package is to define clear boundaries for the application core, allowing it to remain isolated from external concerns while still being able to communicate with the outside world.

## Usage
This package is organized into two main subpackages:
- `in`: Contains input port interfaces that define the operations that can be performed on the application
- `out`: Contains output port interfaces that define the operations that the application needs from the outside world

## Examples

### Input Port Example
```java
// ManageCampaignUseCase.java in application.port.in package
public interface ManageCampaignUseCase {

    /**
     * Creates a new campaign with the given details
     */
    CampaignId createCampaign(CreateCampaignCommand command);

    /**
     * Updates an existing campaign with new details
     */
    void updateCampaign(UpdateCampaignCommand command);

    /**
     * Submits a campaign for approval
     */
    void submitForApproval(CampaignId campaignId);

    /**
     * Command for creating a campaign
     */
    @Value
    class CreateCampaignCommand {
        @NotNull String name;
        @NotNull String description;
        @NotNull LocalDate startDate;
        @NotNull LocalDate endDate;
        @NotNull TargetAudience targetAudience;
        Set<GeographicArea> geographicRestrictions;
        int maxUsagePerCustomer;
    }

    /**
     * Command for updating a campaign
     */
    @Value
    class UpdateCampaignCommand {
        @NotNull CampaignId campaignId;
        String name;
        String description;
        LocalDate startDate;
        LocalDate endDate;
        TargetAudience targetAudience;
        Set<GeographicArea> geographicRestrictions;
        Integer maxUsagePerCustomer;
    }
}
```

### Output Port Example
```java
// CampaignRepository.java in application.port.out package
public interface CampaignRepository {

    /**
     * Saves a campaign to the persistent storage
     */
    void save(Campaign campaign);

    /**
     * Finds a campaign by its ID
     */
    Optional<Campaign> findById(CampaignId campaignId);

    /**
     * Finds all active campaigns
     */
    List<Campaign> findActiveCampaigns();

    /**
     * Finds campaigns by status
     */
    List<Campaign> findByStatus(CampaignStatus status);

    /**
     * Finds campaigns that are valid during the specified date
     */
    List<Campaign> findValidCampaignsOnDate(LocalDate date);

    /**
     * Deletes a campaign
     */
    void delete(CampaignId campaignId);
}
```

### Port Usage Example
```java
// CreateCampaignService.java in application.usecase package
@Service
@RequiredArgsConstructor
public class CreateCampaignService implements ManageCampaignUseCase {

    private final CampaignRepository campaignRepository;
    private final CampaignValidator campaignValidator;

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

        // Return the ID of the created campaign
        return campaignId;
    }

    @Override
    @Transactional
    public void updateCampaign(UpdateCampaignCommand command) {
        // Implementation omitted for brevity
    }

    @Override
    @Transactional
    public void submitForApproval(CampaignId campaignId) {
        // Implementation omitted for brevity
    }
}
```

## Rules
1. Port interfaces should be defined in terms of the application's domain model, not in terms of external representations
2. Input ports should define methods that represent use cases or user actions
3. Output ports should define methods that the application needs to interact with external systems
4. Port interfaces should be stable and change only when the application's requirements change
5. Port interfaces should be designed with testability in mind
6. Input ports should accept and return DTOs (Data Transfer Objects) that are specific to the use case
7. Output ports should be designed to be easily mockable for testing
8. Port interfaces should follow the Interface Segregation Principle (ISP) - clients should not be forced to depend on methods they do not use
