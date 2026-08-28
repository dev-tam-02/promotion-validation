package vn.viettel.vds.promotion.validation.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * Outbound port for communicating with pp-rule-engine.
 *
 * <p>pp-rule-engine exposes:
 * <ul>
 *   <li>POST /v1/rules {id, drl} → {bundleHash}</li>
 *   <li>PUT  /v1/rules/{id} {drl} → {bundleHash}</li>
 *   <li>DELETE /v1/rules/{id}</li>
 *   <li>POST /v1/rules/evaluate {ruleIds, facts, mode} → verdict + trace</li>
 * </ul>
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
     * Bao rule-engine rang subject nay DA gan rule nhung bien dich THAT BAI — de no chan subject
     * thay vi coi nhu "chua gan rule".
     *
     * <p><b>Lo hong ma no bit.</b> Khi saga gan rule hong o buoc bien dich
     * ({@code COMPILE_DEPLOY_ERROR}), binding DA duoc luu voi {@code active = 1} nhung
     * {@code ValidationRuleSettingAppliedEvent} chi phat khi THANH CONG — nen rule-engine khong he
     * biet campaign nay co dieu kien. No tra {@code NO_RULE_CONFIGURED}, va pp-redemption CO Y
     * fail-open voi ma do (da so campaign that su khong co dieu kien). Ket qua do that tren 229
     * ngay 28/08/2026: campaign khai {@code order.total >= 100.000}, don 50.000d van nhan uu dai.
     *
     * <p>Duong vay duy nhat truoc day la reconcile dinh ky ben rule-engine — cua so toi 15 phut,
     * va do tren 229 thi sau ~10 phut van con lot.
     *
     * <p><b>BEST-EFFORT:</b> hong thi chi log, KHONG duoc nem ra ngoai. Saga da that bai san roi;
     * bien mot lan goi phu hong thanh exception moi chi lam mat thong diep loi that
     * ({@code COMPILE_DEPLOY_ERROR}) ma nguoi dung can doc.
     *
     * @param subjectType loai subject ben rule-engine (vd {@code DISCOUNT_COUPON}, {@code CAMPAIGN})
     * @param subjectKey  dinh danh subject (campaignId)
     * @param ruleId      rule vua bien dich hong
     */
    void markBoundButNotReady(String subjectType, String subjectKey, String ruleId);

    /**
     * Simulate rule evaluation against provided facts.
     *
     * <p>Calls {@code POST /v1/rules/evaluate} with {@code mode=SIMULATE}.
     * The engine attaches an {@code AgendaEventListener} and returns per-node
     * trace showing which Drools patterns matched and which did not.
     *
     * @param ruleId unique rule identifier (already registered in the engine)
     * @param facts  flat fact map keyed by fact-type (order, customer, …)
     * @return simulation response containing verdict + trace
     * @throws RuleEngineException if the engine is unreachable or returns an error
     */
    SimulateResponse simulate(String ruleId, Map<String, Object> facts);

    /**
     * Response from a SIMULATE evaluation on pp-rule-engine.
     */
    record SimulateResponse(
            String verdict,
            List<TraceEntry> trace,
            List<String> matchedNodes,
            List<String> unmatchedNodes,
            List<String> reasonCodes
    ) {
        /**
         * Single per-node trace entry captured from an {@code AfterMatchFiredEvent} or
         * {@code MatchCancelledEvent} in the Drools engine.
         */
        public record TraceEntry(
                String nodeId,
                String type,
                String operator,
                boolean result,
                String reason
        ) {
        }
    }

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
