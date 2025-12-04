package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promix.platform.core.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.*;
import vn.viettel.vds.promotion.validation.application.service.dto.AssignmentSnapshotData;
import vn.viettel.vds.promotion.validation.domain.exception.SnapshotSerializationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing assignment snapshots.
 *
 * This service handles:
 * - Creating snapshots before update/delete operations
 * - Restoring assignments from snapshots during saga compensation
 * - Cleaning up expired snapshots
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentSnapshotService {

    private final AssignmentSnapshotRepository snapshotRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final AssignmentApplicabilityRuleJpaRepository applicabilityRuleRepository;
    private final RuleTemporalLinkJpaRepository temporalLinkRepository;
    private final TemporalPolicyJpaRepository temporalPolicyRepository;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_SNAPSHOT_RETENTION_DAYS = 30;

    /**
     * Create a snapshot of the current assignment state before modification.
     *
     * @param assignmentId the assignment ID to snapshot
     * @param sagaId the saga ID (for correlation during compensation)
     * @param correlationId the correlation ID
     * @param reason the reason for creating the snapshot
     * @return the created snapshot entity with version info
     */
    @Transactional
    public AssignmentSnapshotEntity createSnapshot(
            String assignmentId,
            String sagaId,
            String correlationId,
            AssignmentSnapshotEntity.SnapshotReason reason) {

        log.info("Creating snapshot for assignment: {}, sagaId: {}, reason: {}",
                assignmentId, sagaId, reason);

        // Load the complete aggregate
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found: " + assignmentId));

        // Convert to snapshot data
        AssignmentSnapshotData snapshotData = toSnapshotData(assignment);

        // Serialize to JSON
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotData);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize snapshot data for assignment: {}", assignmentId, e);
            throw new SnapshotSerializationException("Failed to serialize snapshot data", e);
        }

        // Get next version number
        Long nextVersion = snapshotRepository.findLatestVersionByAssignmentId(assignmentId)
                .map(v -> v + 1)
                .orElse(1L);

        // Create snapshot entity
        AssignmentSnapshotEntity snapshot = AssignmentSnapshotEntity.builder()
                .id(IdGenerator.generateId())
                .assignmentId(assignmentId)
                .snapshotVersion(nextVersion)
                .snapshotData(snapshotJson)
                .sagaId(sagaId)
                .correlationId(correlationId)
                .snapshotReason(reason)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(DEFAULT_SNAPSHOT_RETENTION_DAYS, ChronoUnit.DAYS))
                .build();

        AssignmentSnapshotEntity saved = snapshotRepository.save(snapshot);
        log.info("Created snapshot: {} for assignment: {} version: {}",
                saved.getId(), assignmentId, nextVersion);

        return saved;
    }

    /**
     * Restore an assignment from a snapshot.
     *
     * @param assignmentId the assignment ID to restore
     * @param targetVersion the version to restore to
     * @return the restored assignment entity
     */
    @Transactional
    public AssignmentEntity restoreFromSnapshot(String assignmentId, Long targetVersion) {
        log.info("Restoring assignment: {} to version: {}", assignmentId, targetVersion);

        // Find the snapshot
        AssignmentSnapshotEntity snapshot = snapshotRepository
                .findByAssignmentIdAndSnapshotVersion(assignmentId, targetVersion)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Snapshot not found for assignment: %s version: %d",
                                assignmentId, targetVersion)));

        // Deserialize snapshot data
        AssignmentSnapshotData snapshotData;
        try {
            snapshotData = objectMapper.readValue(
                    snapshot.getSnapshotData(), AssignmentSnapshotData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize snapshot data for assignment: {}", assignmentId, e);
            throw new SnapshotSerializationException("Failed to deserialize snapshot data", e);
        }

        // Load current entity
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found: " + assignmentId));

        // Restore from snapshot
        restoreAssignmentFromSnapshotData(assignment, snapshotData);

        // Save restored entity
        AssignmentEntity restored = assignmentRepository.save(assignment);
        log.info("Restored assignment: {} to version: {}", assignmentId, targetVersion);

        return restored;
    }

    /**
     * Get the latest snapshot version for an assignment.
     */
    public Optional<Long> getLatestSnapshotVersion(String assignmentId) {
        return snapshotRepository.findLatestVersionByAssignmentId(assignmentId);
    }

    /**
     * Check if a snapshot exists.
     */
    public boolean snapshotExists(String assignmentId, Long version) {
        return snapshotRepository.existsByAssignmentIdAndSnapshotVersion(assignmentId, version);
    }

    /**
     * Convert an AssignmentEntity to snapshot data DTO.
     */
    private AssignmentSnapshotData toSnapshotData(AssignmentEntity assignment) {
        AssignmentSnapshotData data = new AssignmentSnapshotData();

        // Assignment basic info
        data.setId(assignment.getId());
        data.setEntityType(assignment.getEntityType());
        data.setEntityId(assignment.getEntityId());
        data.setRuleId(assignment.getRuleId());
        data.setPriority(assignment.getPriority());
        data.setActive(assignment.getActive());
        data.setTemporalBundleHash(assignment.getTemporalBundleHash());
        data.setIncludedAll(assignment.getIncludedAll());
        data.setCreatedAt(assignment.getCreatedAt());
        data.setUpdatedAt(assignment.getUpdatedAt());

        // Applicability rules
        List<AssignmentApplicabilityRuleEntity> applicabilityRules =
                applicabilityRuleRepository.findByAssignmentId(assignment.getId());
        if (applicabilityRules != null && !applicabilityRules.isEmpty()) {
            List<AssignmentSnapshotData.ApplicabilityRuleSnapshot> ruleSnapshots = applicabilityRules.stream()
                    .map(this::toApplicabilityRuleSnapshot)
                    .toList();
            data.setApplicabilityRules(ruleSnapshots);
        }

        // Temporal links
        List<RuleTemporalLinkEntity> temporalLinks =
                temporalLinkRepository.findByAssignmentId(assignment.getId());
        if (temporalLinks != null && !temporalLinks.isEmpty()) {
            List<AssignmentSnapshotData.TemporalLinkSnapshot> linkSnapshots = temporalLinks.stream()
                    .map(this::toTemporalLinkSnapshot)
                    .toList();
            data.setTemporalLinks(linkSnapshots);
        }

        return data;
    }

    private AssignmentSnapshotData.ApplicabilityRuleSnapshot toApplicabilityRuleSnapshot(
            AssignmentApplicabilityRuleEntity rule) {
        AssignmentSnapshotData.ApplicabilityRuleSnapshot snapshot =
                new AssignmentSnapshotData.ApplicabilityRuleSnapshot();
        snapshot.setId(rule.getId());
        snapshot.setRuleType(rule.getRuleType());
        snapshot.setObjectType(rule.getObjectType());
        snapshot.setObjectId(rule.getObjectId());
        snapshot.setEffect(rule.getEffect());
        snapshot.setTarget(rule.getTarget());
        snapshot.setSkipInitially(rule.getSkipInitially());
        snapshot.setRepeatCount(rule.getRepeatCount());
        snapshot.setCreatedAt(rule.getCreatedAt());
        snapshot.setUpdatedAt(rule.getUpdatedAt());
        return snapshot;
    }

    private AssignmentSnapshotData.TemporalLinkSnapshot toTemporalLinkSnapshot(RuleTemporalLinkEntity link) {
        AssignmentSnapshotData.TemporalLinkSnapshot snapshot = new AssignmentSnapshotData.TemporalLinkSnapshot();
        snapshot.setId(link.getId());
        snapshot.setMode(link.getMode());
        snapshot.setCreatedAt(link.getCreatedAt());
        snapshot.setUpdatedAt(link.getUpdatedAt());

        // Embed temporal policy
        if (link.getTemporalPolicy() != null) {
            snapshot.setTemporalPolicy(toTemporalPolicySnapshot(link.getTemporalPolicy()));
        }

        return snapshot;
    }

    private AssignmentSnapshotData.TemporalPolicySnapshot toTemporalPolicySnapshot(TemporalPolicyEntity policy) {
        AssignmentSnapshotData.TemporalPolicySnapshot snapshot = new AssignmentSnapshotData.TemporalPolicySnapshot();
        snapshot.setId(policy.getId());
        snapshot.setName(policy.getName());
        snapshot.setTz(policy.getTz());
        snapshot.setStartTs(policy.getStartTs());
        snapshot.setEndTs(policy.getEndTs());
        snapshot.setRrule(policy.getRrule());
        snapshot.setRdate(policy.getRdate());
        snapshot.setExrule(policy.getExrule());
        snapshot.setExdate(policy.getExdate());
        snapshot.setMetadata(policy.getMetadata());
        snapshot.setCreatedAt(policy.getCreatedAt());
        snapshot.setUpdatedAt(policy.getUpdatedAt());

        // Time windows
        if (policy.getTimeOfDayWindows() != null && !policy.getTimeOfDayWindows().isEmpty()) {
            List<AssignmentSnapshotData.TimeWindowSnapshot> windows = policy.getTimeOfDayWindows().stream()
                    .map(w -> {
                        AssignmentSnapshotData.TimeWindowSnapshot ws = new AssignmentSnapshotData.TimeWindowSnapshot();
                        ws.setId(w.getId());
                        ws.setStart(w.getStart());
                        ws.setEnd(w.getEnd());
                        return ws;
                    })
                    .toList();
            snapshot.setTimeWindows(windows);
        }

        return snapshot;
    }

    /**
     * Restore an AssignmentEntity from snapshot data.
     */
    private void restoreAssignmentFromSnapshotData(AssignmentEntity assignment,
                                                    AssignmentSnapshotData snapshotData) {
        // Restore basic info
        assignment.setEntityType(snapshotData.getEntityType());
        assignment.setEntityId(snapshotData.getEntityId());
        assignment.setRuleId(snapshotData.getRuleId());
        assignment.setPriority(snapshotData.getPriority());
        assignment.setActive(snapshotData.getActive());
        assignment.setTemporalBundleHash(snapshotData.getTemporalBundleHash());
        assignment.setIncludedAll(snapshotData.getIncludedAll());
        assignment.setUpdatedAt(Instant.now());

        // Delete and restore applicability rules
        applicabilityRuleRepository.deleteByAssignmentId(assignment.getId());
        if (snapshotData.getApplicabilityRules() != null) {
            for (AssignmentSnapshotData.ApplicabilityRuleSnapshot ruleSnapshot : snapshotData.getApplicabilityRules()) {
                AssignmentApplicabilityRuleEntity rule = new AssignmentApplicabilityRuleEntity();
                rule.setId(IdGenerator.generateId()); // New ID for restored entity
                rule.setAssignment(assignment);
                rule.setRuleType(ruleSnapshot.getRuleType());
                rule.setObjectType(ruleSnapshot.getObjectType());
                rule.setObjectId(ruleSnapshot.getObjectId());
                rule.setEffect(ruleSnapshot.getEffect());
                rule.setTarget(ruleSnapshot.getTarget());
                rule.setSkipInitially(ruleSnapshot.getSkipInitially());
                rule.setRepeatCount(ruleSnapshot.getRepeatCount());
                rule.setCreatedAt(Instant.now());
                rule.setUpdatedAt(Instant.now());
                applicabilityRuleRepository.save(rule);
            }
        }

        // Delete and restore temporal links with policies
        List<RuleTemporalLinkEntity> existingLinks = temporalLinkRepository.findByAssignmentId(assignment.getId());
        for (RuleTemporalLinkEntity link : existingLinks) {
            String policyId = link.getTemporalPolicy() != null ? link.getTemporalPolicy().getId() : null;
            temporalLinkRepository.delete(link);
            // Delete policy if no longer referenced
            if (policyId != null && temporalLinkRepository.findByTemporalPolicyId(policyId).isEmpty()) {
                temporalPolicyRepository.deleteById(policyId);
            }
        }

        if (snapshotData.getTemporalLinks() != null) {
            for (AssignmentSnapshotData.TemporalLinkSnapshot linkSnapshot : snapshotData.getTemporalLinks()) {
                // Create temporal policy first
                TemporalPolicyEntity policy = null;
                if (linkSnapshot.getTemporalPolicy() != null) {
                    policy = restoreTemporalPolicy(linkSnapshot.getTemporalPolicy());
                }

                // Create temporal link
                RuleTemporalLinkEntity link = new RuleTemporalLinkEntity();
                link.setId(IdGenerator.generateId());
                link.setAssignment(assignment);
                link.setTemporalPolicy(policy);
                link.setMode(linkSnapshot.getMode());
                link.setCreatedAt(Instant.now());
                link.setUpdatedAt(Instant.now());
                temporalLinkRepository.save(link);
            }
        }
    }

    private TemporalPolicyEntity restoreTemporalPolicy(AssignmentSnapshotData.TemporalPolicySnapshot policySnapshot) {
        TemporalPolicyEntity policy = new TemporalPolicyEntity();
        policy.setId(IdGenerator.generateId());
        policy.setName(policySnapshot.getName());
        policy.setTz(policySnapshot.getTz());
        policy.setStartTs(policySnapshot.getStartTs());
        policy.setEndTs(policySnapshot.getEndTs());
        policy.setRrule(policySnapshot.getRrule());
        policy.setRdate(policySnapshot.getRdate());
        policy.setExrule(policySnapshot.getExrule());
        policy.setExdate(policySnapshot.getExdate());
        policy.setMetadata(policySnapshot.getMetadata());
        policy.setCreatedAt(Instant.now());
        policy.setUpdatedAt(Instant.now());

        // Save policy first
        policy = temporalPolicyRepository.save(policy);

        // Restore time windows
        if (policySnapshot.getTimeWindows() != null) {
            List<TemporalPolicyWindowEntity> windows = new ArrayList<>();
            for (AssignmentSnapshotData.TimeWindowSnapshot ws : policySnapshot.getTimeWindows()) {
                TemporalPolicyWindowEntity window = new TemporalPolicyWindowEntity();
                window.setId(IdGenerator.generateId());
                window.setTemporalPolicy(policy);
                window.setStart(ws.getStart());
                window.setEnd(ws.getEnd());
                window.setCreatedAt(Instant.now());
                window.setUpdatedAt(Instant.now());
                windows.add(window);
            }
            policy.setTimeOfDayWindows(windows);
            policy = temporalPolicyRepository.save(policy);
        }

        return policy;
    }

    /**
     * Clean up expired snapshots.
     */
    @Transactional
    public int cleanupExpiredSnapshots() {
        int deleted = snapshotRepository.deleteExpiredSnapshots(Instant.now());
        log.info("Cleaned up {} expired assignment snapshots", deleted);
        return deleted;
    }
}
