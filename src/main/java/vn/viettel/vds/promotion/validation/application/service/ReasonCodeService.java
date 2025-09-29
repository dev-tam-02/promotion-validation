package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ResponseInfo;
import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.persistence.repository.ReasonCodeRepository;
import vn.viettel.vds.promotion.validation.domain.entity.ReasonCode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReasonCodeService {

    private static final Logger logger = LoggerFactory.getLogger(ReasonCodeService.class);

    private final ReasonCodeRepository reasonCodeRepository;

    public ReasonCodeService(ReasonCodeRepository reasonCodeRepository) {
        this.reasonCodeRepository = reasonCodeRepository;
    }

    /**
     * Create a new reason code
     */
    public ReasonCode createReasonCode(String tenantId, String id, String category,
                                     ReasonCode.Severity severity, Map<String, String> labels) {
        logger.info("Creating reason code: tenant={}, id={}", tenantId, id);

        // Check if reason code already exists
        if (reasonCodeRepository.existsByIdAndTenant(tenantId, id)) {
            throw new BusinessException(new ResponseInfo("REASON_CODE_EXISTS",
                "Reason code '" + id + "' already exists for tenant " + tenantId, 400));
        }

        ReasonCode reasonCode = new ReasonCode();
        reasonCode.setId(id);
        reasonCode.setTenantId(tenantId);
        reasonCode.setCategory(category);
        reasonCode.setSeverity(severity);
        reasonCode.setLabels(labels);
        reasonCode.setCreatedAt(Instant.now());

        ReasonCode saved = reasonCodeRepository.save(reasonCode);

        logger.info("Reason code created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Get reason code by ID and tenant
     */
    @Transactional(readOnly = true)
    public ReasonCode getReasonCode(String tenantId, String id) {
        return reasonCodeRepository.findByIdAndTenant(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException());
    }

    /**
     * Check if reason code exists (including global codes)
     */
    @Transactional(readOnly = true)
    public boolean existsReasonCode(String tenantId, String id) {
        return reasonCodeRepository.existsByIdAndTenant(tenantId, id);
    }

    /**
     * Get reason codes by tenant and category
     */
    @Transactional(readOnly = true)
    public List<ReasonCode> getReasonCodesByCategory(String tenantId, String category) {
        return reasonCodeRepository.findByTenantAndCategory(tenantId, category);
    }

    /**
     * Find reason codes with filters
     */
    @Transactional(readOnly = true)
    public Page<ReasonCode> findReasonCodes(String tenantId, String categoryPattern,
                                          ReasonCode.Severity severity, Pageable pageable) {
        return reasonCodeRepository.findWithFilters(tenantId, categoryPattern, severity, pageable);
    }

    /**
     * Get all reason codes for tenant (including global)
     */
    @Transactional(readOnly = true)
    public Page<ReasonCode> getAllReasonCodes(String tenantId, Pageable pageable) {
        return reasonCodeRepository.findByTenant(tenantId, pageable);
    }

    /**
     * Get global reason codes
     */
    @Transactional(readOnly = true)
    public List<ReasonCode> getGlobalReasonCodes() {
        return reasonCodeRepository.findByTenantIdIsNull();
    }

    /**
     * Get reason codes by severity
     */
    @Transactional(readOnly = true)
    public List<ReasonCode> getReasonCodesBySeverity(String tenantId, ReasonCode.Severity severity) {
        return reasonCodeRepository.findByTenantAndSeverity(tenantId, severity);
    }

    /**
     * Update reason code labels
     */
    public ReasonCode updateReasonCodeLabels(String tenantId, String id, Map<String, String> labels) {
        logger.info("Updating reason code labels: tenant={}, id={}", tenantId, id);

        ReasonCode reasonCode = getReasonCode(tenantId, id);
        reasonCode.setLabels(labels);

        ReasonCode saved = reasonCodeRepository.save(reasonCode);

        logger.info("Reason code labels updated successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Delete reason code (only tenant-specific codes)
     */
    public void deleteReasonCode(String tenantId, String id) {
        logger.info("Deleting reason code: tenant={}, id={}", tenantId, id);

        ReasonCode reasonCode = getReasonCode(tenantId, id);

        // Only allow deletion of tenant-specific codes
        if (reasonCode.getTenantId() == null) {
            throw new BusinessException(new ResponseInfo("CANNOT_DELETE_GLOBAL",
                "Cannot delete global reason code: " + id, 400));
        }

        reasonCodeRepository.delete(reasonCode);

        logger.info("Reason code deleted successfully: id={}", id);
    }

    /**
     * Get localized message for reason code
     */
    @Transactional(readOnly = true)
    public String getLocalizedMessage(String tenantId, String reasonCodeId, String locale) {
        try {
            ReasonCode reasonCode = getReasonCode(tenantId, reasonCodeId);

            if (reasonCode.getLabels() == null) {
                return reasonCodeId; // Fallback to ID
            }

            // Try specific locale first, then fallback to English, then any available
            String message = reasonCode.getLabels().get(locale);
            if (message != null) {
                return message;
            }

            message = reasonCode.getLabels().get("en");
            if (message != null) {
                return message;
            }

            // Return first available message
            return reasonCode.getLabels().values().stream()
                .findFirst()
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
            if (!existsReasonCode(tenantId, defaultCode.getId())) {
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
        private final Map<String, String> labels;

        public DefaultReasonCode(String id, String category, ReasonCode.Severity severity, Map<String, String> labels) {
            this.id = id;
            this.category = category;
            this.severity = severity;
            this.labels = labels;
        }

        public String getId() { return id; }
        public String getCategory() { return category; }
        public ReasonCode.Severity getSeverity() { return severity; }
        public Map<String, String> getLabels() { return labels; }
    }
}