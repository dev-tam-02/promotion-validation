package vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.OperatorCategoryEntity;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

        @Test
        @DisplayName("Should return empty list when json is null")
        void shouldReturnEmpty_whenJsonIsNull() {
            List<OperatorOption.ValueOption> result = mapper.parseValueOptions(null, objectMapper);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when json is blank")
        void shouldReturnEmpty_whenJsonIsBlank() {
            List<OperatorOption.ValueOption> result = mapper.parseValueOptions("   ", objectMapper);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list on malformed JSON (no exception propagated)")
        void shouldReturnEmpty_onMalformedJson() {
            List<OperatorOption.ValueOption> result = mapper.parseValueOptions("not-valid-json", objectMapper);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when JSON is an empty array")
        void shouldReturnEmpty_whenJsonArrayIsEmpty() {
            List<OperatorOption.ValueOption> result = mapper.parseValueOptions("[]", objectMapper);
            assertThat(result).isEmpty();
        }
    }
}
