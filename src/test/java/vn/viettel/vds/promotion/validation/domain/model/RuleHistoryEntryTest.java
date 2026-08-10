package vn.viettel.vds.promotion.validation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleHistoryEntry Tests")
class RuleHistoryEntryTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {
        @Test
        @DisplayName("Should create entry with all fields")
        void shouldCreateEntry() {
            Instant now = Instant.now();
            Map<String, Object> dsl = Map.of("logic", "AND");
            RuleHistoryEntry entry = new RuleHistoryEntry(
                    "h-1", "r-1", 1L,
                    RuleHistoryEntry.ChangeType.CREATE,
                    "admin", now, dsl, "hash123", "PUBLISHED", "Initial creation"
            );

            assertThat(entry.getId()).isEqualTo("h-1");
            assertThat(entry.getRuleId()).isEqualTo("r-1");
            assertThat(entry.getRuleVersion()).isEqualTo(1L);
            assertThat(entry.getChangeType()).isEqualTo(RuleHistoryEntry.ChangeType.CREATE);
            assertThat(entry.getChangedBy()).isEqualTo("admin");
            assertThat(entry.getChangedAt()).isEqualTo(now);
            assertThat(entry.getDslSnapshot()).containsEntry("logic", "AND");
            assertThat(entry.getBundleHash()).isEqualTo("hash123");
            assertThat(entry.getState()).isEqualTo("PUBLISHED");
            assertThat(entry.getChangeReason()).isEqualTo("Initial creation");
        }
    }

    @Nested
    @DisplayName("ChangeType enum")
    class ChangeTypeTests {
        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveExpectedValues() {
            assertThat(RuleHistoryEntry.ChangeType.values()).containsExactly(
                    RuleHistoryEntry.ChangeType.CREATE,
                    RuleHistoryEntry.ChangeType.UPDATE,
                    RuleHistoryEntry.ChangeType.RESTORE,
                    RuleHistoryEntry.ChangeType.ARCHIVE
            );
        }
    }
}
