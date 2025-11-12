package vn.viettel.vds.promotion.validation.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client for communicating with Customer microservice.
 * Uses Eureka service discovery for dynamic service resolution.
 * <p>
 * Configuration:
 * - name: Eureka service name from application.yml
 * - path: Base path for all customer API endpoints
 *
 * @author Validation Module Team
 * @since 1.0.0
 */
@FeignClient(
        name = "${promix.validation.external.customer.name:promotion-customer}",
        path = "${promix.validation.external.customer.path:/promotion/promotion-customer/api/v1/customers}"
)
public interface CustomerFeignClient {

    /**
     * Fetch customer data by customer ID.
     *
     * @param customerId Customer ID
     * @return Customer data as Map
     */
    @GetMapping("/{customerId}")
    Map<String, Object> getCustomerById(@PathVariable("customerId") String customerId);
}
