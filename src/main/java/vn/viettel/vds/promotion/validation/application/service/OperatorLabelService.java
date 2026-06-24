package vn.viettel.vds.promotion.validation.application.service;

import org.springframework.stereotype.Service;
import com.promix.platform.validation.condition.ConditionOperator;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder.I18nLabel;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder.OperatorResponse;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorLabelPort;

import java.util.Map;

import static vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter.OperatorLabelJpaAdapter.FIELD_LABEL;
import static vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.adapter.OperatorLabelJpaAdapter.FIELD_LABEL_DATE;

/**
 * Resolves canonical-operator display labels from the {@code translations} table
 * (entity_type = CONDITION_OPERATOR), so the rule-builder catalog is data-driven
 * instead of carrying hardcoded label maps. A compiled-in fallback (mirroring FE
 * {@code OPERATOR_LABELS}) covers the offline/dev case where the table has no rows.
 *
 * <p>Label resolution order (matrix §10): translations {@code label_date} when the
 * field is DATE-like and a date override exists → translations {@code label} →
 * compiled-in fallback → the raw canonical code.
 */
@Service
public class OperatorLabelService {

    private static final String EN = "en";
    private static final String VI = "vi";

    private final OperatorLabelPort operatorLabelPort;

    public OperatorLabelService(OperatorLabelPort operatorLabelPort) {
        this.operatorLabelPort = operatorLabelPort;
    }

    /**
     * Build an {@link OperatorResponse} for one canonical operator code, with its
     * {@code value} = the canonical code and {@code label} localized (vi + en).
     *
     * @param canonical canonical {@link ConditionOperator} code (e.g. GREATER_THAN);
     *                  non-canonical codes (e.g. legacy metadata comparators) pass
     *                  through with the code as their own label
     * @param dateLike  whether the owning field is DATE/DATETIME (uses label_date)
     * @param labels    pre-loaded translation index (see {@link OperatorLabelPort})
     */
    public OperatorResponse toOperatorResponse(
            String canonical,
            boolean dateLike,
            Map<String, Map<String, Map<String, String>>> labels) {
        String vi = resolve(canonical, VI, dateLike, labels);
        String en = resolve(canonical, EN, dateLike, labels);
        return OperatorResponse.of(canonical, en, vi, canonicalOrNull(canonical), valueShape(canonical));
    }

    /** The canonical code itself, or null for non-canonical (legacy) comparators. */
    private String canonicalOrNull(String code) {
        try {
            ConditionOperator.valueOf(code);
            return code;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Value form the operator needs (NONE/SINGLE/MULTI/RANGE); null when non-canonical. */
    private String valueShape(String code) {
        try {
            return ConditionOperator.valueOf(code).valueShape().name();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String resolve(String canonical, String locale, boolean dateLike,
                           Map<String, Map<String, Map<String, String>>> labels) {
        Map<String, Map<String, String>> byField = labels.get(canonical);
        if (byField != null) {
            if (dateLike) {
                String dateLabel = value(byField.get(FIELD_LABEL_DATE), locale);
                if (dateLabel != null) {
                    return dateLabel;
                }
            }
            String label = value(byField.get(FIELD_LABEL), locale);
            if (label != null) {
                return label;
            }
        }
        return fallback(canonical, locale, dateLike);
    }

    private String value(Map<String, String> byLocale, String locale) {
        return byLocale == null ? null : byLocale.get(locale);
    }

    /**
     * Compiled-in fallback mirroring FE {@code OPERATOR_LABELS} — only used when the
     * translations table has no row for the operator (offline/dev). Not the source
     * of truth (that is the {@code translations} table seeded by changelog 078).
     */
    private String fallback(String canonical, String locale, boolean dateLike) {
        ConditionOperator op;
        try {
            op = ConditionOperator.valueOf(canonical);
        } catch (IllegalArgumentException e) {
            return canonical; // non-canonical (e.g. legacy metadata comparator)
        }
        boolean en = EN.equals(locale);
        return switch (op) {
            case EQUALS -> en ? "equals" : (dateLike ? "Vào ngày" : "Bằng");
            case NOT_EQUALS -> en ? "not equals" : "Khác";
            case GREATER_THAN -> en ? "greater than" : (dateLike ? "Sau" : "Lớn hơn");
            case GREATER_OR_EQUAL -> en ? "at least" : (dateLike ? "Từ ngày" : "Lớn hơn hoặc bằng");
            case LESS_THAN -> en ? "less than" : (dateLike ? "Trước" : "Nhỏ hơn");
            case LESS_OR_EQUAL -> en ? "at most" : (dateLike ? "Đến ngày" : "Nhỏ hơn hoặc bằng");
            case BETWEEN -> en ? "between" : "Trong khoảng";
            case IN -> en ? "in" : "Thuộc";
            case NOT_IN -> en ? "not in" : "Không thuộc";
            case CONTAINS -> en ? "contains" : "Chứa";
            case NOT_CONTAINS -> en ? "not contains" : "Không chứa";
            case STARTS_WITH -> en ? "starts with" : "Bắt đầu bằng";
            case ENDS_WITH -> en ? "ends with" : "Kết thúc bằng";
            case IS_TRUE -> en ? "is true" : "Đúng";
            case IS_FALSE -> en ? "is false" : "Sai";
            case EXISTS -> en ? "exists" : "Có tồn tại";
            case NOT_EXISTS -> en ? "not exists" : "Không tồn tại";
            case SIZE_GTE -> en ? "size ≥" : "Số phần tử ≥";
            case SIZE_LTE -> en ? "size ≤" : "Số phần tử ≤";
            case MATCHES -> en ? "matches" : "Khớp mẫu";
        };
    }

    /**
     * Load the operator-label index once for a catalog request. Callers reuse the
     * returned map across all operators in the response to avoid N queries.
     */
    public Map<String, Map<String, Map<String, String>>> loadLabels() {
        return operatorLabelPort.loadOperatorLabels();
    }
}
