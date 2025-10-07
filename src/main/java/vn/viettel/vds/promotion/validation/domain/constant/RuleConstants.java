package vn.viettel.vds.promotion.validation.domain.constant;

/**
 * Constants for validation rules and rule processing.
 *
 * Centralized location for all magic strings and default values used in rule processing.
 */
public final class RuleConstants {

    private RuleConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * Subject types for rule assignments
     */
    public static final class SubjectType {
        public static final String CAMPAIGN = "campaign";
        public static final String PRODUCT = "product";
        public static final String CATEGORY = "category";
        public static final String CUSTOMER = "customer";
        public static final String SEGMENT = "segment";

        private SubjectType() {}
    }

    /**
     * Node types in rule tree
     */
    public static final class NodeType {
        public static final String GROUP = "GROUP";
        public static final String CONDITION = "COND";

        private NodeType() {}
    }

    /**
     * Common operators used in rule evaluation
     */
    public static final class Operator {
        // Comparison operators
        public static final String EQUALS = "equals";
        public static final String NOT_EQUALS = "not_equals";
        public static final String GREATER_THAN = "greater_than";
        public static final String GREATER_THAN_OR_EQUAL = "greater_than_or_equal";
        public static final String LESS_THAN = "less_than";
        public static final String LESS_THAN_OR_EQUAL = "less_than_or_equal";

        // Collection operators
        public static final String IN = "in";
        public static final String NOT_IN = "not_in";
        public static final String CONTAINS = "contains";
        public static final String NOT_CONTAINS = "not_contains";

        // Null check operators
        public static final String IS_NULL = "is_null";
        public static final String IS_NOT_NULL = "is_not_null";

        // String operators
        public static final String STARTS_WITH = "starts_with";
        public static final String ENDS_WITH = "ends_with";
        public static final String MATCHES = "matches";

        // Special operators
        public static final String PRODUCT_APPLICABILITY_IN = "product.applicability.in";
        public static final String SEGMENT_IN = "segment.in";

        private Operator() {}
    }

    /**
     * Rule types
     */
    public static final class RuleType {
        public static final String REQUIRED = "REQUIRED";
        public static final String FORMAT = "FORMAT";
        public static final String RANGE = "RANGE";
        public static final String PATTERN = "PATTERN";
        public static final String CUSTOM = "CUSTOM";
        public static final String BUSINESS_RULE = "BUSINESS_RULE";
        public static final String BLACKLIST = "BLACKLIST";
        public static final String ELIGIBILITY = "ELIGIBILITY";
        public static final String VALIDATION = "VALIDATION";

        private RuleType() {}
    }

    /**
     * Rule states
     */
    public static final class RuleState {
        public static final String DRAFT = "DRAFT";
        public static final String PUBLISHED = "PUBLISHED";
        public static final String ARCHIVED = "ARCHIVED";
        public static final String DEPRECATED = "DEPRECATED";

        private RuleState() {}
    }

    /**
     * Logic types for combining conditions
     */
    public static final class LogicType {
        public static final String ALL = "ALL";   // AND
        public static final String ANY = "ANY";   // OR
        public static final String NONE = "NONE"; // NOT
        public static final String XOR = "XOR";   // Exactly one

        private LogicType() {}
    }

    /**
     * Default values
     */
    public static final class Defaults {
        public static final int DEFAULT_PRIORITY = 100;
        public static final int DEFAULT_TRAFFIC_PERCENT = 100;
        public static final int DEFAULT_VERSION = 1;
        public static final boolean DEFAULT_ACTIVE = true;
        public static final String DEFAULT_LOGIC = LogicType.ALL;
        public static final String DEFAULT_SUBJECT_TYPE = SubjectType.CAMPAIGN;

        private Defaults() {}
    }

    /**
     * Timeframe modes
     */
    public static final class TimeFrameMode {
        public static final String ALWAYS = "ALWAYS";
        public static final String SCHEDULED = "SCHEDULED";
        public static final String RECURRING = "RECURRING";
        public static final String DATE_RANGE = "DATE_RANGE";

        private TimeFrameMode() {}
    }

    /**
     * Sticky key strategies for traffic splitting
     */
    public static final class StickyKeyStrategy {
        public static final String CUSTOMER_ID = "CUSTOMER_ID";
        public static final String SESSION_ID = "SESSION_ID";
        public static final String DEVICE_ID = "DEVICE_ID";
        public static final String IP_ADDRESS = "IP_ADDRESS";

        private StickyKeyStrategy() {}
    }

    /**
     * Field names commonly used in validation
     */
    public static final class Field {
        // Customer fields
        public static final String CUSTOMER_ID = "customer.id";
        public static final String CUSTOMER_SEGMENT = "customer.segment";
        public static final String CUSTOMER_TYPE = "customer.type";
        public static final String CUSTOMER_STATUS = "customer.status";

        // Product fields
        public static final String PRODUCT_ID = "product.id";
        public static final String PRODUCT_CATEGORY = "product.category";
        public static final String PRODUCT_SKU = "product.sku";
        public static final String PRODUCT_APPLICABILITY = "product.applicability";

        // Order fields
        public static final String ORDER_ID = "order.id";
        public static final String ORDER_VALUE = "order.value";
        public static final String ORDER_QUANTITY = "order.quantity";
        public static final String ORDER_DATE = "order.date";

        // Campaign fields
        public static final String CAMPAIGN_ID = "campaign.id";
        public static final String CAMPAIGN_CODE = "campaign.code";

        private Field() {}
    }

    /**
     * Error codes (legacy - prefer ErrorCode enum)
     * @deprecated Use {@link vn.viettel.vds.promotion.validation.domain.common.ErrorCode} instead
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    public static final class ErrorCodes {
        public static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";
        public static final String MISSING_ASSIGN_RULE = "MISSING_ASSIGN_RULE";
        public static final String MISSING_CAMPAIGN_ID = "MISSING_CAMPAIGN_ID";
        public static final String RULE_NOT_FOUND = "RULE_NOT_FOUND";
        public static final String RULE_MISSING_APPLICABILITY_NODE = "RULE_MISSING_APPLICABILITY_NODE";
        public static final String PROCESSING_ERROR = "PROCESSING_ERROR";

        private ErrorCodes() {}
    }

    /**
     * Configuration keys
     */
    public static final class ConfigKey {
        public static final String VALIDATION_ENABLED = "validation.enabled";
        public static final String CACHE_ENABLED = "validation.cache.enabled";
        public static final String CACHE_TTL = "validation.cache.ttl";
        public static final String MAX_RULE_DEPTH = "validation.rule.max-depth";
        public static final String MAX_NODE_COUNT = "validation.rule.max-nodes";
        public static final String DEPLOYMENT_ENABLED = "validation.deployment.enabled";

        private ConfigKey() {}
    }

    /**
     * Metric names for monitoring
     */
    public static final class Metrics {
        public static final String RULE_EVALUATION_TIME = "validation.rule.evaluation.time";
        public static final String RULE_EVALUATION_COUNT = "validation.rule.evaluation.count";
        public static final String RULE_EVALUATION_ERROR = "validation.rule.evaluation.error";
        public static final String COMMAND_PROCESSING_TIME = "validation.command.processing.time";
        public static final String DEPLOYMENT_SUCCESS = "validation.deployment.success";
        public static final String DEPLOYMENT_FAILURE = "validation.deployment.failure";

        private Metrics() {}
    }
}
