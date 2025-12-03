package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promix.platform.core.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleSnapshotRepository;
import vn.viettel.vds.promotion.validation.application.service.dto.ValidationRuleSnapshotData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing validation rule snapshots.
 *
 * This service handles:
 * - Creating snapshots before update/delete operations
 * - Restoring rules from snapshots during saga compensation
 * - Cleaning up expired snapshots
 *
 * Design: Aggregate Snapshot Pattern
 * - Stores complete rule aggregate (rule + nodes + limits + timeframes) as JSON
 * - Enables version-based rollback for saga compensation
 * - JSON format provides schema flexibility and easy debugging
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ValidationRuleSnapshotService {

    private final ValidationRuleSnapshotRepository snapshotRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_SNAPSHOT_RETENTION_DAYS = 30;
    private static final int DEFAULT_MAX_SNAPSHOTS_PER_RULE = 10;

    /**
     * Create a snapshot of the current validation rule state before modification.
     *
     * @param validationRuleId the rule ID to snapshot
     * @param sagaId the saga ID (for correlation during compensation)
     * @param correlationId the correlation ID
     * @param reason the reason for creating the snapshot
     * @return the created snapshot entity
     */
    @Transactional
    public ValidationRuleSnapshotEntity createSnapshot(
            String validationRuleId,
            String sagaId,
            String correlationId,
            ValidationRuleSnapshotEntity.SnapshotReason reason) {

        log.info("Creating snapshot for validation rule: {}, sagaId: {}, reason: {}",
                validationRuleId, sagaId, reason);

        // Load the complete aggregate
        ValidationRuleEntity rule = validationRuleRepository.findById(validationRuleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Validation rule not found: " + validationRuleId));

        // Convert to snapshot data
        ValidationRuleSnapshotData snapshotData = toSnapshotData(rule);

        // Serialize to JSON
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotData);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize snapshot data for rule: {}", validationRuleId, e);
            throw new RuntimeException("Failed to serialize snapshot data", e);
        }

        // Create snapshot entity
        ValidationRuleSnapshotEntity snapshot = ValidationRuleSnapshotEntity.builder()
                .id(IdGenerator.generateId())
                .validationRuleId(validationRuleId)
                .version(rule.getRuleVersion())
                .snapshotData(snapshotJson)
                .sagaId(sagaId)
                .correlationId(correlationId)
                .snapshotReason(reason)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(DEFAULT_SNAPSHOT_RETENTION_DAYS, ChronoUnit.DAYS))
                .build();

        ValidationRuleSnapshotEntity saved = snapshotRepository.save(snapshot);
        log.info("Created snapshot: {} for rule: {} version: {}",
                saved.getId(), validationRuleId, rule.getRuleVersion());

        return saved;
    }

    /**
     * Restore a validation rule from a snapshot.
     * This is used during saga compensation to revert to a previous version.
     *
     * @param validationRuleId the rule ID to restore
     * @param targetVersion the version to restore to
     * @return the restored rule entity
     */
    @Transactional
    public ValidationRuleEntity restoreFromSnapshot(String validationRuleId, Long targetVersion) {
        log.info("Restoring validation rule: {} to version: {}", validationRuleId, targetVersion);

        // Find the snapshot
        ValidationRuleSnapshotEntity snapshot = snapshotRepository
                .findByValidationRuleIdAndVersion(validationRuleId, targetVersion)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Snapshot not found for rule: %s version: %d",
                                validationRuleId, targetVersion)));

        // Deserialize snapshot data
        ValidationRuleSnapshotData snapshotData;
        try {
            snapshotData = objectMapper.readValue(
                    snapshot.getSnapshotData(), ValidationRuleSnapshotData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize snapshot data for rule: {}", validationRuleId, e);
            throw new RuntimeException("Failed to deserialize snapshot data", e);
        }

        // Load current entity
        ValidationRuleEntity rule = validationRuleRepository.findById(validationRuleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Validation rule not found: " + validationRuleId));

        // Restore from snapshot
        restoreRuleFromSnapshotData(rule, snapshotData);

        // Save restored entity
        ValidationRuleEntity restored = validationRuleRepository.save(rule);
        log.info("Restored validation rule: {} to version: {}", validationRuleId, targetVersion);

        return restored;
    }

    /**
     * Check if a snapshot exists for the given rule and version.
     */
    public boolean snapshotExists(String validationRuleId, Long version) {
        return snapshotRepository.existsByValidationRuleIdAndVersion(validationRuleId, version);
    }

    /**
     * Get snapshot by rule ID and version.
     */
    public Optional<ValidationRuleSnapshotEntity> getSnapshot(String validationRuleId, Long version) {
        return snapshotRepository.findByValidationRuleIdAndVersion(validationRuleId, version);
    }

    /**
     * Get the latest snapshot for a rule.
     */
    public Optional<ValidationRuleSnapshotEntity> getLatestSnapshot(String validationRuleId) {
        return snapshotRepository.findFirstByValidationRuleIdOrderByVersionDesc(validationRuleId);
    }

    /**
     * Clean up expired snapshots.
     *
     * @return number of deleted snapshots
     */
    @Transactional
    public int cleanupExpiredSnapshots() {
        int deleted = snapshotRepository.deleteExpiredSnapshots(Instant.now());
        log.info("Cleaned up {} expired snapshots", deleted);
        return deleted;
    }

    /**
     * Keep only the most recent N snapshots for each rule.
     *
     * @param validationRuleId the rule ID
     * @param keepCount number of snapshots to keep
     * @return number of deleted snapshots
     */
    @Transactional
    public int pruneOldSnapshots(String validationRuleId, int keepCount) {
        int deleted = snapshotRepository.deleteOldSnapshots(validationRuleId, keepCount);
        log.info("Pruned {} old snapshots for rule: {}, keeping: {}",
                deleted, validationRuleId, keepCount);
        return deleted;
    }

    /**
     * Convert a ValidationRuleEntity to snapshot data DTO.
     */
    private ValidationRuleSnapshotData toSnapshotData(ValidationRuleEntity rule) {
        ValidationRuleSnapshotData data = new ValidationRuleSnapshotData();

        // Rule basic info
        data.setId(rule.getId());
        data.setCode(rule.getCode());
        data.setName(rule.getName());
        data.setState(rule.getState());
        data.setRuleVersion(rule.getRuleVersion());
        data.setLogic(rule.getLogic());
        data.setDsl(rule.getDsl());
        data.setPublishedAt(rule.getPublishedAt());
        data.setPublishedBy(rule.getPublishedBy());
        data.setBundleHash(rule.getBundleHash());
        data.setCreatedAt(rule.getCreatedAt());
        data.setCreatedBy(rule.getCreatedBy());
        data.setUpdatedAt(rule.getUpdatedAt());
        data.setUpdatedBy(rule.getUpdatedBy());
        data.setVersion(rule.getVersion());

        // Usage limits
        if (rule.getLimits() != null) {
            ValidationRuleSnapshotData.UsageLimitsSnapshot limits =
                    new ValidationRuleSnapshotData.UsageLimitsSnapshot();
            limits.setId(rule.getLimits().getId());
            limits.setPerCodeTotal(rule.getLimits().getPerCodeTotal());
            limits.setPerCustomer(rule.getLimits().getPerCustomer());
            limits.setPerDay(rule.getLimits().getPerDay());
            data.setLimits(limits);
        }

        // Nodes
        if (rule.getNodes() != null) {
            List<ValidationRuleSnapshotData.NodeSnapshot> nodes = rule.getNodes().stream()
                    .map(this::toNodeSnapshot)
                    .collect(Collectors.toList());
            data.setNodes(nodes);
        }

        // Time frames
        if (rule.getTimeFrames() != null) {
            List<ValidationRuleSnapshotData.TimeFrameSnapshot> timeFrames = rule.getTimeFrames().stream()
                    .map(this::toTimeFrameSnapshot)
                    .collect(Collectors.toList());
            data.setTimeFrames(timeFrames);
        }

        return data;
    }

    private ValidationRuleSnapshotData.NodeSnapshot toNodeSnapshot(RuleNodeEntity node) {
        ValidationRuleSnapshotData.NodeSnapshot snapshot = new ValidationRuleSnapshotData.NodeSnapshot();
        snapshot.setId(node.getId());
        snapshot.setNodeId(node.getNodeId());
        snapshot.setType(node.getType());
        snapshot.setGroupLogic(node.getGroupLogic());
        snapshot.setChildrenIds(node.getChildrenIds());
        snapshot.setOrder(node.getOrder());
        snapshot.setOperatorName(node.getOperatorName());
        snapshot.setParams(node.getParams());
        snapshot.setReasonCode(node.getReasonCode());
        snapshot.setParentId(node.getParent() != null ? node.getParent().getId() : null);
        return snapshot;
    }

    private ValidationRuleSnapshotData.TimeFrameSnapshot toTimeFrameSnapshot(RuleTimeFrameEntity timeFrame) {
        ValidationRuleSnapshotData.TimeFrameSnapshot snapshot =
                new ValidationRuleSnapshotData.TimeFrameSnapshot();
        snapshot.setId(timeFrame.getId());
        snapshot.setTimeFrameId(timeFrame.getTimeFrameId());
        snapshot.setMode(timeFrame.getMode());
        return snapshot;
    }

    /**
     * Restore a ValidationRuleEntity from snapshot data.
     */
    private void restoreRuleFromSnapshotData(ValidationRuleEntity rule,
                                              ValidationRuleSnapshotData snapshotData) {
        // Restore basic info
        rule.setCode(snapshotData.getCode());
        rule.setName(snapshotData.getName());
        rule.setState(snapshotData.getState());
        rule.setRuleVersion(snapshotData.getRuleVersion());
        rule.setLogic(snapshotData.getLogic());
        rule.setDsl(snapshotData.getDsl());
        rule.setPublishedAt(snapshotData.getPublishedAt());
        rule.setPublishedBy(snapshotData.getPublishedBy());
        rule.setBundleHash(snapshotData.getBundleHash());

        // Restore usage limits
        if (snapshotData.getLimits() != null) {
            if (rule.getLimits() == null) {
                rule.setLimits(new RuleUsageLimitsEntity());
                rule.getLimits().setValidationRule(rule);
            }
            rule.getLimits().setPerCodeTotal(snapshotData.getLimits().getPerCodeTotal());
            rule.getLimits().setPerCustomer(snapshotData.getLimits().getPerCustomer());
            rule.getLimits().setPerDay(snapshotData.getLimits().getPerDay());
        }

        // Clear and restore nodes
        rule.getNodes().clear();
        if (snapshotData.getNodes() != null) {
            for (ValidationRuleSnapshotData.NodeSnapshot nodeSnapshot : snapshotData.getNodes()) {
                RuleNodeEntity node = new RuleNodeEntity();
                node.setId(nodeSnapshot.getId());
                node.setNodeId(nodeSnapshot.getNodeId());
                node.setType(nodeSnapshot.getType());
                node.setGroupLogic(nodeSnapshot.getGroupLogic());
                node.setChildrenIds(nodeSnapshot.getChildrenIds());
                node.setOrder(nodeSnapshot.getOrder());
                node.setOperatorName(nodeSnapshot.getOperatorName());
                node.setParams(nodeSnapshot.getParams());
                node.setReasonCode(nodeSnapshot.getReasonCode());
                node.setValidationRule(rule);
                rule.getNodes().add(node);
            }
        }

        // Clear and restore time frames
        rule.getTimeFrames().clear();
        if (snapshotData.getTimeFrames() != null) {
            for (ValidationRuleSnapshotData.TimeFrameSnapshot tfSnapshot : snapshotData.getTimeFrames()) {
                RuleTimeFrameEntity tf = new RuleTimeFrameEntity();
                tf.setId(tfSnapshot.getId());
                tf.setTimeFrameId(tfSnapshot.getTimeFrameId());
                tf.setMode(tfSnapshot.getMode());
                tf.setValidationRule(rule);
                rule.getTimeFrames().add(tf);
            }
        }
    }
}
