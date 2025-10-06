package vn.viettel.vds.promotion.validation.application.port.in.query;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import vn.viettel.vds.promotion.validation.domain.model.RuleStatus;

/**
 * Query for listing rules with pagination and filtering
 */
public class ListRulesQuery {

    @NotBlank(message = "Tenant ID is required")
    private final String tenantId;

    private final RuleStatus status;
    private final String searchTerm;

    @Min(value = 0, message = "Page number must be non-negative")
    private final int page;

    @Min(value = 1, message = "Page size must be at least 1")
    private final int size;

    private final String sortBy;
    private final SortDirection sortDirection;

    public ListRulesQuery(
            String tenantId,
            RuleStatus status,
            String searchTerm,
            int page,
            int size,
            String sortBy,
            SortDirection sortDirection
    ) {
        this.tenantId = tenantId;
        this.status = status;
        this.searchTerm = searchTerm;
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getTenantId() {
        return tenantId;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public SortDirection getSortDirection() {
        return sortDirection;
    }

    public enum SortDirection {
        ASC, DESC
    }

    public static class Builder {
        private String tenantId;
        private RuleStatus status;
        private String searchTerm;
        private int page = 0;
        private int size = 10;
        private String sortBy = "createdAt";
        private SortDirection sortDirection = SortDirection.DESC;

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder status(RuleStatus status) {
            this.status = status;
            return this;
        }

        public Builder searchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder sortDirection(SortDirection sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        public ListRulesQuery build() {
            return new ListRulesQuery(tenantId, status, searchTerm, page, size, sortBy, sortDirection);
        }
    }
}