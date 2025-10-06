# validation

Absolutely—here’s a MongoDB design that mirrors the “builder → publish → bundle” model (Voucherify-style), including
time windows. I’ll show: collections, suggested schemas, indexes, sample docs, and a few operational tips. (English
descriptions as requested.)

---

# Collections Overview

1. `operator_registry` — catalog of operators (contexts + param schemas + compiler hooks).
2. `validation_rules` — rule header + **embedded rule tree** (nodes) + optional time links.
3. `rule_assignments` — attach a rule to a subject (voucher/campaign/tier…).
4. `rule_bundles` — **immutable compiled artifacts** (Drools KieModule bytes).
5. `metadata_schemas` — JSON Schemas for custom metadata (customer/order/item…).
6. `resources` — external data sources (for selector options).
7. `operator_resources` — which operator uses which resource (optional; can be embedded).
8. **Time modeling** (choose one, or run both during migration):

    * A) `time_frames`, `validity_day_of_weeks`, `time_exceptions`, `rule_time_frames`
    * B) `temporal_policies`, `rule_temporal_links` (RRULE-style)

> In MongoDB, prefer **embedding** when data is read together and has bounded growth (e.g., rule nodes), and *
*referencing** when reused across many parents (e.g., a time frame used by many rules).

---

# 1) `operator_registry`

**Purpose (EN):** Defines supported operators for the builder: their context, parameter JSON Schema, and how the
compiler maps them to Drools (compiler template ID).

**Document (sample):**

```json
{
  "_id": "order.total.gte",
  "context": "order",                          // "order" | "customer" | "time" | "metadata" | ...
  "jsonSchema": {                              // JSON Schema for params
    "type": "object",
    "properties": {
      "amount": { "type": "number" },
      "currency": { "type": "string" }
    },
    "required": ["amount"]
  },
  "compilerId": "tpl_order_total_gte_v1",
  "createdAt": { "$date": "2025-09-19T10:00:00Z" },
  "updatedAt": { "$date": "2025-09-19T10:00:00Z" }
}
```

**Indexes:**

* `_id` (natural); add `{ context: 1 }` for filtering by context.

---

# 2) `validation_rules` (with embedded rule tree)

**Purpose (EN):** Stores rule header (state/version/limits) and a **tree of nodes** (GROUP/COND) that the builder edits;
published later to bundles.

**Document (sample):**

```json
{
  "_id": "vr_abc123",
  "code": "WEEKEND_VIP_500K",
  "name": "Weekend VIP ≥500k, HCM express",
  "state": "draft",                             // "draft" | "published" | "archived"
  "version": 3,
  "logic": "ALL",                               // root logic for implicit top-level
  "limits": {                                   // optional; redemption-service enforces
    "perCodeTotal": 1000,
    "perCustomer": 3,
    "perDay": 200
  },
  "dsl": { /* optional raw DSL snapshot for audit */ },

  "nodes": [                                    // embedded rule tree (adjacency list in doc)
    {
      "id": "n1",
      "type": "GROUP",
      "groupLogic": "ALL",
      "children": ["n2","n3","n4"],
      "order": 0
    },
    {
      "id": "n2",
      "type": "COND",
      "operatorName": "time.window.active",
      "params": { "policyId": "tp_weekend_morning", "tz": "Asia/Bangkok" },
      "reasonCode": "TIME_WINDOW"
    },
    {
      "id": "n3",
      "type": "COND",
      "operatorName": "order.total.gte",
      "params": { "amount": 500000, "currency": "VND" },
      "reasonCode": "ORDER_TOTAL_MIN"
    },
    {
      "id": "n4",
      "type": "GROUP",
      "groupLogic": "ALL",
      "children": ["n5","n6"],
      "order": 1
    },
    {
      "id": "n5",
      "type": "COND",
      "operatorName": "customer.in_segment",
      "params": { "segments": ["VIP"] },
      "reasonCode": "AUDIENCE_SEGMENT"
    },
    {
      "id": "n6",
      "type": "COND",
      "operatorName": "metadata.match.equals",
      "params": { "path": "order.shipping.method", "value": "EXPRESS" },
      "reasonCode": "META_MATCH"
    }
  ],

  "publishedAt": null,
  "publishedBy": null,

  "createdAt": { "$date": "2025-09-19T10:00:00Z" },
  "createdBy": "admin",
  "updatedAt": { "$date": "2025-09-19T10:00:00Z" },
  "updatedBy": "admin"
}
```

**Notes:**

* Use **embedded nodes** for fast read by rules-service during compile.
* Node ordering uses a simple `order` field; parent/child via IDs.
* If the tree could become very large (hundreds of nodes), you can split into a `validation_rule_nodes` collection keyed
  by `ruleId`, but most promotion trees stay moderate.

**Indexes:**

* `{ code: 1 }` unique.
* `{ state: 1, version: -1 }` for admin listings.

---

# 3) `rule_assignments`

**Purpose (EN):** Attaches rules to “subjects” (voucher/campaign/tier/reward). Supports activation windows and canary
rollout.

**Document (sample):**

```json
{
  "_id": "as_001",
  "ruleId": "vr_abc123",
  "subject": { "type": "voucher", "key": "SAVE20" },  // or {type:"campaign", key:"cmp_123"}
  "assignmentVersion": 4,
  "active": true,
  "validFrom": { "$date": "2025-09-20T00:00:00Z" },
  "validTo":   { "$date": "2025-12-31T23:59:59Z" },
  "trafficPercent": 100,

  "createdAt": { "$date": "2025-09-19T10:00:00Z" },
  "updatedAt": { "$date": "2025-09-19T10:00:00Z" }
}
```

**Indexes:**

* `{ "subject.type": 1, "subject.key": 1, "active": 1 }`
* `{ ruleId: 1, assignmentVersion: -1 }`

---

# 4) `rule_bundles` (immutable)

**Purpose (EN):** Stores compiled Drools KieModule artifact bytes keyed by subject and versions. **Do not update**;
always append new bundle.

**Document (sample):**

```json
{
  "_id": "rb_9f4b",
  "bundleHash": "sha256:5b8f...e1",                  // immutable key
  "subject": { "type": "voucher", "key": "SAVE20" },
  "ruleVersion": 3,
  "assignmentVersion": 4,
  "kieModuleBytes": { "$binary": { "base64": "AAECAwQ...", "subType": "00" } },
  "createdAt": { "$date": "2025-09-19T10:05:00Z" }
}
```

**Storage options:**

* If artifacts are large, store them in **GridFS** (`fs.files`/`fs.chunks`) and keep only `bundleHash` + GridFS file ID
  here.

**Indexes:**

* `{ "subject.type": 1, "subject.key": 1, "ruleVersion": -1, "assignmentVersion": -1 }`
* `{ bundleHash: 1 }` unique

**Typical runtime query (validation-service):**

```js
db.rule_bundles.findOne(
  { "subject.type": "voucher", "subject.key": "SAVE20" },
  { sort: { ruleVersion: -1, assignmentVersion: -1 } }
)
```

---

# 5)
`metadata_schemas` (manager in metadata-schemas-service, this service store key-value pairs and references to metadata schemas)

**Purpose (EN):** Declares JSON Schemas for custom metadata spaces used in rules (e.g., customer/order/item).

**Document (sample in `metadata-schemas-service`):**

```json
{
  "_id": "ms_order_v2",
  "resourceType": "order",
  "schema": { /* JSON Schema */ },
  "version": 2,
  "active": true,
  "createdAt": { "$date": "2025-09-19T10:00:00Z" },
  "updatedAt": { "$date": "2025-09-19T10:00:00Z" }
}
```

**Indexes:** `{ resourceType: 1, active: 1, version: -1 }`.

---

# 6) `resources` & `operator_resources` (optional)

**Purpose (EN):** External sources for selector options (collections, segments, etc.) and linkage to operators.

**`resources` sample:**

```json
{
  "_id": "res_segments",
  "label": "CDP Segments",
  "endpoint": "https://cdp.example.com/api/segments",
  "method": "GET",
  "authMethod": "bearer",
  "authConfig": "secret/cdp-token",
  "headers": { "Accept": "application/json" },
  "fieldKey": "id",
  "fieldValue": "name",
  "timeoutSeconds": 5,
  "retryCount": 2,
  "backoffMs": 200,
  "createdAt": { "$date": "2025-09-19T10:00:00Z" }
}
```

**`operator_resources` sample (or embed array into `operator_registry`):**

```json
{
  "_id": "or_1",
  "operatorName": "customer.in_segment",
  "resourceId": "res_segments"
}
```

**Indexes:** `{ operatorName: 1 }`, `{ resourceId: 1 }`.

---

# 7A) Time via `time_frames` + `validity_day_of_weeks` (+ exceptions)

**Purpose (EN):** Model absolute windows, weekly schedules, and blackout/override; link frames to rules.

**`time_frames` sample:**

```json
{
  "_id": "tf_001",
  "campaignId": "cmp_123",
  "tz": "Asia/Bangkok",
  "startDate": { "$date": "2025-09-20T00:00:00Z" },   // absolute window (UTC instant)
  "endDate":   { "$date": "2025-12-31T23:59:59Z" },
  "validityInterval": "DAY",                           // optional semantic fields
  "validityDuration": 30,
  "activityDurationAfterPublishing": 0,
  "createdAt": { "$date": "2025-09-19T10:00:00Z" }
}
```

**`validity_day_of_weeks` sample:**

```json
{
  "_id": "vdow_1",
  "timeFrameId": "tf_001",
  "dayOfWeek": "SAT",          // "MON".."SUN"
  "startTime": "09:00:00",
  "endTime":   "12:00:00",
  "weekOfMonth": null,         // 1..5 or -1 (last), optional
  "monthOfYear": null,         // 1..12, optional
  "createdAt": { "$date": "2025-09-19T10:00:00Z" }
}
```

**`time_exceptions` sample:**

```json
{
  "_id": "tex_1",
  "timeFrameId": "tf_001",
  "fromTs": { "$date": "2025-10-20T09:00:00Z" },
  "toTs":   { "$date": "2025-10-20T12:00:00Z" },
  "mode": "DENY",              // or "ALLOW"
  "reason": "Holiday"
}
```

**`rule_time_frames` link:**

```json
{
  "_id": "rtf_1",
  "ruleId": "vr_abc123",
  "timeFrameId": "tf_001",
  "mode": "ALLOW"              // or "DENY" (blackout)
}
```

**Indexes:**

* `time_frames`: `{ tz: 1, startDate: 1, endDate: 1 }`
* `validity_day_of_weeks`: `{ timeFrameId: 1, dayOfWeek: 1 }`
* `rule_time_frames`: `{ ruleId: 1 }`, `{ timeFrameId: 1 }`
* `time_exceptions`: `{ timeFrameId: 1, fromTs: 1 }`

---

# 7B) (Alternative) Time via `temporal_policies` + `rule_temporal_links` (RRULE)

**Purpose (EN):** Single powerful structure to model complex recurrences + exceptions.

**`temporal_policies` sample:**

```json
{
  "_id": "tp_weekend_morning",
  "name": "Weekend mornings",
  "tz": "Asia/Bangkok",
  "startTs": null,
  "endTs": null,
  "rrule": "FREQ=WEEKLY;BYDAY=SA,SU;BYHOUR=9,10,11;BYMINUTE=0;BYSECOND=0",
  "rdate": [],
  "exrule": null,
  "exdate": ["2025-10-20T09:00:00+07:00"],
  "timeOfDayWindows": [ { "start": "09:00", "end": "12:00" } ],
  "metadata": {},
  "createdAt": { "$date": "2025-09-19T10:00:00Z" }
}
```

**`rule_temporal_links` sample:**

```json
{
  "_id": "rtl_1",
  "ruleId": "vr_abc123",
  "policyId": "tp_weekend_morning",
  "mode": "ALLOW"           // or "DENY"
}
```

**Indexes:** `{ ruleId: 1 }`, `{ policyId: 1 }`.

---

# JSON Schema Validation (collection-level, optional)

MongoDB supports **\$jsonSchema** validators—use them for key collections to catch bad configs early.

**Example (partial) validator for `validation_rules`:**

```js
db.runCommand({
  collMod: "validation_rules",
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["state","version","nodes"],
      properties: {
        state: { enum: ["draft","published","archived"] },
        version: { bsonType: "int" },
        nodes: {
          bsonType: "array",
          items: {
            oneOf: [
              {
                required: ["id","type","groupLogic"],
                properties: {
                  type: { enum: ["GROUP"] },
                  groupLogic: { enum: ["ALL","ANY","NONE"] }
                }
              },
              {
                required: ["id","type","operatorName","params"],
                properties: {
                  type: { enum: ["COND"] },
                  operatorName: { bsonType: "string" },
                  params: { bsonType: "object" }
                }
              }
            ]
          }
        }
      }
    }
  }
});
```

---

# Indexing & Sharding Guidance

* **Hot read path (runtime):** `rule_bundles` by subject → index
  `{ "subject.type": 1, "subject.key": 1, ruleVersion: -1, assignmentVersion: -1 }`.
* **Admin UI:**

    * `validation_rules` by `state`, `code`.
    * `operator_registry` by `context`.
* **If sharding:** shard `rule_bundles` by `{ "subject.type", "subject.key" }` to colocate bundles of same subject; low
  cardinality? Consider hashed shard key on `bundleHash`.
* **Large artifacts:** consider **GridFS**; store file id in `rule_bundles.kieModuleFileId`.

---

# Publish Pipeline (recap)

1. **Validate DSL** using `operator_registry.jsonSchema`.
2. **Lower to IR**; **compile** to Drools (using `compilerId`), produce bytes.
3. **Insert** new doc in `rule_bundles` (immutable) with `bundleHash`.
4. (Optional) Emit `BundlePublished` event with `{subject, bundleHash}` for cache warm-up.

---

# Example Queries

* Get latest bundle for a voucher:

```js
db.rule_bundles.findOne(
  { "subject.type": "voucher", "subject.key": "SAVE20" },
  { sort: { ruleVersion: -1, assignmentVersion: -1 } }
);
```

* List active assignments for a subject:

```js
db.rule_assignments.find({
  "subject.type": "campaign",
  "subject.key": "cmp_123",
  active: true,
  $or: [
    { validFrom: null }, { validFrom: { $lte: new Date() } }
  ],
  $or: [
    { validTo: null }, { validTo: { $gte: new Date() } }
  ]
});
```

* Resolve a rule tree to compile:

```js
const rule = db.validation_rules.findOne({ _id: "vr_abc123" });
// rule.nodes is embedded → compile directly
```

---

# Operational Notes

* **Immutability:** Never update `rule_bundles`; append for each publish/assignment change.
* **Concurrency:** Keep publish/assignment changes in a single writer flow (transactions if you update multiple
  collections together).
* **Explainability:** Keep `reasonCode` per condition node; surface in validation responses.
* **Time zones:** Always store instants in UTC; store `tz` in time frames or policies to evaluate local calendars.
* **Testing:** Build golden tests (payload → expected decision) per rule version.
