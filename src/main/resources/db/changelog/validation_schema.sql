-- ============================================================================
-- DDL Script for Validation Rule Engine - MariaDB
-- Generated from Liquibase changelog files
-- ============================================================================
-- Note: COLLATE utf8mb4_unicode_ci is set for name and description fields only
-- ============================================================================
use promotion_validation;
-- ============================================================================
-- 1. validation_rules - Core validation rules table
-- ============================================================================
CREATE TABLE validation_rules (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    code VARCHAR(100) COMMENT 'Rule code identifier',
    name VARCHAR(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Rule name',
    state VARCHAR(20) NOT NULL COMMENT 'Rule state (draft, published, archived)',
    rule_version BIGINT NOT NULL COMMENT 'Rule version number',
    logic VARCHAR(50) COMMENT 'Root logic for rule evaluation',
    dsl TEXT COMMENT 'DSL snapshot in JSON format',
    bundle_hash VARCHAR(200) COMMENT 'Hash of the compiled rule bundle from validation-engine',
    published_at TIMESTAMP NULL COMMENT 'When rule was published',
    published_by VARCHAR(36) COMMENT 'Who published the rule',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Validation rules with state management and versioning';

CREATE INDEX idx_validation_rules_state_version ON validation_rules (state, rule_version DESC);
CREATE UNIQUE INDEX idx_validation_rules_code ON validation_rules (code);
CREATE INDEX idx_validation_rules_bundle_hash ON validation_rules (bundle_hash);

-- ============================================================================
-- 2. rule_nodes - Rule nodes forming tree structure for complex rules
-- ============================================================================
CREATE TABLE rule_nodes (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    node_id VARCHAR(100) NOT NULL COMMENT 'Node identifier',
    type VARCHAR(20) NOT NULL COMMENT 'Node type (GROUP or COND)',
    group_logic VARCHAR(20) COMMENT 'Logic for GROUP type (ALL, ANY, NONE)',
    children_ids TEXT COMMENT 'JSON array of child node IDs',
    node_order INT COMMENT 'Order of node in parent',
    operator_name VARCHAR(100) COMMENT 'Operator name for COND type',
    params TEXT COMMENT 'JSON parameters for operator',
    reason_code VARCHAR(100) COMMENT 'Reason code for failed validation',
    validation_rule_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to validation_rules',
    parent_id VARCHAR(36) COMMENT 'Self-referencing parent node',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Rule nodes forming tree structure for complex rules';

CREATE INDEX idx_rule_nodes_validation_rule_id ON rule_nodes (validation_rule_id);
CREATE INDEX idx_rule_nodes_parent_id ON rule_nodes (parent_id);
CREATE INDEX idx_rule_nodes_node_order ON rule_nodes (node_order);

-- ============================================================================
-- 3. rule_usage_limits - Usage limits for validation rules
-- ============================================================================
CREATE TABLE rule_usage_limits (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    validation_rule_id VARCHAR(36) NOT NULL UNIQUE COMMENT 'Foreign key to validation_rules',
    per_code_total INT COMMENT 'Total usage limit per code',
    per_customer INT COMMENT 'Usage limit per customer',
    per_day INT COMMENT 'Daily usage limit',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Usage limits for validation rules';

-- ============================================================================
-- 4. operators - Operator registry with JSON schemas
-- ============================================================================
CREATE TABLE operators (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    name VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Operator name',
    operator_version INT NOT NULL COMMENT 'Operator version',
    context VARCHAR(100) COMMENT 'Operator context (order, customer, time, etc.)',
    json_schema TEXT COMMENT 'JSON Schema for operator parameters',
    compiler_id VARCHAR(100) COMMENT 'Compiler template ID',
    status VARCHAR(20) NOT NULL COMMENT 'Operator status (ACTIVE, DEPRECATED)',
    created_at TIMESTAMP NOT NULL COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Operator registry with JSON schemas';

CREATE UNIQUE INDEX idx_operators_name_version ON operators (name, operator_version);
CREATE INDEX idx_operators_context_status ON operators (context, status);

-- ============================================================================
-- 5. temporal_policies - Temporal policies using RRULE format
-- ============================================================================
CREATE TABLE temporal_policies (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    name VARCHAR(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Policy name',
    tz VARCHAR(50) COMMENT 'Timezone',
    start_ts TIMESTAMP NULL COMMENT 'Start timestamp',
    end_ts TIMESTAMP NULL COMMENT 'End timestamp',
    rrule VARCHAR(1000) COMMENT 'RRULE recurrence rule',
    rdate TEXT COMMENT 'RDATE list as JSON',
    exrule VARCHAR(1000) COMMENT 'EXRULE exclusion rule',
    exdate TEXT COMMENT 'EXDATE list as JSON',
    metadata TEXT COMMENT 'Additional metadata as JSON',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Temporal policies using RRULE format';

-- ============================================================================
-- 6. temporal_policy_windows - Time of day windows for temporal policies
-- ============================================================================
CREATE TABLE temporal_policy_windows (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    temporal_policy_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to temporal_policies',
    start_time TIME COMMENT 'Window start time',
    end_time TIME COMMENT 'Window end time',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Time of day windows for temporal policies';

CREATE INDEX idx_policy_windows_policy_id ON temporal_policy_windows (temporal_policy_id);

-- ============================================================================
-- 7. time_exceptions - Time exceptions for temporal policies
-- ============================================================================
CREATE TABLE time_exceptions (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    temporal_policy_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to temporal_policies',
    from_ts TIMESTAMP NULL COMMENT 'Exception start time',
    to_ts TIMESTAMP NULL COMMENT 'Exception end time',
    mode VARCHAR(20) COMMENT 'Exception mode (ALLOW, DENY)',
    reason VARCHAR(500) COMMENT 'Exception reason',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Time exceptions for temporal policies';

CREATE INDEX idx_time_exceptions_policy_id ON time_exceptions (temporal_policy_id);

-- ============================================================================
-- 8. assignments - Alternative assignment structure (main table)
-- ============================================================================
CREATE TABLE assignments (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    entity_type VARCHAR(100) COMMENT 'Entity type',
    entity_id VARCHAR(100) COMMENT 'Entity identifier',
    rule_id VARCHAR(100) COMMENT 'Rule identifier',
    priority INT COMMENT 'Assignment priority',
    active BOOLEAN DEFAULT TRUE COMMENT 'Whether assignment is active',
    temporal_bundle_hash VARCHAR(255) COMMENT 'SHA-256 hash of the compiled temporal DRL bundle (timeframe.drl) for this assignment',
    included_all BOOLEAN DEFAULT FALSE COMMENT 'Whether applicability applies to all products',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Alternative assignment structure';

CREATE INDEX idx_assignments_entity ON assignments (entity_type, entity_id);

-- ============================================================================
-- 9. rule_temporal_links - Links between assignments and temporal policies
-- ============================================================================
CREATE TABLE rule_temporal_links (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    assignment_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to assignments table',
    temporal_policy_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to temporal_policies',
    mode VARCHAR(20) COMMENT 'Link mode (ALLOW, DENY)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Links between assignments and temporal policies (assignment-specific time constraints)';

CREATE INDEX idx_temporal_links_assignment_id ON rule_temporal_links (assignment_id);
CREATE INDEX idx_temporal_links_policy_id ON rule_temporal_links (temporal_policy_id);

-- ============================================================================
-- 10. resources - External resources for selector options
-- ============================================================================
CREATE TABLE resources (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    label VARCHAR(255) COMMENT 'Resource label',
    endpoint VARCHAR(500) COMMENT 'API endpoint URL',
    method VARCHAR(20) COMMENT 'HTTP method',
    auth_method VARCHAR(50) COMMENT 'Authentication method',
    auth_config VARCHAR(500) COMMENT 'Auth configuration reference',
    headers TEXT COMMENT 'HTTP headers as JSON',
    field_key VARCHAR(100) COMMENT 'Key field name',
    field_value VARCHAR(100) COMMENT 'Value field name',
    timeout_seconds INT COMMENT 'Timeout in seconds',
    retry_count INT COMMENT 'Retry count',
    backoff_ms INT COMMENT 'Backoff milliseconds',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='External resources for selector options';

-- ============================================================================
-- 11. operator_resources - Link between operators and resources
-- ============================================================================
CREATE TABLE operator_resources (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    operator_name VARCHAR(100) NOT NULL COMMENT 'Operator name',
    resource_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to resources',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Link between operators and resources';

CREATE INDEX idx_operator_resources_operator ON operator_resources (operator_name);
CREATE INDEX idx_operator_resources_resource ON operator_resources (resource_id);

-- ============================================================================
-- 12. operator_registry - Registry of available operators with contexts
-- ============================================================================
CREATE TABLE operator_registry (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    operator_id VARCHAR(100) NOT NULL UNIQUE COMMENT 'Operator unique identifier',
    context VARCHAR(100) COMMENT 'Operator context',
    json_schema TEXT COMMENT 'JSON Schema for parameters',
    compiler_id VARCHAR(100) COMMENT 'Compiler template ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Registry of available operators with contexts';

CREATE INDEX idx_operator_registry_context ON operator_registry (context);

-- ============================================================================
-- 13. reason_codes - Reason codes for validation failures
-- ============================================================================
CREATE TABLE reason_codes (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT 'Reason code',
    description VARCHAR(500) COLLATE utf8mb4_unicode_ci COMMENT 'Code description',
    message_template VARCHAR(1000) COMMENT 'Message template',
    category VARCHAR(50) COMMENT 'Category for grouping',
    severity VARCHAR(20) COMMENT 'Severity level',
    active BOOLEAN DEFAULT TRUE COMMENT 'Whether code is active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Reason codes for validation failures';

CREATE INDEX idx_reason_codes_category ON reason_codes (category);

-- ============================================================================
-- 14. publish_jobs - Asynchronous publish job tracking
-- ============================================================================
CREATE TABLE publish_jobs (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    validation_rule_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to validation_rules',
    status VARCHAR(50) NOT NULL COMMENT 'Job status (PENDING, RUNNING, COMPLETED, FAILED)',
    initiated_by VARCHAR(36) NOT NULL COMMENT 'User who initiated publish',
    error_message TEXT COMMENT 'Error message if failed',
    started_at TIMESTAMP NULL COMMENT 'When job started',
    completed_at TIMESTAMP NULL COMMENT 'When job completed',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Asynchronous publish job tracking';

CREATE INDEX idx_publish_jobs_status ON publish_jobs (status);
CREATE INDEX idx_publish_jobs_rule_id ON publish_jobs (validation_rule_id);

-- ============================================================================
-- 15. rule_versions - Historical versions of rules
-- ============================================================================
CREATE TABLE rule_versions (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    validation_rule_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to validation_rules',
    version_number BIGINT NOT NULL COMMENT 'Version number',
    snapshot TEXT NOT NULL COMMENT 'Complete rule snapshot as JSON',
    change_summary VARCHAR(1000) COMMENT 'Summary of changes',
    created_by VARCHAR(36) COMMENT 'Who created this version',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Historical versions of rules';

CREATE UNIQUE INDEX idx_rule_versions_rule_version ON rule_versions (validation_rule_id, version_number DESC);

-- ============================================================================
-- 16. rule_time_frames - Time frames for rules
-- ============================================================================
CREATE TABLE rule_time_frames (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    validation_rule_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to validation_rules',
    time_frame_id VARCHAR(100) NOT NULL COMMENT 'ID of the time frame',
    mode VARCHAR(20) NOT NULL DEFAULT 'ALLOW' COMMENT 'Time frame mode (ALLOW or DENY for blackout)',
    start_time TIMESTAMP NULL COMMENT 'Frame start time',
    end_time TIMESTAMP NULL COMMENT 'Frame end time',
    timezone VARCHAR(50) COMMENT 'Timezone',
    recurrence_rule VARCHAR(1000) COMMENT 'Recurrence rule',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Time frames for rules';

CREATE INDEX idx_rule_time_frames_rule_id ON rule_time_frames (validation_rule_id);
CREATE INDEX idx_rule_time_frames_frame_id ON rule_time_frames (time_frame_id);

-- ============================================================================
-- 17. time_links - Links between entities and time frames
-- ============================================================================
CREATE TABLE time_links (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    entity_type VARCHAR(100) COMMENT 'Entity type',
    entity_id VARCHAR(100) COMMENT 'Entity identifier',
    time_frame_id VARCHAR(100) COMMENT 'Time frame identifier',
    link_mode VARCHAR(20) COMMENT 'Link mode (ALLOW, DENY)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    created_by VARCHAR(36) COMMENT 'Creator',
    updated_by VARCHAR(36) COMMENT 'Last updater',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Links between entities and time frames';

CREATE INDEX idx_time_links_entity ON time_links (entity_type, entity_id);

-- ============================================================================
-- 18. rule_configuration - Configuration key-value pairs for rules
-- ============================================================================
CREATE TABLE rule_configuration (
    rule_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to validation_rules',
    config_key VARCHAR(255) NOT NULL COMMENT 'Configuration key',
    config_value TEXT COMMENT 'Configuration value',
    PRIMARY KEY (rule_id, config_key)
) ENGINE=InnoDB COMMENT='Configuration key-value pairs for rules (from RuleJpaEntity)';

-- ============================================================================
-- 19. rule_target_segments - Target segments for rules
-- ============================================================================
CREATE TABLE rule_target_segments (
    rule_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to validation_rules',
    segment VARCHAR(255) NOT NULL COMMENT 'Segment identifier',
    PRIMARY KEY (rule_id, segment)
) ENGINE=InnoDB COMMENT='Target segments for rules';

-- ============================================================================
-- 20. outbox_events - Transactional outbox pattern with Spring Batch
-- ============================================================================
CREATE TABLE outbox_events (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    tenant_id VARCHAR(50) COMMENT 'Tenant ID',
    aggregate_type VARCHAR(100) NOT NULL COMMENT 'Aggregate type',
    aggregate_id VARCHAR(100) NOT NULL COMMENT 'Aggregate identifier',
    event_type VARCHAR(100) NOT NULL COMMENT 'Event type',
    payload TEXT COMMENT 'Event payload',
    destination VARCHAR(500) COMMENT 'Destination topic/queue',
    metadata TEXT COMMENT 'Additional metadata',
    status VARCHAR(20) NOT NULL COMMENT 'Event status',
    attempts INT NOT NULL DEFAULT 0 COMMENT 'Number of delivery attempts',
    max_attempts INT NOT NULL DEFAULT 3 COMMENT 'Maximum delivery attempts',
    last_error TEXT COMMENT 'Last error message',
    created_at TIMESTAMP NOT NULL COMMENT 'Creation timestamp',
    last_attempt_at TIMESTAMP NULL COMMENT 'Last delivery attempt timestamp',
    published_at TIMESTAMP NULL COMMENT 'When event was published',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Transactional outbox events for transactional outbox pattern with Spring Batch';

CREATE INDEX idx_outbox_status ON outbox_events (status);
CREATE INDEX idx_outbox_created_at ON outbox_events (created_at);
CREATE INDEX idx_outbox_tenant_id ON outbox_events (tenant_id);
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_event_type ON outbox_events (event_type);
CREATE INDEX idx_outbox_status_attempts ON outbox_events (status, attempts);

-- ============================================================================
-- 21. assignment_applicability_rules - Applicability rules for assignments
-- ============================================================================
CREATE TABLE assignment_applicability_rules (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    assignment_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to assignments table',
    rule_type VARCHAR(20) NOT NULL COMMENT 'Rule type (INCLUDED or EXCLUDED)',
    object_type VARCHAR(20) NOT NULL COMMENT 'Object type (COLLECTION, PRODUCT, SKU)',
    object_id VARCHAR(100) NOT NULL COMMENT 'Object identifier (collection/product/SKU ID)',
    effect VARCHAR(30) DEFAULT 'APPLY_TO_EVERY' COMMENT 'Effect type (APPLY_TO_EVERY, APPLY_TO_CHEAPEST, APPLY_TO_MOST_EXPENSIVE)',
    target VARCHAR(20) DEFAULT 'ITEM' COMMENT 'Target type (ITEM, ORDER, CUSTOMER)',
    skip_initially INT DEFAULT 0 COMMENT 'Number of items to skip initially',
    repeat_count INT DEFAULT 1 COMMENT 'Number of times to repeat application',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Applicability rules for assignments (included/excluded products, collections, SKUs)';

CREATE INDEX idx_applicability_rules_assignment_id ON assignment_applicability_rules (assignment_id);
CREATE INDEX idx_applicability_rules_type ON assignment_applicability_rules (rule_type);
CREATE INDEX idx_applicability_rules_object ON assignment_applicability_rules (object_type, object_id);

-- ============================================================================
-- 22. validation_rules_assignment_deleted - Soft delete audit trail
-- ============================================================================
CREATE TABLE validation_rules_assignment_deleted (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID for deleted record',
    original_assignment_id VARCHAR(36) NOT NULL COMMENT 'Original assignment ID from assignments table',
    entity_type VARCHAR(100) COMMENT 'Entity type (e.g., CAMPAIGN, VOUCHER)',
    entity_id VARCHAR(100) COMMENT 'Entity identifier (object_id from SRS)',
    rule_id VARCHAR(100) COMMENT 'Rule identifier (validation_rule_id from SRS)',
    priority INT COMMENT 'Assignment priority at time of deletion',
    was_active BOOLEAN COMMENT 'Whether assignment was active before deletion',
    temporal_bundle_hash VARCHAR(255) COMMENT 'Temporal bundle hash at time of deletion',
    included_all BOOLEAN COMMENT 'Was included_all flag set',
    original_created_at TIMESTAMP NULL COMMENT 'Original creation timestamp',
    original_updated_at TIMESTAMP NULL COMMENT 'Original last update timestamp',
    deleted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when assignment was deleted (DD/MM/YYYY HH:MM:SS as per SRS)',
    deleted_by VARCHAR(100) COMMENT 'User who deleted this assignment',
    version INT COMMENT 'Version number (incremented from original on delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Stores soft-deleted assignment records for audit purposes (SRS PRM_KBNV_API_VALD008)';

CREATE INDEX idx_assignment_deleted_original_id ON validation_rules_assignment_deleted (original_assignment_id);
CREATE INDEX idx_assignment_deleted_rule ON validation_rules_assignment_deleted (rule_id);
CREATE INDEX idx_assignment_deleted_entity ON validation_rules_assignment_deleted (entity_type, entity_id);
CREATE INDEX idx_assignment_deleted_at ON validation_rules_assignment_deleted (deleted_at);

-- ============================================================================
-- 23. validation_rule_snapshots - Snapshots for saga compensation
-- ============================================================================
CREATE TABLE validation_rule_snapshots (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    validation_rule_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to validation_rules',
    version BIGINT NOT NULL COMMENT 'Version number',
    snapshot_data LONGTEXT NOT NULL COMMENT 'JSON containing full aggregate state (rule + nodes + limits + timeframes)',
    saga_id VARCHAR(100) COMMENT 'Saga ID that triggered the snapshot',
    correlation_id VARCHAR(100) COMMENT 'Correlation ID for tracing',
    snapshot_reason VARCHAR(50) NOT NULL COMMENT 'Reason for snapshot - BEFORE_UPDATE, BEFORE_DELETE, MANUAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    created_by VARCHAR(50) COMMENT 'Creator',
    expires_at TIMESTAMP NULL COMMENT 'Optional expiration time for auto-cleanup',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Validation rule snapshots for saga compensation';

CREATE UNIQUE INDEX idx_snapshot_rule_version ON validation_rule_snapshots (validation_rule_id, version);
CREATE INDEX idx_snapshot_saga_id ON validation_rule_snapshots (saga_id);
CREATE INDEX idx_snapshot_created_at ON validation_rule_snapshots (created_at);

-- ============================================================================
-- 24. assignment_snapshots - Assignment snapshots for saga compensation
-- ============================================================================
CREATE TABLE assignment_snapshots (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key UUID',
    assignment_id VARCHAR(36) NOT NULL COMMENT 'Foreign key to assignments',
    snapshot_version BIGINT NOT NULL COMMENT 'Incremental version for this assignment snapshots',
    snapshot_data LONGTEXT NOT NULL COMMENT 'JSON containing assignment + applicability rules + temporal links',
    saga_id VARCHAR(100) COMMENT 'Saga ID that triggered the snapshot',
    correlation_id VARCHAR(100) COMMENT 'Correlation ID for tracing',
    snapshot_reason VARCHAR(50) NOT NULL COMMENT 'Reason for snapshot - BEFORE_UPDATE, BEFORE_DELETE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    created_by VARCHAR(50) COMMENT 'Creator',
    expires_at TIMESTAMP NULL COMMENT 'Optional expiration time for auto-cleanup',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Assignment snapshots for saga compensation';

CREATE UNIQUE INDEX idx_assignment_snapshot_version ON assignment_snapshots (assignment_id, snapshot_version);
CREATE INDEX idx_assignment_snapshot_saga_id ON assignment_snapshots (saga_id);
CREATE INDEX idx_assignment_snapshot_created_at ON assignment_snapshots (created_at);
