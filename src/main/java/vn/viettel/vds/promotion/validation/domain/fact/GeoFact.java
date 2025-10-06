package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeoFact(
        String country,
        String region,
        String city,
        String postalCode,
        Double latitude,
        Double longitude,
        String timezone,
        String isp,
        Map<String, Object> geoData
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String country;
        private String region;
        private String city;
        private String postalCode;
        private Double latitude;
        private Double longitude;
        private String timezone;
        private String isp;
        private Map<String, Object> geoData;

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder isp(String isp) {
            this.isp = isp;
            return this;
        }

        public Builder geoData(Map<String, Object> geoData) {
            this.geoData = geoData;
            return this;
        }

        public GeoFact build() {
            return new GeoFact(country, region, city, postalCode, latitude, longitude, timezone, isp, geoData);
        }
    }
}