package vn.viettel.vds.promotion.validation.application.fact.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.GeoFact;

import java.util.Map;

@Component
public class GeoFactMapper {

    public GeoFact map(Object data) {
        if (data == null) {
            return null;
        }

        if (data instanceof GeoFact) {
            return (GeoFact) data;
        }

        if (data instanceof Map) {
            // Handle case where data comes as Map (from cache or other sources)
            Map<String, Object> mapData = (Map<String, Object>) data;
            return mapFromData(mapData);
        }

        return null;
    }

    private GeoFact mapFromData(Map<String, Object> data) {
        GeoFact.Builder builder = GeoFact.builder();

        if (data.get("country") != null) {
            builder.country((String) data.get("country"));
        }
        if (data.get("region") != null) {
            builder.region((String) data.get("region"));
        }
        if (data.get("city") != null) {
            builder.city((String) data.get("city"));
        }
        if (data.get("postalCode") != null) {
            builder.postalCode((String) data.get("postalCode"));
        }
        if (data.get("latitude") != null) {
            builder.latitude(Double.parseDouble(data.get("latitude").toString()));
        }
        if (data.get("longitude") != null) {
            builder.longitude(Double.parseDouble(data.get("longitude").toString()));
        }
        if (data.get("timezone") != null) {
            builder.timezone((String) data.get("timezone"));
        }
        if (data.get("isp") != null) {
            builder.isp((String) data.get("isp"));
        }
        if (data.get("geoData") instanceof Map) {
            builder.geoData((Map<String, Object>) data.get("geoData"));
        }

        return builder.build();
    }
}