package vn.viettel.vds.promotion.validation.domain.fact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetadataFact(
        String requestId,
        String sessionId,
        String userAgent,
        String ipAddress,
        String deviceId,
        String platform,
        String version,
        String channel,
        String referrer,
        String timezone,
        Instant requestTime,
        Map<String, Object> headers,
        Map<String, Object> context,
        Map<String, Object> experiments
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String requestId;
        private String sessionId;
        private String userAgent;
        private String ipAddress;
        private String deviceId;
        private String platform;
        private String version;
        private String channel;
        private String referrer;
        private String timezone;
        private Instant requestTime;
        private Map<String, Object> headers;
        private Map<String, Object> context;
        private Map<String, Object> experiments;

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder platform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder referrer(String referrer) {
            this.referrer = referrer;
            return this;
        }

        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder requestTime(Instant requestTime) {
            this.requestTime = requestTime;
            return this;
        }

        public Builder headers(Map<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public Builder experiments(Map<String, Object> experiments) {
            this.experiments = experiments;
            return this;
        }

        public MetadataFact build() {
            return new MetadataFact(
                    requestId, sessionId, userAgent, ipAddress, deviceId, platform,
                    version, channel, referrer, timezone, requestTime, headers, context, experiments
            );
        }
    }
}