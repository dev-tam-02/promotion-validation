# Domain Enums Package

## Description

The domain.enums package contains enumeration classes that represent fixed sets of values with business meaning in the
domain. These enums encapsulate business concepts that have a limited set of possible values.

## Purpose

The purpose of this package is to provide type-safe representations of business concepts that have a fixed set of
possible values, making the code more readable, maintainable, and less prone to errors.

## Usage

This package typically contains:

- Enumerations representing status values (e.g., CampaignStatus, ApprovalStatus)
- Enumerations representing types or categories (e.g., CampaignType, PromotionType)
- Enumerations representing business rules or constraints (e.g., DiscountMethod, TargetAudience)
- Enumerations representing units of measurement or time periods (e.g., TimeUnit, FrequencyUnit)

## Examples

### Simple Enum Example

```java
// CampaignStatus.java
public enum CampaignStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
    REJECTED
}

// Usage example
Campaign campaign = new Campaign(campaignId, "Summer Sale", dateRange);
if (campaign.getStatus() == CampaignStatus.APPROVED) {
    campaign.activate();
}
```

### Enum with Properties and Methods

```java
// DiscountMethod.java
public enum DiscountMethod {
    PERCENTAGE("Percentage discount", true),
    FIXED_AMOUNT("Fixed amount discount", true),
    BUY_X_GET_Y("Buy X get Y free", false),
    BUNDLE_PRICE("Bundle price", false);

    private final String description;
    private final boolean requiresDiscountValue;

    DiscountMethod(String description, boolean requiresDiscountValue) {
        this.description = description;
        this.requiresDiscountValue = requiresDiscountValue;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresDiscountValue() {
        return requiresDiscountValue;
    }

    public boolean isValueBased() {
        return this == PERCENTAGE || this == FIXED_AMOUNT;
    }
}

// Usage example
Promotion promotion = new Promotion("Summer Discount", DiscountMethod.PERCENTAGE);
if (promotion.getDiscountMethod().requiresDiscountValue()) {
    promotion.setDiscountValue(20.0); // 20% discount
}
```

### Enum with Behavior

```java
// TargetAudience.java
public enum TargetAudience {
    ALL_CUSTOMERS {
        @Override
        public boolean isEligible(Customer customer) {
            return true;
        }
    },
    NEW_CUSTOMERS {
        @Override
        public boolean isEligible(Customer customer) {
            return customer.getRegistrationDate().isAfter(LocalDate.now().minusMonths(3));
        }
    },
    PREMIUM_CUSTOMERS {
        @Override
        public boolean isEligible(Customer customer) {
            return customer.isPremium();
        }
    },
    RETURNING_CUSTOMERS {
        @Override
        public boolean isEligible(Customer customer) {
            return customer.getPurchaseCount() > 0 && 
                   customer.getLastPurchaseDate().isAfter(LocalDate.now().minusMonths(6));
        }
    };

    public abstract boolean isEligible(Customer customer);

    public static TargetAudience fromString(String value) {
        for (TargetAudience audience : values()) {
            if (audience.name().equalsIgnoreCase(value)) {
                return audience;
            }
        }
        throw new IllegalArgumentException("Unknown target audience: " + value);
    }
}

// Usage example
Campaign campaign = getCampaign();
Customer customer = getCustomer();
if (campaign.getTargetAudience().isEligible(customer)) {
    // Show campaign to customer
}
```

## Rules

1. Enum names should be singular and represent the concept they encapsulate
2. Enum values should be in uppercase with underscores separating words (e.g., ACTIVE, IN_PROGRESS)
3. Enums should include documentation explaining the business meaning of each value
4. Enums should be immutable and thread-safe
5. Complex enums can include additional properties and methods to encapsulate behavior
6. Enums should represent business concepts, not technical implementation details
7. Enum values should be stable and changes should be carefully managed to avoid breaking existing code
8. Consider providing utility methods for common operations (e.g., isActive(), isTerminal())
9. Enums should be used consistently throughout the application
