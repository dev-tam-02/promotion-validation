package vn.viettel.vds.promotion.validation.application.fact.mapper;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.domain.fact.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class FactMapper {

    private final CustomerFactMapper customerMapper;
    private final OrderFactMapper orderMapper;
    private final CandidateFactMapper candidateMapper;
    private final SegmentsFactMapper segmentsMapper;
    private final LimitsFactMapper limitsMapper;
    private final MetadataFactMapper metadataMapper;
    private final GeoFactMapper geoMapper;
    private final DerivedFieldsCalculator derivedCalculator;

    public FactMapper(
            CustomerFactMapper customerMapper,
            OrderFactMapper orderMapper,
            CandidateFactMapper candidateMapper,
            SegmentsFactMapper segmentsMapper,
            LimitsFactMapper limitsMapper,
            MetadataFactMapper metadataMapper,
            GeoFactMapper geoMapper,
            DerivedFieldsCalculator derivedCalculator
    ) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.candidateMapper = candidateMapper;
        this.segmentsMapper = segmentsMapper;
        this.limitsMapper = limitsMapper;
        this.metadataMapper = metadataMapper;
        this.geoMapper = geoMapper;
        this.derivedCalculator = derivedCalculator;
    }

    public FactPack mapToFactPack(Map<String, Object> rawFacts, FactRequest request) {
        CustomerFact customer = customerMapper.map((Map<String, Object>) rawFacts.get("customer"));
        OrderFact order = orderMapper.map(rawFacts.get("order"));
        CandidateFact candidate = candidateMapper.map(rawFacts.get("candidate"));
        SegmentsFact segments = segmentsMapper.map(rawFacts.get("segments"));
        LimitsFact limits = limitsMapper.map(rawFacts.get("limits"));
        MetadataFact metadata = metadataMapper.map(request, rawFacts);
        GeoFact geo = geoMapper.map(rawFacts.get("geo"));

        Map<String, Object> derived = derivedCalculator.calculate(customer, order, candidate);

        List<ProvenanceInfo.SourceInfo> sources = (List<ProvenanceInfo.SourceInfo>) rawFacts.get("_sources");
        ProvenanceInfo provenance = ProvenanceInfo.builder()
                .sources(sources)
                .aggregatedAt(Instant.now())
                .build();

        return FactPack.builder()
                .factPackVersion("1.0")
                .timestamp(Instant.now())
                .customer(customer)
                .order(order)
                .candidate(candidate)
                .segments(segments)
                .limits(limits)
                .metadata(metadata)
                .geo(geo)
                .derived(derived)
                .provenance(provenance)
                .build();
    }
}