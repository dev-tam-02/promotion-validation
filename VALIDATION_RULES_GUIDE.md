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

Module **validation** quản lý các quy tắc xác thực (validation rules) cho nền tảng khuyến mại. Mỗi rule có thể bao gồm
nhiều điều kiện (conditions) được tổ chức theo cấu trúc cây (tree structure).

### Kiến Trúc Chính

- **Database**: MariaDB với Liquibase migrations
- **Validation Engine**: Tích hợp với Drools-based validation-engine để compile và execute rules
- **Messaging**: Kafka cho việc gán rule (assignment) và events

### Thành Phần Chính

| Thành Phần          | Mô Tả                                           |
|---------------------|-------------------------------------------------|
| `validation_rules`  | Bảng chính lưu trữ rule definitions             |
| `rule_nodes`        | Cấu trúc cây cho các điều kiện                  |
| `assignments`       | Gắn rules với objects (campaign, voucher, etc.) |
| `temporal_policies` | Ràng buộc thời gian áp dụng                     |

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

| Type    | Mô Tả              | Thuộc Tính                               |
|---------|--------------------|------------------------------------------|
| `GROUP` | Nhóm các điều kiện | `group_logic`: ALL, ANY, NONE            |
| `COND`  | Điều kiện đơn      | `operator_name`, `params`, `reason_code` |

### 2.4 Logic Types

| Logic  | Mô Tả                      |
|--------|----------------------------|
| `ALL`  | Tất cả children phải TRUE  |
| `ANY`  | Ít nhất 1 child TRUE       |
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

| State        | Mô Tả                        | Có thể chuyển sang   |
|--------------|------------------------------|----------------------|
| `DRAFT`      | Rule đang tạo/chỉnh sửa      | PUBLISHED, ARCHIVED  |
| `PUBLISHED`  | Active và đang áp dụng       | DEPRECATED, ARCHIVED |
| `DEPRECATED` | Lỗi thời nhưng vẫn hoạt động | PUBLISHED, ARCHIVED  |
| `ARCHIVED`   | Lưu trữ vĩnh viễn            | (không chuyển được)  |

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

## 4. Quy Trình Khai Báo Rule Mới (Database - Dành cho Vận Hành)

Hướng dẫn này dành cho **đội vận hành** để khai báo validation rule trực tiếp vào database bằng SQL.

### 4.1 Tổng Quan Quy Trình

```
┌────────────────────────────────────────────────────────────────┐
│  BƯỚC 1: Insert vào bảng validation_rules                      │
│  └─→ Khai báo thông tin cơ bản của rule (code, name, logic)    │
├────────────────────────────────────────────────────────────────┤
│  BƯỚC 2: Insert ROOT node vào bảng rule_nodes                  │
│  └─→ Node gốc loại GROUP, chứa danh sách children              │
├────────────────────────────────────────────────────────────────┤
│  BƯỚC 3: Insert các COND nodes vào bảng rule_nodes             │
│  └─→ Các điều kiện con với operator và params                  │
├────────────────────────────────────────────────────────────────┤
│  BƯỚC 4: (Tùy chọn) Insert rule_usage_limits                   │
│  └─→ Giới hạn sử dụng rule                                     │
├────────────────────────────────────────────────────────────────┤
│  BƯỚC 5: Verify dữ liệu đã insert                              │
│  └─→ Chạy query kiểm tra                                       │
└────────────────────────────────────────────────────────────────┘
```

### 4.1.1 Kết Nối Database

```bash
# MariaDB connection
mysql -h <HOST> -P 3306 -u <USER> -p validation

# Ví dụ môi trường dev
mysql -h promotion-mariadb -P 3306 -u root -p validation
```

### 4.2 Cấu Trúc Dữ Liệu Cần Insert

#### 4.2.1 Bảng `validation_rules` (Bắt buộc)

| Column         | Type         | Bắt buộc | Mô tả                        |
|----------------|--------------|----------|------------------------------|
| `id`           | VARCHAR(36)  | ✅        | UUID dạng UUIDv7             |
| `code`         | VARCHAR(100) | ✅        | Mã unique của rule           |
| `name`         | VARCHAR(200) | ✅        | Tên hiển thị                 |
| `state`        | VARCHAR(20)  | ✅        | Luôn đặt `DRAFT` khi tạo mới |
| `rule_version` | BIGINT       | ✅        | Bắt đầu từ `1`               |
| `logic`        | VARCHAR(50)  | ✅        | `ALL`, `ANY`, hoặc `NONE`    |
| `dsl`          | TEXT         | ❌        | JSON DSL snapshot (để `{}`)  |
| `created_by`   | VARCHAR(100) | ❌        | Username tạo                 |
| `updated_by`   | VARCHAR(100) | ❌        | Username update              |

#### 4.2.2 Bảng `rule_nodes` (Bắt buộc)

**A. ROOT Node (GROUP) - Luôn cần 1 root node:**

| Column               | Type         | Bắt buộc | Mô tả                      |
|----------------------|--------------|----------|----------------------------|
| `id`                 | VARCHAR(36)  | ✅        | UUID unique                |
| `node_id`            | VARCHAR(100) | ✅        | Identifier ngắn (vd: `n1`) |
| `type`               | VARCHAR(20)  | ✅        | `GROUP`                    |
| `group_logic`        | VARCHAR(20)  | ✅        | `ALL`, `ANY`, `NONE`       |
| `children_ids`       | TEXT         | ✅        | JSON array child node_ids  |
| `node_order`         | INT          | ❌        | Thứ tự (bắt đầu từ 0)      |
| `validation_rule_id` | VARCHAR(36)  | ✅        | FK đến validation_rules    |
| `parent_id`          | VARCHAR(36)  | ❌        | NULL cho root node         |

**B. COND Nodes (Điều kiện):**

| Column               | Type         | Bắt buộc | Mô tả                      |
|----------------------|--------------|----------|----------------------------|
| `id`                 | VARCHAR(36)  | ✅        | UUID unique                |
| `node_id`            | VARCHAR(100) | ✅        | Identifier ngắn (vd: `n2`) |
| `type`               | VARCHAR(20)  | ✅        | `COND`                     |
| `operator_name`      | VARCHAR(100) | ✅        | Tên operator               |
| `params`             | TEXT         | ✅        | JSON parameters            |
| `reason_code`        | VARCHAR(100) | ✅        | Mã lỗi khi fail            |
| `validation_rule_id` | VARCHAR(36)  | ✅        | FK đến validation_rules    |
| `parent_id`          | VARCHAR(36)  | ✅        | FK đến parent GROUP node   |
| `node_order`         | INT          | ❌        | Thứ tự trong parent        |

#### 4.2.3 Bảng `rule_usage_limits` (Tùy chọn)

| Column               | Type        | Mô tả                   |
|----------------------|-------------|-------------------------|
| `id`                 | VARCHAR(36) | UUID unique             |
| `validation_rule_id` | VARCHAR(36) | FK đến validation_rules |
| `per_code_total`     | INT         | Tổng số lần sử dụng     |
| `per_customer`       | INT         | Số lần/khách hàng       |
| `per_day`            | INT         | Số lần/ngày             |

### 4.3 Giải Thích Chi Tiết Các Giá Trị

#### 4.3.1 Bảng `validation_rules` - Giá Trị Các Cột

| Cột              | Giá Trị     | Khi Nào Chọn                         | Ví Dụ                                   |
|------------------|-------------|--------------------------------------|-----------------------------------------|
| **state**        | `DRAFT`     | Rule mới tạo, chưa active            | Luôn dùng khi insert mới                |
|                  | `PUBLISHED` | Rule đã được activate và publish     | Chỉ set qua API, KHÔNG insert trực tiếp |
|                  | `ARCHIVED`  | Rule đã lưu trữ, không dùng nữa      | Chỉ set qua API                         |
| **logic**        | `ALL`       | Tất cả điều kiện con phải đúng (AND) | Khách VIP **VÀ** đơn >= 500k            |
|                  | `ANY`       | Ít nhất 1 điều kiện đúng (OR)        | Khách VIP **HOẶC** khách mới            |
|                  | `NONE`      | Tất cả điều kiện phải sai (NOT)      | KHÔNG thuộc blacklist                   |
| **rule_version** | `1`         | Phiên bản đầu tiên                   | Luôn bắt đầu từ 1                       |

**⚠️ Lưu ý quan trọng về `state`:**

- Khi insert mới: **LUÔN dùng `DRAFT`**
- Không bao giờ insert trực tiếp `PUBLISHED` - phải qua API activate + publish
- Quy trình: `DRAFT` → (API activate) → `PUBLISHED`

---

#### 4.3.2 Bảng `rule_nodes` - Giá Trị Cột `type`

| Giá Trị     | Mô Tả                              | Khi Nào Chọn                                          | Các Cột Bắt Buộc                         |
|-------------|------------------------------------|-------------------------------------------------------|------------------------------------------|
| **`GROUP`** | Node nhóm, chứa các node con       | Khi cần **nhóm nhiều điều kiện** với logic AND/OR/NOT | `group_logic`, `children_ids`            |
| **`COND`**  | Node điều kiện, thực hiện kiểm tra | Khi cần **kiểm tra 1 điều kiện cụ thể**               | `operator_name`, `params`, `reason_code` |

**Quy tắc:**

- Mỗi rule **phải có ít nhất 1 ROOT node** loại `GROUP`
- ROOT node **không có `parent_id`** (NULL)
- `COND` node **luôn là con** của `GROUP` node

**Ví dụ trực quan:**

```
Rule: Khách VIP + (Đơn >= 500k HOẶC khách mới)

ROOT (GROUP, logic=ALL)           ← type='GROUP', parent_id=NULL
  ├── COND: customer.in_segment   ← type='COND', parent_id=ROOT
  └── GROUP (logic=ANY)           ← type='GROUP', parent_id=ROOT
        ├── COND: order.total.gte ← type='COND', parent_id=GROUP_ANY
        └── COND: customer.is_new ← type='COND', parent_id=GROUP_ANY
```

---

#### 4.3.3 Bảng `rule_nodes` - Giá Trị Cột `group_logic`

**Chỉ áp dụng cho node type = `GROUP`**

| Giá Trị    | Logic                       | Khi Nào Chọn                                          | Ví Dụ                                   |
|------------|-----------------------------|-------------------------------------------------------|-----------------------------------------|
| **`ALL`**  | AND - Tất cả con phải TRUE  | Khi **tất cả điều kiện** đều phải thỏa mãn            | VIP **VÀ** đơn >= 500k **VÀ** cuối tuần |
| **`ANY`**  | OR - Ít nhất 1 con TRUE     | Khi **chỉ cần 1** điều kiện thỏa mãn                  | VIP **HOẶC** GOLD **HOẶC** PLATINUM     |
| **`NONE`** | NOT - Tất cả con phải FALSE | Khi **không được phép** thỏa mãn bất kỳ điều kiện nào | KHÔNG blacklist **VÀ** KHÔNG fraud      |

**Bảng truth table:**

```
ALL (AND):
  - TRUE + TRUE = TRUE
  - TRUE + FALSE = FALSE
  - FALSE + FALSE = FALSE

ANY (OR):
  - TRUE + TRUE = TRUE
  - TRUE + FALSE = TRUE
  - FALSE + FALSE = FALSE

NONE (NOT):
  - TRUE + TRUE = FALSE
  - TRUE + FALSE = FALSE
  - FALSE + FALSE = TRUE
```

---

#### 4.3.4 Danh Sách Operators (`operator_name`)

**Chỉ áp dụng cho node type = `COND`**

##### A. Order Operators (Kiểm tra đơn hàng)

| Operator                        | Mô Tả                 | Params                                              | Khi Nào Dùng           |
|---------------------------------|-----------------------|-----------------------------------------------------|------------------------|
| `order.total.gte`               | Tổng đơn >= giá trị   | `{"amount": 500000, "currency": "VND"}`             | Đơn hàng tối thiểu     |
| `order.total.lte`               | Tổng đơn <= giá trị   | `{"amount": 1000000, "currency": "VND"}`            | Đơn hàng tối đa        |
| `order.total.between`           | Tổng đơn trong khoảng | `{"min": 100000, "max": 500000, "currency": "VND"}` | Đơn trong khoảng giá   |
| `order.item.count.gte`          | Số lượng item >=      | `{"count": 3}`                                      | Mua ít nhất X sản phẩm |
| `order.item.product.applicable` | Sản phẩm áp dụng      | `{"include": ["P1","P2"], "exclude": ["P3"]}`       | Giới hạn sản phẩm      |

##### B. Customer Operators (Kiểm tra khách hàng)

| Operator                   | Mô Tả               | Params                          | Khi Nào Dùng                |
|----------------------------|---------------------|---------------------------------|-----------------------------|
| `customer.in_segment`      | Thuộc segment       | `{"segments": ["VIP", "GOLD"]}` | Khách VIP/GOLD/...          |
| `customer.not_in_segment`  | KHÔNG thuộc segment | `{"segments": ["BLACKLIST"]}`   | Loại trừ blacklist          |
| `customer.is_new`          | Khách hàng mới      | `{}`                            | Ưu đãi khách mới            |
| `customer.order_count.gte` | Số đơn >=           | `{"count": 5}`                  | Khách thân thiết (>= 5 đơn) |
| `customer.order_count.lte` | Số đơn <=           | `{"count": 2}`                  | Khách mới (<= 2 đơn)        |
| `customer.tier.in`         | Thuộc tier          | `{"tiers": [1, 2, 3]}`          | Tier membership             |

##### C. Time Operators (Kiểm tra thời gian)

| Operator             | Mô Tả             | Params                                         | Khi Nào Dùng         |
|----------------------|-------------------|------------------------------------------------|----------------------|
| `time.window.active` | Trong khung giờ   | Xem chi tiết bên dưới                          | Flash sale, giờ vàng |
| `time.day.in`        | Ngày trong tuần   | `{"days": ["SATURDAY", "SUNDAY"]}`             | Promo cuối tuần      |
| `time.date.between`  | Trong khoảng ngày | `{"start": "2024-01-01", "end": "2024-12-31"}` | Campaign theo mùa    |

**Chi tiết `time.window.active` params:**

```json
{
  "startTime": "09:00:00",        // Giờ bắt đầu (HH:mm:ss)
  "endTime": "17:00:00",          // Giờ kết thúc
  "timezone": "Asia/Bangkok",     // Timezone
  "daysOfWeek": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "spansMidnight": false          // true nếu qua đêm (22:00-06:00)
}
```

**Giá trị `daysOfWeek`:**

- `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

##### D. Product Operators (Kiểm tra sản phẩm)

| Operator              | Mô Tả          | Params                              | Khi Nào Dùng        |
|-----------------------|----------------|-------------------------------------|---------------------|
| `product.in_category` | Thuộc category | `{"categories": ["FOOD", "DRINK"]}` | Promo theo danh mục |
| `product.in_brand`    | Thuộc brand    | `{"brands": ["NIKE", "ADIDAS"]}`    | Promo theo brand    |
| `product.sku.in`      | SKU cụ thể     | `{"skus": ["SKU001", "SKU002"]}`    | Promo SKU cụ thể    |

---

#### 4.3.5 Reason Codes (`reason_code`)

**Mã lỗi trả về khi điều kiện KHÔNG thỏa mãn**

| Reason Code              | Mô Tả                       | Dùng Với Operator               |
|--------------------------|-----------------------------|---------------------------------|
| `ORDER_TOTAL_MIN`        | Đơn hàng chưa đạt tối thiểu | `order.total.gte`               |
| `ORDER_TOTAL_MAX`        | Đơn hàng vượt tối đa        | `order.total.lte`               |
| `AUDIENCE_SEGMENT`       | Không thuộc segment         | `customer.in_segment`           |
| `CUSTOMER_BLACKLIST`     | Khách trong blacklist       | `customer.not_in_segment`       |
| `NOT_NEW_CUSTOMER`       | Không phải khách mới        | `customer.is_new`               |
| `TIME_WINDOW`            | Ngoài khung giờ             | `time.window.active`            |
| `NOT_WEEKEND`            | Không phải cuối tuần        | `time.day.in`                   |
| `PRODUCT_NOT_APPLICABLE` | Sản phẩm không áp dụng      | `order.item.product.applicable` |
| `USAGE_LIMIT_EXCEEDED`   | Vượt giới hạn sử dụng       | Usage limits                    |

**⚠️ Có thể tự định nghĩa reason_code mới**, miễn là:

- Viết UPPER_CASE
- Không có dấu cách
- Mô tả rõ ràng lý do fail

---

#### 4.3.6 Bảng `rule_usage_limits` - Giải Thích

| Cột              | Mô Tả                        | Giá Trị                                   | Ví Dụ                        |
|------------------|------------------------------|-------------------------------------------|------------------------------|
| `per_code_total` | Tổng số lần sử dụng của rule | Số nguyên > 0, hoặc NULL (không giới hạn) | `1000` = tối đa 1000 lần     |
| `per_customer`   | Số lần mỗi khách được dùng   | Số nguyên > 0, hoặc NULL                  | `3` = mỗi khách tối đa 3 lần |
| `per_day`        | Số lần sử dụng mỗi ngày      | Số nguyên > 0, hoặc NULL                  | `200` = tối đa 200 lần/ngày  |

**Ví dụ:**

```sql
-- Rule chỉ cho 1000 người đầu tiên, mỗi người 1 lần
per_code_total = 1000
per_customer = 1
per_day = NULL  -- không giới hạn theo ngày

-- Flash sale 100 suất mỗi ngày, không giới hạn tổng
per_code_total = NULL
per_customer = 1
per_day = 100
```

---

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

### 4.5 SQL Template - Tạo Rule Mới

#### Template Chuẩn (Copy và thay thế các giá trị)

```sql
-- ============================================================
-- VALIDATION RULE: [TÊN RULE]
-- Mô tả: [MÔ TẢ NGẮN]
-- Ngày tạo: [NGÀY]
-- Người tạo: [TÊN]
-- ============================================================

-- Bắt đầu transaction
START TRANSACTION;

-- BƯỚC 1: Insert validation_rules
INSERT INTO validation_rules (
    id, code, name, state, rule_version, logic, dsl,
    created_at, updated_at, created_by, updated_by, version
) VALUES (
    '[UUID-RULE]',           -- id: UUID duy nhất
    '[RULE_CODE]',           -- code: mã rule (unique)
    '[Rule Display Name]',   -- name: tên hiển thị
    'DRAFT',                 -- state: luôn bắt đầu với DRAFT
    1,                       -- rule_version
    'ALL',                   -- logic: ALL, ANY, hoặc NONE
    '{}',                    -- dsl: để trống
    NOW(),                   -- created_at
    NOW(),                   -- updated_at
    'admin',                 -- created_by
    'admin',                 -- updated_by
    0                        -- version (optimistic locking)
);

-- BƯỚC 2: Insert ROOT GROUP node
INSERT INTO rule_nodes (
    id, node_id, type, group_logic, children_ids, node_order,
    validation_rule_id, parent_id, created_at, updated_at, version
) VALUES (
    '[UUID-ROOT-NODE]',      -- id: UUID duy nhất
    'n1',                    -- node_id: identifier ngắn
    'GROUP',                 -- type: GROUP cho root
    'ALL',                   -- group_logic: ALL, ANY, NONE
    '["n2","n3"]',           -- children_ids: JSON array các node_id con
    0,                       -- node_order
    '[UUID-RULE]',           -- validation_rule_id: FK đến rule
    NULL,                    -- parent_id: NULL cho root
    NOW(), NOW(), 0
);

-- BƯỚC 3: Insert COND nodes
-- Condition 1
INSERT INTO rule_nodes (
    id, node_id, type, operator_name, params, reason_code,
    validation_rule_id, parent_id, node_order, created_at, updated_at, version
) VALUES (
    '[UUID-COND-1]',         -- id
    'n2',                    -- node_id: phải khớp với children_ids ở trên
    'COND',                  -- type
    'order.total.gte',       -- operator_name
    '{"amount":500000,"currency":"VND"}',  -- params: JSON
    'ORDER_TOTAL_MIN',       -- reason_code: mã lỗi khi fail
    '[UUID-RULE]',           -- validation_rule_id
    '[UUID-ROOT-NODE]',      -- parent_id: trỏ đến root
    0,                       -- node_order
    NOW(), NOW(), 0
);

-- Condition 2
INSERT INTO rule_nodes (
    id, node_id, type, operator_name, params, reason_code,
    validation_rule_id, parent_id, node_order, created_at, updated_at, version
) VALUES (
    '[UUID-COND-2]',
    'n3',
    'COND',
    'customer.in_segment',
    '{"segments":["VIP","GOLD"]}',
    'AUDIENCE_SEGMENT',
    '[UUID-RULE]',
    '[UUID-ROOT-NODE]',
    1,
    NOW(), NOW(), 0
);

-- BƯỚC 4 (Tùy chọn): Insert usage limits
INSERT INTO rule_usage_limits (
    id, validation_rule_id, per_code_total, per_customer, per_day,
    created_at, updated_at
) VALUES (
    '[UUID-LIMITS]',
    '[UUID-RULE]',
    1000,    -- per_code_total: tổng số lần sử dụng
    3,       -- per_customer: số lần/khách hàng
    200,     -- per_day: số lần/ngày
    NOW(), NOW()
);

-- Commit transaction
COMMIT;
```

### 4.6 Ví Dụ 1: Rule Đơn Giản - "VIP Segment Only"

**Yêu cầu:** Chỉ khách hàng VIP mới được áp dụng

```sql
-- ============================================================
-- VALIDATION RULE: VIP_SEGMENT_ONLY
-- Mô tả: Chỉ cho phép khách hàng thuộc segment VIP
-- ============================================================

START TRANSACTION;

-- Insert rule
INSERT INTO validation_rules (
    id, code, name, state, rule_version, logic, dsl,
    created_at, updated_at, created_by, updated_by, version
) VALUES (
    '01932b6f-0005-7000-8000-000000000010',
    'VIP_SEGMENT_ONLY',
    'VIP Segment Only Validation',
    'DRAFT',
    1,
    'ALL',
    '{}',
    NOW(), NOW(), 'admin', 'admin', 0
);

-- Insert ROOT GROUP node
INSERT INTO rule_nodes (
    id, node_id, type, group_logic, children_ids, node_order,
    validation_rule_id, parent_id, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000010',
    'n1',
    'GROUP',
    'ALL',
    '["n2"]',
    0,
    '01932b6f-0005-7000-8000-000000000010',
    NULL,
    NOW(), NOW(), 0
);

-- Insert COND node: VIP segment check
INSERT INTO rule_nodes (
    id, node_id, type, operator_name, params, reason_code,
    validation_rule_id, parent_id, node_order, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000011',
    'n2',
    'COND',
    'customer.in_segment',
    '{"segments":["VIP"]}',
    'AUDIENCE_SEGMENT',
    '01932b6f-0005-7000-8000-000000000010',
    '01932b6f-0006-7000-8000-000000000010',
    0,
    NOW(), NOW(), 0
);

COMMIT;

-- Verify
SELECT * FROM validation_rules WHERE code = 'VIP_SEGMENT_ONLY';
SELECT * FROM rule_nodes WHERE validation_rule_id = '01932b6f-0005-7000-8000-000000000010';
```

### 4.7 Ví Dụ 2: Rule Nhiều Điều Kiện - "Weekend VIP 500k"

**Yêu cầu:** Khách VIP + Đơn hàng >= 500k + Cuối tuần

```sql
-- ============================================================
-- VALIDATION RULE: WEEKEND_VIP_500K
-- Mô tả: VIP + Đơn >= 500k + Cuối tuần
-- Structure:
--   ROOT (ALL)
--     ├── n2: order.total.gte (500k)
--     ├── n3: customer.in_segment (VIP)
--     └── n4: time.window.active (weekend)
-- ============================================================

START TRANSACTION;

-- Insert rule
INSERT INTO validation_rules (
    id, code, name, state, rule_version, logic, dsl,
    created_at, updated_at, created_by, updated_by, version
) VALUES (
    '01932b6f-0005-7000-8000-000000000001',
    'WEEKEND_VIP_500K',
    'Weekend VIP ≥500k, HCM express',
    'DRAFT',
    1,
    'ALL',
    '{}',
    NOW(), NOW(), 'admin', 'admin', 0
);

-- Insert ROOT GROUP node
INSERT INTO rule_nodes (
    id, node_id, type, group_logic, children_ids, node_order,
    validation_rule_id, parent_id, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000001',
    'n1',
    'GROUP',
    'ALL',
    '["n2","n3","n4"]',
    0,
    '01932b6f-0005-7000-8000-000000000001',
    NULL,
    NOW(), NOW(), 0
);

-- COND 1: Order >= 500k
INSERT INTO rule_nodes (
    id, node_id, type, operator_name, params, reason_code,
    validation_rule_id, parent_id, node_order, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000002',
    'n2',
    'COND',
    'order.total.gte',
    '{"amount":500000,"currency":"VND"}',
    'ORDER_TOTAL_MIN',
    '01932b6f-0005-7000-8000-000000000001',
    '01932b6f-0006-7000-8000-000000000001',
    0,
    NOW(), NOW(), 0
);

-- COND 2: VIP segment
INSERT INTO rule_nodes (
    id, node_id, type, operator_name, params, reason_code,
    validation_rule_id, parent_id, node_order, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000003',
    'n3',
    'COND',
    'customer.in_segment',
    '{"segments":["VIP"]}',
    'AUDIENCE_SEGMENT',
    '01932b6f-0005-7000-8000-000000000001',
    '01932b6f-0006-7000-8000-000000000001',
    1,
    NOW(), NOW(), 0
);

-- COND 3: Weekend only
INSERT INTO rule_nodes (
    id, node_id, type, operator_name, params, reason_code,
    validation_rule_id, parent_id, node_order, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000004',
    'n4',
    'COND',
    'time.window.active',
    '{"startTime":"00:00:00","endTime":"23:59:59","timezone":"Asia/Bangkok","daysOfWeek":["SATURDAY","SUNDAY"],"spansMidnight":false}',
    'TIME_WINDOW',
    '01932b6f-0005-7000-8000-000000000001',
    '01932b6f-0006-7000-8000-000000000001',
    2,
    NOW(), NOW(), 0
);

-- Usage limits
INSERT INTO rule_usage_limits (
    id, validation_rule_id, per_code_total, per_customer, per_day,
    created_at, updated_at
) VALUES (
    '01932b6f-0007-7000-8000-000000000001',
    '01932b6f-0005-7000-8000-000000000001',
    1000,
    3,
    200,
    NOW(), NOW()
);

COMMIT;
```

### 4.8 Ví Dụ 3: Rule Phức Tạp với Nested Groups

**Yêu cầu:** Khách VIP + (Đơn >= 500k HOẶC là khách mới)

```sql
-- ============================================================
-- VALIDATION RULE: VIP_HIGH_VALUE_OR_NEW
-- Mô tả: VIP + (Đơn >= 500k HOẶC khách mới)
-- Structure:
--   ROOT (ALL)
--     ├── n2: customer.in_segment (VIP)
--     └── n3: GROUP (ANY)
--             ├── n4: order.total.gte (500k)
--             └── n5: customer.is_new
-- ============================================================

START TRANSACTION;

-- Insert rule
INSERT INTO validation_rules (
    id, code, name, state, rule_version, logic, dsl,
    created_at, updated_at, created_by, updated_by, version
) VALUES (
    '01932b6f-0005-7000-8000-000000000020',
    'VIP_HIGH_VALUE_OR_NEW',
    'VIP High Value or New Customer',
    'DRAFT',
    1,
    'ALL',
    '{}',
    NOW(), NOW(), 'admin', 'admin', 0
);

-- ROOT node (n1) - ALL
INSERT INTO rule_nodes (
    id, node_id, type, group_logic, children_ids, node_order,
    validation_rule_id, parent_id, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000020',
    'n1',
    'GROUP',
    'ALL',
    '["n2","n3"]',
    0,
    '01932b6f-0005-7000-8000-000000000020',
    NULL,
    NOW(), NOW(), 0
);

-- COND node (n2) - VIP segment
INSERT INTO rule_nodes (
    id, node_id, type, operator_name, params, reason_code,
    validation_rule_id, parent_id, node_order, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000021',
    'n2',
    'COND',
    'customer.in_segment',
    '{"segments":["VIP"]}',
    'AUDIENCE_SEGMENT',
    '01932b6f-0005-7000-8000-000000000020',
    '01932b6f-0006-7000-8000-000000000020',
    0,
    NOW(), NOW(), 0
);

-- Nested GROUP node (n3) - ANY
INSERT INTO rule_nodes (
    id, node_id, type, group_logic, children_ids, node_order,
    validation_rule_id, parent_id, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000022',
    'n3',
    'GROUP',
    'ANY',
    '["n4","n5"]',
    1,
    '01932b6f-0005-7000-8000-000000000020',
    '01932b6f-0006-7000-8000-000000000020',
    NOW(), NOW(), 0
);

-- COND node (n4) - Order >= 500k
INSERT INTO rule_nodes (
    id, node_id, type, operator_name, params, reason_code,
    validation_rule_id, parent_id, node_order, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000023',
    'n4',
    'COND',
    'order.total.gte',
    '{"amount":500000,"currency":"VND"}',
    'ORDER_TOTAL_MIN',
    '01932b6f-0005-7000-8000-000000000020',
    '01932b6f-0006-7000-8000-000000000022',
    0,
    NOW(), NOW(), 0
);

-- COND node (n5) - New customer
INSERT INTO rule_nodes (
    id, node_id, type, operator_name, params, reason_code,
    validation_rule_id, parent_id, node_order, created_at, updated_at, version
) VALUES (
    '01932b6f-0006-7000-8000-000000000024',
    'n5',
    'COND',
    'customer.is_new',
    '{}',
    'NOT_NEW_CUSTOMER',
    '01932b6f-0005-7000-8000-000000000020',
    '01932b6f-0006-7000-8000-000000000022',
    1,
    NOW(), NOW(), 0
);

COMMIT;
```

### 4.9 Generate UUID

**Cách 1: Online Generator**

- https://www.uuidtools.com/v7
- https://www.uuidgenerator.net/version7

**Cách 2: SQL trong MariaDB**

```sql
SELECT UUID() as new_uuid;
```

**Cách 3: Convention cho validation module**

```
Base prefix: 01932b6f-XXXX-7000-8000-XXXXXXXXXXXX

validation_rules:  01932b6f-0005-7000-8000-00000000XXXX
rule_nodes:        01932b6f-0006-7000-8000-00000000XXXX
rule_usage_limits: 01932b6f-0007-7000-8000-00000000XXXX

Tăng số cuối: 0001, 0002, 0003, ...
```

### 4.10 Checklist Khai Báo Rule

| #  | Task                                           | Check |
|----|------------------------------------------------|-------|
| 1  | Generate UUID cho validation_rules             | ☐     |
| 2  | Generate UUID cho mỗi rule_node                | ☐     |
| 3  | Code rule phải unique (check trước khi insert) | ☐     |
| 4  | ROOT node không có parent_id (NULL)            | ☐     |
| 5  | COND nodes có parent_id trỏ đến GROUP          | ☐     |
| 6  | children_ids chứa node_id (không phải id)      | ☐     |
| 7  | parent_id chứa id (không phải node_id)         | ☐     |
| 8  | JSON params đúng format                        | ☐     |
| 9  | state = 'DRAFT' cho rule mới                   | ☐     |
| 10 | Chạy COMMIT sau khi insert                     | ☐     |

### 4.11 Query Kiểm Tra Sau Khi Insert

```sql
-- Kiểm tra rule đã tạo
SELECT id, code, name, state, logic, rule_version
FROM validation_rules
WHERE code = 'YOUR_RULE_CODE';

-- Kiểm tra tất cả nodes của rule
SELECT
    n.node_id,
    n.type,
    COALESCE(n.group_logic, n.operator_name) as logic_or_operator,
    n.params,
    n.reason_code,
    p.node_id as parent_node_id
FROM rule_nodes n
LEFT JOIN rule_nodes p ON n.parent_id = p.id
WHERE n.validation_rule_id = 'YOUR-RULE-UUID'
ORDER BY n.node_order;

-- Kiểm tra tree structure (hiển thị dạng cây)
SELECT
    CASE
        WHEN n.parent_id IS NULL THEN n.node_id
        ELSE CONCAT('  └── ', n.node_id)
    END as tree_view,
    n.type,
    COALESCE(n.group_logic, n.operator_name) as logic_or_operator
FROM rule_nodes n
WHERE n.validation_rule_id = 'YOUR-RULE-UUID'
ORDER BY n.parent_id IS NOT NULL, n.node_order;

-- Kiểm tra usage limits
SELECT * FROM rule_usage_limits
WHERE validation_rule_id = 'YOUR-RULE-UUID';
```

### 4.12 Rollback Nếu Có Lỗi

```sql
-- Nếu chưa COMMIT và cần rollback
ROLLBACK;

-- Nếu đã COMMIT và cần xóa rule
START TRANSACTION;

-- Xóa usage limits
DELETE FROM rule_usage_limits WHERE validation_rule_id = 'YOUR-RULE-UUID';

-- Xóa nodes
DELETE FROM rule_nodes WHERE validation_rule_id = 'YOUR-RULE-UUID';

-- Xóa rule
DELETE FROM validation_rules WHERE id = 'YOUR-RULE-UUID';

COMMIT;
```

---

## 4B. Khai Báo Rule Qua REST API (Alternative)

Ngoài cách insert trực tiếp database, có thể dùng REST API (dành cho dev/test):

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

| Method | Endpoint                      | Mô Tả                     |
|--------|-------------------------------|---------------------------|
| POST   | `/v1/rules`                   | Tạo rule mới              |
| GET    | `/v1/rules/{ruleId}`          | Lấy rule theo ID          |
| GET    | `/v1/rules`                   | List rules với pagination |
| PATCH  | `/v1/rules/{ruleId}`          | Update rule (chỉ DRAFT)   |
| POST   | `/v1/rules/{ruleId}:activate` | Activate rule             |
| POST   | `/v1/rules/{ruleId}:archive`  | Archive rule              |
| POST   | `/v1/rules/{ruleId}:clone`    | Clone rule                |
| POST   | `/v1/rules:lint`              | Validate rule structure   |

### 9.2 Rule Publishing

| Method | Endpoint                                       | Mô Tả                   |
|--------|------------------------------------------------|-------------------------|
| POST   | `/v1/rules/publishing/rules/{ruleId}`          | Publish rule            |
| POST   | `/v1/rules/publishing/batch`                   | Batch publish           |
| DELETE | `/v1/rules/publishing/rules/{ruleId}`          | Unpublish rule          |
| GET    | `/v1/rules/publishing/rules/{ruleId}/status`   | Get deployment status   |
| POST   | `/v1/rules/publishing/rules/{ruleId}/validate` | Validate for publishing |

### 9.3 Rule Simulation

| Method | Endpoint                            | Mô Tả                 |
|--------|-------------------------------------|-----------------------|
| POST   | `/v1/rules/{ruleId}/simulate`       | Test rule với context |
| POST   | `/v1/rules/{ruleId}/simulate:batch` | Batch test cases      |

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
