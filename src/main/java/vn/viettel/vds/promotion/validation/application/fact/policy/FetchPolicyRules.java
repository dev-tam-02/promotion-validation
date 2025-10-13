package vn.viettel.vds.promotion.validation.application.fact.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.ProvenanceInfo;

@Component
public class FetchPolicyRules {

    private static final Logger log = LoggerFactory.getLogger(FetchPolicyRules.class);

    public ProvenanceInfo.FetchPolicy evaluate(FactRequest request) {
        // Rule 1: If embedded payload is provided and substantial, prefer EMBEDDED_PAYLOAD
        if (hasSubstantialEmbeddedPayload(request)) {
            log.debug("Using EMBEDDED_PAYLOAD policy due to substantial embedded data");
            return ProvenanceInfo.FetchPolicy.EMBEDDED_PAYLOAD;
        }

        // Rule 2: If embedded payload is partial, use HYBRID approach
        if (hasPartialEmbeddedPayload(request)) {
            log.debug("Using HYBRID policy due to partial embedded data");
            return ProvenanceInfo.FetchPolicy.HYBRID;
        }

        // Rule 3: If high-priority request or low latency required, allow PARTIAL
        if (isHighPriorityRequest(request)) {
            log.debug("Using PARTIAL policy due to high priority request");
            return ProvenanceInfo.FetchPolicy.PARTIAL;
        }

        // Rule 4: If customer/order IDs are provided, use FROM_IDS
        if (hasRequiredIds(request)) {
            log.debug("Using FROM_IDS policy due to available IDs");
            return ProvenanceInfo.FetchPolicy.FROM_IDS;
        }

        // Rule 5: Default to PARTIAL for graceful degradation
        log.debug("Using PARTIAL policy as default fallback");
        return ProvenanceInfo.FetchPolicy.PARTIAL;
    }

    private boolean hasSubstantialEmbeddedPayload(FactRequest request) {
        if (request.embeddedPayload() == null || request.embeddedPayload().isEmpty()) {
            return false;
        }

        // Check if embedded payload has essential entities
        boolean hasCustomer = request.embeddedPayload().containsKey("customer");
        boolean hasOrder = request.embeddedPayload().containsKey("order");
        boolean hasCandidate = request.embeddedPayload().containsKey("candidate");

        // Consider substantial if has at least 2 of the main entities
        int entityCount = (hasCustomer ? 1 : 0) + (hasOrder ? 1 : 0) + (hasCandidate ? 1 : 0);
        return entityCount >= 2;
    }

    private boolean hasPartialEmbeddedPayload(FactRequest request) {
        if (request.embeddedPayload() == null || request.embeddedPayload().isEmpty()) {
            return false;
        }

        // Has some embedded data but not substantial
        return !hasSubstantialEmbeddedPayload(request);
    }

    private boolean isHighPriorityRequest(FactRequest request) {
        // Check for high priority indicators
        if (request.fetchOptions() != null && request.fetchOptions().timeoutMs() != null) {
            // If timeout is very short, consider it high priority
            return request.fetchOptions().timeoutMs() < 3000;
        }

        // Check for high-value customer indicators
        if (request.context() != null) {
            Object priority = request.context().get("priority");
            if ("HIGH".equals(priority) || "URGENT".equals(priority)) {
                return true;
            }

            Object customerTier = request.context().get("customerTier");
            if ("VIP".equals(customerTier) || "PREMIUM".equals(customerTier)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRequiredIds(FactRequest request) {
        // At minimum, need customer ID
        if (request.customerId() == null || request.customerId().trim().isEmpty()) {
            return false;
        }

        // For order-related requests, need order ID
        if (request.orderId() != null && !request.orderId().trim().isEmpty()) {
            return true;
        }

        // For promotion-related requests, candidate key is preferred
        // Just customer ID is sufficient for basic fact resolution
        return true;
    }
}