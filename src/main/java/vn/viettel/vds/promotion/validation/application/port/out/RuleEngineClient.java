package vn.viettel.vds.promotion.validation.application.port.out;

/**
 * Outbound port for communicating with pp-rule-engine.
 *
 * <p>pp-rule-engine is expected to expose:
 * <ul>
 *   <li>POST /v1/rules {id, drl} → {bundleHash}</li>
 *   <li>PUT  /v1/rules/{id} {drl} → {bundleHash}</li>
 *   <li>DELETE /v1/rules/{id}</li>
 * </ul>
 *
 * <p>NOTE: As of Task 07, the {@code POST /v1/rules} endpoint does not yet
 * exist in pp-rule-engine (the engine currently exposes {@code POST /v1/compile}
 * which takes a structured node tree rather than pre-compiled DRL text).
 * A separate sub-task for pp-rule-engine must be created to implement
 * {@code POST/PUT/DELETE /v1/rules} accepting {@code {id, drl}} payloads.
 * Tests in pp-validation stub this endpoint with WireMock.
 */
public interface RuleEngineClient {

    /**
     * Register a new rule with the rule engine.
     *
     * @param ruleId the unique rule identifier
     * @param drl    compiled Drools DRL text
     * @return bundleHash returned by the rule engine (sha256 of DRL)
     * @throws RuleEngineException if the engine rejects the DRL (compile error) or is unreachable
     */
    String register(String ruleId, String drl);

    /**
     * Update an existing rule's DRL in the rule engine.
     *
     * @param ruleId the unique rule identifier
     * @param drl    updated Drools DRL text
     * @return new bundleHash
     * @throws RuleEngineException if compilation fails or engine is unreachable
     */
    String update(String ruleId, String drl);

    /**
     * Remove a rule from the rule engine's registry.
     *
     * @param ruleId the unique rule identifier
     */
    void delete(String ruleId);

    /**
     * Exception thrown when pp-rule-engine returns an error or is unreachable.
     */
    class RuleEngineException extends RuntimeException {
        private final int statusCode;

        public RuleEngineException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public RuleEngineException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = -1;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
