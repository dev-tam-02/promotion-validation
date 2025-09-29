package vn.viettel.vds.promotion.validation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for validation proxy endpoints
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class ValidationProxyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "validationEngineRestTemplate")
    private RestTemplate mockRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldProxyFastCheckSuccessfully() throws Exception {
        // Given: Mock validation-engine response
        Map<String, Object> mockResponse = Map.of(
            "decision", "ALLOW",
            "reasonCode", "FAST_CHECK_PASSED",
            "explanation", "Fast check completed successfully"
        );

        when(mockRestTemplate.exchange(
            contains("/v1/fast-check"),
            eq(HttpMethod.POST),
            any(),
            eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Given: Request payload
        Map<String, Object> request = Map.of(
            "tenantId", "default",
            "campaignId", "CAMPAIGN001",
            "customerId", "CUST001",
            "orderTotal", 1000000,
            "currency", "VND"
        );

        // When & Then: Call proxy endpoint
        mockMvc.perform(post("/api/validation/fast-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("ALLOW"))
                .andExpect(jsonPath("$.reasonCode").value("FAST_CHECK_PASSED"))
                .andExpect(jsonPath("$.explanation").value("Fast check completed successfully"));
    }

    @Test
    void shouldProxyExecuteSuccessfully() throws Exception {
        // Given: Mock validation-engine response
        Map<String, Object> mockResponse = Map.of(
            "ok", true,
            "decision", "ALLOW",
            "reasonCodes", List.of(),
            "explain", List.of("All validation rules passed")
        );

        when(mockRestTemplate.exchange(
            contains("/v1/execute"),
            eq(HttpMethod.POST),
            any(),
            eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Given: Request payload
        Map<String, Object> request = Map.of(
            "bundleHash", "CAMPAIGN001",
            "customer", Map.of("id", "CUST001", "segments", List.of("VIP")),
            "order", Map.of("id", "ORDER001", "total", 1000000, "currency", "VND"),
            "executionContext", Map.of("tenantId", "default", "now", "2024-01-01T00:00:00Z"),
            "candidate", Map.of("id", "CAMPAIGN001", "type", "CAMPAIGN")
        );

        // When & Then: Call proxy endpoint
        mockMvc.perform(post("/api/validation/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.decision").value("ALLOW"))
                .andExpect(jsonPath("$.explain[0]").value("All validation rules passed"));
    }

    @Test
    void shouldHandleFastCheckValidationEngineError() throws Exception {
        // Given: Mock validation-engine error
        when(mockRestTemplate.exchange(
            contains("/v1/fast-check"),
            eq(HttpMethod.POST),
            any(),
            eq(Map.class)
        )).thenThrow(new RuntimeException("Connection refused"));

        // Given: Request payload
        Map<String, Object> request = Map.of(
            "tenantId", "default",
            "campaignId", "CAMPAIGN001",
            "customerId", "CUST001"
        );

        // When & Then: Call proxy endpoint should return error response
        mockMvc.perform(post("/api/validation/fast-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENY"))
                .andExpect(jsonPath("$.reasonCode").value("FAST_CHECK_ERROR"))
                .andExpect(jsonPath("$.explanation").value(containsString("Fast check service unavailable")));
    }

    @Test
    void shouldHandleExecuteValidationEngineError() throws Exception {
        // Given: Mock validation-engine error
        when(mockRestTemplate.exchange(
            contains("/v1/execute"),
            eq(HttpMethod.POST),
            any(),
            eq(Map.class)
        )).thenThrow(new RuntimeException("Service unavailable"));

        // Given: Request payload
        Map<String, Object> request = Map.of(
            "bundleHash", "CAMPAIGN001",
            "customer", Map.of("id", "CUST001")
        );

        // When & Then: Call proxy endpoint should return error response
        mockMvc.perform(post("/api/validation/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.decision").value("DENY"))
                .andExpect(jsonPath("$.reasonCodes[0]").value("EXECUTION_ERROR"))
                .andExpect(jsonPath("$.explain[0]").value(containsString("Validation execution service unavailable")));
    }

    @Test
    void shouldProvideHealthCheck() throws Exception {
        // Given: Mock healthy validation-engine
        when(mockRestTemplate.getForEntity(
            contains("/actuator/health"),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        // When & Then: Call health endpoint
        mockMvc.perform(get("/api/validation/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.validation-engine").value("AVAILABLE"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReportUnhealthyWhenValidationEngineDown() throws Exception {
        // Given: Mock unhealthy validation-engine
        when(mockRestTemplate.getForEntity(
            contains("/actuator/health"),
            eq(String.class)
        )).thenThrow(new RuntimeException("Connection failed"));

        // When & Then: Call health endpoint
        mockMvc.perform(get("/api/validation/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.validation-engine").value("ERROR"))
                .andExpect(jsonPath("$.error").value("Connection failed"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}