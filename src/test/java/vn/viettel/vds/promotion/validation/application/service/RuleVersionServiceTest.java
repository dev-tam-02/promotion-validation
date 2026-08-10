package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.application.port.out.RuleVersionPersistencePort;
import vn.viettel.vds.promotion.validation.domain.exception.RuleVersionNotFoundException;
import vn.viettel.vds.promotion.validation.domain.model.RuleVersion;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link RuleVersionService}.
 * Verifies rule version CRUD operations, version comparison, and version number management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuleVersionService Tests")
class RuleVersionServiceTest {

    @Mock
    private RuleVersionPersistencePort ruleVersionPersistencePort;

    private RuleVersionService sut;

    @BeforeEach
    void setUp() {
        sut = new RuleVersionService(ruleVersionPersistencePort);
    }

    private RuleVersion testVersion(String ruleId, Integer version) {
        return RuleVersion.builder()
                .id("rv-" + version)
                .ruleId(ruleId)
                .version(version)
                .code("RULE_CODE")
                .logic(RuleVersion.LogicType.ALL)
                .nodes(List.of())
                .limits(Map.of())
                .operatorsFingerprint("fingerprint-" + version)
                .createdAt(Instant.now())
                .createdBy("test-user")
                .build();
    }

    @Nested
    @DisplayName("getRuleVersion()")
    class GetRuleVersionTests {

        @Test
        @DisplayName("Should return version when found")
        void shouldReturnVersion() {
            // Given
            RuleVersion rv = testVersion("r-1", 1);
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 1))
                    .thenReturn(Optional.of(rv));

            // When
            RuleVersion result = sut.getRuleVersion("r-1", 1);

            // Then
            assertThat(result.getRuleId()).isEqualTo("r-1");
            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getCode()).isEqualTo("RULE_CODE");
            verify(ruleVersionPersistencePort).findByRuleIdAndVersion("r-1", 1);
        }

        @Test
        @DisplayName("Should throw when version not found")
        void shouldThrowWhenNotFound() {
            // Given
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 99))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sut.getRuleVersion("r-1", 99))
                    .isInstanceOf(RuleVersionNotFoundException.class)
                    .hasMessageContaining("r-1")
                    .hasMessageContaining("99");
            verify(ruleVersionPersistencePort).findByRuleIdAndVersion("r-1", 99);
        }

        @Test
        @DisplayName("Should handle different version numbers")
        void shouldHandleDifferentVersions() {
            // Given
            RuleVersion rv3 = testVersion("r-1", 3);
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 3))
                    .thenReturn(Optional.of(rv3));

            // When
            RuleVersion result = sut.getRuleVersion("r-1", 3);

            // Then
            assertThat(result.getVersion()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getNextVersionNumber()")
    class GetNextVersionNumberTests {

        @Test
        @DisplayName("Should return 1 when no versions exist")
        void shouldReturn1WhenNoVersions() {
            // Given
            when(ruleVersionPersistencePort.findFirstByRuleIdOrderByVersionDesc("r-1"))
                    .thenReturn(Optional.empty());

            // When
            Integer nextVersion = sut.getNextVersionNumber("r-1");

            // Then
            assertThat(nextVersion).isEqualTo(1);
            verify(ruleVersionPersistencePort).findFirstByRuleIdOrderByVersionDesc("r-1");
        }

        @Test
        @DisplayName("Should return current + 1")
        void shouldReturnNextVersion() {
            // Given
            when(ruleVersionPersistencePort.findFirstByRuleIdOrderByVersionDesc("r-1"))
                    .thenReturn(Optional.of(testVersion("r-1", 3)));

            // When
            Integer nextVersion = sut.getNextVersionNumber("r-1");

            // Then
            assertThat(nextVersion).isEqualTo(4);
        }

        @Test
        @DisplayName("Should return 1 when latest version is null")
        void shouldReturn1WhenVersionNull() {
            // Given
            RuleVersion rv = RuleVersion.builder()
                    .id("rv-1")
                    .ruleId("r-1")
                    .version(null)  // null version
                    .build();
            when(ruleVersionPersistencePort.findFirstByRuleIdOrderByVersionDesc("r-1"))
                    .thenReturn(Optional.of(rv));

            // When
            Integer nextVersion = sut.getNextVersionNumber("r-1");

            // Then
            assertThat(nextVersion).isEqualTo(1);
        }

        @Test
        @DisplayName("Should increment from version 1")
        void shouldIncrementFromVersion1() {
            // Given
            when(ruleVersionPersistencePort.findFirstByRuleIdOrderByVersionDesc("r-1"))
                    .thenReturn(Optional.of(testVersion("r-1", 1)));

            // When
            Integer nextVersion = sut.getNextVersionNumber("r-1");

            // Then
            assertThat(nextVersion).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("versionExists()")
    class VersionExistsTests {

        @Test
        @DisplayName("Should return true when version exists")
        void shouldReturnTrue() {
            // Given
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 1))
                    .thenReturn(Optional.of(testVersion("r-1", 1)));

            // When
            boolean exists = sut.versionExists("r-1", 1);

            // Then
            assertThat(exists).isTrue();
            verify(ruleVersionPersistencePort).findByRuleIdAndVersion("r-1", 1);
        }

        @Test
        @DisplayName("Should return false when version not exists")
        void shouldReturnFalse() {
            // Given
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 99))
                    .thenReturn(Optional.empty());

            // When
            boolean exists = sut.versionExists("r-1", 99);

            // Then
            assertThat(exists).isFalse();
            verify(ruleVersionPersistencePort).findByRuleIdAndVersion("r-1", 99);
        }
    }

    @Nested
    @DisplayName("countVersions()")
    class CountVersionsTests {

        @Test
        @DisplayName("Should return count from port")
        void shouldReturnCount() {
            // Given
            when(ruleVersionPersistencePort.countByRuleId("r-1")).thenReturn(5L);

            // When
            long count = sut.countVersions("r-1");

            // Then
            assertThat(count).isEqualTo(5L);
            verify(ruleVersionPersistencePort).countByRuleId("r-1");
        }

        @Test
        @DisplayName("Should return zero when no versions")
        void shouldReturnZero() {
            // Given
            when(ruleVersionPersistencePort.countByRuleId("r-1")).thenReturn(0L);

            // When
            long count = sut.countVersions("r-1");

            // Then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("compareVersions()")
    class CompareVersionsTests {

        @Test
        @DisplayName("Should detect no differences for identical versions")
        void shouldDetectNoDifferences() {
            // Given
            RuleVersion v1 = RuleVersion.builder()
                    .id("rv-1")
                    .ruleId("r-1")
                    .version(1)
                    .nodes(List.of(Map.of("type", "COND")))
                    .limits(Map.of("max", 10))
                    .logic(RuleVersion.LogicType.ALL)
                    .build();
            RuleVersion v2 = RuleVersion.builder()
                    .id("rv-2")
                    .ruleId("r-1")
                    .version(2)
                    .nodes(List.of(Map.of("type", "COND")))
                    .limits(Map.of("max", 10))
                    .logic(RuleVersion.LogicType.ALL)
                    .build();

            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 1))
                    .thenReturn(Optional.of(v1));
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 2))
                    .thenReturn(Optional.of(v2));

            // When
            RuleVersionService.VersionDiff diff = sut.compareVersions("r-1", 1, 2);

            // Then
            assertThat(diff.hasDifferences()).isFalse();
            assertThat(diff.isNodesDifferent()).isFalse();
            assertThat(diff.isLimitsDifferent()).isFalse();
            assertThat(diff.isLogicDifferent()).isFalse();
            assertThat(diff.getChanges()).isEmpty();
        }

        @Test
        @DisplayName("Should detect differences when logic changed")
        void shouldDetectLogicDifference() {
            // Given
            RuleVersion v1 = RuleVersion.builder()
                    .id("rv-1")
                    .ruleId("r-1")
                    .version(1)
                    .logic(RuleVersion.LogicType.ALL)
                    .build();
            RuleVersion v2 = RuleVersion.builder()
                    .id("rv-2")
                    .ruleId("r-1")
                    .version(2)
                    .logic(RuleVersion.LogicType.ANY)
                    .build();

            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 1))
                    .thenReturn(Optional.of(v1));
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 2))
                    .thenReturn(Optional.of(v2));

            // When
            RuleVersionService.VersionDiff diff = sut.compareVersions("r-1", 1, 2);

            // Then
            assertThat(diff.hasDifferences()).isTrue();
            assertThat(diff.isLogicDifferent()).isTrue();
            assertThat(diff.isNodesDifferent()).isFalse();
            assertThat(diff.isLimitsDifferent()).isFalse();
            assertThat(diff.getChanges()).contains("Root logic modified");
        }

        @Test
        @DisplayName("Should detect differences when nodes changed")
        void shouldDetectNodesDifference() {
            // Given
            RuleVersion v1 = RuleVersion.builder()
                    .id("rv-1")
                    .ruleId("r-1")
                    .version(1)
                    .nodes(List.of(Map.of("type", "COND")))
                    .build();
            RuleVersion v2 = RuleVersion.builder()
                    .id("rv-2")
                    .ruleId("r-1")
                    .version(2)
                    .nodes(List.of(Map.of("type", "ACTION")))
                    .build();

            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 1))
                    .thenReturn(Optional.of(v1));
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 2))
                    .thenReturn(Optional.of(v2));

            // When
            RuleVersionService.VersionDiff diff = sut.compareVersions("r-1", 1, 2);

            // Then
            assertThat(diff.hasDifferences()).isTrue();
            assertThat(diff.isNodesDifferent()).isTrue();
            assertThat(diff.getChanges()).contains("Rule nodes modified");
        }

        @Test
        @DisplayName("Should detect differences when limits changed")
        void shouldDetectLimitsDifference() {
            // Given
            RuleVersion v1 = RuleVersion.builder()
                    .id("rv-1")
                    .ruleId("r-1")
                    .version(1)
                    .limits(Map.of("max", 10))
                    .build();
            RuleVersion v2 = RuleVersion.builder()
                    .id("rv-2")
                    .ruleId("r-1")
                    .version(2)
                    .limits(Map.of("max", 20))
                    .build();

            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 1))
                    .thenReturn(Optional.of(v1));
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 2))
                    .thenReturn(Optional.of(v2));

            // When
            RuleVersionService.VersionDiff diff = sut.compareVersions("r-1", 1, 2);

            // Then
            assertThat(diff.hasDifferences()).isTrue();
            assertThat(diff.isLimitsDifferent()).isTrue();
            assertThat(diff.getChanges()).contains("Rule limits modified");
        }

        @Test
        @DisplayName("Should detect multiple differences")
        void shouldDetectMultipleDifferences() {
            // Given
            RuleVersion v1 = RuleVersion.builder()
                    .id("rv-1")
                    .ruleId("r-1")
                    .version(1)
                    .logic(RuleVersion.LogicType.ALL)
                    .nodes(List.of(Map.of("type", "COND")))
                    .limits(Map.of("max", 10))
                    .build();
            RuleVersion v2 = RuleVersion.builder()
                    .id("rv-2")
                    .ruleId("r-1")
                    .version(2)
                    .logic(RuleVersion.LogicType.ANY)
                    .nodes(List.of(Map.of("type", "ACTION")))
                    .limits(Map.of("max", 20))
                    .build();

            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 1))
                    .thenReturn(Optional.of(v1));
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 2))
                    .thenReturn(Optional.of(v2));

            // When
            RuleVersionService.VersionDiff diff = sut.compareVersions("r-1", 1, 2);

            // Then
            assertThat(diff.hasDifferences()).isTrue();
            assertThat(diff.isLogicDifferent()).isTrue();
            assertThat(diff.isNodesDifferent()).isTrue();
            assertThat(diff.isLimitsDifferent()).isTrue();
            assertThat(diff.getChanges()).hasSize(3);
            assertThat(diff.getChanges()).contains(
                    "Rule nodes modified",
                    "Rule limits modified",
                    "Root logic modified"
            );
        }

        @Test
        @DisplayName("Should handle null values in comparison")
        void shouldHandleNullValues() {
            // Given
            RuleVersion v1 = RuleVersion.builder()
                    .id("rv-1")
                    .ruleId("r-1")
                    .version(1)
                    .logic(null)
                    .nodes(null)
                    .limits(null)
                    .build();
            RuleVersion v2 = RuleVersion.builder()
                    .id("rv-2")
                    .ruleId("r-1")
                    .version(2)
                    .logic(null)
                    .nodes(null)
                    .limits(null)
                    .build();

            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 1))
                    .thenReturn(Optional.of(v1));
            when(ruleVersionPersistencePort.findByRuleIdAndVersion("r-1", 2))
                    .thenReturn(Optional.of(v2));

            // When
            RuleVersionService.VersionDiff diff = sut.compareVersions("r-1", 1, 2);

            // Then
            assertThat(diff.hasDifferences()).isFalse();
        }
    }

    @Nested
    @DisplayName("getLatestRuleVersion()")
    class GetLatestRuleVersionTests {

        @Test
        @DisplayName("Should return latest version")
        void shouldReturnLatest() {
            // Given
            RuleVersion rv = testVersion("r-1", 3);
            when(ruleVersionPersistencePort.findFirstByRuleIdOrderByVersionDesc("r-1"))
                    .thenReturn(Optional.of(rv));

            // When
            Optional<RuleVersion> result = sut.getLatestRuleVersion("r-1");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getVersion()).isEqualTo(3);
            assertThat(result.get().getRuleId()).isEqualTo("r-1");
            verify(ruleVersionPersistencePort).findFirstByRuleIdOrderByVersionDesc("r-1");
        }

        @Test
        @DisplayName("Should return empty when no versions")
        void shouldReturnEmptyWhenNone() {
            // Given
            when(ruleVersionPersistencePort.findFirstByRuleIdOrderByVersionDesc("r-1"))
                    .thenReturn(Optional.empty());

            // When
            Optional<RuleVersion> result = sut.getLatestRuleVersion("r-1");

            // Then
            assertThat(result).isEmpty();
            verify(ruleVersionPersistencePort).findFirstByRuleIdOrderByVersionDesc("r-1");
        }
    }

    @Nested
    @DisplayName("getAllRuleVersions()")
    class GetAllRuleVersionsTests {

        @Test
        @DisplayName("Should return all versions sorted desc")
        void shouldReturnAllVersions() {
            // Given
            List<RuleVersion> versions = List.of(
                    testVersion("r-1", 3),
                    testVersion("r-1", 2),
                    testVersion("r-1", 1)
            );
            when(ruleVersionPersistencePort.findByRuleIdOrderByVersionDesc("r-1"))
                    .thenReturn(versions);

            // When
            List<RuleVersion> result = sut.getAllRuleVersions("r-1");

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getVersion()).isEqualTo(3);
            assertThat(result.get(1).getVersion()).isEqualTo(2);
            assertThat(result.get(2).getVersion()).isEqualTo(1);
            verify(ruleVersionPersistencePort).findByRuleIdOrderByVersionDesc("r-1");
        }

        @Test
        @DisplayName("Should return empty list when no versions")
        void shouldReturnEmptyList() {
            // Given
            when(ruleVersionPersistencePort.findByRuleIdOrderByVersionDesc("r-1"))
                    .thenReturn(List.of());

            // When
            List<RuleVersion> result = sut.getAllRuleVersions("r-1");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRuleVersions()")
    class GetRuleVersionsWithPaginationTests {

        @Test
        @DisplayName("Should return paginated versions")
        void shouldReturnPaginatedVersions() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<RuleVersion> versions = List.of(
                    testVersion("r-1", 3),
                    testVersion("r-1", 2),
                    testVersion("r-1", 1)
            );
            Page<RuleVersion> page = new PageImpl<>(versions, pageable, 3);
            when(ruleVersionPersistencePort.findByRuleId("r-1", pageable))
                    .thenReturn(page);

            // When
            Page<RuleVersion> result = sut.getRuleVersions("r-1", pageable);

            // Then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getNumber()).isZero();
            verify(ruleVersionPersistencePort).findByRuleId("r-1", pageable);
        }
    }

    @Nested
    @DisplayName("getRuleVersionByBundleHash()")
    class GetRuleVersionByBundleHashTests {

        @Test
        @DisplayName("Should return version by bundle hash")
        void shouldReturnVersionByBundleHash() {
            // Given
            RuleVersion rv = testVersion("r-1", 1);
            when(ruleVersionPersistencePort.findByBundleHash("hash-123"))
                    .thenReturn(Optional.of(rv));

            // When
            Optional<RuleVersion> result = sut.getRuleVersionByBundleHash("hash-123");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getRuleId()).isEqualTo("r-1");
        }

        @Test
        @DisplayName("Should return empty when bundle hash not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(ruleVersionPersistencePort.findByBundleHash("unknown"))
                    .thenReturn(Optional.empty());

            // When
            Optional<RuleVersion> result = sut.getRuleVersionByBundleHash("unknown");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRuleVersionsByCode()")
    class GetRuleVersionsByCodeTests {

        @Test
        @DisplayName("Should return versions by code")
        void shouldReturnVersionsByCode() {
            // Given
            List<RuleVersion> versions = List.of(
                    testVersion("r-1", 2),
                    testVersion("r-1", 1)
            );
            when(ruleVersionPersistencePort.findByCodeOrderByVersionDesc("RULE_CODE"))
                    .thenReturn(versions);

            // When
            List<RuleVersion> result = sut.getRuleVersionsByCode("RULE_CODE");

            // Then
            assertThat(result).hasSize(2);
            verify(ruleVersionPersistencePort).findByCodeOrderByVersionDesc("RULE_CODE");
        }
    }

    @Nested
    @DisplayName("findVersionsWithOperatorsFingerprint()")
    class FindVersionsWithOperatorsFingerprintTests {

        @Test
        @DisplayName("Should return versions with matching fingerprint")
        void shouldReturnVersionsWithFingerprint() {
            // Given
            List<RuleVersion> versions = List.of(
                    testVersion("r-1", 1),
                    testVersion("r-2", 1)
            );
            when(ruleVersionPersistencePort.findByOperatorsFingerprint("fingerprint-1"))
                    .thenReturn(versions);

            // When
            List<RuleVersion> result = sut.findVersionsWithOperatorsFingerprint("fingerprint-1");

            // Then
            assertThat(result).hasSize(2);
            verify(ruleVersionPersistencePort).findByOperatorsFingerprint("fingerprint-1");
        }
    }
}
