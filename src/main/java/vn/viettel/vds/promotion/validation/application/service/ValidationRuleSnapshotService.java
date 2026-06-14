package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promix.platform.core.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.entity.*;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.RuleBindingJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleJpaRepository;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository.ValidationRuleSnapshotRepository;
import vn.viettel.vds.promotion.validation.application.service.dto.ValidationRuleSnapshotData;
import vn.viettel.vds.promotion.validation.domain.exception.SnapshotSerializationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing validation rule snapshots.
 * <p>
 * This service handles:
 * - Creating snapshots before update/delete operations
 * - Restoring rules from snapshots during saga compensation
 * - Cleaning up expired snapshots
 * <p>
 * Design: Aggregate Snapshot Pattern
 * - Stores complete rule aggregate (rule + nodes + limits + timeframes) as JSON
 * - Enables version-based rollback for saga compensation
 * - JSON format provides schema flexibility and easy debugging
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ValidationRuleSnapshotService {

    private static final int DEFAULT_SNAPSHOT_RETENTION_DAYS = 30;
    private final ValidationRuleSnapshotRepository snapshotRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final RuleBindingJpaRepository ruleBindingRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a snapshot capturing rule + optional binding state for later revert.
     * {@code bindingId} may be null for rule-only edits.
     *
     * <p>PROM-942 round 3: propagation = REQUIRES_NEW so that a failure here (e.g.
     * JSON serialization, unique-constraint race on
     * {@code idx_snapshot_rule_version}) is isolated from the caller's transaction.
     * Previously REQUIRED joined the outer {@code UpdateValidationRuleCommandHandler}
     * transaction; any RuntimeException thrown out of this method marked that tx
     * rollback-only, even though the caller caught the exception and treated the
     * snapshot as best-effort. The result was a silent rollback of the binding
     * update (rule_bindings.time_windows / active / updated_at all stayed at the
     * pre-update values) while the kafka SUCCESS event still went out (kafka send
     * is non-transactional). REQUIRES_NEW gives the snapshot its own tx so a
     * failure costs only the snapshot row, not the binding update.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ValidationRuleSnapshotEntity createSnapshot(
            String validationRuleId,
            String bindingId,
            String sagaId,
            String correlationId,
            ValidationRuleSnapshotEntity.SnapshotReason reason) {

        log.info("Creating snapshot for validation rule: {}, bindingId: {}, sagaId: {}, reason: {}",
                validationRuleId, bindingId, sagaId, reason);

        // Load the complete aggregate
        ValidationRuleEntity rule = validationRuleRepository.findById(validationRuleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Validation rule not found: " + validationRuleId));

        Long currentVersion = rule.getRuleVersion();

        // Check if snapshot already exists for this rule and version
        Optional<ValidationRuleSnapshotEntity> existingSnapshot =
                snapshotRepository.findByValidationRuleIdAndVersion(validationRuleId, currentVersion);

        if (existingSnapshot.isPresent()) {
            log.info("Snapshot already exists for rule: {} version: {}, returning existing snapshot: {}",
                    validationRuleId, currentVersion, existingSnapshot.get().getId());
            return existingSnapshot.get();
        }

        // Convert to snapshot data (rule + optional binding capture)
        ValidationRuleSnapshotData snapshotData = toSnapshotData(rule);
        if (bindingId != null && !bindingId.isBlank()) {
            ruleBindingRepository.findById(bindingId).ifPresent(binding ->
                    snapshotData.setBinding(toBindingSnapshot(binding)));
        }

        // Serialize to JSON
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotData);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize snapshot data for rule: {}", validationRuleId, e);
            throw new SnapshotSerializationException("Failed to serialize snapshot data", e);
        }

        // Create snapshot entity
        ValidationRuleSnapshotEntity snapshot = ValidationRuleSnapshotEntity.builder()
                .id(IdGenerator.generateId())
                .validationRuleId(validationRuleId)
                .version(currentVersion)
                .snapshotData(snapshotJson)
                .sagaId(sagaId)
                .correlationId(correlationId)
                .snapshotReason(reason)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(DEFAULT_SNAPSHOT_RETENTION_DAYS, ChronoUnit.DAYS))
                .build();

        ValidationRuleSnapshotEntity saved = snapshotRepository.save(snapshot);
        log.info("Created snapshot: {} for rule: {} version: {}",
                saved.getId(), validationRuleId, currentVersion);

        return saved;
    }

    /**
     * Create a snapshot for a RULE-LESS binding (timeframe-only campaign, no
     * validation rule attached). The snapshot row is keyed by the BINDING id in the
     * {@code validation_rule_id} column and by the binding's optimistic-lock version;
     * its data carries only the {@code binding} section (no rule fields).
     * {@link #restoreFromSnapshot} detects this shape and restores the binding alone.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ValidationRuleSnapshotEntity createBindingOnlySnapshot(
            String bindingId,
            String sagaId,
            String correlationId,
            ValidationRuleSnapshotEntity.SnapshotReason reason) {

        log.info("Creating binding-only snapshot: bindingId={}, sagaId={}, reason={}",
                bindingId, sagaId, reason);

        RuleBindingEntity binding = ruleBindingRepository.findById(bindingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rule binding not found: " + bindingId));

        Long currentVersion = binding.getVersion();

        Optional<ValidationRuleSnapshotEntity> existingSnapshot =
                snapshotRepository.findByValidationRuleIdAndVersion(bindingId, currentVersion);
        if (existingSnapshot.isPresent()) {
            log.info("Binding-only snapshot already exists for binding: {} version: {}, returning existing: {}",
                    bindingId, currentVersion, existingSnapshot.get().getId());
            return existingSnapshot.get();
        }

        ValidationRuleSnapshotData snapshotData = new ValidationRuleSnapshotData();
        snapshotData.setBinding(toBindingSnapshot(binding));

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotData);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize binding-only snapshot data for binding: {}", bindingId, e);
            throw new SnapshotSerializationException("Failed to serialize snapshot data", e);
        }

        ValidationRuleSnapshotEntity snapshot = ValidationRuleSnapshotEntity.builder()
                .id(IdGenerator.generateId())
                .validationRuleId(bindingId)
                .version(currentVersion)
                .snapshotData(snapshotJson)
                .sagaId(sagaId)
                .correlationId(correlationId)
                .snapshotReason(reason)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(DEFAULT_SNAPSHOT_RETENTION_DAYS, ChronoUnit.DAYS))
                .build();

        ValidationRuleSnapshotEntity saved = snapshotRepository.save(snapshot);
        log.info("Created binding-only snapshot: {} for binding: {} version: {}",
                saved.getId(), bindingId, currentVersion);
        return saved;
    }

    /**
     * Restore a validation rule from a snapshot.
     * This is used during saga compensation to revert to a previous version.
     *
     * @param validationRuleId the rule ID to restore — or the BINDING id for a
     *                         binding-only snapshot (rule-less timeframe binding)
     * @param targetVersion    the version to restore to
     * @return the restored rule entity, or {@code null} for a binding-only snapshot
     *         (only the binding row is restored, no rule exists)
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
            throw new SnapshotSerializationException("Failed to deserialize snapshot data", e);
        }

        // Binding-only snapshot (rule-less timeframe binding): no rule fields were
        // captured — restore the binding row and return null, there is no rule entity.
        if (snapshotData.getId() == null) {
            if (snapshotData.getBinding() != null && snapshotData.getBinding().getId() != null) {
                restoreBindingFromSnapshotData(snapshotData.getBinding());
                log.info("Restored binding-only snapshot: bindingId={}, version={}",
                        validationRuleId, targetVersion);
            } else {
                log.warn("Binding-only snapshot has no binding data — nothing to restore: subjectId={}, version={}",
                        validationRuleId, targetVersion);
            }
            return null;
        }

        // Load current entity
        ValidationRuleEntity rule = validationRuleRepository.findById(validationRuleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Validation rule not found: " + validationRuleId));

        // Restore from snapshot
        restoreRuleFromSnapshotData(rule, snapshotData);

        // Save restored entity
        ValidationRuleEntity restored = validationRuleRepository.save(rule);

        // Restore the rule_binding row separately when present — update path
        // modifies the binding (validFrom/validTo/timezone/rrule/timeWindows/
        // applicability/etc.) which is a distinct entity from validation_rule.
        if (snapshotData.getBinding() != null && snapshotData.getBinding().getId() != null) {
            restoreBindingFromSnapshotData(snapshotData.getBinding());
        }

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
     * @param keepCount        number of snapshots to keep
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
        // VRUL001: rules no longer carry a state — nothing to snapshot
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
                    .toList();
            data.setNodes(nodes);
        }

        // Time frames
        if (rule.getTimeFrames() != null) {
            List<ValidationRuleSnapshotData.TimeFrameSnapshot> timeFrames = rule.getTimeFrames().stream()
                    .map(this::toTimeFrameSnapshot)
                    .toList();
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

    private ValidationRuleSnapshotData.BindingSnapshot toBindingSnapshot(RuleBindingEntity binding) {
        ValidationRuleSnapshotData.BindingSnapshot snapshot =
                new ValidationRuleSnapshotData.BindingSnapshot();
        snapshot.setId(binding.getId());
        snapshot.setRuleId(binding.getRuleId());
        snapshot.setRuleVersionPinned(binding.getRuleVersionPinned());
        snapshot.setObjectType(binding.getObjectType());
        snapshot.setObjectId(binding.getObjectId());
        snapshot.setPriority(binding.getPriority());
        snapshot.setActive(binding.getActive());
        snapshot.setTrafficPercent(binding.getTrafficPercent());
        snapshot.setValidFrom(binding.getValidFrom());
        snapshot.setValidTo(binding.getValidTo());
        snapshot.setTimezone(binding.getTimezone());
        snapshot.setRrule(binding.getRrule());
        snapshot.setDuration(binding.getDuration());
        snapshot.setActivityDurationAfterPublishing(binding.getActivityDurationAfterPublishing());
        snapshot.setTimeWindows(binding.getTimeWindows());
        snapshot.setExcludedDates(binding.getExcludedDates());
        snapshot.setIncludedAll(binding.getIncludedAll());
        snapshot.setIncludedProducts(binding.getIncludedProducts());
        snapshot.setExcludedProducts(binding.getExcludedProducts());
        snapshot.setIncludedCategories(binding.getIncludedCategories());
        snapshot.setExcludedCategories(binding.getExcludedCategories());
        snapshot.setIncludedBrands(binding.getIncludedBrands());
        snapshot.setExcludedBrands(binding.getExcludedBrands());
        snapshot.setBundleHash(binding.getBundleHash());
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
        // VRUL001: rules no longer carry a state
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

    /**
     * Restore a {@code rule_binding} row from the binding section of the snapshot.
     * Looks up the binding by id (from the snapshot) and copies the captured
     * fields. Skips silently when the binding no longer exists (e.g. deleted
     * between snapshot creation and revert).
     */
    private void restoreBindingFromSnapshotData(ValidationRuleSnapshotData.BindingSnapshot snap) {
        ruleBindingRepository.findById(snap.getId()).ifPresentOrElse(binding -> {
            binding.setRuleId(snap.getRuleId());
            binding.setRuleVersionPinned(snap.getRuleVersionPinned());
            binding.setObjectType(snap.getObjectType());
            binding.setObjectId(snap.getObjectId());
            binding.setPriority(snap.getPriority());
            binding.setActive(snap.getActive());
            binding.setTrafficPercent(snap.getTrafficPercent());
            binding.setValidFrom(snap.getValidFrom());
            binding.setValidTo(snap.getValidTo());
            binding.setTimezone(snap.getTimezone());
            binding.setRrule(snap.getRrule());
            binding.setDuration(snap.getDuration());
            binding.setActivityDurationAfterPublishing(snap.getActivityDurationAfterPublishing());
            binding.setTimeWindows(snap.getTimeWindows());
            binding.setExcludedDates(snap.getExcludedDates());
            binding.setIncludedAll(snap.getIncludedAll());
            binding.setIncludedProducts(snap.getIncludedProducts());
            binding.setExcludedProducts(snap.getExcludedProducts());
            binding.setIncludedCategories(snap.getIncludedCategories());
            binding.setExcludedCategories(snap.getExcludedCategories());
            binding.setIncludedBrands(snap.getIncludedBrands());
            binding.setExcludedBrands(snap.getExcludedBrands());
            binding.setBundleHash(snap.getBundleHash());
            ruleBindingRepository.save(binding);
            log.info("Restored rule_binding: id={}, ruleId={}", binding.getId(), binding.getRuleId());
        }, () -> log.warn("Skipping binding restore — binding not found: id={}", snap.getId()));
    }
}
