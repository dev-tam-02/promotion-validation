# Domain Service Package

## Description

The domain.service package contains domain services that implement complex business logic that doesn't naturally fit
within a single entity or value object. These services operate on multiple domain objects and encapsulate business rules
that span across them.

## Purpose

The purpose of this package is to provide a home for business logic that doesn't belong to any specific entity or value
object, while keeping this logic within the domain layer and separate from application concerns.

## Usage

This package typically contains:

- Domain services that operate on multiple entities or aggregates
- Complex calculation services (e.g., discount calculators, eligibility evaluators)
- Policy services that implement business policies
- Validation services that implement complex validation rules
- Domain-specific algorithms and business processes

## Examples

### Discount Calculation Service Example

```java
// DiscountCalculationService.java
public class DiscountCalculationService {

    /**
     * Calculates the final price after applying a promotion to an order
     */
    public Money calculateDiscountedPrice(Order order, Promotion promotion) {
        Money originalTotal = order.calculateTotal();

        switch (promotion.getDiscountMethod()) {
            case PERCENTAGE:
                return calculatePercentageDiscount(originalTotal, promotion.getDiscountValue());
            case FIXED_AMOUNT:
                return calculateFixedAmountDiscount(originalTotal, promotion.getDiscountValue(), promotion.getCurrency());
            case BUY_X_GET_Y:
                return calculateBuyXGetYDiscount(order, promotion.getMinQuantity(), promotion.getFreeQuantity());
            case BUNDLE_PRICE:
                return calculateBundleDiscount(order, promotion.getBundlePrice());
            default:
                throw new IllegalArgumentException("Unsupported discount method: " + promotion.getDiscountMethod());
        }
    }

    private Money calculatePercentageDiscount(Money originalPrice, double percentageOff) {
        if (percentageOff <= 0 || percentageOff > 100) {
            throw new IllegalArgumentException("Percentage discount must be between 0 and 100");
        }

        BigDecimal discountFactor = BigDecimal.ONE.subtract(BigDecimal.valueOf(percentageOff).divide(BigDecimal.valueOf(100)));
        BigDecimal discountedAmount = originalPrice.getAmount().multiply(discountFactor);

        return new Money(discountedAmount, originalPrice.getCurrency());
    }

    private Money calculateFixedAmountDiscount(Money originalPrice, double amountOff, Currency currency) {
        Money discount = new Money(BigDecimal.valueOf(amountOff), currency);

        if (discount.getAmount().compareTo(originalPrice.getAmount()) > 0) {
            return new Money(BigDecimal.ZERO, originalPrice.getCurrency());
        }

        return originalPrice.subtract(discount);
    }

    private Money calculateBuyXGetYDiscount(Order order, int minQuantity, int freeQuantity) {
        // Implementation for Buy X Get Y discount logic
        // ...
        return Money.of(0, Currency.getInstance("USD")); // Placeholder
    }

    private Money calculateBundleDiscount(Order order, Money bundlePrice) {
        // Implementation for bundle discount logic
        // ...
        return Money.of(0, Currency.getInstance("USD")); // Placeholder
    }
}
```

### Eligibility Service Example

```java
// CampaignEligibilityService.java
public class CampaignEligibilityService {

    /**
     * Determines if a customer is eligible for a specific campaign
     */
    public boolean isCustomerEligibleForCampaign(Customer customer, Campaign campaign) {
        // Check if campaign is active
        if (!campaign.isActive()) {
            return false;
        }

        // Check if customer meets target audience criteria
        if (!meetsTargetAudienceCriteria(customer, campaign.getTargetAudience())) {
            return false;
        }

        // Check if customer has reached usage limit
        if (hasReachedUsageLimit(customer, campaign)) {
            return false;
        }

        // Check if customer is in the right geographic location
        if (!isInValidGeographicArea(customer, campaign.getGeographicRestrictions())) {
            return false;
        }

        return true;
    }

    private boolean meetsTargetAudienceCriteria(Customer customer, TargetAudience targetAudience) {
        return targetAudience.isEligible(customer);
    }

    private boolean hasReachedUsageLimit(Customer customer, Campaign campaign) {
        int usageCount = customer.getUsageCountForCampaign(campaign.getId());
        return usageCount >= campaign.getMaxUsagePerCustomer();
    }

    private boolean isInValidGeographicArea(Customer customer, Set<GeographicArea> validAreas) {
        if (validAreas.isEmpty()) {
            return true; // No restrictions
        }

        GeographicArea customerArea = customer.getGeographicArea();
        return validAreas.contains(customerArea);
    }
}
```

### Validation Service Example

```java
// PromotionValidationService.java
public class PromotionValidationService {

    /**
     * Validates that a promotion is correctly configured and can be added to a campaign
     */
    public ValidationResult validatePromotion(Promotion promotion, Campaign campaign) {
        ValidationResult result = new ValidationResult();

        // Check basic properties
        if (promotion.getName() == null || promotion.getName().trim().isEmpty()) {
            result.addError("Promotion name cannot be empty");
        }

        if (promotion.getDiscountMethod() == null) {
            result.addError("Discount method must be specified");
        }

        // Check discount value based on discount method
        if (promotion.getDiscountMethod() != null && promotion.getDiscountMethod().requiresDiscountValue()) {
            if (promotion.getDiscountValue() <= 0) {
                result.addError("Discount value must be greater than zero for " + 
                               promotion.getDiscountMethod().getDescription());
            }

            if (promotion.getDiscountMethod() == DiscountMethod.PERCENTAGE && promotion.getDiscountValue() > 100) {
                result.addError("Percentage discount cannot exceed 100%");
            }
        }

        // Check that promotion validity period is within campaign validity period
        if (promotion.getValidityPeriod() != null && campaign.getValidityPeriod() != null) {
            if (promotion.getValidityPeriod().getStartDate().isBefore(campaign.getValidityPeriod().getStartDate()) ||
                promotion.getValidityPeriod().getEndDate().isAfter(campaign.getValidityPeriod().getEndDate())) {
                result.addError("Promotion validity period must be within campaign validity period");
            }
        }

        // Check product applicability
        if (promotion.getApplicableProducts().isEmpty() && promotion.getApplicableCategories().isEmpty()) {
            result.addError("Promotion must apply to at least one product or category");
        }

        return result;
    }
}
```

## Rules

1. Domain services should operate on domain entities and value objects
2. Domain services should be stateless
3. Domain services should implement business logic that doesn't naturally fit within a single entity
4. Domain services should be named after a business activity or process, using verbs (e.g., CampaignEligibilityService)
5. Domain services should not depend on infrastructure concerns
6. Domain services should not depend on application services
7. Domain services should be testable in isolation
8. Domain services should use the ubiquitous language of the business domain
9. Domain services should not contain persistence logic
10. Domain services should be focused on a single responsibility or a closely related group of responsibilities
11. Domain services should not expose implementation details of the domain model
