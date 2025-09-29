package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Paginated response wrapper")
public record PageResponse<T>(
    @Schema(description = "List of items in current page")
    @JsonProperty("content")
    List<T> content,

    @Schema(description = "Current page number (0-based)", example = "0")
    @JsonProperty("page")
    int page,

    @Schema(description = "Page size", example = "20")
    @JsonProperty("size")
    int size,

    @Schema(description = "Total number of elements", example = "150")
    @JsonProperty("totalElements")
    long totalElements,

    @Schema(description = "Total number of pages", example = "8")
    @JsonProperty("totalPages")
    int totalPages,

    @Schema(description = "Whether this is the first page", example = "true")
    @JsonProperty("first")
    boolean first,

    @Schema(description = "Whether this is the last page", example = "false")
    @JsonProperty("last")
    boolean last,

    @Schema(description = "Number of elements in current page", example = "20")
    @JsonProperty("numberOfElements")
    int numberOfElements,

    @Schema(description = "Whether the page is empty", example = "false")
    @JsonProperty("empty")
    boolean empty
) {
    // Static factory method
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.getNumberOfElements(),
            page.isEmpty()
        );
    }
}