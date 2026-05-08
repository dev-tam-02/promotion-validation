package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.TestFixtures;
import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OutboxEvent Tests")
class OutboxEventTest {

    @Nested
    @DisplayName("hasExceededMaxAttempts()")
    class HasExceededMaxAttemptsTests {

        @Test
        @DisplayName("Should return true when attempts >= maxAttempts")
        void shouldReturnTrue_whenExceeded() {
            var event = TestFixtures.failedEvent("evt-1", 3, 3);
            assertThat(event.hasExceededMaxAttempts()).isTrue();
        }

        @Test
        @DisplayName("Should return false when attempts < maxAttempts")
        void shouldReturnFalse_whenNotExceeded() {
            var event = TestFixtures.failedEvent("evt-1", 1, 3);
            assertThat(event.hasExceededMaxAttempts()).isFalse();
        }

        @Test
        @DisplayName("Should return false when attempts is null")
        void shouldReturnFalse_whenAttemptsNull() {
            var event = OutboxEvent.builder()
                    .id("evt-1")
                    .maxAttempts(3)
                    .build();

            assertThat(event.hasExceededMaxAttempts()).isFalse();
        }

        @Test
        @DisplayName("Should return false when maxAttempts is null")
        void shouldReturnFalse_whenMaxAttemptsNull() {
            var event = OutboxEvent.builder()
                    .id("evt-1")
                    .attempts(5)
                    .build();

            assertThat(event.hasExceededMaxAttempts()).isFalse();
        }
    }

    @Nested
    @DisplayName("canRetry()")
    class CanRetryTests {

        @Test
        @DisplayName("Should return true when FAILED and not exceeded max attempts")
        void shouldReturnTrue_whenCanRetry() {
            var event = TestFixtures.failedEvent("evt-1", 1, 3);
            assertThat(event.canRetry()).isTrue();
        }

        @Test
        @DisplayName("Should return false when not FAILED status")
        void shouldReturnFalse_whenNotFailed() {
            var event = TestFixtures.pendingEvent("evt-1");
            assertThat(event.canRetry()).isFalse();
        }

        @Test
        @DisplayName("Should return false when exceeded max attempts")
        void shouldReturnFalse_whenExceededMax() {
            var event = TestFixtures.failedEvent("evt-1", 3, 3);
            assertThat(event.canRetry()).isFalse();
        }
    }

    @Nested
    @DisplayName("isFinalState()")
    class IsFinalStateTests {

        @Test
        @DisplayName("Should return true when PUBLISHED")
        void shouldReturnTrue_whenPublished() {
            var event = OutboxEvent.builder()
                    .id("evt-1")
                    .status(OutboxEventStatus.PUBLISHED)
                    .build();

            assertThat(event.isFinalState()).isTrue();
        }

        @Test
        @DisplayName("Should return true when DEAD_LETTER")
        void shouldReturnTrue_whenDeadLetter() {
            var event = OutboxEvent.builder()
                    .id("evt-1")
                    .status(OutboxEventStatus.DEAD_LETTER)
                    .build();

            assertThat(event.isFinalState()).isTrue();
        }

        @Test
        @DisplayName("Should return false when PENDING")
        void shouldReturnFalse_whenPending() {
            var event = TestFixtures.pendingEvent("evt-1");
            assertThat(event.isFinalState()).isFalse();
        }

        @Test
        @DisplayName("Should return false when FAILED")
        void shouldReturnFalse_whenFailed() {
            var event = TestFixtures.failedEvent("evt-1", 1, 3);
            assertThat(event.isFinalState()).isFalse();
        }
    }

    @Nested
    @DisplayName("withIncrementedAttempts()")
    class WithIncrementedAttemptsTests {

        @Test
        @DisplayName("Should increment attempts from zero")
        void shouldIncrementFromZero() {
            var event = TestFixtures.pendingEvent("evt-1");
            var incremented = event.withIncrementedAttempts();

            assertThat(incremented.getAttempts()).isEqualTo(1);
            assertThat(incremented.getLastAttemptAt()).isNotNull();
        }

        @Test
        @DisplayName("Should increment existing attempts")
        void shouldIncrementExisting() {
            var event = TestFixtures.failedEvent("evt-1", 2, 5);
            var incremented = event.withIncrementedAttempts();

            assertThat(incremented.getAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle null attempts by setting to 1")
        void shouldHandleNullAttempts() {
            var event = OutboxEvent.builder().id("evt-1").build();
            var incremented = event.withIncrementedAttempts();

            assertThat(incremented.getAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not modify original event (immutability)")
        void shouldNotModifyOriginal() {
            var event = TestFixtures.pendingEvent("evt-1");
            event.withIncrementedAttempts();

            assertThat(event.getAttempts()).isZero();
        }
    }

    @Nested
    @DisplayName("markAsPublished()")
    class MarkAsPublishedTests {

        @Test
        @DisplayName("Should set status to PUBLISHED and set publishedAt")
        void shouldMarkAsPublished() {
            var event = TestFixtures.pendingEvent("evt-1");
            var published = event.markAsPublished();

            assertThat(published.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
            assertThat(published.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not modify original event")
        void shouldNotModifyOriginal() {
            var event = TestFixtures.pendingEvent("evt-1");
            event.markAsPublished();

            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("markAsFailed()")
    class MarkAsFailedTests {

        @Test
        @DisplayName("Should set status to FAILED when retries remain")
        void shouldMarkAsFailed_whenRetriesRemain() {
            var event = TestFixtures.failedEvent("evt-1", 1, 3);
            var failed = event.markAsFailed("Timeout");

            assertThat(failed.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(failed.getLastError()).isEqualTo("Timeout");
        }

        @Test
        @DisplayName("Should set status to DEAD_LETTER when max attempts exceeded")
        void shouldMarkAsDeadLetter_whenMaxExceeded() {
            var event = TestFixtures.failedEvent("evt-1", 3, 3);
            var failed = event.markAsFailed("Final failure");

            assertThat(failed.getStatus()).isEqualTo(OutboxEventStatus.DEAD_LETTER);
        }
    }

    @Nested
    @DisplayName("markAsProcessing()")
    class MarkAsProcessingTests {

        @Test
        @DisplayName("Should set status to PROCESSING")
        void shouldMarkAsProcessing() {
            var event = TestFixtures.pendingEvent("evt-1");
            var processing = event.markAsProcessing();

            assertThat(processing.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
            assertThat(processing.getLastAttemptAt()).isNotNull();
        }
    }
}
