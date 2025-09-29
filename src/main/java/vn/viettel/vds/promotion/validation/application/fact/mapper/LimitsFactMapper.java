package vn.viettel.vds.promotion.validation.application.fact.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.LimitsFact;

import java.util.Map;

@Component
public class LimitsFactMapper {

    public LimitsFact map(Object data) {
        if (data == null) {
            return null;
        }

        if (data instanceof LimitsFact) {
            return (LimitsFact) data;
        }

        if (data instanceof Map) {
            // Handle case where data comes as Map (from cache or other sources)
            Map<String, Object> mapData = (Map<String, Object>) data;
            return mapFromData(mapData);
        }

        return null;
    }

    private LimitsFact mapFromData(Map<String, Object> data) {
        LimitsFact.Builder builder = LimitsFact.builder();

        if (data.get("globalLimits") instanceof java.util.List) {
            java.util.List<Map<String, Object>> globalLimitsData = (java.util.List<Map<String, Object>>) data.get("globalLimits");
            java.util.List<LimitsFact.LimitInfo> globalLimits = globalLimitsData.stream()
                .map(this::mapToLimitInfo)
                .collect(java.util.stream.Collectors.toList());
            builder.globalLimits(globalLimits);
        }

        if (data.get("customerLimits") instanceof java.util.List) {
            java.util.List<Map<String, Object>> customerLimitsData = (java.util.List<Map<String, Object>>) data.get("customerLimits");
            java.util.List<LimitsFact.LimitInfo> customerLimits = customerLimitsData.stream()
                .map(this::mapToLimitInfo)
                .collect(java.util.stream.Collectors.toList());
            builder.customerLimits(customerLimits);
        }

        if (data.get("campaignLimits") instanceof java.util.List) {
            java.util.List<Map<String, Object>> campaignLimitsData = (java.util.List<Map<String, Object>>) data.get("campaignLimits");
            java.util.List<LimitsFact.LimitInfo> campaignLimits = campaignLimitsData.stream()
                .map(this::mapToLimitInfo)
                .collect(java.util.stream.Collectors.toList());
            builder.campaignLimits(campaignLimits);
        }

        if (data.get("counters") instanceof Map) {
            Map<String, Map<String, Object>> countersData = (Map<String, Map<String, Object>>) data.get("counters");
            Map<String, LimitsFact.UsageCounter> counters = countersData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> mapToUsageCounter(entry.getValue())
                ));
            builder.counters(counters);
        }

        if (data.get("snapshotAt") != null) {
            builder.snapshotAt(java.time.Instant.parse(data.get("snapshotAt").toString()));
        }

        return builder.build();
    }

    private LimitsFact.LimitInfo mapToLimitInfo(Map<String, Object> data) {
        return new LimitsFact.LimitInfo(
            (String) data.get("type"),
            (String) data.get("scope"),
            (String) data.get("period"),
            data.get("limit") != null ? new java.math.BigDecimal(data.get("limit").toString()) : null,
            data.get("used") != null ? new java.math.BigDecimal(data.get("used").toString()) : null,
            data.get("remaining") != null ? new java.math.BigDecimal(data.get("remaining").toString()) : null
        );
    }

    private LimitsFact.UsageCounter mapToUsageCounter(Map<String, Object> data) {
        return new LimitsFact.UsageCounter(
            (String) data.get("key"),
            data.get("count") != null ? new java.math.BigDecimal(data.get("count").toString()) : null,
            (String) data.get("period"),
            data.get("lastUpdated") != null ? java.time.Instant.parse(data.get("lastUpdated").toString()) : null,
            data.get("resetAt") != null ? java.time.Instant.parse(data.get("resetAt").toString()) : null
        );
    }
}