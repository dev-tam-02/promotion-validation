# Domain Model Package

## Description
The domain.model package contains the core business entities, value objects, and aggregates that represent the fundamental business concepts of the campaign module. These classes encapsulate both data and behavior related to the business domain.

## Purpose
The purpose of this package is to model the business domain in a way that captures its essential concepts, relationships, and rules, providing a rich, object-oriented representation of the business reality.

## Usage
This package typically contains:
- Entity classes that represent business objects with identity (e.g., Campaign, Promotion)
- Value objects that represent immutable concepts defined by their attributes (e.g., Money, DateRange)
- Aggregates that group related entities and value objects into a cohesive unit
- Domain events that represent significant occurrences in the domain
- Factories that encapsulate the creation logic for complex domain objects

## Examples

### Entity Example
```java
// Campaign.java
public class Campaign {
    private final CampaignId id;
    private String name;
    private String description;
    private DateRange validityPeriod;
    private CampaignStatus status;
    private Set<Promotion> promotions;

    public Campaign(CampaignId id, String name, DateRange validityPeriod) {
        if (id == null) {
            throw new IllegalArgumentException("Campaign ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign name cannot be null or empty");
        }
        if (validityPeriod == null) {
            throw new IllegalArgumentException("Validity period cannot be null");
        }

        this.id = id;
        this.name = name;
        this.validityPeriod = validityPeriod;
        this.status = CampaignStatus.DRAFT;
        this.promotions = new HashSet<>();
    }

    // Identity is based on ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Campaign campaign = (Campaign) o;
        return id.equals(campaign.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Business methods
    public void addPromotion(Promotion promotion) {
        if (promotion == null) {
            throw new IllegalArgumentException("Promotion cannot be null");
        }
        if (status != CampaignStatus.DRAFT && status != CampaignStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot add promotions to a campaign that is not in DRAFT or PENDING_APPROVAL state");
        }

        promotions.add(promotion);
    }

    public void submitForApproval() {
        if (status != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT campaigns can be submitted for approval");
        }
        if (promotions.isEmpty()) {
            throw new IllegalStateException("Cannot submit a campaign without promotions");
        }

        status = CampaignStatus.PENDING_APPROVAL;
    }

    // More business methods...
}
```

### Value Object Example
```java
// Money.java
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }

        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    // Factory methods
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    // Value objects are immutable, so we provide methods that return new instances
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }

        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }

        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    // Equality is based on all attributes
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount.toString() + " " + currency.getCurrencyCode();
    }
}
```

### Aggregate Example
```java
// Order.java (Aggregate Root)
public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private final LocalDateTime orderDate;
    private Address shippingAddress;
    private Address billingAddress;
    private PaymentDetails paymentDetails;

    // Constructor and other methods...

    // Methods to manage the aggregate
    public void addItem(Product product, int quantity) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify items for an order that is not in DRAFT state");
        }

        // Check if the product is already in the order
        for (OrderItem item : items) {
            if (item.getProductId().equals(product.getId())) {
                item.increaseQuantity(quantity);
                return;
            }
        }

        // Add new item
        OrderItem newItem = new OrderItem(new OrderItemId(), this.id, product.getId(), product.getName(), 
                                         product.getPrice(), quantity);
        items.add(newItem);
    }

    public void removeItem(OrderItemId orderItemId) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify items for an order that is not in DRAFT state");
        }

        items.removeIf(item -> item.getId().equals(orderItemId));
    }

    public Money calculateTotal() {
        return items.stream()
                .map(OrderItem::calculateSubtotal)
                .reduce(Money.zero(Currency.getInstance("USD")), Money::add);
    }

    public void placeOrder() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT orders can be placed");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot place an order without items");
        }
        if (shippingAddress == null) {
            throw new IllegalStateException("Shipping address is required");
        }
        if (paymentDetails == null) {
            throw new IllegalStateException("Payment details are required");
        }

        status = OrderStatus.PLACED;
        // Publish domain event
        DomainEventPublisher.publish(new OrderPlacedEvent(this.id, this.customerId, calculateTotal()));
    }

    // More business methods...
}
```

## Rules
1. Domain models should encapsulate both data and behavior
2. Entities should have a clear identity concept and equality based on identity
3. Value objects should be immutable and equality should be based on all attributes
4. Domain models should always be in a valid state
5. Business rules should be expressed explicitly in the domain model
6. Domain models should be independent of infrastructure concerns
7. Use rich domain models rather than anemic ones (behavior in addition to data)
8. Domain models should use ubiquitous language from the business domain
9. Aggregates should define clear boundaries and have a single root entity
10. Domain events should be used to communicate changes between aggregates
11. Avoid circular dependencies between domain models
12. Domain models should be testable in isolation
