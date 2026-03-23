package vn.viettel.vds.promotion.validation.adapter.out.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

public class ValidationEngineErrorHandler implements ResponseErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEngineErrorHandler.class);

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().is4xxClientError() ||
                response.getStatusCode().is5xxServerError();
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();

        logger.error("Validation engine request failed with status: {}", statusCode.value());

        String errorMessage = String.format("Validation engine request failed: %s", statusCode.value());

        throw new ValidationEngineException(errorMessage, "http-request",
                statusCode.value(), null);
    }
}