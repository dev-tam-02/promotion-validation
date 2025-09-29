package vn.viettel.vds.promotion.validation.application.fact.resolver;

import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.ProvenanceInfo;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface FactResolver<T> {

    String getContextName();

    CompletableFuture<T> resolve(FactRequest request, ProvenanceInfo.FetchPolicy fetchPolicy);

    T resolveFromIds(FactRequest request);

    T resolveFromEmbeddedPayload(FactRequest request, Map<String, Object> embeddedData);

    boolean supportsEmbeddedPayload();

    boolean isEnabled();

    int getTimeoutMs();

    int getPriority();
}