package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DisplayName("GroupNode Tests")
class GroupNodeTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {
        @Test
        @DisplayName("Should build valid GroupNode with all fields")
        void shouldBuildValidGroupNode() {
            Instant now = Instant.now();
            GroupNode node = GroupNode.builder()
                    .id("group-1")
                    .ruleId("rule-1")
                    .parentId("parent-1")
                    .groupLogic(GroupNode.GroupLogic.AND)
                    .displayOrder(1)
                    .createdAt(now)
                    .build();

            assertThat(node.getId()).isEqualTo("group-1");
            assertThat(node.getRuleId()).isEqualTo("rule-1");
            assertThat(node.getParentId()).isEqualTo("parent-1");
            assertThat(node.getGroupLogic()).isEqualTo(GroupNode.GroupLogic.AND);
            assertThat(node.getDisplayOrder()).isEqualTo(1);
            assertThat(node.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should throw when id is null")
        void shouldThrowWhenIdNull() {
            assertThatThrownBy(() -> GroupNode.builder()
                    .groupLogic(GroupNode.GroupLogic.AND)
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("Should throw when groupLogic is null")
        void shouldThrowWhenGroupLogicNull() {
            assertThatThrownBy(() -> GroupNode.builder()
                    .id("group-1")
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("groupLogic");
        }

        @Test
        @DisplayName("Should initialize empty children list")
        void shouldInitializeEmptyChildren() {
            GroupNode node = GroupNode.builder()
                    .id("group-1")
                    .groupLogic(GroupNode.GroupLogic.OR)
                    .build();
            assertThat(node.getChildren()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should add child via addChild")
        void shouldAddChild() {
            CondNode child = CondNode.builder()
                    .id("cond-1")
                    .operatorName("gte")
                    .build();
            GroupNode node = GroupNode.builder()
                    .id("group-1")
                    .groupLogic(GroupNode.GroupLogic.AND)
                    .addChild(child)
                    .build();
            assertThat(node.getChildren()).hasSize(1);
        }

        @Test
        @DisplayName("Should handle null children list")
        void shouldHandleNullChildren() {
            GroupNode node = GroupNode.builder()
                    .id("group-1")
                    .groupLogic(GroupNode.GroupLogic.AND)
                    .children(null)
                    .build();
            assertThat(node.getChildren()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("isRoot()")
    class IsRootTests {
        @Test
        @DisplayName("Should return true when parentId is null")
        void shouldReturnTrueWhenRoot() {
            GroupNode node = GroupNode.builder()
                    .id("group-1")
                    .groupLogic(GroupNode.GroupLogic.AND)
                    .build();
            assertThat(node.isRoot()).isTrue();
        }

        @Test
        @DisplayName("Should return false when parentId is set")
        void shouldReturnFalseWhenNotRoot() {
            GroupNode node = GroupNode.builder()
                    .id("group-1")
                    .parentId("parent-1")
                    .groupLogic(GroupNode.GroupLogic.AND)
                    .build();
            assertThat(node.isRoot()).isFalse();
        }
    }

    @Nested
    @DisplayName("GroupLogic enum")
    class GroupLogicTests {
        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveExpectedValues() {
            assertThat(GroupNode.GroupLogic.values()).containsExactly(
                    GroupNode.GroupLogic.AND,
                    GroupNode.GroupLogic.OR,
                    GroupNode.GroupLogic.NONE
            );
        }
    }
}
