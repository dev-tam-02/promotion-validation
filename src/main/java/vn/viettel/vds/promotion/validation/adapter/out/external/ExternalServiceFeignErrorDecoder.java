package vn.viettel.vds.promotion.validation.adapter.out.external;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom error decoder for external service Feign clients
 */
public class ExternalServiceFeignErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceFeignErrorDecoder.class);
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (log.isWarnEnabled()) {
            log.warn("External service call failed: method={}, status={}, reason={}",
                    methodKey, response.status(), response.reason());
        }

        switch (response.status()) {
            case 400:
                return new ExternalServiceException("Bad request to external service: " + response.reason());
            case 404:
                return new ExternalServiceException("External service endpoint not found: " + response.reason());
            case 500:
                return new ExternalServiceException("External service internal error: " + response.reason());
            case 503:
                return new ExternalServiceException("External service unavailable: " + response.reason());
            default:
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }

    public static class ExternalServiceException extends Exception {
        public ExternalServiceException(String message) {
            super(message);
        }
    }
}