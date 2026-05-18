package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorCategoryEntity;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for OperatorCategoryMapper.parseValueOptions()
 *
 * Covers:
 *  - Label as JSON object {"en": "...", "vi": "..."} → full i18n
 *  - Label as plain string (legacy shape) → labelEn = labelVi = label
 *  - Missing "vi" key → fallback to "en"
 *  - Missing "en" key → fallback to "vi"
 *  - Both keys missing → null labels (graceful, no crash)
 *  - Null / blank JSON → empty list
 *  - Malformed JSON → empty list (no exception propagated)
 */
@DisplayName("OperatorCategoryMapper.parseValueOptions()")
@SuppressWarnings("deprecation") // Tests deliberately verify the legacy label BC field
class OperatorCategoryMapperTest {

    /**
     * Minimal stub that implements the two abstract MapStruct methods so we can
     * exercise the default method {@code parseValueOptions} directly.
     */
    private static class MapperStub implements OperatorCategoryMapper {
        @Override
        public OperatorCategory toDomain(OperatorCategoryEntity entity) {
            throw new UnsupportedOperationException("stub only");
        }

        @Override
        public OperatorCategoryEntity toEntity(OperatorCategory domain) {
            throw new UnsupportedOperationException("stub only");
        }
    }

    private OperatorCategoryMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = new MapperStub();
        objectMapper = new ObjectMapper();
    }

    // =========================================================================
    // Label as JSON object — full i18n shape
    // =========================================================================

    @Nested
    @DisplayName("Label as object {en, vi}")
    class LabelObject {

        @Test
        @DisplayName("Should parse labelEn and labelVi when label is {en, vi} object")
        void shouldParseBothLabels_whenLabelIsObject() {
            String json = """
                    [
                      {"value": "paid", "label": {"en": "Paid", "vi": "Trả phí"}},
                      {"value": "organic", "label": {"en": "Organic", "vi": "Tự nhiên"}}
                    ]
                    """;

            List<OperatorOption.ValueOption> result = mapper.parseValueOptions(json, objectMapper);

            assertThat(result).hasSize(2);

            OperatorOption.ValueOption paid = result.get(0);
            assertThat(paid.getValue()).isEqualTo("paid");
            assertThat(paid.getLabelEn()).isEqualTo("Paid");
            assertThat(paid.getLabelVi()).isEqualTo("Trả phí");
            assertThat(paid.getLabel()).isEqualTo("Paid");    // backward compat

            OperatorOption.ValueOption organic = result.get(1);
            assertThat(organic.getLabelEn()).isEqualTo("Organic");
            assertThat(organic.getLabelVi()).isEqualTo("Tự nhiên");
        }

        @Test
        @DisplayName("Should fall back to 'en' when 'vi' key is missing from label object")
        void shouldFallBackToEn_whenViMissing() {
            String json = """
                    [{"value": "paid", "label": {"en": "Paid"}}]
                    """;

            List<OperatorOption.ValueOption> result = mapper.parseValueOptions(json, objectMapper);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLabelEn()).isEqualTo("Paid");
            assertThat(result.get(0).getLabelVi()).isEqualTo("Paid");  // falls back to en
        }

        @Test
        @DisplayName("Should fall back to 'vi' when 'en' key is missing from label object")
        void shouldFallBackToVi_whenEnMissing() {
            String json = """
                    [{"value": "paid", "label": {"vi": "Trả phí"}}]
                    """;

            List<OperatorOption.ValueOption> result = mapper.parseValueOptions(json, objectMapper);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLabelVi()).isEqualTo("Trả phí");
            assertThat(result.get(0).getLabelEn()).isEqualTo("Trả phí");  // falls back to vi
        }

        @Test
        @DisplayName("Should set both labels to null when label object has no en or vi keys")
        void shouldSetNullLabels_whenObjectHasNoEnOrViKeys() {
            String json = """
                    [{"value": "paid", "label": {"other": "something"}}]
                    """;

            List<OperatorOption.ValueOption> result = mapper.parseValueOptions(json, objectMapper);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLabelEn()).isNull();
            assertThat(result.get(0).getLabelVi()).isNull();
        }
    }

    // =========================================================================
    // Label as plain string — legacy shape
    // =========================================================================

    @Nested
    @DisplayName("Label as plain string (legacy)")
    class LabelString {

        @Test
        @DisplayName("Should set labelEn = labelVi = label when label is plain string")
        void shouldSetBothLabels_whenLabelIsString() {
            String json = """
                    [
                      {"value": "paid", "label": "Paid"},
                      {"value": "organic", "label": "Organic"}
                    ]
                    """;

            List<OperatorOption.ValueOption> result = mapper.parseValueOptions(json, objectMapper);

            assertThat(result).hasSize(2);

            OperatorOption.ValueOption paid = result.get(0);
            assertThat(paid.getValue()).isEqualTo("paid");
            assertThat(paid.getLabelEn()).isEqualTo("Paid");
            assertThat(paid.getLabelVi()).isEqualTo("Paid");
            assertThat(paid.getLabel()).isEqualTo("Paid");

            OperatorOption.ValueOption organic = result.get(1);
            assertThat(organic.getLabelEn()).isEqualTo("Organic");
            assertThat(organic.getLabelVi()).isEqualTo("Organic");
        }
    }

    // =========================================================================
    // Defensive / edge cases
    // =========================================================================

    @Nested
    @DisplayName("Defensive edge cases")
    class EdgeCases {

        static Stream<org.junit.jupiter.params.provider.Arguments> emptyResultInputs() {
            return Stream.of(
                    arguments("null json", null),
                    arguments("blank json", "   "),
                    arguments("malformed json (no exception propagated)", "not-valid-json"),
                    arguments("empty JSON array", "[]")
            );
        }

        @ParameterizedTest(name = "Should return empty list for {0}")
        @MethodSource("emptyResultInputs")
        void shouldReturnEmpty_forDefensiveInputs(String label, String json) {
            List<OperatorOption.ValueOption> result = mapper.parseValueOptions(json, objectMapper);
            assertThat(result).isEmpty();
        }
    }
}
