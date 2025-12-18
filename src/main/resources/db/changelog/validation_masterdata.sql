-- ============================================================================
-- INSERT Sample Data for Validation Rule Engine - MariaDB
-- Generated from Liquibase changelog files
-- ============================================================================
use promotion_validation;
-- ============================================================================
-- 1. operators - Sample operators
-- ============================================================================

INSERT INTO operators (id, name, operator_version, context, json_schema, compiler_id, status, created_at, updated_at)
VALUES ('01932b6f-0001-7000-8000-000000000001', 'order.total.gte', 1, 'order',
        '{"type":"object","properties":{"amount":{"type":"number"},"currency":{"type":"string"}},"required":["amount"]}',
        'tpl_order_total_gte_v1', 'ACTIVE', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),

       ('01932b6f-0001-7000-8000-000000000002', 'customer.in_segment', 1, 'customer',
        '{"type":"object","properties":{"segments":{"type":"array","items":{"type":"string"}}},"required":["segments"]}',
        'tpl_customer_segment_v1', 'ACTIVE', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),

       ('01932b6f-0001-7000-8000-000000000003', 'time.window.active', 1, 'time',
        '{"type":"object","properties":{"startTime":{"type":"string","pattern":"^([0-1][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$"},"endTime":{"type":"string","pattern":"^([0-1][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$"},"timezone":{"type":"string"},"daysOfWeek":{"type":"array","items":{"type":"string","enum":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"]}},"spansMidnight":{"type":"boolean"}},"required":["startTime","endTime"]}',
        'tpl_time_window_v1', 'ACTIVE', '2025-01-01 00:00:00', '2025-01-01 00:00:00');

-- ============================================================================
-- 2. temporal_policies - Sample temporal policies
-- Note: RRULE values are already fixed (no BYHOUR) per changeset 006
-- ============================================================================
INSERT INTO temporal_policies (id, name, tz, rrule, rdate, exdate, metadata)
VALUES ('01932b6f-0002-7000-8000-000000000001', 'Weekend Mornings', 'Asia/Bangkok',
        'FREQ=WEEKLY;BYDAY=SA,SU', '[]', '["2025-10-20T09:00:00+07:00"]', '{}'),

       ('01932b6f-0002-7000-8000-000000000002', 'Business Hours', 'Asia/Bangkok',
        'FREQ=DAILY', '[]', '[]', '{}');

-- ============================================================================
-- 3. temporal_policy_windows - Sample policy windows
-- Note: end_time for Weekend Mornings is 12:00:00 per changeset 006
-- ============================================================================
INSERT INTO temporal_policy_windows (id, temporal_policy_id, start_time, end_time)
VALUES ('01932b6f-0003-7000-8000-000000000001', '01932b6f-0002-7000-8000-000000000001',
        '09:00:00', '12:00:00'),

       ('01932b6f-0003-7000-8000-000000000002', '01932b6f-0002-7000-8000-000000000002',
        '09:00:00', '17:00:00');

-- ============================================================================
-- 4. reason_codes - Sample reason codes
-- ============================================================================
INSERT INTO reason_codes (id, code, description, message_template, category, severity, active)
VALUES ('01932b6f-0004-7000-8000-000000000001', 'ORDER_TOTAL_MIN',
        'Order total does not meet minimum requirement',
        'Order total must be at least {amount} {currency}',
        'ORDER_VALIDATION', 'ERROR', TRUE),

       ('01932b6f-0004-7000-8000-000000000002', 'AUDIENCE_SEGMENT',
        'Customer not in target segment',
        'This promotion is only available for {segments} customers',
        'CUSTOMER_VALIDATION', 'ERROR', TRUE),

       ('01932b6f-0004-7000-8000-000000000003', 'TIME_WINDOW',
        'Outside valid time window',
        'Promotion is not active at this time',
        'TIME_VALIDATION', 'ERROR', TRUE),

       ('01932b6f-0004-7000-8000-000000000004', 'META_MATCH',
        'Metadata does not match required criteria',
        '{field} must be {value}',
        'METADATA_VALIDATION', 'ERROR', TRUE);

-- ============================================================================
-- 5. validation_rules - Sample validation rules
-- ============================================================================
INSERT INTO validation_rules (id, code, name, state, rule_version, logic, dsl, published_at, published_by, created_at,
                              updated_at, created_by, updated_by, version, bundle_hash)
VALUES ('01932b6f-0005-7000-8000-000000000001', 'BETTING_FEE_REFUND',
        'Tập segment 100% Betting Fee Refund - Weekend Comeback Deal', 'DRAFT', 1, 'ALL',
        '{}', null, null, '2025-11-13 09:22:28', '2025-11-13 09:22:28', 'admin', 'admin', 0, null);


INSERT INTO validation_rules (id, code, name, state, rule_version, logic, dsl, published_at, published_by, created_at,
                              updated_at, created_by, updated_by, version, bundle_hash)
VALUES ('01932b6f-0005-7000-8000-000000000002', 'FTTH_FEE_REFUND', 'Tập segment  575012 - 100 % FTTH Fee Cashback',
        'DRAFT', 1, 'ALL',
        '{}', null, null, '2025-11-13 09:22:28', '2025-11-13 09:22:28', 'admin', 'admin', 0, null);


-- ============================================================================
-- 6. rule_nodes - Sample rule nodes
-- Note: node_order values include fixes from changeset 011
-- ============================================================================

INSERT INTO rule_nodes (id, node_id, type, group_logic, children_ids, node_order, operator_name, params, reason_code,
                        validation_rule_id, parent_id, created_at, updated_at, created_by, updated_by, version)
VALUES ('01932b6f-0006-7000-8000-000000000010', 'n1', 'GROUP', 'ALL', '["n2"]', 0, null, null, null,
        '01932b6f-0005-7000-8000-000000000001', null, '2025-11-13 09:22:29', '2025-11-13 09:22:29', null, null, 0);
INSERT INTO rule_nodes (id, node_id, type, group_logic, children_ids, node_order, operator_name, params, reason_code,
                        validation_rule_id, parent_id, created_at, updated_at, created_by, updated_by, version)
VALUES ('01932b6f-0006-7000-8000-000000000011', 'n2', 'COND', null, null, 0, 'customer.in_segment',
        '{"segments":["BETTINGREFUND"]}', 'AUDIENCE_SEGMENT', '01932b6f-0005-7000-8000-000000000001',
        '01932b6f-0006-7000-8000-000000000010', '2025-11-13 09:22:29', '2025-11-13 09:22:29', null, null, 0);


INSERT INTO rule_nodes (id, node_id, type, group_logic, children_ids, node_order, operator_name, params, reason_code,
                        validation_rule_id, parent_id, created_at, updated_at, created_by, updated_by, version)
VALUES ('01932b6f-0006-7000-8000-000000000012', 'n1', 'GROUP', 'ALL', '["n2"]', 0, null, null, null,
        '01932b6f-0005-7000-8000-000000000002', null, '2025-11-13 09:22:29', '2025-11-13 09:22:29', null, null, 0);

INSERT INTO rule_nodes (id, node_id, type, group_logic, children_ids, node_order, operator_name, params, reason_code,
                        validation_rule_id, parent_id, created_at, updated_at, created_by, updated_by, version)
VALUES ('01932b6f-0006-7000-8000-000000000013', 'n2', 'COND', null, null, 0, 'customer.in_segment',
        '{"segments":["FTTHCASHBACK"]}', 'AUDIENCE_SEGMENT', '01932b6f-0005-7000-8000-000000000002',
        '01932b6f-0006-7000-8000-000000000012', '2025-11-13 09:22:29', '2025-11-13 09:22:29', null, null, 0);


-- ============================================================================
-- 7. rule_usage_limits - Sample usage limits
-- ============================================================================
INSERT INTO rule_usage_limits (id, validation_rule_id, per_code_total, per_customer, per_day)
VALUES ('01932b6f-0007-7000-8000-000000000001', '01932b6f-0005-7000-8000-000000000001',
        1000, 3, 200);

-- ============================================================================
-- 8. assignments - Sample assignments
-- Note: rule_assignments table was dropped in changeset 008
-- Using assignments table instead
-- ============================================================================
INSERT INTO assignments (id, entity_type, entity_id, rule_id, priority, active)
VALUES ('01932b6f-0009-7000-8000-000000000001', 'voucher', 'SAVE20',
        '01932b6f-0005-7000-8000-000000000001', 1, TRUE);

-- ============================================================================
-- 9. resources - Sample resources
-- ============================================================================
INSERT INTO resources (id, label, endpoint, method, auth_method, auth_config, headers,
                       field_key, field_value, timeout_seconds, retry_count, backoff_ms)
VALUES ('01932b6f-000a-7000-8000-000000000001', 'CDP Segments',
        'https://cdp.example.com/api/segments', 'GET', 'bearer', 'secret/cdp-token',
        '{"Accept":"application/json"}', 'id', 'name', 5, 2, 200);

-- ============================================================================
-- 10. operator_registry - Sample operator registry
-- ============================================================================
INSERT INTO operator_registry (id, operator_id, context, json_schema, compiler_id)
VALUES ('01932b6f-000b-7000-8000-000000000001', 'order.total.gte', 'order',
        '{"type":"object","properties":{"amount":{"type":"number"},"currency":{"type":"string"}},"required":["amount"]}',
        'tpl_order_total_gte_v1'),

       ('01932b6f-000b-7000-8000-000000000002', 'customer.in_segment', 'customer',
        '{"type":"object","properties":{"segments":{"type":"array","items":{"type":"string"}}},"required":["segments"]}',
        'tpl_customer_segment_v1');
