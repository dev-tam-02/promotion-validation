package vn.viettel.vds.promotion.validation.application.fact.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.CandidateFact;
import vn.viettel.vds.promotion.validation.domain.fact.CustomerFact;
import vn.viettel.vds.promotion.validation.domain.fact.OrderFact;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class DerivedFieldsCalculator {

    public Map<String, Object> calculate(CustomerFact customer, OrderFact order, CandidateFact candidate) {
        Map<String, Object> derived = new HashMap<>();

        // Customer-derived fields
        if (customer != null) {
            derived.putAll(calculateCustomerDerived(customer));
        }

        // Order-derived fields
        if (order != null) {
            derived.putAll(calculateOrderDerived(order));
        }

        // Cross-entity derived fields
        if (customer != null && order != null) {
            derived.putAll(calculateCrossEntityDerived(customer, order));
        }

        // Promotion-specific derived fields
        if (candidate != null) {
            derived.putAll(calculatePromotionDerived(candidate));
        }

        return derived;
    }

    private Map<String, Object> calculateCustomerDerived(CustomerFact customer) {
        Map<String, Object> derived = new HashMap<>();

        // Customer age calculation
        if (customer.registrationDate() != null) {
            long daysSinceRegistration = Duration.between(customer.registrationDate(), Instant.now()).toDays();
            derived.put("customerAge", daysSinceRegistration);
            derived.put("isNewCustomer", daysSinceRegistration <= 30);
            derived.put("isVeteranCustomer", daysSinceRegistration >= 365);
        }

        // Activity recency
        if (customer.lastActivityDate() != null) {
            long daysSinceLastActivity = Duration.between(customer.lastActivityDate(), Instant.now()).toDays();
            derived.put("daysSinceLastActivity", daysSinceLastActivity);
            derived.put("isActiveCustomer", daysSinceLastActivity <= 7);
            derived.put("isDormantCustomer", daysSinceLastActivity >= 90);
        }

        // Tier-based flags
        if (customer.tier() != null) {
            derived.put("isPremiumCustomer", "PREMIUM".equals(customer.tier()) || "VIP".equals(customer.tier()));
            derived.put("isStandardCustomer", "STANDARD".equals(customer.tier()));
        }

        return derived;
    }

    private Map<String, Object> calculateOrderDerived(OrderFact order) {
        Map<String, Object> derived = new HashMap<>();

        // Order value categorization
        if (order.totalAmount() != null) {
            BigDecimal amount = order.totalAmount();
            derived.put("isHighValueOrder", amount.compareTo(new BigDecimal("1000")) >= 0);
            derived.put("isMediumValueOrder", amount.compareTo(new BigDecimal("100")) >= 0 && amount.compareTo(new BigDecimal("1000")) < 0);
            derived.put("isLowValueOrder", amount.compareTo(new BigDecimal("100")) < 0);
        }

        // Item analysis
        if (order.items() != null && !order.items().isEmpty()) {
            derived.put("itemCount", order.items().size());
            derived.put("isSingleItemOrder", order.items().size() == 1);
            derived.put("isMultiItemOrder", order.items().size() > 1);
            derived.put("isBulkOrder", order.items().size() >= 10);

            // Price analysis
            if (order.cheapestItemPrice() != null && order.mostExpensiveItemPrice() != null) {
                BigDecimal priceRange = order.mostExpensiveItemPrice().subtract(order.cheapestItemPrice());
                derived.put("priceRange", priceRange);
                derived.put("hasWidePageRange", priceRange.compareTo(new BigDecimal("100")) >= 0);
            }
        }

        // Discount analysis
        if (order.appliedDiscounts() != null && !order.appliedDiscounts().isEmpty()) {
            derived.put("hasExistingDiscounts", true);
            derived.put("existingDiscountCount", order.appliedDiscounts().size());
        } else {
            derived.put("hasExistingDiscounts", false);
            derived.put("existingDiscountCount", 0);
        }

        // Channel analysis
        if (order.channel() != null) {
            derived.put("isOnlineOrder", "ONLINE".equalsIgnoreCase(order.channel()) || "WEB".equalsIgnoreCase(order.channel()));
            derived.put("isMobileOrder", "MOBILE".equalsIgnoreCase(order.channel()) || "APP".equalsIgnoreCase(order.channel()));
            derived.put("isInStoreOrder", "STORE".equalsIgnoreCase(order.channel()) || "POS".equalsIgnoreCase(order.channel()));
        }

        return derived;
    }

    private Map<String, Object> calculateCrossEntityDerived(CustomerFact customer, OrderFact order) {
        Map<String, Object> derived = new HashMap<>();

        // Customer-Order relationship
        derived.put("isCustomerOrderMatch", customer.customerId().equals(order.customerId()));

        // Customer tier vs order value
        if (customer.tier() != null && order.totalAmount() != null) {
            String tier = customer.tier();
            BigDecimal amount = order.totalAmount();

            boolean isHighValueForTier = switch (tier) {
                case "VIP" -> amount.compareTo(new BigDecimal("5000")) >= 0;
                case "PREMIUM" -> amount.compareTo(new BigDecimal("2000")) >= 0;
                case "STANDARD" -> amount.compareTo(new BigDecimal("500")) >= 0;
                default -> amount.compareTo(new BigDecimal("100")) >= 0;
            };

            derived.put("isHighValueForTier", isHighValueForTier);
        }

        // Regional consistency
        if (customer.region() != null) {
            derived.put("customerRegion", customer.region());
        }

        return derived;
    }

    private Map<String, Object> calculatePromotionDerived(CandidateFact candidate) {
        Map<String, Object> derived = new HashMap<>();

        // Promotion type analysis
        if (candidate.type() != null) {
            derived.put("isVoucherPromotion", "VOUCHER".equalsIgnoreCase(candidate.type()));
            derived.put("isDiscountPromotion", "DISCOUNT".equalsIgnoreCase(candidate.type()));
            derived.put("isCashbackPromotion", "CASHBACK".equalsIgnoreCase(candidate.type()));
            derived.put("isPointsPromotion", "POINTS".equalsIgnoreCase(candidate.type()));
        }

        // Promotion timing
        if (candidate.startDate() != null && candidate.endDate() != null) {
            Instant now = Instant.now();
            boolean isActive = !now.isBefore(candidate.startDate()) && !now.isAfter(candidate.endDate());
            derived.put("isPromotionActive", isActive);

            if (candidate.endDate() != null) {
                long hoursUntilExpiry = Duration.between(now, candidate.endDate()).toHours();
                derived.put("hoursUntilExpiry", hoursUntilExpiry);
                derived.put("isExpiringPromotion", hoursUntilExpiry <= 24 && hoursUntilExpiry > 0);
            }
        }

        // Promotion status
        if (candidate.status() != null) {
            derived.put("isActiveStatus", "ACTIVE".equalsIgnoreCase(candidate.status()));
            derived.put("isPausedStatus", "PAUSED".equalsIgnoreCase(candidate.status()));
        }

        return derived;
    }
}