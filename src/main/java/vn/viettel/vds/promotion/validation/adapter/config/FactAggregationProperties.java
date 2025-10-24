package vn.viettel.vds.promotion.validation.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fact.aggregation")
public class FactAggregationProperties {

    private Cache cache = new Cache();
    private Resolvers resolvers = new Resolvers();
    private Policy policy = new Policy();

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Resolvers getResolvers() {
        return resolvers;
    }

    public void setResolvers(Resolvers resolvers) {
        this.resolvers = resolvers;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public static class Cache {
        private static final int DEFAULT_TTL_SECONDS = 60;
        private static final int DEFAULT_MAX_SIZE = 1000;

        private int defaultTtlSeconds = DEFAULT_TTL_SECONDS;
        private int maxSize = DEFAULT_MAX_SIZE;
        private boolean enabled = true;

        public int getDefaultTtlSeconds() {
            return defaultTtlSeconds;
        }

        public void setDefaultTtlSeconds(int defaultTtlSeconds) {
            this.defaultTtlSeconds = defaultTtlSeconds;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Resolvers {
        private ServiceConfig customer = new ServiceConfig("http://customer-service");
        private ServiceConfig order = new ServiceConfig("http://order-service");
        private ServiceConfig catalog = new ServiceConfig("http://catalog-service");
        private ServiceConfig redemption = new ServiceConfig("http://redemption-service");
        private ServiceConfig segments = new ServiceConfig("http://segments-service");
        private ServiceConfig geo = new ServiceConfig("http://geo-service");

        public ServiceConfig getCustomer() {
            return customer;
        }

        public void setCustomer(ServiceConfig customer) {
            this.customer = customer;
        }

        public ServiceConfig getOrder() {
            return order;
        }

        public void setOrder(ServiceConfig order) {
            this.order = order;
        }

        public ServiceConfig getCatalog() {
            return catalog;
        }

        public void setCatalog(ServiceConfig catalog) {
            this.catalog = catalog;
        }

        public ServiceConfig getRedemption() {
            return redemption;
        }

        public void setRedemption(ServiceConfig redemption) {
            this.redemption = redemption;
        }

        public ServiceConfig getSegments() {
            return segments;
        }

        public void setSegments(ServiceConfig segments) {
            this.segments = segments;
        }

        public ServiceConfig getGeo() {
            return geo;
        }

        public void setGeo(ServiceConfig geo) {
            this.geo = geo;
        }
    }

    public static class ServiceConfig {
        private static final int DEFAULT_TIMEOUT_MS = 5000;
        private static final int DEFAULT_PRIORITY = 100;
        private String baseUrl;
        private int timeoutMs = DEFAULT_TIMEOUT_MS;
        private boolean enabled = true;
        private int priority = DEFAULT_PRIORITY;

        public ServiceConfig() {
        }

        public ServiceConfig(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    public static class Policy {
        private static final long DEFAULT_TIMEOUT_MS = 10000L;

        private long defaultTimeoutMs = DEFAULT_TIMEOUT_MS;
        private boolean allowPartialResults = true;
        private boolean enableFallback = true;

        public long getDefaultTimeoutMs() {
            return defaultTimeoutMs;
        }

        public void setDefaultTimeoutMs(long defaultTimeoutMs) {
            this.defaultTimeoutMs = defaultTimeoutMs;
        }

        public boolean isAllowPartialResults() {
            return allowPartialResults;
        }

        public void setAllowPartialResults(boolean allowPartialResults) {
            this.allowPartialResults = allowPartialResults;
        }

        public boolean isEnableFallback() {
            return enableFallback;
        }

        public void setEnableFallback(boolean enableFallback) {
            this.enableFallback = enableFallback;
        }
    }
}