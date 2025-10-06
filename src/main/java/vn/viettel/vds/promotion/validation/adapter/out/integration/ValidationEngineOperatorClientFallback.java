package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.SupportedOperatorDto;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.ValidateEngineOperatorsRequest;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.ValidateEngineOperatorsResponse;

import java.util.Collections;
import java.util.List;

@Component
public class ValidationEngineOperatorClientFallback implements ValidationEngineOperatorClient {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEngineOperatorClientFallback.class);

    @Override
    public List<SupportedOperatorDto> getSupportedOperators() {
        logger.error("Fallback: Failed to fetch supported operators from validation engine");
        return Collections.emptyList();
    }

    @Override
    public List<String> getSupportedOperatorNames() {
        logger.error("Fallback: Failed to fetch supported operator names from validation engine");
        return Collections.emptyList();
    }

    @Override
    public ValidateEngineOperatorsResponse validateOperators(ValidateEngineOperatorsRequest request) {
        logger.error("Fallback: Failed to validate {} operators with validation engine",
                request.getOperators().size());

        // Convert OperatorValidationItem to UnsupportedOperator
        List<ValidateEngineOperatorsResponse.UnsupportedOperator> unsupportedOperators =
                request.getOperators().stream()
                        .map(item -> new ValidateEngineOperatorsResponse.UnsupportedOperator(
                                item.getOperatorName(),
                                item.getVersion(),
                                "Validation engine service is unavailable"))
                        .toList();

        ValidateEngineOperatorsResponse fallbackResponse = new ValidateEngineOperatorsResponse();
        fallbackResponse.setValid(false);
        fallbackResponse.setSupportedOperators(Collections.emptyList());
        fallbackResponse.setUnsupportedOperators(unsupportedOperators);
        fallbackResponse.setMessage("Validation engine service is unavailable");
        return fallbackResponse;
    }

    @Override
    public Boolean isOperatorSupported(String operatorName, Integer version) {
        logger.error("Fallback: Failed to check if operator {} version {} is supported",
                operatorName, version);
        return false;
    }
}