package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Order data for rule execution")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDto {

    @Schema(description = "Order identifier", example = "order123", required = true)
    @NotBlank(message = "Order ID is required")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Order total amount", example = "500000", required = true)
    @NotNull(message = "Total is required")
    @JsonProperty("total")
    private BigDecimal total;

    @Schema(description = "Currency code", example = "VND", required = true)
    @NotBlank(message = "Currency is required")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Order items")
    @Valid
    @JsonProperty("items")
    private List<OrderItemDto> items;

    @Schema(description = "Order metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public OrderDto() {}

    public OrderDto(String id, BigDecimal total, String currency) {
        this.id = id;
        this.total = total;
        this.currency = currency;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}