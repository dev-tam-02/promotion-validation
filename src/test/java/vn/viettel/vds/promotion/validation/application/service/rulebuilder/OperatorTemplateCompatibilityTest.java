package vn.viettel.vds.promotion.validation.application.service.rulebuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.viettel.vds.promotion.validation.application.service.DrlCompiler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Task 09 V7 — Parameter typing end-to-end compatibility matrix.
 *
 * <p>For each of the 9 seeded operators, this test:
 * <ol>
 *   <li>Renders the DRL template via {@link DrlCompiler} with template-compatible params.</li>
 *   <li>Asserts the DRL snippet contains expected fact class name and field references
 *       (string-contains proxy for type-alignment — no Drools runtime in pp-validation).</li>
 *   <li>Validates the operator's {@code json_schema} (from seed YAML) against those same params
 *       using networknt JSON Schema validator (draft-07), explicitly documenting whether
 *       schema and template param names are aligned.</li>
 *   <li>Catches param key drift: operators whose schema uses different param keys than the
 *       template are flagged as {@code schemaDrifted=true}; the test asserts schema
 *       validation fails for template-compatible params — making drift visible in CI.</li>
 * </ol>
 *
 * <h2>Drift inventory (Task 09 V7 findings)</h2>
 * <ul>
 *   <li><b>product.in_category</b>: schema requires {@code categoryIds} (array),
 *       template uses {@code {{categoryId}}} (single string). FE sends array → DRL renders
 *       empty {@code categoryId == ""} → silent miss at runtime.</li>
 *   <li><b>product.in_list</b>: schema requires {@code productIds} (array),
 *       template uses {@code {{productId}}} (single string). Same silent-miss risk.</li>
 *   <li><b>time.within_window</b>: schema requires {@code startTime}/{@code endTime},
 *       template uses {@code {{from}}}/{@code {{to}}}. Window condition renders empty.</li>
 * </ul>
 *
 * <h2>Follow-up (long-term, out-of-scope MVP)</h2>
 * <p>An annotation processor that code-generates DRL templates FROM {@code json_schema}
 * (JSON Schema → Java types → Drools DSL) would eliminate this entire class of drift.
 * Tracked as a separate backlog item.
 *
 * <p>Follow-up: Fix the 3 drifted operator schemas so {@code json_schema} matches template param keys:
 * <ul>
 *   <li>{@code product.in_category} → change schema to {@code categoryId: {type: string}}</li>
 *   <li>{@code product.in_list} → change schema to {@code productId: {type: string}}</li>
 *   <li>{@code time.within_window} → change schema to {@code from}/{@code to} instead of
 *       {@code startTime}/{@code endTime}</li>
 * </ul>
 *
 * @see DrlCompiler
 * @see vn.viettel.vds.promotion.validation.application.service.DrlCompilerTest
 */
@DisplayName("Task 09 V7 — Operator template ↔ json_schema compatibility matrix")
class OperatorTemplateCompatibilityTest {

    private DrlCompiler compiler;
    private ObjectMapper objectMapper;
    private JsonSchemaFactory schemaFactory;

    /**
     * One entry per seeded operator.
     *
     * <p>Fields per argument:
     * <ol>
     *   <li>{@code operatorName} — operator registry name</li>
     *   <li>{@code compilerId} — DRL template file ID (without .drl.mustache suffix)</li>
     *   <li>{@code jsonSchemaStr} — json_schema exactly as stored in DB seed</li>
     *   <li>{@code templateParams} — params that work with the template (may differ from schema)</li>
     *   <li>{@code schemaDrifted} — true when schema param keys ≠ template param keys</li>
     *   <li>{@code expectedDrlTokens} — substrings the rendered DRL snippet must contain</li>
     *   <li>{@code expectedFieldTypes} — field → Java type name mapping (documentation; verified via DRL string)</li>
     * </ol>
     */
    static Stream<Arguments> operatorSamples() {
        return Stream.of(

                // ── 1. order.total.gte ────────────────────────────────────────────
                Arguments.of(
                        "order.total.gte",
                        "tpl_order_total_gte_v1",
                        // json_schema (from 002-insert + 025-add-operator-registry-v2-fields):
                        "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\"},\"currency\":{\"type\":\"string\"}},\"required\":[\"amount\"]}",
                        // template uses: {{amount}} (number → BigDecimal in OrderFact.totalAmount), {{currency}} (String)
                        Map.of("amount", 500_000, "currency", "VND"),
                        /* schemaDrifted= */ false,
                        List.of("OrderFact", "totalAmount", "500000", "VND"),
                        // expectedFieldTypes: totalAmount → BigDecimal, currency → String
                        Map.of("totalAmount", "java.math.BigDecimal", "currency", "java.lang.String")
                ),

                // ── 2. customer.in_segment ────────────────────────────────────────
                Arguments.of(
                        "customer.in_segment",
                        "tpl_customer_in_segment_v1",
                        // json_schema (from 025):
                        "{\"type\":\"object\",\"properties\":{\"segments\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"segments\"]}",
                        // template iterates segments list: segments contains "VIP_GOLD"
                        Map.of("segments", List.of("VIP_GOLD")),
                        /* schemaDrifted= */ false,
                        List.of("CustomerFact", "segments contains", "VIP_GOLD"),
                        // expectedFieldTypes: segments → Set<String> (actual: Set<String> in CustomerFact)
                        Map.of("segments", "java.util.Set")
                ),

                // ── 3. customer.loyalty_tier.gte ─────────────────────────────────
                Arguments.of(
                        "customer.loyalty_tier.gte",
                        "tpl_customer_loyalty_tier_gte_v1",
                        // json_schema (from 028-seed-spec-operators-audience):
                        "{\"type\":\"object\",\"required\":[\"tier\"],\"properties\":{\"tier\":{\"type\":\"string\",\"enum\":[\"BRONZE\",\"SILVER\",\"GOLD\",\"PLATINUM\",\"DIAMOND\"]}}}",
                        // template: loyaltyTier.compareTo("{{tier}}") >= 0
                        Map.of("tier", "GOLD"),
                        /* schemaDrifted= */ false,
                        List.of("CustomerFact", "loyaltyTier", "GOLD", "compareTo"),
                        // expectedFieldTypes: loyaltyTier → String (compareTo-based comparison)
                        Map.of("loyaltyTier", "java.lang.String")
                ),

                // ── 4. product.in_category ─────────────────────────────────────── ⚠️ DRIFT
                // DRIFT: schema requires "categoryIds" (array), template uses {{categoryId}} (single string).
                // When FE sends schema-valid params (categoryIds=[...]), template renders empty categoryId == "".
                // Fix: align schema to use "categoryId": {type: string} to match template.
                Arguments.of(
                        "product.in_category",
                        "tpl_product_in_category_v1",
                        // json_schema (from 028-seed-spec-operators-product) — has DRIFT with template:
                        "{\"type\":\"object\",\"required\":[\"categoryIds\"],\"properties\":{\"categoryIds\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"minItems\":1}}}",
                        // template-compatible params (what template actually expects):
                        Map.of("categoryId", "cat-electronics"),
                        /* schemaDrifted= */ true,   // schema "categoryIds" ≠ template "categoryId"
                        List.of("CartItemFact", "cat-electronics"),
                        // expectedFieldTypes: categoryId → String in CartItemFact
                        Map.of("categoryId", "java.lang.String")
                ),

                // ── 5. product.in_list ─────────────────────────────────────────── ⚠️ DRIFT
                // DRIFT: schema requires "productIds" (array), template uses {{productId}} (single string).
                // Fix: align schema to use "productId": {type: string} to match template.
                Arguments.of(
                        "product.in_list",
                        "tpl_product_in_list_v1",
                        // json_schema (from 028) — has DRIFT with template:
                        "{\"type\":\"object\",\"required\":[\"productIds\"],\"properties\":{\"productIds\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"minItems\":1}}}",
                        // template-compatible params:
                        Map.of("productId", "prod-001"),
                        /* schemaDrifted= */ true,   // schema "productIds" ≠ template "productId"
                        List.of("CartItemFact", "prod-001"),
                        // expectedFieldTypes: productId → String in CartItemFact
                        Map.of("productId", "java.lang.String")
                ),

                // ── 6. cart.has_product ───────────────────────────────────────────
                Arguments.of(
                        "cart.has_product",
                        "tpl_cart_has_product_v1",
                        // json_schema (from 028):
                        "{\"type\":\"object\",\"required\":[\"productId\"],\"properties\":{\"productId\":{\"type\":\"string\"},\"minQuantity\":{\"type\":\"integer\",\"minimum\":1}}}",
                        Map.of("productId", "sku-abc"),
                        /* schemaDrifted= */ false,
                        List.of("CartItemFact", "productId", "sku-abc", "exists"),
                        // expectedFieldTypes: productId → String
                        Map.of("productId", "java.lang.String")
                ),

                // ── 7. time.within_window ─────────────────────────────────────── ⚠️ DRIFT
                // DRIFT: schema requires "startTime"/"endTime", template uses {{from}}/{{to}}.
                // Fix: align schema to use "from"/"to" to match template param keys.
                Arguments.of(
                        "time.within_window",
                        "tpl_time_within_window_v1",
                        // json_schema (from 028) — has DRIFT with template:
                        "{\"type\":\"object\",\"required\":[\"startTime\",\"endTime\"],\"properties\":{\"startTime\":{\"type\":\"string\"},\"endTime\":{\"type\":\"string\"},\"timezone\":{\"type\":\"string\"}}}",
                        // template-compatible params:
                        Map.of("from", "2026-01-01T00:00:00Z", "to", "2026-12-31T23:59:59Z"),
                        /* schemaDrifted= */ true,   // schema "startTime/endTime" ≠ template "from/to"
                        List.of("ExecutionContextFact", "2026-01-01T00:00:00Z", "2026-12-31T23:59:59Z"),
                        // expectedFieldTypes: now → Instant/ZonedDateTime in ExecutionContextFact
                        Map.of("now", "java.time.Instant")
                ),

                // ── 8. order.items.count.gte ─────────────────────────────────────
                Arguments.of(
                        "order.items.count.gte",
                        "tpl_order_items_count_gte_v1",
                        // json_schema (from 028):
                        "{\"type\":\"object\",\"required\":[\"count\"],\"properties\":{\"count\":{\"type\":\"integer\",\"minimum\":1}}}",
                        Map.of("count", 3),
                        /* schemaDrifted= */ false,
                        List.of("OrderFact", "items.size", "3"),
                        // expectedFieldTypes: items → List<OrderItemFact> (size checked as int)
                        Map.of("items", "java.util.List")
                ),

                // ── 9. customer.is_owner ──────────────────────────────────────────
                Arguments.of(
                        "customer.is_owner",
                        "tpl_customer_is_owner_v1",
                        // json_schema (from 030-seed-owner-only-operator-reason-code):
                        "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}",
                        // no params — operator derives ownership via cross-fact equality
                        Map.of(),
                        /* schemaDrifted= */ false,
                        List.of("VoucherFact", "CustomerFact", "ownerCustomerId", "id == $v.ownerCustomerId"),
                        // expectedFieldTypes: ownerCustomerId → String (VoucherFact), id → String (CustomerFact)
                        Map.of("ownerCustomerId", "java.lang.String", "id", "java.lang.String")
                )
        );
    }

    // ─── Operator sample definitions ─────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        compiler = new DrlCompiler();
        objectMapper = new ObjectMapper();
        schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    // ─── Main compatibility matrix ────────────────────────────────────────────────

    /**
     * Primary matrix: render template with template-compatible params, assert DRL tokens,
     * and validate json_schema alignment.
     *
     * <p>For operators with {@code schemaDrifted=true}, schema validation of the
     * template-compatible params is expected to FAIL — this is the drift detection mechanism.
     * If a previously-drifted operator starts passing schema validation, it means the drift
     * has been fixed (update {@code schemaDrifted} to {@code false}).
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("operatorSamples")
    @DisplayName("operatorTemplate_renderAndFactCompat")
    void operatorTemplate_renderAndFactCompat(
            String operatorName,
            String compilerId,
            String jsonSchemaStr,
            Map<String, Object> templateParams,
            boolean schemaDrifted,
            List<String> expectedDrlTokens,
            Map<String, String> expectedFieldTypes
    ) throws Exception {
        // ── Step 1: Render DRL template with template-compatible params ──────
        String drlSnippet = compiler.renderCondTemplate(compilerId, templateParams);

        assertThat(drlSnippet)
                .as("Operator '%s' — DRL snippet must not be blank", operatorName)
                .isNotBlank();

        // ── Step 2: Assert DRL contains expected fact class name and fields ──
        for (String token : expectedDrlTokens) {
            assertThat(drlSnippet)
                    .as("Operator '%s' — DRL must reference token '%s'", operatorName, token)
                    .contains(token);
        }

        // ── Step 3: Field-type alignment (string-level) ──────────────────────
        // Each expectedFieldTypes key must appear in the DRL (the field is referenced).
        // The value (Java type name) is documentation validated by code review + DrlCompilerTest.
        for (String fieldName : expectedFieldTypes.keySet()) {
            assertThat(drlSnippet)
                    .as("Operator '%s' — DRL must reference field '%s' (expected type: %s)",
                            operatorName, fieldName, expectedFieldTypes.get(fieldName))
                    .contains(fieldName);
        }

        // ── Step 4: JSON Schema validation of template params ─────────────────
        JsonNode schemaNode = objectMapper.readTree(jsonSchemaStr);
        JsonSchema jsonSchema = schemaFactory.getSchema(schemaNode);
        JsonNode paramsNode = objectMapper.valueToTree(templateParams);
        Set<ValidationMessage> violations = jsonSchema.validate(paramsNode);

        if (schemaDrifted) {
            // Drift confirmed: template-compatible params fail schema validation.
            // This is the expected state — it documents the known drift.
            // Fix: align the operator's json_schema param keys to match the template.
            assertThat(violations)
                    .as("Operator '%s' has known schema drift — schema validation SHOULD fail " +
                                    "for template-compatible params (fix: align json_schema param keys to template)",
                            operatorName)
                    .isNotEmpty();
        } else {
            // No drift: schema-valid params == template-compatible params.
            assertThat(violations)
                    .as("Operator '%s' — template params must satisfy json_schema (no violations)", operatorName)
                    .isEmpty();
        }
    }

    // ─── Null / missing required params ──────────────────────────────────────────

    /**
     * Verifies that null params passed to {@link DrlCompiler#renderCondTemplate} never causes
     * a {@link NullPointerException} — DrlCompiler converts null → empty map internally.
     *
     * <p>Rendered DRL may be syntactically invalid (empty placeholders), but the service
     * must not crash with NPE. Callers must validate params against json_schema
     * (via {@link vn.viettel.vds.promotion.validation.application.service.RuleValidator})
     * before reaching DrlCompiler.
     */
    @ParameterizedTest(name = "[{index}] {0} — null params no NPE")
    @MethodSource("operatorSamples")
    @DisplayName("nullParams_doesNotThrowNpe")
    void nullParams_doesNotThrowNpe(
            String operatorName,
            String compilerId,
            String jsonSchemaStr,
            Map<String, Object> templateParams,
            boolean schemaDrifted,
            List<String> expectedDrlTokens,
            Map<String, String> expectedFieldTypes
    ) {
        assertThatCode(() -> compiler.renderCondTemplate(compilerId, null))
                .as("Operator '%s' — null params must not throw NPE", operatorName)
                .doesNotThrowAnyException();
    }

    // ─── Multi-value segment rendering ───────────────────────────────────────────

    @Test
    @DisplayName("customer.in_segment — OR-joins multiple segments")
    void customerInSegment_multipleSegments_orJoined() {
        String snippet = compiler.renderCondTemplate(
                "tpl_customer_in_segment_v1",
                Map.of("segments", List.of("VIP", "GOLD", "PLATINUM")));

        assertThat(snippet)
                .contains("CustomerFact")
                .contains("segments contains \"VIP\"")
                .contains("segments contains \"GOLD\"")
                .contains("segments contains \"PLATINUM\"")
                .contains("||");
    }

    // ─── Schema drift: schema-valid params produce broken DRL ─────────────────────

    /**
     * Demonstrates the schema drift for {@code product.in_category}.
     *
     * <p>When the FE sends schema-valid params ({@code categoryIds=[...]}), the template
     * renders with an EMPTY {@code categoryId == ""} — the condition is silently broken.
     * This test asserts that exact broken behavior to lock it in — fixing the drift
     * will break this test, which is the intended signal to update it.
     *
     * <p>Follow-up: Fix drift — change schema from {@code categoryIds: array} to
     * {@code categoryId: string}.
     */
    @Test
    @DisplayName("[DRIFT] product.in_category — schema-valid params produce empty categoryId in DRL")
    void drift_productInCategory_schemaParamsProduceEmptyDrl() {
        // Schema-valid params (what json_schema says to send):
        Map<String, Object> schemaValidParams = Map.of("categoryIds", List.of("cat-electronics", "cat-fashion"));

        String snippet = compiler.renderCondTemplate("tpl_product_in_category_v1", schemaValidParams);

        // Template uses {{categoryId}} — not present in schemaValidParams — renders as empty string:
        assertThat(snippet)
                .as("Drift confirmed: schema-valid params produce empty categoryId placeholder")
                .contains("CartItemFact")
                .as("Drift confirmed: 'cat-electronics' must NOT appear (schema key mismatch)")
                .doesNotContain("cat-electronics");
    }

    /**
     * Demonstrates the schema drift for {@code product.in_list}.
     *
     * <p>Follow-up: Fix drift — change schema from {@code productIds: array} to
     * {@code productId: string}.
     */
    @Test
    @DisplayName("[DRIFT] product.in_list — schema-valid params produce empty productId in DRL")
    void drift_productInList_schemaParamsProduceEmptyDrl() {
        Map<String, Object> schemaValidParams = Map.of("productIds", List.of("prod-001", "prod-002"));

        String snippet = compiler.renderCondTemplate("tpl_product_in_list_v1", schemaValidParams);

        assertThat(snippet)
                .as("Drift confirmed: schema-valid params produce empty productId placeholder")
                .contains("CartItemFact")
                .as("Drift confirmed: 'prod-001' must NOT appear (schema key mismatch)")
                .doesNotContain("prod-001");
    }

    /**
     * Demonstrates the schema drift for {@code time.within_window}.
     *
     * <p>Follow-up: Fix drift — change schema from {@code startTime}/{@code endTime} to
     * {@code from}/{@code to}.
     */
    @Test
    @DisplayName("[DRIFT] time.within_window — schema-valid params produce empty from/to in DRL")
    void drift_timeWithinWindow_schemaParamsProduceEmptyDrl() {
        Map<String, Object> schemaValidParams = Map.of(
                "startTime", "08:00",
                "endTime", "22:00"
        );

        String snippet = compiler.renderCondTemplate("tpl_time_within_window_v1", schemaValidParams);

        assertThat(snippet)
                .as("Drift confirmed: schema-valid params produce empty from/to placeholders")
                .contains("ExecutionContextFact")
                .as("Drift confirmed: '08:00' must NOT appear (schema key mismatch)")
                .doesNotContain("08:00");
    }

    // ─── Missing required param produces broken DRL (not NPE) ────────────────────

    @Test
    @DisplayName("order.total.gte — missing required 'amount' renders DRL with empty placeholder")
    void missingRequiredParam_orderTotalGte_rendersWithEmptyPlaceholder() {
        // No 'amount' supplied — schema says it's required, but DrlCompiler doesn't validate
        Map<String, Object> missingRequired = Map.of("currency", "VND");

        assertThatCode(() -> {
            String snippet = compiler.renderCondTemplate("tpl_order_total_gte_v1", missingRequired);
            // Renders without NPE; 'amount' placeholder is empty → invalid DRL syntax
            assertThat(snippet).contains("OrderFact");
            assertThat(snippet).contains("totalAmount >=");
            // Value is absent — Handlebars renders empty string for missing key
            assertThat(snippet).doesNotContain("500000");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("customer.loyalty_tier.gte — missing required 'tier' renders DRL with empty placeholder")
    void missingRequiredParam_loyaltyTierGte_rendersWithEmptyPlaceholder() {
        assertThatCode(() -> {
            String snippet = compiler.renderCondTemplate("tpl_customer_loyalty_tier_gte_v1", Map.of());
            assertThat(snippet).contains("CustomerFact");
            assertThat(snippet).contains("loyaltyTier");
            // compareTo("") — empty tier, not NPE
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cart.has_product — missing required 'productId' renders DRL with empty placeholder")
    void missingRequiredParam_cartHasProduct_rendersWithEmptyPlaceholder() {
        assertThatCode(() -> {
            String snippet = compiler.renderCondTemplate("tpl_cart_has_product_v1", Map.of());
            assertThat(snippet).contains("CartItemFact");
            assertThat(snippet).contains("exists");
        }).doesNotThrowAnyException();
    }
}
