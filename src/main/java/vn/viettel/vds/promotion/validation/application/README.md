# Application Package

## Description

The application package contains the core business logic and use cases of the campaign module. It orchestrates the flow
of data to and from the domain entities and implements the business rules specific to the application.

## Purpose

The purpose of this package is to encapsulate the application's use cases and define the boundaries between the domain
layer and the outside world through ports.

## Usage

This package is organized into two main subpackages:

- `port`: Contains interfaces that define the boundaries of the application
    - `in`: Input ports that define the operations that can be performed on the application
    - `out`: Output ports that define the operations that the application needs from the outside world
- `usecase`: Contains implementations of the application's use cases, which orchestrate the flow of data and implement
  business rules

## Examples

### Input Port Interface Example

```java
// CreateCampaignUseCase.java in application.port.in package
public interface CreateCampaignUseCase {

    CampaignId createCampaign(CreateCampaignCommand command);

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
}
```

### Output Port Interface Example

```java
// SaveCampaignPort.java in application.port.out package
public interface SaveCampaignPort {

    void saveCampaign(Campaign campaign);

    Optional<Campaign> findCampaignById(CampaignId campaignId);

    List<Campaign> findActiveCampaigns();
}
```

### Use Case Implementation Example

```java
// CreateCampaignService.java in application.usecase package
@Service
@RequiredArgsConstructor
public class CreateCampaignService implements CreateCampaignUseCase {

    private final SaveCampaignPort saveCampaignPort;
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
        saveCampaignPort.saveCampaign(campaign);

        // Return the ID of the created campaign
        return campaignId;
    }
}
```

### Application Service Example

```java
// CampaignApplicationService.java in application package
@Service
@RequiredArgsConstructor
public class CampaignApplicationService {

    private final CreateCampaignUseCase createCampaignUseCase;
    private final UpdateCampaignUseCase updateCampaignUseCase;
    private final ApproveCampaignUseCase approveCampaignUseCase;
    private final ActivateCampaignUseCase activateCampaignUseCase;
    private final GetCampaignUseCase getCampaignUseCase;

    // Method that coordinates multiple use cases
    @Transactional
    public CampaignId createAndActivateCampaign(CreateCampaignCommand createCommand) {
        // Create the campaign
        CampaignId campaignId = createCampaignUseCase.createCampaign(createCommand);

        // Submit for approval
        updateCampaignUseCase.submitForApproval(campaignId);

        // Approve the campaign
        approveCampaignUseCase.approveCampaign(campaignId, "Automatic approval for immediate activation");

        // Activate the campaign
        activateCampaignUseCase.activateCampaign(campaignId);

        return campaignId;
    }

    // Method that transforms domain objects to DTOs
    public CampaignSummaryDto getCampaignSummary(CampaignId campaignId) {
        Campaign campaign = getCampaignUseCase.getCampaignById(campaignId);

        return new CampaignSummaryDto(
            campaign.getId().getValue(),
            campaign.getName(),
            campaign.getDescription(),
            campaign.getStatus().name(),
            campaign.getValidityPeriod().getStartDate(),
            campaign.getValidityPeriod().getEndDate(),
            campaign.getPromotions().size()
        );
    }
}
```

## Rules

1. Application services should depend on domain entities and should not contain domain logic
2. Use cases should implement the interfaces defined in the port.in package
3. Use cases should depend on the interfaces defined in the port.out package, not on their implementations
4. Business rules that span multiple domain entities should be implemented here
5. This layer should be independent of any infrastructure or UI concerns
6. Data transformation between the domain and external representations should happen here
