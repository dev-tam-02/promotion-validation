package vn.viettel.vds.promotion.validation.application.fact.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.CandidateFact;

import java.util.Map;

@Component
public class CandidateFactMapper {

    public CandidateFact map(Object data) {
        if (data == null) {
            return null;
        }

        if (data instanceof CandidateFact candidateFact) {
            return candidateFact;
        }

        if (data instanceof Map) {
            // Handle case where data comes as Map (from cache or other sources)
            Map<String, Object> mapData = (Map<String, Object>) data;
            return mapFromData(mapData);
        }

        return null;
    }

    private CandidateFact mapFromData(Map<String, Object> data) {
        CandidateFact.Builder builder = CandidateFact.builder();

        if (data.get("type") != null) {
            builder.type((String) data.get("type"));
        }
        if (data.get("key") != null) {
            builder.key((String) data.get("key"));
        }
        if (data.get("campaignId") != null) {
            builder.campaignId((String) data.get("campaignId"));
        }
        if (data.get("promotionId") != null) {
            builder.promotionId((String) data.get("promotionId"));
        }
        if (data.get("name") != null) {
            builder.name((String) data.get("name"));
        }
        if (data.get("description") != null) {
            builder.description((String) data.get("description"));
        }
        if (data.get("status") != null) {
            builder.status((String) data.get("status"));
        }
        if (data.get("startDate") != null) {
            builder.startDate(java.time.Instant.parse(data.get("startDate").toString()));
        }
        if (data.get("endDate") != null) {
            builder.endDate(java.time.Instant.parse(data.get("endDate").toString()));
        }
        if (data.get("configuration") != null) {
            builder.configuration((Map<String, Object>) data.get("configuration"));
        }
        if (data.get("constraints") != null) {
            builder.constraints((Map<String, Object>) data.get("constraints"));
        }

        return builder.build();
    }
}