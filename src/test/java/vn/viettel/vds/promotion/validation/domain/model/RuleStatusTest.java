package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleStatus Tests")
class RuleStatusTest {

    @Nested
    @DisplayName("canTransitionTo()")
    class CanTransitionToTests {

        @Test
        @DisplayName("Should allow DRAFT to PENDING_REVIEW")
        void shouldAllowDraftToPendingReview() {
            assertThat(RuleStatus.DRAFT.canTransitionTo(RuleStatus.PENDING_REVIEW)).isTrue();
        }

        @Test
        @DisplayName("Should allow DRAFT to ARCHIVED")
        void shouldAllowDraftToArchived() {
            assertThat(RuleStatus.DRAFT.canTransitionTo(RuleStatus.ARCHIVED)).isTrue();
        }

        @Test
        @DisplayName("Should not allow DRAFT to PUBLISHED")
        void shouldNotAllowDraftToPublished() {
            assertThat(RuleStatus.DRAFT.canTransitionTo(RuleStatus.PUBLISHED)).isFalse();
        }

        @Test
        @DisplayName("Should allow PENDING_REVIEW to APPROVED")
        void shouldAllowPendingReviewToApproved() {
            assertThat(RuleStatus.PENDING_REVIEW.canTransitionTo(RuleStatus.APPROVED)).isTrue();
        }

        @Test
        @DisplayName("Should allow PENDING_REVIEW to DRAFT (rejection)")
        void shouldAllowPendingReviewToDraft() {
            assertThat(RuleStatus.PENDING_REVIEW.canTransitionTo(RuleStatus.DRAFT)).isTrue();
        }

        @Test
        @DisplayName("Should allow APPROVED to PUBLISHED")
        void shouldAllowApprovedToPublished() {
            assertThat(RuleStatus.APPROVED.canTransitionTo(RuleStatus.PUBLISHED)).isTrue();
        }

        @Test
        @DisplayName("Should allow PUBLISHED to DEPRECATED")
        void shouldAllowPublishedToDeprecated() {
            assertThat(RuleStatus.PUBLISHED.canTransitionTo(RuleStatus.DEPRECATED)).isTrue();
        }

        @Test
        @DisplayName("Should allow DEPRECATED to ARCHIVED")
        void shouldAllowDeprecatedToArchived() {
            assertThat(RuleStatus.DEPRECATED.canTransitionTo(RuleStatus.ARCHIVED)).isTrue();
        }

        @Test
        @DisplayName("Should allow DEPRECATED back to PUBLISHED")
        void shouldAllowDeprecatedToPublished() {
            assertThat(RuleStatus.DEPRECATED.canTransitionTo(RuleStatus.PUBLISHED)).isTrue();
        }

        @Test
        @DisplayName("Should not allow any transition from ARCHIVED")
        void shouldNotAllowTransitionFromArchived() {
            for (RuleStatus status : RuleStatus.values()) {
                assertThat(RuleStatus.ARCHIVED.canTransitionTo(status)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("isEditable()")
    class IsEditableTests {

        @Test
        @DisplayName("Should be editable when DRAFT")
        void shouldBeEditableWhenDraft() {
            assertThat(RuleStatus.DRAFT.isEditable()).isTrue();
        }

        @Test
        @DisplayName("Should be editable when PENDING_REVIEW")
        void shouldBeEditableWhenPendingReview() {
            assertThat(RuleStatus.PENDING_REVIEW.isEditable()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = RuleStatus.class, names = {"APPROVED", "PUBLISHED", "DEPRECATED", "ARCHIVED"})
        @DisplayName("Should not be editable for non-editable statuses")
        void shouldNotBeEditable_forNonEditableStatuses(RuleStatus status) {
            assertThat(status.isEditable()).isFalse();
        }
    }

    @Nested
    @DisplayName("isActive()")
    class IsActiveTests {

        @Test
        @DisplayName("Should be active only when PUBLISHED")
        void shouldBeActiveOnlyWhenPublished() {
            assertThat(RuleStatus.PUBLISHED.isActive()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = RuleStatus.class, names = {"DRAFT", "PENDING_REVIEW", "APPROVED", "DEPRECATED", "ARCHIVED"})
        @DisplayName("Should not be active for non-published statuses")
        void shouldNotBeActive_forNonPublished(RuleStatus status) {
            assertThat(status.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("getDisplayName() and getDescription()")
    class MetadataTests {

        @Test
        @DisplayName("Should return display name")
        void shouldReturnDisplayName() {
            assertThat(RuleStatus.DRAFT.getDisplayName()).isEqualTo("Draft");
            assertThat(RuleStatus.PUBLISHED.getDisplayName()).isEqualTo("Published");
        }

        @Test
        @DisplayName("Should return description")
        void shouldReturnDescription() {
            assertThat(RuleStatus.DRAFT.getDescription()).isNotBlank();
            assertThat(RuleStatus.PUBLISHED.getDescription()).isNotBlank();
        }
    }
}
