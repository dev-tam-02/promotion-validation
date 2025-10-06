package vn.viettel.vds.promotion.validation.application.port.in.dto;

import java.util.List;

/**
 * Response DTO for paginated list of rules
 */
public class PagedRulesResponse {

    private final List<RuleResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    private PagedRulesResponse(Builder builder) {
        this.content = builder.content;
        this.page = builder.page;
        this.size = builder.size;
        this.totalElements = builder.totalElements;
        this.totalPages = builder.totalPages;
        this.first = builder.first;
        this.last = builder.last;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public List<RuleResponse> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }

    public static class Builder {
        private List<RuleResponse> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;

        public Builder content(List<RuleResponse> content) {
            this.content = content;
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

        public Builder totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public Builder totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder first(boolean first) {
            this.first = first;
            return this;
        }

        public Builder last(boolean last) {
            this.last = last;
            return this;
        }

        public PagedRulesResponse build() {
            // Calculate derived fields if not set
            if (totalPages == 0 && size > 0) {
                totalPages = (int) Math.ceil((double) totalElements / size);
            }
            first = (page == 0);
            last = (page >= totalPages - 1);

            return new PagedRulesResponse(this);
        }
    }
}