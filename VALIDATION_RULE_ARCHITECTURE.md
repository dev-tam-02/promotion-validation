# Validation Module - Rule Architecture

## Tổng quan

Module validation quản lý validation rules theo kiến trúc **Rule Tree + Operator + External Resource**, cho phép xây
dựng các rule phức tạp với khả năng gọi external services để fetch data động.

## ✅ Product Applicability - Operator Approach (Updated)

Product applicability (include/exclude products) được quản lý thông qua operator `product.applicability.in`, tương tự
như `customer.segment.in`. Đây là cách tiếp cận nhất quán và mở rộng tốt.

### Operator: product.applicability.in

```json
{
  "_id": "operator-product-applicability",
  "name": "product.applicability.in",
  "version": 1,
  "context": "order",
  "status": "ACTIVE",
  "jsonSchema": {
    "type": "object",
    "properties": {
      "included": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "object": {"type": "string", "enum": ["PRODUCT", "COLLECTION", "SKU"]},
            "id": {"type": "string"},
            "effect": {"type": "string", "enum": ["APPLY_TO_EVERY", "APPLY_TO_CHEAPEST", "APPLY_TO_MOST_EXPENSIVE"]},
            "target": {"type": "string", "enum": ["ITEM", "ORDER", "CUSTOMER"]},
            "skipInitially": {"type": "integer", "default": 0},
            "repeat": {"type": "integer", "default": 1}
          },
          "required": ["object", "id"]
        }
      },
      "excluded": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "object": {"type": "string"},
            "id": {"type": "string"}
          }
        }
      },
      "includedAll": {"type": "boolean", "default": false}
    }
  }
}
```

### Example Usage

```json
{
  "type": "COND",
  "operatorName": "product.applicability.in",
  "params": {
    "included": [
      {
        "object": "PRODUCT",
        "id": "PRODUCT-001",
        "effect": "APPLY_TO_EVERY",
        "target": "ITEM",
        "skipInitially": 0,
        "repeat": 1
      }
    ],
    "excluded": [
      {"object": "PRODUCT", "id": "PRODUCT-999"}
    ],
    "includedAll": false
  }
}
```

### Assignment.subject Format (CORRECTED)

**✅ CORRECT:**

```java
Assignment.subject {
  type: "campaign"           // Always "campaign"
  key: "<campaign-id>"       // From command.subject
}
```

**❌ INCORRECT (Deprecated):**

```java
Assignment.subject {
  type: "product"            // WRONG! Do not use
  key: "<product-id>"        // WRONG! Product info goes in rule tree
}
```

**Migration Note:** `CommandMappingService.createSubjectFromApplicableTo()` has been deprecated. Always extract campaign
ID from `command.subject`.

## Kiến trúc Core

### 1. Rule Structure (Rule Tree)

```java
Rule {
  id: String
  code: String                    // VIP_WEEKEND_500K
  name: String
  state: RuleState               // DRAFT, PUBLISHED, ARCHIVED
  logic: LogicType               // ALL, ANY, NONE
  limits: Map<String, Object>    // perCodeTotal, perCustomer, perDay
  nodes: List<RuleNode>          // Tree structure
}
```

### 2. RuleNode (Tree Node)

```java
RuleNode {
  id: String
  type: NodeType                 // GROUP or COND

  // For GROUP nodes
  groupLogic: LogicType         // ALL, ANY, NONE
  children: List<RuleNode>      // Nested nodes

  // For COND (condition) nodes
  operatorName: String          // "customer.segment.in"
  params: Map<String, Object>   // Operator parameters
  reasonCode: String            // Error code nếu fail
}
```

### 3. Operator (Pluggable Operators)

```java
Operator {
  id: String
  name: String                  // "customer.segment.in"
  version: Integer
  context: String               // "customer", "order", "time"
  jsonSchema: Map               // Parameter validation schema
  compilerId: String            // Link to validation-engine
  status: OperatorStatus        // ACTIVE, DEPRECATED
}
```

### 4. Resource (External Service Integration)

```java
Resource {
  id: String
  label: String                 // "Segment Service"
  endpoint: String              // "http://segment-service/api/segments"
  authMethod: String            // "JWT", "BASIC", "APIKEY"
  fieldKey: String              // Field name to extract
  fieldValue: String            // Field value to extract
  timeoutSeconds: Integer
}
```

### 5. OperatorResource (Linking)

```java
OperatorResource {
  id: String
  operatorName: String          // "customer.segment.in"
  operatorVersion: Integer
  resourceId: String            // Link to Resource
}
```

## Example: Segment Rule

### Rule Definition

```json
{
  "_id": "rule-001",
  "code": "VIP_WEEKEND_500K",
  "name": "VIP Weekend 500K Rule",
  "state": "PUBLISHED",
  "logic": "ALL",
  "limits": {
    "perCodeTotal": 1000,
    "perCustomer": 1,
    "perDay": 100
  },
  "nodes": [
    {
      "id": "n1",
      "type": "GROUP",
      "groupLogic": "ALL",
      "children": [
        {
          "id": "n2",
          "type": "COND",
          "operatorName": "customer.segment.in",
          "params": {
            "segments": ["VIP"]
          },
          "reasonCode": "CUSTOMER_SEGMENT_VIP"
        },
        {
          "id": "n3",
          "type": "COND",
          "operatorName": "order.amount.gte",
          "params": {
            "amount": 500000,
            "currency": "VND"
          },
          "reasonCode": "ORDER_AMOUNT_MIN"
        },
        {
          "id": "n4",
          "type": "COND",
          "operatorName": "time.window.active",
          "params": {
            "policyId": "weekend_policy",
            "tz": "Asia/Ho_Chi_Minh"
          },
          "reasonCode": "TIME_WINDOW_WEEKEND"
        }
      ]
    }
  ]
}
```

### Operator: customer.segment.in

```json
{
  "_id": "op-001",
  "name": "customer.segment.in",
  "version": 1,
  "context": "customer",
  "status": "ACTIVE",
  "jsonSchema": {
    "type": "object",
    "properties": {
      "segments": {
        "type": "array",
        "items": { "type": "string" }
      }
    },
    "required": ["segments"]
  }
}
```

### Resource: Segment Service

```json
{
  "_id": "res-001",
  "label": "Segment Service",
  "endpoint": "http://segment-service/api/customers/{customerId}/segments",
  "authMethod": "JWT",
  "fieldKey": "segments",
  "fieldValue": "segmentIds",
  "timeoutSeconds": 3
}
```

### OperatorResource Linking

```json
{
  "_id": "or-001",
  "operatorName": "customer.segment.in",
  "operatorVersion": 1,
  "resourceId": "res-001"
}
```

## Validation Flow

### 1. Fact Resolution (Data Collection)

```
Request {
  customerId: "CUST-001",
  orderId: "ORDER-123"
}
  ↓
FactOrchestrator.resolve()
  ↓
Parallel Fact Resolvers:
  ├─ CustomerResolver → fetch customer data
  ├─ OrderResolver → fetch order data
  ├─ SegmentResolver → fetch segments từ segment service
  └─ LimitsResolver → check usage limits
  ↓
FactPack {
  customer: CustomerFact,
  order: OrderFact,
  segments: SegmentsFact {
    segmentIds: ["VIP", "GOLD"],
    segments: [
      {id: "VIP", name: "VIP Customer", type: "TIER"},
      {id: "GOLD", name: "Gold Member", type: "LOYALTY"}
    ]
  },
  limits: LimitsFact,
  metadata: MetadataFact
}
```

### 2. Rule Assignment Lookup

```
Campaign ID: "CAMPAIGN-ABC"
  ↓
Assignment {
  ruleId: "rule-001",
  subject: {
    type: "campaign",      // ✅ ĐÚNG: campaign/voucher/tier
    key: "CAMPAIGN-ABC"    // ✅ ĐÚNG: campaign ID
  },
  active: true
}
```

**LƯU Ý**: Assignment KHÔNG chứa applicability rules (product include/exclude). Đó là thuộc tính của ValidationRule
configuration.

### 3. Rule Evaluation

```
Rule Tree Evaluation:
  ↓
Node n1 (GROUP, logic=ALL):
  ├─ Node n2 (COND): customer.segment.in(["VIP"])
  │   ↓ Operator evaluates:
  │   FactPack.segments.segmentIds.contains("VIP")
  │   ✅ PASS (customer has VIP segment)
  │
  ├─ Node n3 (COND): order.amount.gte(500000)
  │   ↓ Operator evaluates:
  │   FactPack.order.totalAmount >= 500000
  │   ✅ PASS (order = 600000)
  │
  └─ Node n4 (COND): time.window.active("weekend_policy")
      ↓ Operator evaluates:
      Current time in weekend window?
      ✅ PASS (Saturday 10:00 AM)

Final: ALL children PASS → Rule PASSES
```

### 4. Response

```json
{
  "valid": true,
  "ruleCode": "VIP_WEEKEND_500K",
  "decision": "ALLOW",
  "reasons": [],
  "metadata": {
    "evaluatedAt": "2025-09-30T10:00:00Z",
    "processingTimeMs": 45
  }
}
```

## Fact Pack Architecture

### FactPack Structure

```java
FactPack {
  factPackVersion: "1.0",
  timestamp: Instant,

  // Core Facts
  customer: CustomerFact {
    customerId: String,
    email: String,
    tier: String,              // "VIP", "GOLD", "STANDARD"
    region: String,
    tags: List<String>,
    attributes: Map<String, Object>
  },

  order: OrderFact {
    orderId: String,
    totalAmount: BigDecimal,
    items: List<OrderItemFact>,
    currency: String,
    channel: String
  },

  segments: SegmentsFact {
    segmentIds: List<String>,  // ["VIP", "GOLD", "HIGH_SPENDER"]
    segments: List<SegmentInfo> {
      id: String,
      name: String,
      type: String,            // "TIER", "LOYALTY", "BEHAVIOR"
      score: Double,
      properties: Map<String, Object>
    }
  },

  limits: LimitsFact {
    perCodeTotal: Integer,
    perCustomer: Integer,
    perDay: Integer,
    currentUsage: Map<String, Integer>
  },

  candidate: CandidateFact {
    campaignId: String,
    voucherId: String,
    promotionType: String
  },

  geo: GeoFact {
    country: String,
    city: String,
    coordinates: [lat, lon]
  },

  metadata: MetadataFact {
    source: String,
    requestId: String,
    additionalData: Map
  },

  // Provenance tracking
  provenance: ProvenanceInfo {
    aggregatedAt: Instant,
    aggregationId: String,
    processingTimeMs: Long,
    sources: List<SourceInfo> {
      name: String,
      type: String,
      fetchedAt: Instant,
      responseTimeMs: Long,
      status: String,
      cached: Boolean
    }
  }
}
```

### Fact Resolution với External Services

#### Segment Data Flow

```
1. Request arrives với customerId
   ↓
2. FactOrchestrator.resolve()
   ↓
3. CustomerResolver được gọi
   ├─ HTTP GET: http://customer-service/api/customers/{customerId}/profile
   ├─ Response: {
   │     customerId: "CUST-001",
   │     email: "user@example.com",
   │     tier: "VIP"
   │   }
   └─ Map to CustomerFact
   ↓
4. SEGMENTS được lấy riêng từ Segment Service
   ├─ HTTP GET: http://segment-service/api/customers/{customerId}/segments
   ├─ Response: {
   │     customerId: "CUST-001",
   │     segments: [
   │       {id: "VIP", name: "VIP Customer", type: "TIER"},
   │       {id: "GOLD", name: "Gold Member", type: "LOYALTY"}
   │     ]
   │   }
   └─ Map to SegmentsFact
   ↓
5. Build complete FactPack với all facts
```

**LƯU Ý**:

- Segment data KHÔNG NẰM TRONG CustomerFact
- Segment có FactPack riêng: `SegmentsFact`
- Operator `customer.segment.in` đọc từ `FactPack.segments.segmentIds`

## Assignment vs ApplicabilityScope

### ❌ SAI - Implementation hiện tại

```java
// WRONG: Assignment.subject chứa product ID
Assignment {
  ruleId: "RULE-001",
  subject: {
    type: "product",           // ❌ SAI
    key: "PRODUCT-001"         // ❌ SAI
  }
}
```

### ✅ ĐÚNG - Thiết kế chuẩn

```javascript
// Assignment - Gán rule cho campaign
{
  "_id": "assignment-001",
  "ruleId": "RULE-001",
  "subject": {
    "type": "campaign",          // ✅ ĐÚNG
    "key": "CAMPAIGN-ABC"        // ✅ ĐÚNG: campaign ID
  },
  "active": true,
  "trafficPercent": 100
}

// ValidationRule - Chứa applicability configuration
{
  "_id": "RULE-001",
  "code": "VIP_PRODUCT_RULE",
  "name": "VIP Product Eligibility Rule",
  "configuration": {
    "applicableTo": {              // ✅ ĐÂY MỚI LÀ CHỖ LƯU
      "included": [
        {
          "object": "PRODUCT",
          "id": "PRODUCT-001",
          "effect": "APPLY_TO_EVERY",
          "target": "ITEM"
        }
      ],
      "excluded": [
        {
          "object": "PRODUCT",
          "id": "PRODUCT-999"
        }
      ],
      "includedAll": false
    }
  },
  "nodes": [...]
}
```

### Correct Validation Flow với ApplicabilityScope

```
1. Request: CAMPAIGN-ABC + ORDER with PRODUCT-001
   ↓
2. Lookup Assignment:
   campaign=CAMPAIGN-ABC → ruleId=RULE-001
   ↓
3. Load ValidationRule: RULE-001
   ↓
4. Check applicableTo:
   - Is PRODUCT-001 in included? ✅ YES
   - Is PRODUCT-001 in excluded? ❌ NO
   - Effect = APPLY_TO_EVERY
   ↓
5. Evaluate rule conditions (customer.segment, order.amount, etc.)
   ↓
6. Return validation result
```

## Built-in Operators

### Customer Context

| Operator                       | Description                | Params               | Example                         |
|--------------------------------|----------------------------|----------------------|---------------------------------|
| `customer.segment.in`          | Check customer segment     | `segments: string[]` | `{"segments": ["VIP", "GOLD"]}` |
| `customer.tier.equals`         | Check customer tier        | `tier: string`       | `{"tier": "PREMIUM"}`           |
| `customer.region.in`           | Check customer region      | `regions: string[]`  | `{"regions": ["HN", "HCM"]}`    |
| `customer.age.gte`             | Check customer age         | `age: number`        | `{"age": 18}`                   |
| `customer.registration.within` | Registration within period | `days: number`       | `{"days": 30}`                  |

### Order Context

| Operator                 | Description                   | Params                             | Example                                 |
|--------------------------|-------------------------------|------------------------------------|-----------------------------------------|
| `order.amount.gte`       | Order amount greater or equal | `amount: number, currency: string` | `{"amount": 500000, "currency": "VND"}` |
| `order.amount.between`   | Order amount in range         | `min: number, max: number`         | `{"min": 100000, "max": 1000000}`       |
| `order.item.count.gte`   | Item count greater or equal   | `count: number`                    | `{"count": 3}`                          |
| `order.item.category.in` | Order contains categories     | `categories: string[]`             | `{"categories": ["ELECTRONICS"]}`       |
| `order.channel.equals`   | Order channel                 | `channel: string`                  | `{"channel": "MOBILE"}`                 |

### Time Context

| Operator             | Description       | Params                               | Example                                                |
|----------------------|-------------------|--------------------------------------|--------------------------------------------------------|
| `time.window.active` | Check time window | `policyId: string, tz: string`       | `{"policyId": "weekend", "tz": "Asia/Ho_Chi_Minh"}`    |
| `time.hour.between`  | Check hour range  | `startHour: number, endHour: number` | `{"startHour": 9, "endHour": 18}`                      |
| `time.day.in`        | Check day of week | `days: number[]`                     | `{"days": [1,2,3,4,5]}`                                |
| `time.date.between`  | Check date range  | `startDate: string, endDate: string` | `{"startDate": "2025-01-01", "endDate": "2025-12-31"}` |

### Limit Context

| Operator                     | Description            | Params                         | Example                                  |
|------------------------------|------------------------|--------------------------------|------------------------------------------|
| `limit.usage.lt`             | Check usage limit      | `limit: number, scope: string` | `{"limit": 100, "scope": "perCustomer"}` |
| `limit.budget.remaining.gte` | Check remaining budget | `amount: number`               | `{"amount": 10000}`                      |
| `limit.concurrent.lt`        | Concurrent usage check | `limit: number`                | `{"limit": 1000}`                        |

## MongoDB Collections

### Collection: `rules`

```javascript
{
  "_id": "rule-001",
  "tenantId": "DEFAULT",
  "code": "VIP_WEEKEND_500K",
  "name": "VIP Weekend 500K Rule",
  "state": "PUBLISHED",           // DRAFT, PUBLISHED, ARCHIVED
  "latestVersion": 1,
  "logic": "ALL",                  // ALL, ANY, NONE
  "limits": {
    "perCodeTotal": 1000,
    "perCustomer": 1,
    "perDay": 100
  },
  "nodes": [...],                  // Rule tree
  "notes": "Description",
  "createdAt": ISODate("..."),
  "createdBy": "system",
  "updatedAt": ISODate("..."),
  "updatedBy": "system"
}
```

### Collection: `assignments`

```javascript
{
  "_id": "assignment-001",
  "tenantId": "DEFAULT",
  "ruleId": "rule-001",
  "subject": {
    "type": "campaign",            // campaign/voucher/tier/reward
    "key": "CAMPAIGN-ABC"          // campaign ID
  },
  "ruleVersionPinned": 1,          // null = use latest
  "assignmentVersion": 1,
  "active": true,
  "validFrom": ISODate("..."),
  "validTo": ISODate("..."),
  "trafficPercent": 100,           // Gradual rollout
  "stickyKeyStrategy": "CUSTOMER_ID",
  "createdAt": ISODate("..."),
  "updatedAt": ISODate("...")
}
```

### Collection: `operators`

```javascript
{
  "_id": "op-001",
  "tenantId": "DEFAULT",
  "name": "customer.segment.in",
  "version": 1,
  "context": "customer",
  "jsonSchema": {
    "type": "object",
    "properties": {
      "segments": {
        "type": "array",
        "items": {"type": "string"}
      }
    },
    "required": ["segments"]
  },
  "compilerId": "compiler-id",
  "status": "ACTIVE",
  "createdAt": ISODate("..."),
  "updatedAt": ISODate("...")
}
```

### Collection: `resources`

```javascript
{
  "_id": "res-001",
  "tenantId": "DEFAULT",
  "label": "Segment Service",
  "endpoint": "http://segment-service/api/customers/{customerId}/segments",
  "authMethod": "JWT",
  "fieldKey": "segments",
  "fieldValue": "segmentIds",
  "timeoutSeconds": 3,
  "createdAt": ISODate("...")
}
```

### Collection: `operator_resources`

```javascript
{
  "_id": "or-001",
  "tenantId": "DEFAULT",
  "operatorName": "customer.segment.in",
  "operatorVersion": 1,
  "resourceId": "res-001"
}
```

## Code References

### Core Entities

- `Rule.java` - Rule domain entity với tree structure
- `Assignment.java` - Rule assignment to campaigns
- `Operator.java` - Operator registry
- `Resource.java` - External resource configuration
- `OperatorResource.java` - Operator-Resource linking

### Fact Resolution

- `FactOrchestrator.java` - Orchestrates fact collection
- `FactPack.java` - Complete fact bundle
- `CustomerResolver.java` - Resolves customer facts
- `OrderResolver.java` - Resolves order facts
- `SegmentsFact.java` - Segment data structure
- `CustomerFact.java` - Customer data structure

### Validation Flow

- `SettingValidationRuleCommandHandler.java` - Handles rule assignment
- `CommandMappingService.java` - Maps commands to entities (⚠️ CẦN FIX)
- `ValidationEngineAdapter.java` - Calls validation-engine for evaluation

### Sample Data

- `ChangeLogs004Rules.java` - Sample rule definitions
- `ChangeLogs001Operators.java` - Sample operator definitions

## Recommendations

### 1. Fix Assignment.subject Usage

- ❌ Remove product ID từ `Assignment.subject`
- ✅ Chỉ lưu campaign ID vào `Assignment.subject.key`
- ✅ Move `applicableTo` vào `ValidationRule.configuration`

### 2. Add Configuration Field to ValidationRule

```java
@Field("configuration")
private Map<String, Object> configuration;
```

Chứa:

- `applicableTo`: ApplicabilityScope (product include/exclude)
- `additionalConstraints`: Custom constraints
- `metadata`: Rule-specific metadata

### 3. Create Dedicated SegmentResolver

- Implement `SegmentResolver extends AbstractFactResolver<SegmentsFact>`
- Fetch segments từ segment service
- Cache kết quả với TTL

### 4. Document Operator Development Guide

- How to register new operators
- How to link operators to external resources
- How to write operator JSON schemas
- How to test operators

## Summary

**Validation Module Architecture**:

1. ✅ **Rule Tree**: Flexible hierarchical rule structure
2. ✅ **Operator Registry**: Pluggable condition evaluators
3. ✅ **External Resources**: Dynamic data fetching từ external services
4. ✅ **Fact Pack**: Comprehensive context data bundle
5. ❌ **Assignment Usage**: CẦN FIX - đang lưu sai mục đích

**Key Insight**:

- Assignment = "Rule này được dùng bởi campaign nào?"
- ApplicabilityScope = "Rule này áp dụng cho products nào?"
- SegmentFact = "Customer này thuộc segments nào?" (fetched từ external service)