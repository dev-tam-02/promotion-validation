# pp-validation

Promotion Platform — Validation Rule Engine service.

Handles rule authoring (operator registry, rule tree CRUD), rule compilation (DSL → DRL), and evaluation orchestration (delegates Drools execution to `pp-rule-engine`). Acts as the **command-side** for validation rules; `pp-rule-engine` is the **evaluation-side**.

---

## Responsibilities

- **Operator registry** — catalog of typed operators (`order.total.gte`, `customer.in_segment`, …), each with a `json_schema` for params validation and a `compiler_id` mapping to a Mustache DRL template.
- **Rule authoring** — CRUD for `validation_rules` + `rule_nodes` tree (GROUP/COND). Path A: admin-built via FE RuleBuilder. Path B: auto-generated with `binding.validity_window` gate (saga-driven).
- **DRL compilation** — `DrlCompiler` renders Mustache templates → DRL text; delegates `POST /v1/rules` to `pp-rule-engine` for KieBase registration.
- **Rule bindings** — link rules to campaign/voucher resources with scope + temporal constraints.
- **Rule history** — append-only `validation_rules_history` for audit trail (version + change_type + snapshot).
- **RuleBuilder API** — `GET /v1/rule-builder/categories` returns operator categories + rule items (including virtual operators from `metadata_schemas`).

---

## Two-Path Rule Resolution

```
SettingValidationRuleCommand (saga)
         │
         ▼
  ruleId present?
    ├── YES → Path A: load Rule + RuleNodes from DB (admin-authored rule)
    │             └── compileDrlAndRegisterInEngine()
    └── NO  → Path B: auto-generate Rule with binding.validity_window gate
                  └── persist + historyRepo.save(CREATE snapshot) + compileDrlAndRegisterInEngine()
```

**Path A** — caller provides an existing `ruleId`; service loads the rule tree and registers the compiled DRL in KieBase.

**Path B** — no `ruleId`; saga provides a `ValidityTimeframe`; service auto-creates a 1-GROUP-1-COND rule with `binding.validity_window` operator and registers it.

---

## Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `DrlCompiler` | `application/service/DrlCompiler.java` | Compile Rule + RuleNode tree → DRL text using Mustache templates |
| `SettingValidationRuleCommandHandler` | `application/service/` | Saga handler — Path A / Path B resolution + KieBase register |
| `RuleBuilderService` | `application/service/RuleBuilderService.java` | Build operator category response (static + virtual metadata operators) |
| `RuleRegistryBootstrapLoader` | `application/service/RuleRegistryBootstrapLoader.java` | ApplicationRunner @Order(100) — reload all active rules into KieBase on startup |
| `EngineSimulationService` | `application/service/EngineSimulationService.java` | Proxy simulation requests to pp-rule-engine |
| `RuleEngineClient` | `application/port/out/RuleEngineClient.java` | Output port — `POST /v1/rules` on pp-rule-engine |
| `ValidationEngineClient` | `application/port/out/ValidationEngineClient.java` | Output port — `POST /v1/evaluate` on pp-rule-engine |

---

## DRL Templates

Mustache templates under `src/main/resources/rule-templates/`:

| Template ID | Operator | Condition |
|-------------|----------|-----------|
| `tpl_binding_validity_window_v1` | `binding.validity_window` | `now ∈ [startDate, endDate]` |
| `tpl_customer_in_segment_v1` | `customer.in_segment` | Customer segment membership |
| `tpl_order_total_gte_v1` | `order.total.gte` | Order total ≥ amount |
| `tpl_customer_loyalty_tier_gte_v1` | `customer.loyalty_tier.gte` | Loyalty tier rank |
| `tpl_order_items_count_gte_v1` | `order.items.count.gte` | Item count ≥ N |
| `tpl_product_in_category_v1` | `order.item.category.in` | Any item in category list |
| `tpl_product_in_list_v1` | `order.item.sku.in` | Any item SKU in list |
| `tpl_cart_has_product_v1` | `cart.has_product` | Cart contains product |
| `tpl_time_within_window_v1` | `time.within_window` | Time within recurring window |

---

## Database Schema (key tables)

- `operator_categories` — grouping (AUDIENCE, ORDER, PRODUCT, TIME, LOYALTY, …)
- `operator_options` — operator catalog per category (code, name, operator_name, comparison_type, value_type, available_comparators, input_type, json_schema, data_source_type)
- `validation_rules` — rule header (state, bundle_hash, current_version, is_system)
- `rule_nodes` — tree nodes (type=GROUP|COND, parent_id, operator_name, params, group_logic)
- `rule_bindings` — link rule ↔ resource (resource_type, resource_id, active_from, active_to)
- `reason_codes` — typed deny reasons (code, display_message, severity)
- `validation_rules_history` — append-only audit trail (version, change_type, dsl_snapshot, bundle_hash)
- `metadata_schemas` — custom fact schemas for dynamic operator generation (resource_type, fields)

---

## Design Documentation

- [DRL_AUTO_GENERATION_INDEX.md](./docs/design/DRL_AUTO_GENERATION_INDEX.md) — Task index for DRL auto-generation sprint (T0–T22)
- [DRL_AUTO_GENERATION_FOR_DISCOUNT_COUPON.md](./docs/design/DRL_AUTO_GENERATION_FOR_DISCOUNT_COUPON.md) — Detailed design (v1.3): two-path resolution, operator registry, compilation pipeline
- [DRL_AUTO_GENERATION_TASKS.md](./docs/design/DRL_AUTO_GENERATION_TASKS.md) — Task breakdown with AC and test plans

Cross-flow documentation:
- [00-research-and-design.md §4.4](../../document/cross-flow/publication-flow/00-research-and-design.md) — pp-validation overview + gap analysis
- [pp-validation-improvements.md](../../document/cross-flow/publication-flow/pp-validation-improvements.md) — Improvement roadmap V1-V10 with v1.3 status

---

## Running Locally

```bash
cd domains/pp/services/pp-validation
mvn spring-boot:run -Dspring.profiles.active=dev
```

Service port: **16014**. Context-path: `/promotion/promotion-validation`. Swagger UI: `http://localhost:16014/promotion/promotion-validation/swagger-ui.html`

### Dependencies

- **MariaDB** — primary store (rule definitions, bindings, history)
- **pp-rule-engine** — KieBase host; receives `POST /v1/rules` (register DRL) and `POST /v1/evaluate` (evaluate facts)
- **Kafka** — incoming saga commands (`SettingValidationRuleCommand`)

---

## Build & Test

```bash
# Build
mvn clean package -DskipTests

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DrlCompilerComprehensiveTest
mvn test -Dtest=RuleEvaluationIntegrationTest
```

Key test classes:

| Test | Coverage |
|------|----------|
| `DrlCompilerComprehensiveTest` | 42 cases — render+compile gate for all operator templates (V7 type safety) |
| `RuleEvaluationIntegrationTest` | 14 cases — Path A / Path B saga pipeline + DRL text assertions |
