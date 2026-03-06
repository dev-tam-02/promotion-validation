package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.viettel.vds.promotion.validation.domain.exception.ReasonCodeAlreadyExistsException;
import vn.viettel.vds.promotion.validation.domain.exception.ReasonCodeNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.ReasonCodePersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.ReasonCode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReasonCodeService {

    private static final Logger logger = LoggerFactory.getLogger(ReasonCodeService.class);

    private final ReasonCodePersistencePort reasonCodePersistencePort;
    private final ReasonCodeService self;

    public ReasonCodeService(ReasonCodePersistencePort reasonCodePersistencePort,
                             @org.springframework.context.annotation.Lazy ReasonCodeService self) {
        this.reasonCodePersistencePort = reasonCodePersistencePort;
        this.self = self;
    }

    /**
     * Create a new reason code
     */
    public ReasonCode createReasonCode(String tenantId, String id, String category,
                                       ReasonCode.Severity severity, Map<String, Object> labels) {
        logger.info("Creating reason code: tenant={}, id={}", tenantId, id);

        // Check if reason code already exists
        if (reasonCodePersistencePort.existsByIdAndTenant(tenantId, id)) {
            throw new ReasonCodeAlreadyExistsException(id, tenantId);
        }

        Instant now = Instant.now();
        ReasonCode reasonCode = ReasonCode.builder()
                .id(id)
                .category(category)
                .severity(severity)
                .labels(labels)
                .createdAt(now)
                .version(0L)
                .build();

        ReasonCode saved = reasonCodePersistencePort.save(reasonCode);

        logger.info("Reason code created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Get reason code by ID and tenant
     */
    @Transactional(readOnly = true)
    public ReasonCode getReasonCode(String tenantId, String id) {
        return reasonCodePersistencePort.findByIdAndTenant(tenantId, id)
                .orElseThrow(() -> new ReasonCodeNotFoundException(id, tenantId));
    }

    /**
     * Check if reason code exists (including global codes)
     */
    @Transactional(readOnly = true)
    public boolean existsReasonCode(String tenantId, String id) {
        return reasonCodePersistencePort.existsByIdAndTenant(tenantId, id);
    }

    /**
     * Get reason codes by tenant and category
     */
    @Transactional(readOnly = true)
    public List<ReasonCode> getReasonCodesByCategory(String tenantId, String category) {
        return reasonCodePersistencePort.findByTenantAndCategory(tenantId, category);
    }

    /**
     * Find reason codes with filters
     */
    @Transactional(readOnly = true)
    public Page<ReasonCode> findReasonCodes(String tenantId, String categoryPattern,
                                            ReasonCode.Severity severity, Pageable pageable) {
        return reasonCodePersistencePort.findWithFilters(tenantId, categoryPattern, severity, pageable);
    }

    /**
     * Get all reason codes for tenant (including global)
     */
    @Transactional(readOnly = true)
    public Page<ReasonCode> getAllReasonCodes(String tenantId, Pageable pageable) {
        return reasonCodePersistencePort.findByTenant(tenantId, pageable);
    }

    /**
     * Get global reason codes
     */
    @Transactional(readOnly = true)
    public List<ReasonCode> getGlobalReasonCodes() {
        return reasonCodePersistencePort.findByTenantIdIsNull();
    }

    /**
     * Get reason codes by severity
     */
    @Transactional(readOnly = true)
    public List<ReasonCode> getReasonCodesBySeverity(String tenantId, ReasonCode.Severity severity) {
        return reasonCodePersistencePort.findByTenantAndSeverity(tenantId, severity);
    }

    /**
     * Update reason code labels
     */
    public ReasonCode updateReasonCodeLabels(String tenantId, String id, Map<String, Object> labels) {
        logger.info("Updating reason code labels: tenant={}, id={}", tenantId, id);

        ReasonCode reasonCode = self.getReasonCode(tenantId, id);
        ReasonCode updated = reasonCode.toBuilder()
                .labels(labels)
                .build();

        ReasonCode saved = reasonCodePersistencePort.save(updated);

        logger.info("Reason code labels updated successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Delete reason code (only tenant-specific codes)
     */
    public void deleteReasonCode(String tenantId, String id) {
        logger.info("Deleting reason code: tenant={}, id={}", tenantId, id);

        ReasonCode reasonCode = self.getReasonCode(tenantId, id);

        // Allow deletion
        reasonCodePersistencePort.delete(reasonCode);

        logger.info("Reason code deleted successfully: id={}", id);
    }

    /**
     * Get localized message for reason code
     */
    @Transactional(readOnly = true)
    public String getLocalizedMessage(String tenantId, String reasonCodeId, String locale) {
        try {
            ReasonCode reasonCode = self.getReasonCode(tenantId, reasonCodeId);

            if (reasonCode.getLabels() == null) {
                return reasonCodeId; // Fallback to ID
            }

            // Try specific locale first, then fallback to English, then any available
            Object messageObj = reasonCode.getLabels().get(locale);
            if (messageObj != null) {
                return messageObj.toString();
            }

            messageObj = reasonCode.getLabels().get("en");
            if (messageObj != null) {
                return messageObj.toString();
            }

            // Return first available message
            return reasonCode.getLabels().values().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse(reasonCodeId);

        } catch (Exception e) {
            logger.warn("Error getting localized message for reason code: {}", reasonCodeId, e);
            return reasonCodeId; // Fallback to ID
        }
    }

    /**
     * Batch create default reason codes for tenant
     */
    public void createDefaultReasonCodes(String tenantId) {
        logger.info("Creating default reason codes for tenant: {}", tenantId);

        List<DefaultReasonCode> defaults = getDefaultReasonCodes();

        for (DefaultReasonCode defaultCode : defaults) {
            if (!self.existsReasonCode(tenantId, defaultCode.getId())) {
                try {
                    createReasonCode(
                            tenantId,
                            defaultCode.getId(),
                            defaultCode.getCategory(),
                            defaultCode.getSeverity(),
                            defaultCode.getLabels()
                    );
                } catch (Exception e) {
                    logger.warn("Failed to create default reason code: {}", defaultCode.getId(), e);
                }
            }
        }

        logger.info("Default reason codes creation completed for tenant: {}", tenantId);
    }

    private List<DefaultReasonCode> getDefaultReasonCodes() {
        return List.of(
                new DefaultReasonCode("ORDER_TOTAL_MIN", "ORDER", ReasonCode.Severity.WARN,
                        Map.of("en", "Order total below minimum", "vi", "Tổng đơn hàng chưa đạt tối thiểu")),

                new DefaultReasonCode("CUSTOMER_SEGMENT_MISMATCH", "CUSTOMER", ReasonCode.Severity.WARN,
                        Map.of("en", "Customer not in required segment", "vi", "Khách hàng không thuộc phân khúc yêu cầu")),

                new DefaultReasonCode("TIME_WINDOW_INVALID", "TIME", ReasonCode.Severity.INFO,
                        Map.of("en", "Outside valid time window", "vi", "Ngoài khung thời gian có hiệu lực")),

                new DefaultReasonCode("LOCATION_RESTRICTED", "GEO", ReasonCode.Severity.WARN,
                        Map.of("en", "Location not eligible", "vi", "Khu vực không đủ điều kiện")),

                new DefaultReasonCode("USAGE_LIMIT_EXCEEDED", "LIMIT", ReasonCode.Severity.ERROR,
                        Map.of("en", "Usage limit exceeded", "vi", "Đã vượt quá giới hạn sử dụng")),

                new DefaultReasonCode("PRODUCT_CATEGORY_MISMATCH", "PRODUCT", ReasonCode.Severity.WARN,
                        Map.of("en", "Product category not eligible", "vi", "Danh mục sản phẩm không đủ điều kiện"))
        );
    }

    private static class DefaultReasonCode {
        private final String id;
        private final String category;
        private final ReasonCode.Severity severity;
        private final Map<String, Object> labels;

        public DefaultReasonCode(String id, String category, ReasonCode.Severity severity, Map<String, Object> labels) {
            this.id = id;
            this.category = category;
            this.severity = severity;
            this.labels = labels;
        }

        public String getId() {
            return id;
        }

        public String getCategory() {
            return category;
        }

        public ReasonCode.Severity getSeverity() {
            return severity;
        }

        public Map<String, Object> getLabels() {
            return labels;
        }
    }
}