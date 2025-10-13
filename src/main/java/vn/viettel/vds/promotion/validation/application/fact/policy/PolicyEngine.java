package vn.viettel.vds.promotion.validation.application.fact.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.ProvenanceInfo;

import java.util.concurrent.CompletableFuture;

@Component
public class PolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(PolicyEngine.class);

    private final FetchPolicyRules fetchPolicyRules;

    public PolicyEngine(FetchPolicyRules fetchPolicyRules) {
        this.fetchPolicyRules = fetchPolicyRules;
    }

    public CompletableFuture<ProvenanceInfo.FetchPolicy> determineFetchPolicy(FactRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("Determining fetch policy for request: customerId={}, orderId={}",
                    request.customerId(), request.orderId());
        }

        return CompletableFuture.supplyAsync(() -> {
            ProvenanceInfo.FetchPolicy policy = fetchPolicyRules.evaluate(request);
            log.debug("Determined fetch policy: {} for customerId={}", policy, request.customerId());
            return policy;
        });
    }

    public boolean shouldAllowPartialResults(FactRequest request) {
        return request.fetchOptions() != null &&
                Boolean.TRUE.equals(request.fetchOptions().allowPartial());
    }

    public boolean shouldEnableFallback(FactRequest request) {
        return request.fetchOptions() == null ||
                !Boolean.FALSE.equals(request.fetchOptions().enableFallback());
    }

    public boolean shouldSkipCache(FactRequest request) {
        return request.fetchOptions() != null &&
                Boolean.TRUE.equals(request.fetchOptions().skipCache());
    }

    public long getTimeoutMs(FactRequest request) {
        if (request.fetchOptions() != null && request.fetchOptions().timeoutMs() != null) {
            return request.fetchOptions().timeoutMs();
        }
        return 10000L; // 10 seconds default
    }
}