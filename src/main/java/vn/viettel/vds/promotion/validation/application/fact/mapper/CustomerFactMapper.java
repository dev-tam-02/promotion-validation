package vn.viettel.vds.promotion.validation.application.fact.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.CustomerFact;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class CustomerFactMapper {

    public CustomerFact map(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        CustomerFact.Builder builder = CustomerFact.builder();

        builder.customerId(sanitizeString(data, "customerId"));
        builder.email(sanitizeEmail(data, "email"));
        builder.phone(sanitizePhone(data, "phone"));
        builder.tier(normalizeTier(data, "tier"));
        builder.registrationDate(parseInstant(data, "registrationDate"));
        builder.lastActivityDate(parseInstant(data, "lastActivityDate"));
        builder.isActive(parseBoolean(data, "isActive", true));
        builder.region(normalizeRegion(data, "region"));
        builder.language(normalizeLanguage(data, "language"));
        builder.currency(normalizeCurrency(data, "currency"));
        builder.tags(sanitizeStringList(data, "tags"));
        builder.attributes(sanitizeAttributes(data, "attributes"));
        builder.preferences(sanitizeAttributes(data, "preferences"));

        return builder.build();
    }

    private String sanitizeString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        String str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }

    private String sanitizeEmail(Map<String, Object> data, String key) {
        String email = sanitizeString(data, key);
        if (email != null && email.contains("@")) {
            return email.toLowerCase();
        }
        return null;
    }

    private String sanitizePhone(Map<String, Object> data, String key) {
        String phone = sanitizeString(data, key);
        if (phone != null) {
            return phone.replaceAll("[^+\\d]", "");
        }
        return null;
    }

    private String normalizeTier(Map<String, Object> data, String key) {
        String tier = sanitizeString(data, key);
        if (tier != null) {
            return tier.toUpperCase();
        }
        return "STANDARD"; // Default tier
    }

    private String normalizeRegion(Map<String, Object> data, String key) {
        String region = sanitizeString(data, key);
        if (region != null) {
            return region.toUpperCase();
        }
        return null;
    }

    private String normalizeLanguage(Map<String, Object> data, String key) {
        String language = sanitizeString(data, key);
        if (language != null) {
            return language.toLowerCase();
        }
        return "en"; // Default language
    }

    private String normalizeCurrency(Map<String, Object> data, String key) {
        String currency = sanitizeString(data, key);
        if (currency != null) {
            return currency.toUpperCase();
        }
        return "USD"; // Default currency
    }

    private Instant parseInstant(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean parseBoolean(Map<String, Object> data, String key, Boolean defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        String str = value.toString().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }

    private List<String> sanitizeStringList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }

    private Map<String, Object> sanitizeAttributes(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
}