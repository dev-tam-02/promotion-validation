package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.repository.AuditLogRepository;
import vn.viettel.vds.promotion.validation.domain.entity.AuditLog;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
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

            auditLogRepository.save(auditLog);

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
            return auditLogRepository.findWithFilters(tenantId, action, actorPattern, from, to, pageable);
        } else {
            return auditLogRepository.findByTenantIdOrderByAtDesc(tenantId, pageable);
        }
    }

    /**
     * Get audit logs for specific target
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsForTarget(String tenantId, String targetType, String targetId, Pageable pageable) {
        return auditLogRepository.findByTenantIdAndTargetTypeAndTargetIdOrderByAtDesc(tenantId, targetType, targetId)
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
            auditLogRepository.deleteLogsOlderThan(cutoffTime);
            logger.info("Cleaned up audit logs older than {}", cutoffTime);
        } catch (Exception e) {
            logger.error("Failed to cleanup old audit logs", e);
        }
    }
}