package vn.viettel.vds.promotion.validation.application.fact.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.FactRequest;
import vn.viettel.vds.promotion.validation.domain.fact.MetadataFact;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class MetadataFactMapper {

    public MetadataFact map(FactRequest request, Map<String, Object> rawFacts) {
        if (request == null) {
            return null;
        }

        return MetadataFact.builder()
                .requestId(UUID.randomUUID().toString())
                .timezone(request.timezone())
                .requestTime(Instant.now())
                .context(request.context())
                .build();
    }
}