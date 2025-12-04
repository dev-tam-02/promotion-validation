# Hướng Dẫn Khai Báo và Sử Dụng Validation Rules

## Mục Lục
1. [Tổng Quan](#1-tổng-quan)
2. [Cấu Trúc Database](#2-cấu-trúc-database)
3. [Vòng Đời của Rule](#3-vòng-đời-của-rule)
4. [Quy Trình Khai Báo Rule Mới (Database)](#4-quy-trình-khai-báo-rule-mới-database)
5. [Quy Trình Validate Rule](#5-quy-trình-validate-rule)
6. [Quy Trình Activate Rule](#6-quy-trình-activate-rule)
7. [Quy Trình Publish Rule](#7-quy-trình-publish-rule)
8. [Gán Rule cho Object (Assignment)](#8-gán-rule-cho-object-assignment)
9. [API Endpoints](#9-api-endpoints)
10. [Ví Dụ Thực Tế](#10-ví-dụ-thực-tế)

---

## 1. Tổng Quan

Module **validation** quản lý các quy tắc xác thực (validation rules) cho nền tảng khuyến mại. Mỗi rule có thể bao gồm nhiều điều kiện (conditions) được tổ chức theo cấu trúc cây (tree structure).

### Kiến Trúc Chính
- **Database**: MariaDB với Liquibase migrations
- **Validation Engine**: Tích hợp với Drools-based validation-engine để compile và execute rules
- **Messaging**: Kafka cho việc gán rule (assignment) và events

### Thành Phần Chính
| Thành Phần | Mô Tả |
|------------|-------|
| `validation_rules` | Bảng chính lưu trữ rule definitions |
| `rule_nodes` | Cấu trúc cây cho các điều kiện |
| `assignments` | Gắn rules với objects (campaign, voucher, etc.) |
| `temporal_policies` | Ràng buộc thời gian áp dụng |

---

## 2. Cấu Trúc Database

### 2.1 Bảng `validation_rules`

```sql
CREATE TABLE validation_rules (
    id VARCHAR(36) PRIMARY KEY,           -- UUID của rule
    code VARCHAR(100) UNIQUE NOT NULL,    -- Mã rule duy nhất (vd: WEEKEND_VIP_500K)
    name VARCHAR(200) NOT NULL,           -- Tên hiển thị
    state VARCHAR(20) NOT NULL,           -- Trạng thái: DRAFT, PUBLISHED, ARCHIVED
    rule_version BIGINT NOT NULL,         -- Phiên bản rule
    logic VARCHAR(50),                    -- Logic gốc: ALL, ANY, NONE
    dsl TEXT,                             -- DSL snapshot dạng JSON
    published_at TIMESTAMP,               -- Thời điểm publish
    published_by VARCHAR(50),             -- Người publish
    bundle_hash VARCHAR(200),             -- Hash của compiled bundle
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0              -- Optimistic locking
);
```

**Indexes:**
- `idx_validation_rules_state_version` (state, rule_version DESC)
- `idx_validation_rules_code` (UNIQUE)

### 2.2 Bảng `rule_nodes`

```sql
CREATE TABLE rule_nodes (
    id VARCHAR(36) PRIMARY KEY,
    node_id VARCHAR(100) NOT NULL,        -- Identifier của node
    type VARCHAR(20) NOT NULL,            -- GROUP hoặc COND
    group_logic VARCHAR(20),              -- ALL, ANY, NONE (cho GROUP)
    operator_name VARCHAR(100),           -- Tên operator (cho COND)
    params TEXT,                          -- JSON parameters
    reason_code VARCHAR(100),             -- Mã lỗi khi validation fail
    validation_rule_id VARCHAR(36) NOT NULL, -- FK đến validation_rules
    parent_id VARCHAR(36),                -- Self-reference đến parent node
    node_order INT,                       -- Thứ tự trong parent
    children_ids TEXT                     -- JSON array child IDs
);
```

### 2.3 Node Types

| Type | Mô Tả | Thuộc Tính |
|------|-------|------------|
| `GROUP` | Nhóm các điều kiện | `group_logic`: ALL, ANY, NONE |
| `COND` | Điều kiện đơn | `operator_name`, `params`, `reason_code` |

### 2.4 Logic Types

| Logic | Mô Tả |
|-------|-------|
| `ALL` | Tất cả children phải TRUE |
| `ANY` | Ít nhất 1 child TRUE |
| `NONE` | Tất cả children phải FALSE |

---

## 3. Vòng Đời của Rule

### 3.1 Trạng Thái (States)

```
DRAFT → PUBLISHED → DEPRECATED → ARCHIVED
  ↓                      ↓
  └──────────────────────┘
         (can return)
```

| State | Mô Tả | Có thể chuyển sang |
|-------|-------|-------------------|
| `DRAFT` | Rule đang tạo/chỉnh sửa | PUBLISHED, ARCHIVED |
| `PUBLISHED` | Active và đang áp dụng | DEPRECATED, ARCHIVED |
| `DEPRECATED` | Lỗi thời nhưng vẫn hoạt động | PUBLISHED, ARCHIVED |
| `ARCHIVED` | Lưu trữ vĩnh viễn | (không chuyển được) |

### 3.2 Quy Tắc Chuyển Đổi

**File:** `domain/model/RuleStatus.java`

```java
public boolean canTransitionTo(RuleStatus newStatus) {
    switch (this) {
        case DRAFT:
            return newStatus == PENDING_REVIEW || newStatus == ARCHIVED;
        case PENDING_REVIEW:
            return newStatus == APPROVED || newStatus == DRAFT || newStatus == ARCHIVED;
        case APPROVED:
            return newStatus == PUBLISHED || newStatus == DRAFT || newStatus == ARCHIVED;
        case PUBLISHED:
            return newStatus == DEPRECATED || newStatus == ARCHIVED;
        case DEPRECATED:
            return newStatus == ARCHIVED || newStatus == PUBLISHED;
        case ARCHIVED:
            return false; // Cannot transition from ARCHIVED
        default:
            return false;
    }
}
```

---

## 4. Quy Trình Khai Báo Rule Mới (Database)

Khai báo validation rule trực tiếp vào database thông qua **Liquibase migration files**.

### 4.1 Tổng Quan Quy Trình

```
┌────────────────────────────────────────────────────────────────┐
│  BƯỚC 1: Tạo Liquibase Migration File                          │
│  ├─→ Đặt tên: XXX-add-[rule-name]-validation-rule.yaml        │
│  └─→ Vị trí: src/main/resources/db/changelog/changes/          │
├────────────────────────────────────────────────────────────────┤
│  BƯỚC 2: Insert vào bảng validation_rules                      │
│  └─→ Khai báo thông tin cơ bản của rule                        │
├────────────────────────────────────────────────────────────────┤
│  BƯỚC 3: Insert vào bảng rule_nodes                            │
│  ├─→ Insert ROOT node (GROUP) đầu tiên                         │
│  └─→ Insert các COND nodes (điều kiện con)                     │
├────────────────────────────────────────────────────────────────┤
│  BƯỚC 4: (Tùy chọn) Insert rule_usage_limits                   │
│  └─→ Giới hạn sử dụng rule                                     │
├────────────────────────────────────────────────────────────────┤
│  BƯỚC 5: Thêm migration vào db.changelog-master.yaml           │
└────────────────────────────────────────────────────────────────┘
```

### 4.2 Cấu Trúc Dữ Liệu Cần Insert

#### 4.2.1 Bảng `validation_rules` (Bắt buộc)

| Column | Type | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| `id` | VARCHAR(36) | ✅ | UUID dạng UUIDv7 |
| `code` | VARCHAR(100) | ✅ | Mã unique của rule |
| `name` | VARCHAR(200) | ✅ | Tên hiển thị |
| `state` | VARCHAR(20) | ✅ | Luôn đặt `DRAFT` khi tạo mới |
| `rule_version` | BIGINT | ✅ | Bắt đầu từ `1` |
| `logic` | VARCHAR(50) | ✅ | `ALL`, `ANY`, hoặc `NONE` |
| `dsl` | TEXT | ❌ | JSON DSL snapshot (để `{}`) |
| `created_by` | VARCHAR(100) | ❌ | Username tạo |
| `updated_by` | VARCHAR(100) | ❌ | Username update |

#### 4.2.2 Bảng `rule_nodes` (Bắt buộc)

**A. ROOT Node (GROUP) - Luôn cần 1 root node:**

| Column | Type | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| `id` | VARCHAR(36) | ✅ | UUID unique |
| `node_id` | VARCHAR(100) | ✅ | Identifier ngắn (vd: `n1`) |
| `type` | VARCHAR(20) | ✅ | `GROUP` |
| `group_logic` | VARCHAR(20) | ✅ | `ALL`, `ANY`, `NONE` |
| `children_ids` | TEXT | ✅ | JSON array child node_ids |
| `node_order` | INT | ❌ | Thứ tự (bắt đầu từ 0) |
| `validation_rule_id` | VARCHAR(36) | ✅ | FK đến validation_rules |
| `parent_id` | VARCHAR(36) | ❌ | NULL cho root node |

**B. COND Nodes (Điều kiện):**

| Column | Type | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| `id` | VARCHAR(36) | ✅ | UUID unique |
| `node_id` | VARCHAR(100) | ✅ | Identifier ngắn (vd: `n2`) |
| `type` | VARCHAR(20) | ✅ | `COND` |
| `operator_name` | VARCHAR(100) | ✅ | Tên operator |
| `params` | TEXT | ✅ | JSON parameters |
| `reason_code` | VARCHAR(100) | ✅ | Mã lỗi khi fail |
| `validation_rule_id` | VARCHAR(36) | ✅ | FK đến validation_rules |
| `parent_id` | VARCHAR(36) | ✅ | FK đến parent GROUP node |
| `node_order` | INT | ❌ | Thứ tự trong parent |

#### 4.2.3 Bảng `rule_usage_limits` (Tùy chọn)

| Column | Type | Mô tả |
|--------|------|-------|
| `id` | VARCHAR(36) | UUID unique |
| `validation_rule_id` | VARCHAR(36) | FK đến validation_rules |
| `per_code_total` | INT | Tổng số lần sử dụng |
| `per_customer` | INT | Số lần/khách hàng |
| `per_day` | INT | Số lần/ngày |

### 4.3 Danh Sách Operators Có Sẵn

| Operator Name | Context | Params | Mô Tả |
|---------------|---------|--------|-------|
| `order.total.gte` | order | `{"amount": 500000, "currency": "VND"}` | Đơn hàng >= số tiền |
| `order.total.lte` | order | `{"amount": 1000000, "currency": "VND"}` | Đơn hàng <= số tiền |
| `customer.in_segment` | customer | `{"segments": ["VIP", "GOLD"]}` | Khách thuộc segment |
| `time.window.active` | time | `{"startTime": "09:00:00", "endTime": "17:00:00", ...}` | Trong khung giờ |
| `order.item.product.applicable` | order | `{"include": [...], "exclude": [...]}` | Sản phẩm áp dụng |

### 4.4 Quy Tắc Đặt ID

**Format UUIDv7:** `XXXXXXXX-XXXX-7XXX-XXXX-XXXXXXXXXXXX`

**Convention cho validation module:**
```
Base prefix: 01932b6f-XXXX-7000-8000-XXXXXXXXXXXX

- validation_rules:  01932b6f-0005-7000-8000-00000000XXXX
- rule_nodes:        01932b6f-0006-7000-8000-00000000XXXX
- rule_usage_limits: 01932b6f-0007-7000-8000-00000000XXXX
```

**Quy tắc:**
- Tăng dần số cuối cho mỗi record mới
- Kiểm tra ID không trùng với existing data
- Có thể dùng UUID generator: https://www.uuidtools.com/v7

### 4.5 Template Liquibase Migration

**File:** `src/main/resources/db/changelog/changes/XXX-add-[rule-name]-validation-rule.yaml`

```yaml
databaseChangeLog:
  # ============================================================
  # Rule: [TÊN RULE]
  # Description: [MÔ TẢ NGẮN]
  # Author: [TÊN NGƯỜI TẠO]
  # Date: [NGÀY TẠO]
  # ============================================================

  # BƯỚC 1: Insert validation_rules
  - changeSet:
      id: XXX-add-[rule-code]-validation-rule
      author: [author-name]
      changes:
        - insert:
            tableName: validation_rules
            columns:
              - column:
                  name: id
                  value: [UUID-RULE]
              - column:
                  name: code
                  value: [RULE_CODE]
              - column:
                  name: name
                  value: [Rule Display Name]
              - column:
                  name: state
                  value: DRAFT
              - column:
                  name: rule_version
                  valueNumeric: 1
              - column:
                  name: logic
                  value: ALL
              - column:
                  name: dsl
                  value: '{}'
              - column:
                  name: created_by
                  value: admin
              - column:
                  name: updated_by
                  value: admin

  # BƯỚC 2: Insert ROOT GROUP node
  - changeSet:
      id: XXX-add-[rule-code]-root-node
      author: [author-name]
      changes:
        - insert:
            tableName: rule_nodes
            columns:
              - column:
                  name: id
                  value: [UUID-ROOT-NODE]
              - column:
                  name: node_id
                  value: n1
              - column:
                  name: type
                  value: GROUP
              - column:
                  name: group_logic
                  value: ALL
              - column:
                  name: children_ids
                  value: '["n2", "n3"]'  # List các child node_ids
              - column:
                  name: node_order
                  valueNumeric: 0
              - column:
                  name: validation_rule_id
                  value: [UUID-RULE]
              # parent_id = NULL cho root node

  # BƯỚC 3: Insert COND nodes
  - changeSet:
      id: XXX-add-[rule-code]-cond-nodes
      author: [author-name]
      changes:
        # Condition 1: Order total >= 500k
        - insert:
            tableName: rule_nodes
            columns:
              - column:
                  name: id
                  value: [UUID-COND-1]
              - column:
                  name: node_id
                  value: n2
              - column:
                  name: type
                  value: COND
              - column:
                  name: operator_name
                  value: order.total.gte
              - column:
                  name: params
                  value: '{"amount":500000,"currency":"VND"}'
              - column:
                  name: reason_code
                  value: ORDER_TOTAL_MIN
              - column:
                  name: validation_rule_id
                  value: [UUID-RULE]
              - column:
                  name: parent_id
                  value: [UUID-ROOT-NODE]
              - column:
                  name: node_order
                  valueNumeric: 0

        # Condition 2: Customer in VIP segment
        - insert:
            tableName: rule_nodes
            columns:
              - column:
                  name: id
                  value: [UUID-COND-2]
              - column:
                  name: node_id
                  value: n3
              - column:
                  name: type
                  value: COND
              - column:
                  name: operator_name
                  value: customer.in_segment
              - column:
                  name: params
                  value: '{"segments":["VIP","GOLD"]}'
              - column:
                  name: reason_code
                  value: AUDIENCE_SEGMENT
              - column:
                  name: validation_rule_id
                  value: [UUID-RULE]
              - column:
                  name: parent_id
                  value: [UUID-ROOT-NODE]
              - column:
                  name: node_order
                  valueNumeric: 1

  # BƯỚC 4 (Tùy chọn): Insert usage limits
  - changeSet:
      id: XXX-add-[rule-code]-usage-limits
      author: [author-name]
      changes:
        - insert:
            tableName: rule_usage_limits
            columns:
              - column:
                  name: id
                  value: [UUID-LIMITS]
              - column:
                  name: validation_rule_id
                  value: [UUID-RULE]
              - column:
                  name: per_code_total
                  valueNumeric: 1000
              - column:
                  name: per_customer
                  valueNumeric: 3
              - column:
                  name: per_day
                  valueNumeric: 200
```

### 4.6 Ví Dụ Thực Tế: Rule "VIP Segment Only"

**File:** `012-add-vip-segment-validation-rule.yaml`

```yaml
databaseChangeLog:
  # Create VIP segment validation rule
  - changeSet:
      id: 012-add-vip-segment-validation-rule
      author: harley hoang
      changes:
        # Insert validation rule
        - insert:
            tableName: validation_rules
            columns:
              - column:
                  name: id
                  value: 01932b6f-0005-7000-8000-000000000010
              - column:
                  name: code
                  value: VIP_SEGMENT_ONLY
              - column:
                  name: name
                  value: VIP Segment Only Validation
              - column:
                  name: state
                  value: DRAFT
              - column:
                  name: rule_version
                  valueNumeric: 1
              - column:
                  name: logic
                  value: ALL
              - column:
                  name: dsl
                  value: '{}'
              - column:
                  name: created_by
                  value: admin
              - column:
                  name: updated_by
                  value: admin

        # Insert root GROUP node (n1)
        - insert:
            tableName: rule_nodes
            columns:
              - column:
                  name: id
                  value: 01932b6f-0006-7000-8000-000000000010
              - column:
                  name: node_id
                  value: n1
              - column:
                  name: type
                  value: GROUP
              - column:
                  name: group_logic
                  value: ALL
              - column:
                  name: children_ids
                  value: '["n2"]'
              - column:
                  name: node_order
                  valueNumeric: 0
              - column:
                  name: validation_rule_id
                  value: 01932b6f-0005-7000-8000-000000000010

        # Insert COND node for VIP segment check (n2)
        - insert:
            tableName: rule_nodes
            columns:
              - column:
                  name: id
                  value: 01932b6f-0006-7000-8000-000000000011
              - column:
                  name: node_id
                  value: n2
              - column:
                  name: type
                  value: COND
              - column:
                  name: operator_name
                  value: customer.in_segment
              - column:
                  name: params
                  value: '{"segments":["VIP"]}'
              - column:
                  name: reason_code
                  value: AUDIENCE_SEGMENT
              - column:
                  name: validation_rule_id
                  value: 01932b6f-0005-7000-8000-000000000010
              - column:
                  name: parent_id
                  value: 01932b6f-0006-7000-8000-000000000010
              - column:
                  name: node_order
                  valueNumeric: 0
```

### 4.7 Ví Dụ: Rule Phức Tạp với Nested Groups

**Yêu cầu:** Khách VIP + (Đơn >= 500k HOẶC là khách mới)

```yaml
# Structure:
# ROOT (ALL)
#   ├── n2: customer.in_segment (VIP)
#   └── n3: GROUP (ANY)
#           ├── n4: order.total.gte (500k)
#           └── n5: customer.is_new

databaseChangeLog:
  - changeSet:
      id: XXX-complex-rule
      author: harley hoang
      changes:
        # validation_rules
        - insert:
            tableName: validation_rules
            columns:
              - column: { name: id, value: 01932b6f-0005-7000-8000-000000000020 }
              - column: { name: code, value: VIP_HIGH_VALUE_OR_NEW }
              - column: { name: name, value: VIP High Value or New Customer }
              - column: { name: state, value: DRAFT }
              - column: { name: rule_version, valueNumeric: 1 }
              - column: { name: logic, value: ALL }
              - column: { name: dsl, value: '{}' }
              - column: { name: created_by, value: admin }
              - column: { name: updated_by, value: admin }

        # ROOT node (n1) - ALL
        - insert:
            tableName: rule_nodes
            columns:
              - column: { name: id, value: 01932b6f-0006-7000-8000-000000000020 }
              - column: { name: node_id, value: n1 }
              - column: { name: type, value: GROUP }
              - column: { name: group_logic, value: ALL }
              - column: { name: children_ids, value: '["n2","n3"]' }
              - column: { name: node_order, valueNumeric: 0 }
              - column: { name: validation_rule_id, value: 01932b6f-0005-7000-8000-000000000020 }

        # COND node (n2) - VIP segment
        - insert:
            tableName: rule_nodes
            columns:
              - column: { name: id, value: 01932b6f-0006-7000-8000-000000000021 }
              - column: { name: node_id, value: n2 }
              - column: { name: type, value: COND }
              - column: { name: operator_name, value: customer.in_segment }
              - column: { name: params, value: '{"segments":["VIP"]}' }
              - column: { name: reason_code, value: AUDIENCE_SEGMENT }
              - column: { name: validation_rule_id, value: 01932b6f-0005-7000-8000-000000000020 }
              - column: { name: parent_id, value: 01932b6f-0006-7000-8000-000000000020 }
              - column: { name: node_order, valueNumeric: 0 }

        # Nested GROUP node (n3) - ANY
        - insert:
            tableName: rule_nodes
            columns:
              - column: { name: id, value: 01932b6f-0006-7000-8000-000000000022 }
              - column: { name: node_id, value: n3 }
              - column: { name: type, value: GROUP }
              - column: { name: group_logic, value: ANY }
              - column: { name: children_ids, value: '["n4","n5"]' }
              - column: { name: node_order, valueNumeric: 1 }
              - column: { name: validation_rule_id, value: 01932b6f-0005-7000-8000-000000000020 }
              - column: { name: parent_id, value: 01932b6f-0006-7000-8000-000000000020 }

        # COND node (n4) - Order >= 500k
        - insert:
            tableName: rule_nodes
            columns:
              - column: { name: id, value: 01932b6f-0006-7000-8000-000000000023 }
              - column: { name: node_id, value: n4 }
              - column: { name: type, value: COND }
              - column: { name: operator_name, value: order.total.gte }
              - column: { name: params, value: '{"amount":500000,"currency":"VND"}' }
              - column: { name: reason_code, value: ORDER_TOTAL_MIN }
              - column: { name: validation_rule_id, value: 01932b6f-0005-7000-8000-000000000020 }
              - column: { name: parent_id, value: 01932b6f-0006-7000-8000-000000000022 }
              - column: { name: node_order, valueNumeric: 0 }

        # COND node (n5) - New customer
        - insert:
            tableName: rule_nodes
            columns:
              - column: { name: id, value: 01932b6f-0006-7000-8000-000000000024 }
              - column: { name: node_id, value: n5 }
              - column: { name: type, value: COND }
              - column: { name: operator_name, value: customer.is_new }
              - column: { name: params, value: '{}' }
              - column: { name: reason_code, value: NOT_NEW_CUSTOMER }
              - column: { name: validation_rule_id, value: 01932b6f-0005-7000-8000-000000000020 }
              - column: { name: parent_id, value: 01932b6f-0006-7000-8000-000000000022 }
              - column: { name: node_order, valueNumeric: 1 }
```

### 4.8 Đăng Ký Migration vào Master Changelog

**File:** `src/main/resources/db/changelog/db.changelog-master.yaml`

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-create-validation-rule-engine-schema.yaml
  - include:
      file: db/changelog/changes/002-insert-rule-engine-sample-data.yaml
  # ... existing migrations ...
  - include:
      file: db/changelog/changes/XXX-add-[rule-name]-validation-rule.yaml  # Thêm dòng này
```

### 4.9 Checklist Khai Báo Rule Mới

| # | Task | Check |
|---|------|-------|
| 1 | Tạo file migration với đúng naming convention | ☐ |
| 2 | Generate UUID cho validation_rules | ☐ |
| 3 | Generate UUID cho mỗi rule_node | ☐ |
| 4 | Khai báo ROOT GROUP node với children_ids | ☐ |
| 5 | Khai báo tất cả COND nodes với parent_id | ☐ |
| 6 | Verify JSON format trong params và children_ids | ☐ |
| 7 | Đảm bảo node_id trong children_ids khớp với node_id của children | ☐ |
| 8 | Set state = DRAFT cho rule mới | ☐ |
| 9 | Thêm migration vào db.changelog-master.yaml | ☐ |
| 10 | Chạy Liquibase để apply migration | ☐ |

### 4.10 Verify Sau Khi Insert

**Query kiểm tra rule:**
```sql
-- Kiểm tra rule
SELECT id, code, name, state, logic FROM validation_rules WHERE code = 'YOUR_RULE_CODE';

-- Kiểm tra nodes
SELECT n.id, n.node_id, n.type, n.group_logic, n.operator_name, n.parent_id
FROM rule_nodes n
JOIN validation_rules r ON n.validation_rule_id = r.id
WHERE r.code = 'YOUR_RULE_CODE'
ORDER BY n.node_order;

-- Kiểm tra tree structure
SELECT
    n.node_id,
    n.type,
    COALESCE(n.group_logic, n.operator_name) as logic_or_operator,
    p.node_id as parent_node_id
FROM rule_nodes n
LEFT JOIN rule_nodes p ON n.parent_id = p.id
WHERE n.validation_rule_id = 'YOUR-RULE-UUID';
```

---

## 4B. Khai Báo Rule Qua REST API (Alternative)

Ngoài cách insert trực tiếp database, có thể dùng REST API:

### API Endpoint

```
POST /promotion/promotion-validation/v1/rules
```

### Request Body

```json
{
  "code": "WEEKEND_VIP_500K",
  "name": "Weekend VIP ≥500k promotion",
  "logic": "ALL",
  "limits": {
    "perCodeTotal": 1000,
    "perCustomer": 3
  },
  "nodes": [
    {
      "id": "root",
      "type": "GROUP",
      "groupLogic": "ALL",
      "children": ["cond_1", "cond_2"]
    },
    {
      "id": "cond_1",
      "type": "COND",
      "operatorName": "order.total.gte",
      "params": { "threshold": 500000, "currency": "VND" },
      "reasonCode": "ORDER_TOTAL_INSUFFICIENT"
    },
    {
      "id": "cond_2",
      "type": "COND",
      "operatorName": "customer.segment.in",
      "params": { "segments": ["VIP", "GOLD"] },
      "reasonCode": "CUSTOMER_NOT_IN_SEGMENT"
    }
  ]
}
```

### CURL Example

```bash
curl -X POST "http://localhost:16014/promotion/promotion-validation/v1/rules" \
  -H "Content-Type: application/json" \
  -H "X-User-ID: admin" \
  -d '{
    "code": "MIN_ORDER_100K",
    "name": "Minimum Order 100k",
    "logic": "ALL",
    "nodes": [
      {
        "id": "root",
        "type": "GROUP",
        "groupLogic": "ALL",
        "children": ["cond_1"]
      },
      {
        "id": "cond_1",
        "type": "COND",
        "operatorName": "order.total.gte",
        "params": { "threshold": 100000, "currency": "VND" },
        "reasonCode": "ORDER_BELOW_MINIMUM"
      }
    ]
  }'
```

---

## 5. Quy Trình Validate Rule

### 5.1 Lint API (Validate Structure)

```
POST /promotion/promotion-validation/v1/rules:lint
```

### 5.2 Request Body

```json
{
  "nodes": [
    {
      "id": "root",
      "type": "GROUP",
      "groupLogic": "ALL",
      "children": ["cond_1"]
    },
    {
      "id": "cond_1",
      "type": "COND",
      "operatorName": "order.total.gte",
      "params": { "threshold": 100000 },
      "reasonCode": "ORDER_BELOW_MINIMUM"
    }
  ]
}
```

### 5.3 Response

```json
{
  "ok": true,
  "issues": []
}
```

**Hoặc nếu có lỗi:**

```json
{
  "ok": false,
  "issues": [
    {
      "path": "nodes[1]",
      "message": "Operator name is required for COND nodes",
      "operator": null
    }
  ]
}
```

### 5.4 Validate for Publishing

```
POST /promotion/promotion-validation/v1/rules/publishing/rules/{ruleId}/validate
```

**Response:**
```json
{
  "ruleId": "rul_MIN_ORDER_100K_123456",
  "valid": true,
  "message": "Rule is valid for publishing"
}
```

---

## 6. Quy Trình Activate Rule

### 6.1 API Endpoint

```
POST /promotion/promotion-validation/v1/rules/{ruleId}:activate
```

### 6.2 Điều Kiện

- Rule phải ở trạng thái `DRAFT`
- Rule phải có ít nhất 1 node
- Tất cả nodes phải hợp lệ

### 6.3 Kết Quả

```json
{
  "id": "rul_MIN_ORDER_100K_123456",
  "code": "MIN_ORDER_100K",
  "name": "Minimum Order 100k",
  "state": "PUBLISHED",
  "active": true,
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### 6.4 CURL Example

```bash
curl -X POST "http://localhost:16014/promotion/promotion-validation/v1/rules/rul_MIN_ORDER_100K_123456:activate" \
  -H "X-User-ID: admin"
```

---

## 7. Quy Trình Publish Rule

### 7.1 API Endpoint

```
POST /promotion/promotion-validation/v1/rules/publishing/rules/{ruleId}
```

### 7.2 Workflow Chi Tiết

```
┌──────────────────────────────────────────────────────────────┐
│  1. Load Rule from Database                                  │
│     └─→ Verify rule exists and is valid                      │
├──────────────────────────────────────────────────────────────┤
│  2. Validate Rule for Publishing                             │
│     ├─→ Check state != DRAFT (must be activated first)       │
│     └─→ Validate node structure                              │
├──────────────────────────────────────────────────────────────┤
│  3. Compile Rule in Validation Engine                        │
│     ├─→ Convert nodes to DTOs                                │
│     ├─→ Build CompileRequest                                 │
│     ├─→ Call validation-engine /v1/compiler/compile          │
│     └─→ Receive CompileResponse with bundleHash              │
├──────────────────────────────────────────────────────────────┤
│  4. Warmup Bundle                                            │
│     ├─→ Call validation-engine /v1/bundle/warmup             │
│     └─→ Pre-load compiled DRL into memory                    │
├──────────────────────────────────────────────────────────────┤
│  5. Verify Rule Execution                                    │
│     ├─→ Call validation-engine /v1/execution/execute         │
│     └─→ Test with sample data to verify works                │
├──────────────────────────────────────────────────────────────┤
│  6. Update Rule State                                        │
│     ├─→ state = PUBLISHED                                    │
│     ├─→ published_at = NOW()                                 │
│     ├─→ published_by = current_user                          │
│     └─→ bundle_hash = compiled_hash                          │
└──────────────────────────────────────────────────────────────┘
```

### 7.3 Request Body (Optional)

```json
{
  "force": false,
  "notes": "Publishing for weekend campaign"
}
```

### 7.4 Response

```json
{
  "ruleId": "rul_MIN_ORDER_100K_123456",
  "success": true,
  "bundleHash": "sha256:abc123def456...",
  "artifactSize": 45678,
  "errorMessage": null
}
```

### 7.5 Batch Publish

```
POST /promotion/promotion-validation/v1/rules/publishing/batch
```

```json
{
  "ruleIds": [
    "rul_RULE_1",
    "rul_RULE_2",
    "rul_RULE_3"
  ]
}
```

### 7.6 Get Deployment Status

```
GET /promotion/promotion-validation/v1/rules/publishing/rules/{ruleId}/status
```

**Response:**
```json
{
  "ruleId": "rul_MIN_ORDER_100K_123456",
  "status": "DEPLOYED",
  "deployed": true,
  "bundleHash": "sha256:abc123..."
}
```

---

## 8. Gán Rule cho Object (Assignment)

### 8.1 Qua Kafka Command

**Topic:** `promotion_validation_command`

**Payload:**
```json
{
  "id": "cmd_uuid",
  "type": "SettingValidationRuleCommand",
  "source": "campaign-service",
  "subject": "CAMPAIGN-001",
  "occurredAt": "2024-01-15T10:00:00Z",
  "version": 1,
  "payload": {
    "ruleId": "rul_MIN_ORDER_100K_123456",
    "objectType": "campaign",
    "objectId": "CAMPAIGN-001",
    "active": true,
    "priority": 1,
    "trafficPercent": 100,
    "applicableTo": {
      "includedAll": false,
      "included": [
        { "object": "PRODUCT", "id": "PROD-001", "effect": "APPLY_TO_EVERY" }
      ],
      "excluded": [
        { "object": "PRODUCT", "id": "PROD-GIFT" }
      ]
    },
    "timeframe": {
      "validityTimeframe": {
        "startDate": "2024-01-01T00:00:00Z",
        "expirationDate": "2024-12-31T23:59:59Z"
      },
      "validityHoursPerDay": {
        "startTime": "09:00",
        "endTime": "21:00"
      }
    }
  }
}
```

### 8.2 Bảng `assignments`

```sql
CREATE TABLE assignments (
    id VARCHAR(36) PRIMARY KEY,
    entity_type VARCHAR(100),          -- campaign, voucher, etc.
    entity_id VARCHAR(100),            -- Object ID
    rule_id VARCHAR(100),              -- Rule ID
    priority INT,
    active BOOLEAN DEFAULT true,
    temporal_bundle_hash VARCHAR(255), -- Compiled temporal DRL bundle
    included_all BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### 8.3 Get Rule by Object

```
GET /promotion/promotion-validation/v1/rules/by-object?objectType=campaign&objectId=CAMPAIGN-001
```

**Response:**
```json
{
  "rule": {
    "id": "rul_MIN_ORDER_100K_123456",
    "code": "MIN_ORDER_100K",
    "name": "Minimum Order 100k",
    "state": "PUBLISHED"
  },
  "assignment": {
    "assignmentId": "asgn_uuid",
    "objectType": "campaign",
    "objectId": "CAMPAIGN-001",
    "active": true,
    "validFrom": "2024-01-01T00:00:00Z",
    "validTo": "2024-12-31T23:59:59Z",
    "trafficPercent": 100
  }
}
```

---

## 9. API Endpoints

### 9.1 Rule Management

| Method | Endpoint | Mô Tả |
|--------|----------|-------|
| POST | `/v1/rules` | Tạo rule mới |
| GET | `/v1/rules/{ruleId}` | Lấy rule theo ID |
| GET | `/v1/rules` | List rules với pagination |
| PATCH | `/v1/rules/{ruleId}` | Update rule (chỉ DRAFT) |
| POST | `/v1/rules/{ruleId}:activate` | Activate rule |
| POST | `/v1/rules/{ruleId}:archive` | Archive rule |
| POST | `/v1/rules/{ruleId}:clone` | Clone rule |
| POST | `/v1/rules:lint` | Validate rule structure |

### 9.2 Rule Publishing

| Method | Endpoint | Mô Tả |
|--------|----------|-------|
| POST | `/v1/rules/publishing/rules/{ruleId}` | Publish rule |
| POST | `/v1/rules/publishing/batch` | Batch publish |
| DELETE | `/v1/rules/publishing/rules/{ruleId}` | Unpublish rule |
| GET | `/v1/rules/publishing/rules/{ruleId}/status` | Get deployment status |
| POST | `/v1/rules/publishing/rules/{ruleId}/validate` | Validate for publishing |

### 9.3 Rule Simulation

| Method | Endpoint | Mô Tả |
|--------|----------|-------|
| POST | `/v1/rules/{ruleId}/simulate` | Test rule với context |
| POST | `/v1/rules/{ruleId}/simulate:batch` | Batch test cases |

---

## 10. Ví Dụ Thực Tế

### 10.1 Tạo Rule "VIP Weekend Sale"

**Bước 1: Tạo Rule**

```bash
curl -X POST "http://localhost:16014/promotion/promotion-validation/v1/rules" \
  -H "Content-Type: application/json" \
  -H "X-User-ID: admin" \
  -d '{
    "code": "VIP_WEEKEND_SALE",
    "name": "VIP Weekend Sale - Min 500k",
    "logic": "ALL",
    "nodes": [
      {
        "id": "root",
        "type": "GROUP",
        "groupLogic": "ALL",
        "children": ["check_segment", "check_total", "check_time"]
      },
      {
        "id": "check_segment",
        "type": "COND",
        "operatorName": "customer.segment.in",
        "params": { "segments": ["VIP", "GOLD", "PLATINUM"] },
        "reasonCode": "NOT_VIP_CUSTOMER"
      },
      {
        "id": "check_total",
        "type": "COND",
        "operatorName": "order.total.gte",
        "params": { "threshold": 500000, "currency": "VND" },
        "reasonCode": "ORDER_BELOW_500K"
      },
      {
        "id": "check_time",
        "type": "COND",
        "operatorName": "time.day.in",
        "params": { "days": ["SATURDAY", "SUNDAY"] },
        "reasonCode": "NOT_WEEKEND"
      }
    ]
  }'
```

**Bước 2: Activate Rule**

```bash
curl -X POST "http://localhost:16014/promotion/promotion-validation/v1/rules/rul_VIP_WEEKEND_SALE_123:activate" \
  -H "X-User-ID: admin"
```

**Bước 3: Publish Rule**

```bash
curl -X POST "http://localhost:16014/promotion/promotion-validation/v1/rules/publishing/rules/rul_VIP_WEEKEND_SALE_123" \
  -H "X-User-ID: admin"
```

**Bước 4: Gán cho Campaign (qua Kafka)**

```json
{
  "type": "SettingValidationRuleCommand",
  "payload": {
    "ruleId": "rul_VIP_WEEKEND_SALE_123",
    "objectType": "campaign",
    "objectId": "WEEKEND_PROMO_2024",
    "active": true,
    "timeframe": {
      "validityTimeframe": {
        "startDate": "2024-01-01T00:00:00Z",
        "expirationDate": "2024-12-31T23:59:59Z"
      }
    }
  }
}
```

### 10.2 Simulate Rule

```bash
curl -X POST "http://localhost:16014/promotion/promotion-validation/v1/rules/rul_VIP_WEEKEND_SALE_123/simulate" \
  -H "Content-Type: application/json" \
  -d '{
    "version": 1,
    "context": {
      "now": "2024-01-20T10:00:00Z",
      "tz": "Asia/Bangkok",
      "customer": {
        "id": "CUST-001",
        "segments": ["VIP"]
      },
      "order": {
        "total": 600000,
        "currency": "VND"
      }
    },
    "explain": "FULL"
  }'
```

**Response:**
```json
{
  "decision": "allow",
  "reasonCodes": [],
  "explain": {
    "root": {
      "result": true,
      "children": {
        "check_segment": { "result": true },
        "check_total": { "result": true },
        "check_time": { "result": true }
      }
    }
  }
}
```

---

## Lưu Ý Quan Trọng

### Quy Tắc Khi Tạo Rule

1. **Code phải unique** - Không thể tạo 2 rules cùng code
2. **Chỉ có thể update DRAFT rules** - Sau khi activate/publish phải clone để chỉnh sửa
3. **Mỗi COND node cần reason_code** - Để biết nguyên nhân khi validation fail
4. **Test trước khi publish** - Dùng simulate API để kiểm tra logic

### Quy Tắc Khi Publish

1. **Rule phải được activate trước** - Không thể publish DRAFT rule
2. **Validation-engine phải available** - Để compile thành DRL
3. **Warmup required** - Bundle phải được load vào memory

### Best Practices

1. **Đặt tên code có ý nghĩa**: `SEGMENT_VIP_MIN_ORDER_500K`
2. **Group các điều kiện liên quan**: Dùng nested GROUP nodes
3. **Sử dụng reason codes rõ ràng**: `CUSTOMER_NOT_VIP`, `ORDER_BELOW_MINIMUM`
4. **Version control**: Clone rule thay vì edit trực tiếp
5. **Test đầy đủ**: Dùng batch simulate với nhiều test cases

---

*Tài liệu được tạo cho module validation - Promix Platform*
