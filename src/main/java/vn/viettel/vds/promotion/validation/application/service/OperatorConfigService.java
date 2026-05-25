package vn.viettel.vds.promotion.validation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorCategoryPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;

import java.util.List;

/**
 * Service for managing operator configuration for UI rule builder.
 *
 * <p>Metadata categories (customer / order / redemption) are expanded at
 * the {@link RuleBuilderService} layer by calling pp-metadata directly via
 * {@code MetadataServiceFeignClient}. This service only loads static
 * categories + options from the local catalog tables.
 */
@Service
@Transactional(readOnly = true)
public class OperatorConfigService {

    private static final Logger logger = LoggerFactory.getLogger(OperatorConfigService.class);

    private final OperatorCategoryPersistencePort categoryPort;

    public OperatorConfigService(OperatorCategoryPersistencePort categoryPort) {
        this.categoryPort = categoryPort;
    }

    /**
     * Get all active categories with their options for UI.
     *
     * @param tenantId the tenant identifier
     * @return list of categories with options
     */
    public List<OperatorCategory> getAllCategoriesWithOptions(String tenantId) {
        logger.debug("Getting all categories with options for tenant: {}", tenantId);
        return categoryPort.findAllActiveWithOptions();
    }
}
