package vn.viettel.vds.promotion.validation.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidJsonSchemaException;
import vn.viettel.vds.promotion.validation.domain.exception.OperatorAlreadyExistsException;
import vn.viettel.vds.promotion.validation.domain.exception.OperatorFingerprintException;
import vn.viettel.vds.promotion.validation.domain.exception.OperatorNotFoundException;
import vn.viettel.vds.promotion.validation.domain.exception.OperatorValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.adapter.out.integration.ValidationEngineOperatorClient;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.ValidateEngineOperatorsRequest;
import vn.viettel.vds.promotion.validation.adapter.out.integration.dto.ValidateEngineOperatorsResponse;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.Operator;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class OperatorService {

    private static final Logger logger = LoggerFactory.getLogger(OperatorService.class);

    private final OperatorPersistencePort operatorPersistencePort;
    private final ValidationEngineOperatorClient validationEngineClient;
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final OperatorService self;

    public OperatorService(OperatorPersistencePort operatorPersistencePort,
                           ValidationEngineOperatorClient validationEngineClient,
                           @org.springframework.context.annotation.Lazy OperatorService self) {
        this.operatorPersistencePort = operatorPersistencePort;
        this.validationEngineClient = validationEngineClient;
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.self = self;
    }

    /**
     * Create a new operator
     */
    public Operator createOperator(String name, Integer version, String context,
                                   Map<String, Object> jsonSchema, String compilerId) {
        logger.info("Creating operator: name={}, version={}", name, version);

        // Check if operator with same name and version already exists
        if (operatorPersistencePort.existsByTenantIdAndNameAndVersion(null, name, version)) {
            throw new OperatorAlreadyExistsException(name, version);
        }

        // Validate JSON schema
        validateJsonSchema(jsonSchema);

        // Validate operator support in validation engine
        validateOperatorSupport(name, version);

        Instant now = Instant.now();
        Operator operator = Operator.builder()
                .id(generateOperatorId(name, version))
                .name(name)
                .operatorVersion(version)
                .context(context)
                .jsonSchema(jsonSchema)
                .compilerId(compilerId)
                .status(Operator.OperatorStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        Operator saved = operatorPersistencePort.save(operator);

        logger.info("Operator created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Update operator status
     */
    public Operator updateOperatorStatus(String tenantId, String name, Integer version,
                                         Operator.OperatorStatus status) {
        logger.info("Updating operator status: name={}, version={}, status={}", name, version, status);

        Operator operator = self.getOperator(tenantId, name, version);
        Operator updated = operator.toBuilder()
                .status(status)
                .updatedAt(Instant.now())
                .build();

        return operatorPersistencePort.save(updated);
    }

    /**
     * Get operator by tenant, name and version
     */
    @Transactional(readOnly = true)
    public Operator getOperator(String tenantId, String name, Integer version) {
        return operatorPersistencePort.findByTenantIdAndNameAndVersion(tenantId, name, version)
                .orElseThrow(() -> new OperatorNotFoundException(name, version));
    }

    /**
     * Get latest version of operator
     */
    @Transactional(readOnly = true)
    public Optional<Operator> getLatestOperator(String tenantId, String name) {
        return operatorPersistencePort.findLatestVersion(tenantId, name);
    }

    /**
     * Get all versions of an operator
     */
    @Transactional(readOnly = true)
    public List<Operator> getOperatorVersions(String tenantId, String name) {
        return operatorPersistencePort.findAllVersions(tenantId, name);
    }

    /**
     * Find operators with filters
     */
    @Transactional(readOnly = true)
    public Page<Operator> findOperators(String tenantId, String contextPattern,
                                        Operator.OperatorStatus status, Pageable pageable) {
        return operatorPersistencePort.findWithFilters(tenantId, contextPattern, status, pageable);
    }

    /**
     * Get active operators by context
     */
    @Transactional(readOnly = true)
    public List<Operator> getActiveOperatorsByContext(String tenantId, String context) {
        return operatorPersistencePort.findByTenantIdAndContextAndStatus(tenantId, context,
                Operator.OperatorStatus.ACTIVE, Pageable.unpaged()).getContent();
    }

    /**
     * Get all active operators for tenant
     */
    @Transactional(readOnly = true)
    public List<Operator> getActiveOperators(String tenantId) {
        return operatorPersistencePort.findByTenantIdAndStatus(tenantId, Operator.OperatorStatus.ACTIVE);
    }

    /**
     * Validate operator parameters against JSON schema
     */
    public ValidationResult validateOperatorParams(String tenantId, String operatorName,
                                                   Integer operatorVersion, Map<String, Object> params) {
        logger.debug("Validating operator params: operator={}@{}", operatorName, operatorVersion);

        try {
            Operator operator = self.getOperator(tenantId, operatorName, operatorVersion);

            if (operator.getJsonSchema() == null || operator.getJsonSchema().isEmpty()) {
                return ValidationResult.valid();
            }

            // Convert schema and params to JsonNode
            JsonNode schemaNode = objectMapper.valueToTree(operator.getJsonSchema());
            JsonNode paramsNode = objectMapper.valueToTree(params);

            // Create and validate against schema
            JsonSchema schema = schemaFactory.getSchema(schemaNode);
            Set<ValidationMessage> errors = schema.validate(paramsNode);

            if (errors.isEmpty()) {
                return ValidationResult.valid();
            } else {
                List<ValidationIssue> issues = errors.stream()
                        .map(error -> new ValidationIssue(error.getInstanceLocation().toString(), error.getMessage(), operatorName))
                        .toList();
                return ValidationResult.invalid(issues);
            }

        } catch (Exception e) {
            logger.error("Error validating operator params", e);
            return ValidationResult.invalid(List.of(
                    new ValidationIssue("", "Schema validation error: " + e.getMessage(), operatorName)
            ));
        }
    }

    /**
     * Calculate fingerprint for operators used in a rule
     */
    public String calculateOperatorsFingerprint(String tenantId, List<String> operatorNames) {
        logger.debug("Calculating operators fingerprint for: {}", operatorNames);

        try {
            StringBuilder fingerprintData = new StringBuilder();

            for (String operatorName : operatorNames) {
                Optional<Operator> operator = self.getLatestOperator(tenantId, operatorName);
                if (operator.isPresent()) {
                    fingerprintData.append(operator.get().getName())
                            .append("@")
                            .append(operator.get().getOperatorVersion())
                            .append("|");
                }
            }

            // Generate SHA-256 hash
            byte[] hashBytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(fingerprintData.toString().getBytes());

            // Convert bytes to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return "sha256:" + hexString.toString();

        } catch (Exception e) {
            throw new OperatorFingerprintException("Failed to calculate operators fingerprint", e);
        }
    }

    private void validateJsonSchema(Map<String, Object> jsonSchema) {
        try {
            JsonNode schemaNode = objectMapper.valueToTree(jsonSchema);
            schemaFactory.getSchema(schemaNode);
        } catch (Exception e) {
            throw new InvalidJsonSchemaException(e.getMessage(), e);
        }
    }

    private String generateOperatorId(String name, Integer version) {
        return name + "@" + version;
    }

    /**
     * Validate operator support in validation engine
     */
    private void validateOperatorSupport(String operatorName, Integer version) {
        logger.debug("Validating operator support: name={}, version={}", operatorName, version);

        try {
            // Validate with validation engine
            ValidateEngineOperatorsRequest request = new ValidateEngineOperatorsRequest(List.of(
                    new ValidateEngineOperatorsRequest.OperatorValidationItem(operatorName, version)
            ));

            ValidateEngineOperatorsResponse response = validationEngineClient.validateOperators(request);

            if (response == null) {
                logger.warn("No response from validation engine, skipping operator support validation");
                return; // Allow creation when engine response is null (fallback activated)
            }

            if (!response.isValid()) {
                List<String> reasons = response.getUnsupportedOperators().stream()
                        .map(ValidateEngineOperatorsResponse.UnsupportedOperator::getReason)
                        .toList();

                throw new OperatorValidationException(operatorName, version, String.join(", ", reasons));
            }

            logger.info("Operator {} version {} is supported by validation engine", operatorName, version);

        } catch (OperatorValidationException e) {
            throw e; // Re-throw validation exceptions
        } catch (Exception e) {
            logger.error("Error validating operator support with validation engine: {}", e.getMessage(), e);
            // In case of integration error, log but don't fail operator creation
            // This prevents blocking operator creation due to temporary network issues
            logger.warn("Skipping validation engine check due to integration error, allowing operator creation");
        }
    }

    /**
     * Get supported operators from validation engine
     */
    public List<String> getSupportedOperatorNames() {
        logger.debug("Fetching supported operator names from validation engine");

        try {
            List<String> names = validationEngineClient.getSupportedOperatorNames();
            return names != null ? names : List.of();
        } catch (Exception e) {
            logger.error("Error fetching supported operator names: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Check if specific operator is supported by validation engine
     */
    public boolean isOperatorSupportedByEngine(String operatorName, Integer version) {
        try {
            Boolean supported = validationEngineClient.isOperatorSupported(operatorName, version);
            return Boolean.TRUE.equals(supported);
        } catch (Exception e) {
            logger.error("Error checking operator support: {}", e.getMessage(), e);
            return false; // Conservative approach
        }
    }

    // Validation result classes
    public static class ValidationResult {
        private final boolean valid;
        private final List<ValidationIssue> issues;

        private ValidationResult(boolean valid, List<ValidationIssue> issues) {
            this.valid = valid;
            this.issues = issues;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult invalid(List<ValidationIssue> issues) {
            return new ValidationResult(false, issues);
        }

        public boolean isValid() {
            return valid;
        }

        public List<ValidationIssue> getIssues() {
            return issues;
        }
    }

    public static class ValidationIssue {
        private final String path;
        private final String message;
        private final String operator;

        public ValidationIssue(String path, String message, String operator) {
            this.path = path;
            this.message = message;
            this.operator = operator;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }

        public String getOperator() {
            return operator;
        }
    }
}