# Domain Package

## Description

The domain package contains the core business entities, value objects, and domain services that represent the business
concepts and rules of the campaign module. This is the heart of the application where the business logic resides.

## Purpose

The purpose of this package is to encapsulate the core business logic and rules, independent of any application or
infrastructure concerns. It represents the most stable part of the system that changes only when the business rules
change.

## Usage

This package is organized into three main subpackages:

- `enums`: Contains enumeration classes that represent fixed sets of business values
- `model`: Contains domain entities and value objects that represent the core business concepts
- `service`: Contains domain services that implement complex business logic that doesn't naturally fit within a single
  entity

## Examples

### Domain Entity Example

```java
// Campaign.java in domain.model package
public class Campaign {
    private CampaignId id;
    private String name;
    private String description;
    private DateRange validityPeriod;
    private CampaignStatus status;
    private Set<Promotion> promotions;

    // Constructor ensures the entity is always in a valid state
    public Campaign(CampaignId id, String name, DateRange validityPeriod) {
        validateName(name);
        validateValidityPeriod(validityPeriod);

        this.id = id;
        this.name = name;
        this.validityPeriod = validityPeriod;
        this.status = CampaignStatus.DRAFT;
        this.promotions = new HashSet<>();
    }

    // Business method that encapsulates domain logic
    public void activate() {
        if (status != CampaignStatus.APPROVED) {
            throw new IllegalStateException("Only approved campaigns can be activated");
        }

        if (validityPeriod.isExpired()) {
            throw new IllegalStateException("Cannot activate an expired campaign");
        }

        this.status = CampaignStatus.ACTIVE;
    }

    // More business methods and validations...
}
```

### Value Object Example

```java
// DateRange.java in domain.model package
public class DateRange {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public DateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }

    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    // More methods and validations...
}
```

### Domain Service Example

```java
// CampaignEligibilityService.java in domain.service package
public class CampaignEligibilityService {

    public boolean isCustomerEligibleForCampaign(Customer customer, Campaign campaign) {
        if (!campaign.isActive()) {
            return false;
        }

        if (!meetsTargetAudienceCriteria(customer, campaign.getTargetAudience())) {
            return false;
        }

        if (hasReachedUsageLimit(customer, campaign)) {
            return false;
        }

        return true;
    }

    private boolean meetsTargetAudienceCriteria(Customer customer, TargetAudience targetAudience) {
        // Complex logic to determine if customer meets target audience criteria
        // ...
    }

    private boolean hasReachedUsageLimit(Customer customer, Campaign campaign) {
        // Logic to check if customer has reached usage limit for this campaign
        // ...
    }
}
```

## Rules

1. Domain classes should be independent of any infrastructure or application concerns
2. Domain entities should encapsulate both data and behavior related to the business concept they represent
3. Domain services should implement business logic that spans multiple entities
4. Business rules should be expressed explicitly in the domain model
5. Domain objects should always be in a valid state
6. Use value objects for concepts that are defined by their attributes rather than an identity
7. Domain logic should not depend on any external systems or frameworks
8. Enums should be used to represent fixed sets of values that have business meaning
