package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

@DisplayName("CondNode Tests")
class CondNodeTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {
        @Test
        @DisplayName("Should build valid CondNode with all fields")
        void shouldBuildValidCondNode() {
            // Given
            Instant now = Instant.now();
            Map<String, Object> params = Map.of("threshold", 100);

            // When
            CondNode node = CondNode.builder()
                    .id("cond-1")
                    .ruleId("rule-1")
                    .parentId("group-1")
                    .operatorName("gte")
                    .params(params)
                    .reasonCode("RC001")
                    .displayOrder(1)
                    .createdAt(now)
                    .build();

            // Then
            assertThat(node.getId()).isEqualTo("cond-1");
            assertThat(node.getRuleId()).isEqualTo("rule-1");
            assertThat(node.getParentId()).isEqualTo("group-1");
            assertThat(node.getOperatorName()).isEqualTo("gte");
            assertThat(node.getParams()).containsEntry("threshold", 100);
            assertThat(node.getReasonCode()).isEqualTo("RC001");
            assertThat(node.getDisplayOrder()).isEqualTo(1);
            assertThat(node.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should throw when id is null")
        void shouldThrowWhenIdNull() {
            CondNode.Builder builder = CondNode.builder()
                    .operatorName("gte");
            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("Should throw when id is blank")
        void shouldThrowWhenIdBlank() {
            CondNode.Builder builder = CondNode.builder()
                    .id("  ")
                    .operatorName("gte");
            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("Should throw when operatorName is null")
        void shouldThrowWhenOperatorNameNull() {
            CondNode.Builder builder = CondNode.builder()
                    .id("cond-1");
            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("operatorName");
        }

        @Test
        @DisplayName("Should throw when operatorName is blank")
        void shouldThrowWhenOperatorNameBlank() {
            CondNode.Builder builder = CondNode.builder()
                    .id("cond-1")
                    .operatorName("");
            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("operatorName");
        }

        @Test
        @DisplayName("Should default displayOrder to 0")
        void shouldDefaultDisplayOrder() {
            CondNode node = CondNode.builder()
                    .id("cond-1")
                    .operatorName("gte")
                    .build();
            assertThat(node.getDisplayOrder()).isZero();
        }

        @Test
        @DisplayName("Should allow null parentId")
        void shouldAllowNullParentId() {
            CondNode node = CondNode.builder()
                    .id("cond-1")
                    .operatorName("gte")
                    .build();
            assertThat(node.getParentId()).isNull();
        }
    }
}
