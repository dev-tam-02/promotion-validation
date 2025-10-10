package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Rule execution request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecuteRequest {

    @Schema(description = "Bundle hash to execute", example = "sha256:abc123...", required = true)
    @NotBlank(message = "Bundle hash is required")
    @JsonProperty("bundleHash")
    private String bundleHash;

    @Schema(description = "Customer data", required = true)
    @NotNull(message = "Customer is required")
    @Valid
    @JsonProperty("customer")
    private CustomerDto customer;

    @Schema(description = "Order data", required = true)
    @NotNull(message = "Order is required")
    @Valid
    @JsonProperty("order")
    private OrderDto order;

    @Schema(description = "Candidate data", required = true)
    @NotNull(message = "Candidate is required")
    @Valid
    @JsonProperty("candidate")
    private CandidateDto candidate;

    @Schema(description = "Execution context", required = true)
    @NotNull(message = "Execution context is required")
    @Valid
    @JsonProperty("executionContext")
    private ExecutionContextDto executionContext;

    // Constructors
    public ExecuteRequest() {
    }

    public ExecuteRequest(String bundleHash, CustomerDto customer, OrderDto order,
                          CandidateDto candidate, ExecutionContextDto executionContext) {
        this.bundleHash = bundleHash;
        this.customer = customer;
        this.order = order;
        this.candidate = candidate;
        this.executionContext = executionContext;
    }

    // Getters and setters
    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public CustomerDto getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDto customer) {
        this.customer = customer;
    }

    public OrderDto getOrder() {
        return order;
    }

    public void setOrder(OrderDto order) {
        this.order = order;
    }

    public CandidateDto getCandidate() {
        return candidate;
    }

    public void setCandidate(CandidateDto candidate) {
        this.candidate = candidate;
    }

    public ExecutionContextDto getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(ExecutionContextDto executionContext) {
        this.executionContext = executionContext;
    }
}
