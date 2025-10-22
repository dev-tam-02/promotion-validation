package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.domain.fact.FactOrchestrator;
import vn.viettel.vds.promotion.validation.domain.fact.FactPack;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;

import java.util.concurrent.CompletableFuture;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/facts")
public class FactController {

    private final FactOrchestrator factOrchestrator;

    public FactController(FactOrchestrator factOrchestrator) {
        this.factOrchestrator = factOrchestrator;
    }

    @PostMapping("/resolve")
    public CompletableFuture<FactPack> resolve(@RequestBody FactRequest request) {
        return factOrchestrator.resolveWithCache(request);
    }

    @PostMapping("/resolve/no-cache")
    public CompletableFuture<FactPack> resolveNoCache(@RequestBody FactRequest request) {
        return factOrchestrator.resolve(request);
    }

    @DeleteMapping("/cache")
    public void evictCache(
            @RequestParam String customerId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String candidateKey
    ) {
        FactRequest.Builder requestBuilder = FactRequest.builder()
                .customerId(customerId);

        if (orderId != null) {
            requestBuilder.orderId(orderId);
        }

        if (candidateKey != null) {
            requestBuilder.candidate(new FactRequest.CandidateInfo(null, candidateKey, null));
        }

        String cacheKey = factOrchestrator.generateCacheKey(requestBuilder.build());
        factOrchestrator.evictCache(cacheKey);
    }

    @GetMapping("/health")
    public String health() {
        return "Fact aggregation service is healthy";
    }
}