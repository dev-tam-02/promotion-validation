package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.AuditLogPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.AuditLog;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogPersistencePort auditLogPersistencePort;

    public AuditService(AuditLogPersistencePort auditLogPersistencePort) {
        this.auditLogPersistencePort = auditLogPersistencePort;
    }

    /**
     * Audit stackable discount validation.
     */
    public void auditValidation(
            vn.viettel.vds.promotion.validation.application.port.in.command.ValidateStackableDiscountCommand command,
            vn.viettel.vds.promotion.validation.application.port.in.dto.ValidateStackableDiscountResult result
    ) {
        Map<String, Object> details = new HashMap<>();
        details.put("idempotencyKey", command.idempotencyKey());
        details.put("customerId", command.customerInfo().customerId());
        details.put("orderId", command.orderInfo().orderId());
        details.put("discountCount", command.getDiscountCount());
        details.put("decision", result.decision().name());
        details.put("validatedCount", result.getValidatedCount());
        details.put("rejectedCount", result.getRejectedCount());
        details.put("processingTimeMs", result.processingTimeMs());

        logAuditEvent(
                "system", // tenantId
                "validation-service", // actor
                AuditLog.AuditAction.RULE_EDIT, // Using existing action enum
                "stackable-discount", // targetType
                command.idempotencyKey(), // targetId
                details
        );
    }

    /**
     * Log rule creation
     */
    public void logRuleCreated(String tenantId, String ruleId, String actor) {
        logAuditEvent(tenantId, actor, AuditLog.AuditAction.RULE_CREATE, "rule", ruleId, null);
    }

    /**
     * Log rule update
     */
    public void logRuleUpdated(String tenantId, String ruleId, String actor) {
        logAuditEvent(tenantId, actor, AuditLog.AuditAction.RULE_EDIT, "rule", ruleId, null);
    }

    /**
     * Log rule published
     */
    public void logRulePublished(String tenantId, String ruleId, String actor, Map<String, Object> details) {
        logAuditEvent(tenantId, actor, AuditLog.AuditAction.RULE_PUBLISH, "rule", ruleId, details);
    }

    /**
     * Log rule archived
     */
    public void logRuleArchived(String tenantId, String ruleId, String actor) {
        logAuditEvent(tenantId, actor, AuditLog.AuditAction.RULE_EDIT, "rule", ruleId,
                Map.of("action", "archive"));
    }

    /**
     * Log operator creation
     */
    public void logOperatorCreated(String tenantId, String operatorId, String actor) {
        logAuditEvent(tenantId, actor, AuditLog.AuditAction.OP_CREATE, "operator", operatorId, null);
    }

    /**
     * Log assignment update
     */
    public void logAssignmentUpdated(String tenantId, String assignmentId, String actor, Map<String, Object> details) {
        logAuditEvent(tenantId, actor, AuditLog.AuditAction.ASSIGN_UPDATE, "assignment", assignmentId, details);
    }

    /**
     * Log generic audit event
     */
    public void logAuditEvent(String tenantId, String actor, AuditLog.AuditAction action,
                              String targetType, String targetId, Map<String, Object> diff) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setTenantId(tenantId);
            auditLog.setActor(actor);
            auditLog.setAction(action);

            AuditLog.AuditTarget target = new AuditLog.AuditTarget();
            target.setType(targetType);
            target.setId(targetId);
            auditLog.setTarget(target);

            auditLog.setDiff(diff != null ? diff : new HashMap<>());
            auditLog.setAt(Instant.now());

            auditLogPersistencePort.save(auditLog);

            logger.debug("Audit event logged: tenant={}, action={}, target={}/{}",
                    tenantId, action, targetType, targetId);
        } catch (Exception e) {
            // Don't fail the main operation if audit logging fails
            logger.error("Failed to log audit event: tenant={}, action={}, target={}/{}",
                    tenantId, action, targetType, targetId, e);
        }
    }

    /**
     * Get audit logs with filters
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(String tenantId, AuditLog.AuditAction action, String actorPattern,
                                       Instant from, Instant to, Pageable pageable) {
        if (action != null || actorPattern != null || from != null || to != null) {
            return auditLogPersistencePort.findWithFilters(tenantId, action, actorPattern, from, to, pageable);
        } else {
            return auditLogPersistencePort.findByTenantId(tenantId, pageable);
        }
    }

    /**
     * Get audit logs for specific target
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsForTarget(String tenantId, String targetType, String targetId, Pageable pageable) {
        return auditLogPersistencePort.findByTenantIdAndTarget(tenantId, targetType, targetId)
                .stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(
                                list.subList(
                                        (int) pageable.getOffset(),
                                        Math.min((int) (pageable.getOffset() + pageable.getPageSize()), list.size())
                                ),
                                pageable,
                                list.size()
                        )
                ));
    }

    /**
     * Clean up old audit logs
     */
    public void cleanupOldAuditLogs(Instant cutoffTime) {
        try {
            auditLogPersistencePort.deleteLogsOlderThan(cutoffTime);
            logger.info("Cleaned up audit logs older than {}", cutoffTime);
        } catch (Exception e) {
            logger.error("Failed to cleanup old audit logs", e);
        }
    }
}