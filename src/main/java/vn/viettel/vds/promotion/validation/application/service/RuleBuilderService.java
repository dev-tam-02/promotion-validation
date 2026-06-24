package vn.viettel.vds.promotion.validation.application.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.rulebuilder.*;
import vn.viettel.vds.promotion.validation.adapter.out.external.MetadataServiceFeignClient;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsLookupPort;
import vn.viettel.vds.promotion.validation.application.port.out.RuleOptionsPage;
import vn.viettel.vds.promotion.validation.domain.exception.InvalidRuleStructureException;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;
import vn.viettel.vds.promotion.validation.domain.model.RuleNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Service for Rule Builder API.
 * Provides rule categories and options from database.
 */
@Service
public class RuleBuilderService {

    private static final Logger logger = LoggerFactory.getLogger(RuleBuilderService.class);
    private static final String VERSION = "1.0.0";
    // Canonical operator codes (mirror ConditionOperator), used verbatim as wire by
    // the catalog (static operators AND metadata.access) — the engine
    // (MetadataAccessOperatorTranslator) parses the comparator via
    // ConditionOperator.valueOf(), so metadata.access MUST emit canonical too.
    // Labels are resolved from the translations table via OperatorLabelService.
    private static final String CANON_EQUALS = "EQUALS";
    private static final String CANON_NOT_EQUALS = "NOT_EQUALS";
    private static final String CANON_GREATER_THAN = "GREATER_THAN";
    private static final String CANON_GREATER_OR_EQUAL = "GREATER_OR_EQUAL";
    private static final String CANON_LESS_THAN = "LESS_THAN";
    private static final String CANON_LESS_OR_EQUAL = "LESS_OR_EQUAL";
    private static final String CANON_BETWEEN = "BETWEEN";
    private static final String CANON_IN = "IN";
    private static final String CANON_NOT_IN = "NOT_IN";
    private static final String CANON_CONTAINS = "CONTAINS";
    private static final String CANON_NOT_CONTAINS = "NOT_CONTAINS";
    private static final String CANON_STARTS_WITH = "STARTS_WITH";
    private static final String CANON_IS_TRUE = "IS_TRUE";
    private static final String CANON_IS_FALSE = "IS_FALSE";

    private static final String TYPE_NUMBER = "NUMBER";
    private static final String TYPE_BOOLEAN = "BOOLEAN";
    private static final String TYPE_STRING = "STRING";

    private static final String KEY_VALIDATION = "validation";
    private static final String KEY_STRING_VALIDATION = "stringValidation";
    private static final String KEY_EQUAL_TO_ANY_OF = "equalToAnyOf";
    private static final String MSG_VALUE = "Giá trị '";
    private static final String MSG_VALUE_PLAIN = "Giá trị ";
    private static final String MSG_OF_FIELD = " của trường '";
    private static final String MSG_FIELD_PAREN = " (trường '";

    private static final String DATA_SOURCE_STATIC = "STATIC";
    private static final String METADATA_ACCESS_OPERATOR = "metadata.access";

    private final OperatorConfigService operatorConfigService;
    private final RuleOptionsLookupPort ruleOptionsLookupPort;
    private final MetadataServiceFeignClient metadataServiceFeignClient;
    private final OperatorLabelService operatorLabelService;

    public RuleBuilderService(OperatorConfigService operatorConfigService,
                               RuleOptionsLookupPort ruleOptionsLookupPort,
                               MetadataServiceFeignClient metadataServiceFeignClient,
                               OperatorLabelService operatorLabelService) {
        this.operatorConfigService = operatorConfigService;
        this.ruleOptionsLookupPort = ruleOptionsLookupPort;
        this.metadataServiceFeignClient = metadataServiceFeignClient;
        this.operatorLabelService = operatorLabelService;
    }

    /**
     * Get all rule categories with their rules from database.
     */
    public RuleCategoriesResponse getAllCategories(String tenantId) {
        logger.debug("Getting all rule categories for tenant: {}", tenantId);

        List<OperatorCategory> categories = operatorConfigService.getAllCategoriesWithOptions(tenantId);

        // Load every operator label once per request (translations table) and reuse
        // across all operators to avoid an N-query fan-out.
        Map<String, Map<String, Map<String, String>>> labels = operatorLabelService.loadLabels();

        List<RuleCategoryResponse> categoryResponses = categories.stream()
                .map(category -> mapCategoryToResponse(category, labels))
                .toList();

        return RuleCategoriesResponse.of(categoryResponses, VERSION);
    }

    /**
     * Get options for a specific rule.
     * Routes by dataSourceType: null/"STATIC" uses in-memory predefined options;
     * any other value (e.g. "SEGMENT") delegates to the external lookup port.
     */
    public RuleOptionsResponse getRuleOptions(String ruleId, String search, List<String> ids,
                                              Integer page, Integer size, String tenantId) {
        logger.debug("Getting options for rule: {} (search: {}, ids: {}, page: {}, size: {}, tenant: {})",
                ruleId, search, ids != null ? ids.size() : 0, page, size, tenantId);

        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        List<OperatorCategory> categories = operatorConfigService.getAllCategoriesWithOptions(tenantId);

        // By-id resolution path (display / edit / preview): resolve EXACTLY the
        // requested ids to name + entity type via direct downstream lookups, never
        // by paging the whole catalog and matching client-side. Works regardless of
        // whether the operator's category is still active (pruned rules included).
        if (ids != null && !ids.isEmpty()) {
            return resolveOptionsByIds(ruleId, ids, categories, tenantId);
        }

        // Metadata enum rules ("<categoryCode>.<fieldName>") are not seeded in
        // operator_options; their allowed values come from the pp-schema
        // definition (equalToAnyOf), resolved at request time.
        RuleOptionsResponse metadataOptions =
                resolveMetadataEnumOptions(ruleId, categories, search, pageNumber, pageSize);
        if (metadataOptions != null) {
            return metadataOptions;
        }

        List<OperatorOption> allOptions = categories.stream()
                .flatMap(cat -> cat.getOptions().stream())
                .filter(opt -> opt.getCode().equals(ruleId) || opt.getId().equals(ruleId))
                .toList();

        // Active catalog miss: the rule may belong to a disabled category/option
        // (e.g. PRODUCTS pruned by changelog 066) yet still be referenced by older
        // saved rules whose values need resolving. Fall back to an active-agnostic
        // lookup by id / code / operatorName so the detail/edit/preview surfaces can
        // turn persisted entity ids back into names. The builder catalog stays
        // active-only, so this does NOT re-expose the rule for creation.
        OperatorOption option = allOptions.isEmpty()
                ? operatorConfigService.findOptionForResolution(ruleId).orElse(null)
                : allOptions.get(0);

        if (option == null) {
            return RuleOptionsResponse.paginated(ruleId, Collections.emptyList(), 0, 0, 20);
        }

        String dataSourceType = option.getDataSourceType();

        if (dataSourceType == null || DATA_SOURCE_STATIC.equalsIgnoreCase(dataSourceType)) {
            return getStaticOptions(ruleId, option, search, pageNumber, pageSize);
        }

        return getExternalOptions(ruleId, option, dataSourceType, search, pageNumber, pageSize, tenantId);
    }

    /**
     * Resolve value options for a metadata enum rule. The rule id has the shape
     * {@code <categoryCode>.<fieldName>}; the allowed values are the STRING
     * field's {@code equalToAnyOf} constraint from pp-schema.
     *
     * <p>Returns {@code null} when {@code ruleId} is not a metadata rule (so the
     * caller falls back to operator-option lookup). Returns an empty page when
     * it is a metadata rule but the field has no enum constraint.
     */
    private RuleOptionsResponse resolveMetadataEnumOptions(String ruleId, List<OperatorCategory> categories,
                                                            String search, int pageNumber, int pageSize) {
        int dot = ruleId.indexOf('.');
        if (dot <= 0 || dot >= ruleId.length() - 1) {
            return null;
        }
        String categoryCode = ruleId.substring(0, dot);
        String fieldName = ruleId.substring(dot + 1);

        OperatorCategory category = categories.stream()
                .filter(c -> c.isMetadataCategory() && c.getMetadataSchemaType() != null)
                .filter(c -> c.getCode().equalsIgnoreCase(categoryCode))
                .findFirst()
                .orElse(null);
        if (category == null) {
            return null;
        }

        Map<String, Object> field = findMetadataField(category, fieldName);
        if (field == null) {
            return RuleOptionsResponse.paginated(ruleId, Collections.emptyList(), 0, pageNumber, pageSize);
        }

        String lower = (search == null || search.isBlank()) ? null : search.toLowerCase();
        List<RuleOptionResponse> options = stringEnumValues(field).stream()
                .filter(v -> lower == null || v.toLowerCase().contains(lower))
                .map(v -> RuleOptionResponse.of(v, v, v))
                .toList();

        int totalElements = options.size();
        int fromIndex = Math.min(pageNumber * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        return RuleOptionsResponse.paginated(
                ruleId, options.subList(fromIndex, toIndex), totalElements, pageNumber, pageSize);
    }

    /**
     * Return in-memory predefined value options with client-side filtering and pagination.
     * Uses {@code labelEn}/{@code labelVi} from the domain model for proper i18n;
     * falls back to the legacy {@code label} field if not set (backward compat).
     */
    private RuleOptionsResponse getStaticOptions(String ruleId, OperatorOption option,
                                                  String search, int pageNumber, int pageSize) {
        List<RuleOptionResponse> options = Collections.emptyList();
        if (option.getValueOptions() != null && !option.getValueOptions().isEmpty()) {
            Predicate<OperatorOption.ValueOption> searchFilter = matchSearch(search);
            options = option.getValueOptions().stream()
                    .filter(searchFilter)
                    .map(RuleBuilderService::toRuleOptionResponse)
                    .toList();
        }

        int totalElements = options.size();
        int fromIndex = Math.min(pageNumber * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<RuleOptionResponse> pagedOptions = options.subList(fromIndex, toIndex);

        return RuleOptionsResponse.paginated(ruleId, pagedOptions, totalElements, pageNumber, pageSize);
    }

    private static Predicate<OperatorOption.ValueOption> matchSearch(String search) {
        if (search == null || search.isBlank()) {
            return vo -> true;
        }
        String lower = search.toLowerCase();
        return vo -> {
            String searchTarget = preferredLabel(vo);
            return (searchTarget != null && searchTarget.toLowerCase().contains(lower))
                    || (vo.getValue() != null && vo.getValue().toLowerCase().contains(lower));
        };
    }

    private static RuleOptionResponse toRuleOptionResponse(OperatorOption.ValueOption vo) {
        return RuleOptionResponse.of(vo.getValue(), preferredLabel(vo), preferredLabelVi(vo));
    }

    @SuppressWarnings("deprecation") // Legacy label kept as fallback when labelEn is null
    private static String preferredLabel(OperatorOption.ValueOption vo) {
        return vo.getLabelEn() != null ? vo.getLabelEn() : vo.getLabel();
    }

    @SuppressWarnings("deprecation") // Legacy label kept as fallback when labelVi is null
    private static String preferredLabelVi(OperatorOption.ValueOption vo) {
        return vo.getLabelVi() != null ? vo.getLabelVi() : vo.getLabel();
    }

    /**
     * Delegate to the external lookup port and map the result to the web DTO.
     */
    private RuleOptionsResponse getExternalOptions(String ruleId, OperatorOption option,
                                                    String dataSourceType, String search,
                                                    int pageNumber, int pageSize, String tenantId) {
        RuleOptionsPage resultPage = ruleOptionsLookupPort.lookup(
                dataSourceType, option.getDataSourceEndpoint(), search, pageNumber, pageSize, tenantId);

        List<RuleOptionResponse> mapped = resultPage.items().stream()
                .map(item -> {
                    Map<String, Object> meta = item.metadata();
                    return (meta == null || meta.isEmpty())
                            ? RuleOptionResponse.of(item.value(), item.labelEn(), item.labelVi())
                            : RuleOptionResponse.of(item.value(), item.labelEn(), item.labelVi(), meta);
                })
                .toList();

        return RuleOptionsResponse.paginated(ruleId, mapped, resultPage.totalElements(), pageNumber, pageSize);
    }

    /**
     * Resolve a specific set of value ids to names + entity type. Finds the
     * operator option (active OR pruned/inactive, by id / code / operatorName), then
     * delegates: external sources (PRODUCT / SEGMENT) to {@link RuleOptionsLookupPort#lookupByIds},
     * static value-lists to an in-memory filter. Returns empty when the operator
     * isn't catalog-backed (metadata-enum rules, whose values are their own labels).
     */
    private RuleOptionsResponse resolveOptionsByIds(String ruleId, List<String> ids,
                                                    List<OperatorCategory> categories, String tenantId) {
        OperatorOption option = categories.stream()
                .flatMap(cat -> cat.getOptions().stream())
                .filter(opt -> opt.getCode().equals(ruleId) || opt.getId().equals(ruleId))
                .findFirst()
                .or(() -> operatorConfigService.findOptionForResolution(ruleId))
                .orElse(null);

        if (option == null) {
            return RuleOptionsResponse.paginated(ruleId, Collections.emptyList(), 0, 0, ids.size());
        }

        String dataSourceType = option.getDataSourceType();
        if (dataSourceType == null || DATA_SOURCE_STATIC.equalsIgnoreCase(dataSourceType)) {
            return filterStaticOptionsByIds(ruleId, option, ids);
        }

        RuleOptionsPage resultPage = ruleOptionsLookupPort.lookupByIds(
                dataSourceType, option.getDataSourceEndpoint(), ids, tenantId);

        List<RuleOptionResponse> mapped = resultPage.items().stream()
                .map(item -> {
                    Map<String, Object> meta = item.metadata();
                    return (meta == null || meta.isEmpty())
                            ? RuleOptionResponse.of(item.value(), item.labelEn(), item.labelVi())
                            : RuleOptionResponse.of(item.value(), item.labelEn(), item.labelVi(), meta);
                })
                .toList();

        return RuleOptionsResponse.paginated(ruleId, mapped, mapped.size(), 0, ids.size());
    }

    /**
     * Resolve operator display names (localized) for the given operatorNames,
     * INCLUDING operators whose category/option was disabled (changelog 066). The
     * builder catalog ({@code /categories}) is active-only, so a detail/edit view
     * showing a rule that references a pruned operator can't get its name there;
     * this fills that gap so the field title shows the configured name instead of
     * a title-cased English fallback.
     */
    public Map<String, I18nLabel> getOperatorDisplayNames(List<String> operatorNames) {
        Map<String, I18nLabel> result = new LinkedHashMap<>();
        if (operatorNames == null) {
            return result;
        }
        for (String operatorName : operatorNames) {
            if (operatorName == null || operatorName.isBlank() || result.containsKey(operatorName)) {
                continue;
            }
            operatorConfigService.findOptionForResolution(operatorName).ifPresent(option -> {
                String vi = option.getNameVi() != null ? option.getNameVi() : option.getName();
                result.put(operatorName, I18nLabel.of(option.getName(), vi));
            });
        }
        return result;
    }

    /** In-memory id filter for STATIC value-option lists (boolean / enum). */
    private RuleOptionsResponse filterStaticOptionsByIds(String ruleId, OperatorOption option, List<String> ids) {
        Set<String> want = new HashSet<>(ids);
        List<RuleOptionResponse> options = option.getValueOptions() == null
                ? Collections.emptyList()
                : option.getValueOptions().stream()
                        .filter(vo -> want.contains(vo.getValue()))
                        .map(RuleBuilderService::toRuleOptionResponse)
                        .toList();
        return RuleOptionsResponse.paginated(ruleId, options, options.size(), 0, ids.size());
    }

    /**
     * Map OperatorCategory domain model to RuleCategoryResponse DTO.
     *
     * <p>For metadata categories (customer / order / redemption), rules are
     * not seeded in operator_options. Instead they are resolved at request
     * time by calling pp-metadata for the schema's field definitions, and
     * one rule is synthesized per field. The data type drives the available
     * comparator list per spec.
     */
    private RuleCategoryResponse mapCategoryToResponse(
            OperatorCategory category,
            Map<String, Map<String, Map<String, String>>> labels) {
        List<RuleItemResponse> rules = new ArrayList<>();

        if (category.getOptions() != null) {
            category.getOptions().stream()
                    .map(option -> mapOptionToRuleItem(option, labels))
                    .forEach(rules::add);
        }

        if (category.isMetadataCategory() && category.getMetadataSchemaType() != null) {
            rules.addAll(resolveMetadataRules(category, labels));
        }

        return RuleCategoryResponse.builder()
                .id(category.getCode())
                .code(category.getCode().toUpperCase())
                .name(category.getName(), category.getNameVi() != null ? category.getNameVi() : category.getName())
                .icon(category.getIcon())
                .order(category.getDisplayOrder())
                .rules(rules)
                .build();
    }

    /**
     * Resolve metadata category rules at request time by calling pp-metadata.
     * Each field in the schema becomes one synthesized rule; available
     * comparators are derived from the field's data type.
     *
     * <p>Failure modes (pp-metadata down / no schema / no fields) all degrade
     * to an empty rule list — the category still renders, just empty.
     */
    private List<RuleItemResponse> resolveMetadataRules(
            OperatorCategory category,
            Map<String, Map<String, Map<String, String>>> labels) {
        return fetchMetadataFields(category).stream()
                .map(f -> synthesizeMetadataRule(category, f, labels))
                .toList();
    }

    /**
     * Fetch the pp-schema field definitions for a metadata category. Returns an
     * empty list on any failure (pp-metadata down / no schema / no fields) so the
     * category still renders, just empty.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchMetadataFields(OperatorCategory category) {
        // Look up the schema by its immutable id (e.g. "standard-order"), NOT by name.
        // A name-based LIKE match collides ("order" hits both "Order" and "Order line
        // item") and breaks when a schema is renamed in pp-metadata; the id is stable.
        String schemaId = category.getMetadataSchemaId();
        if (schemaId == null) {
            logger.debug("Category {} has no metadata_schema_id; rendering empty", category.getCode());
            return Collections.emptyList();
        }
        try {
            Map<String, Object> schemaResp = metadataServiceFeignClient.getSchemaById(schemaId, 0, 100);
            Map<String, Object> schemaData = (Map<String, Object>) schemaResp.get("data");
            if (schemaData == null) return Collections.emptyList();
            Map<String, Object> definitions = (Map<String, Object>) schemaData.get("definitions");
            if (definitions == null) return Collections.emptyList();
            List<Map<String, Object>> fields = (List<Map<String, Object>>) definitions.get("content");
            return fields == null ? Collections.emptyList() : fields;
        } catch (FeignException e) {
            logger.error("Failed to fetch metadata fields for category {}: {}",
                    category.getCode(), e.getMessage());
            return Collections.emptyList();
        } catch (RuntimeException e) {
            logger.error("Unexpected error fetching metadata fields for category {}",
                    category.getCode(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Verify every {@code metadata.access} COND references a field that actually
     * exists in its pp-metadata schema, with a matching engine data_type.
     *
     * <p>Checks are done against the live pp-metadata schema (the same source the
     * rule builder synthesizes options from). When the metadata service is
     * unreachable — {@link #fetchMetadataFields} returns empty — the check degrades
     * to a no-op so a transient outage never blocks a save; the structural
     * non-blank guard in {@code RuleNodeSchemaValidator} still applies.
     *
     * @throws InvalidRuleStructureException when a referenced schema_type or
     *         field_key does not exist, or the data_type contradicts the schema
     */
    public void validateMetadataConditions(List<RuleNode> rootNodes) {
        List<RuleNode> conds = new ArrayList<>();
        collectMetadataConds(rootNodes, conds);
        if (conds.isEmpty()) {
            return;
        }

        Optional<Map<String, OperatorCategory>> categoriesByType = loadMetadataCategoriesByType();
        if (categoriesByType.isEmpty()) {
            // Metadata categories unavailable — degrade to no-op rather than block save.
            return;
        }
        Map<String, OperatorCategory> categoryByType = categoriesByType.get();

        // Resolve fields once per schema_type within this call.
        Map<String, Map<String, Map<String, Object>>> fieldsByType = new HashMap<>();
        for (RuleNode cond : conds) {
            validateMetadataCond(cond, categoryByType, fieldsByType);
        }
    }

    /**
     * Load metadata categories indexed by their schema_type. Returns an empty
     * {@link Optional} when the operator-config lookup fails so the caller can
     * degrade to a no-op (distinct from a successfully loaded but empty map).
     */
    private Optional<Map<String, OperatorCategory>> loadMetadataCategoriesByType() {
        Map<String, OperatorCategory> categoryByType = new HashMap<>();
        try {
            // Metadata categories are global (tenant-agnostic); pass null tenant.
            for (OperatorCategory c : operatorConfigService.getAllCategoriesWithOptions(null)) {
                if (c.isMetadataCategory() && c.getMetadataSchemaType() != null) {
                    categoryByType.putIfAbsent(c.getMetadataSchemaType(), c);
                }
            }
        } catch (RuntimeException e) {
            logger.warn("Cannot load metadata categories; skipping metadata field validation", e);
            return Optional.empty();
        }
        return Optional.of(categoryByType);
    }

    /**
     * Validate a single {@code metadata.access} COND: the schema_type must map to a
     * known category, the field_key must exist in that schema, the declared data_type
     * must match the schema, and the comparison value must satisfy the field's rules.
     */
    private void validateMetadataCond(RuleNode cond, Map<String, OperatorCategory> categoryByType,
                                      Map<String, Map<String, Map<String, Object>>> fieldsByType) {
        Map<String, Object> params = cond.getParams();
        String schemaType = params != null ? stringOrEmpty(params.get("schema_type")) : "";
        String fieldKey = params != null ? stringOrEmpty(params.get("field_key")) : "";
        String dataType = params != null ? stringOrEmpty(params.get("data_type")) : "";

        OperatorCategory category = categoryByType.get(schemaType);
        if (category == null) {
            throw new InvalidRuleStructureException(
                    cond.getId(),
                    "Loại schema metadata '" + schemaType + "' không tồn tại");
        }

        Map<String, Map<String, Object>> fields =
                fieldsByType.computeIfAbsent(schemaType, t -> indexFieldsByName(fetchMetadataFields(category)));

        if (fields.isEmpty()) {
            // pp-metadata unreachable or schema carries no fields — degrade to a
            // no-op rather than reject a legitimate save on a transient outage.
            logger.warn("No metadata fields resolved for schema_type={}; skipping field check for cond={}",
                    schemaType, cond.getId());
            return;
        }

        Map<String, Object> field = fields.get(fieldKey);
        if (field == null) {
            throw new InvalidRuleStructureException(
                    cond.getId(),
                    "Trường metadata '" + fieldKey + "' không tồn tại trong schema '" + schemaType + "'");
        }

        String expectedType = mapMetadataDataType(stringOrFallback(field.get("type"), TYPE_STRING).toUpperCase());
        if (!dataType.isBlank() && !expectedType.equalsIgnoreCase(dataType)) {
            throw new InvalidRuleStructureException(
                    cond.getId(),
                    "Kiểu dữ liệu '" + dataType + "' không khớp định nghĩa trường '" + fieldKey
                            + "' (mong đợi '" + expectedType + "') trong schema '" + schemaType + "'");
        }

        // The comparison value must satisfy the field's own validation rules
        // (type + string length/enum + number range/enum), comparator-aware.
        validateMetadataValue(cond.getId(), field, expectedType, params, fieldKey);
    }

    // Comparators whose value represents a FULL field value — enum membership and
    // length/range constraints apply. Partial (CONTAINS/STARTS_WITH), negative
    // (NOT_EQUALS/NOT_IN) and ordinal (GREATER_*/LESS_*) comparators only get a
    // type check, because constraining a substring/exclusion/threshold against the
    // field's own bounds would false-reject legitimate rules. Canonical codes —
    // the engine parses comparator via ConditionOperator.valueOf().
    private static final Set<String> FULL_MATCH_COMPARATORS = Set.of(CANON_EQUALS, CANON_IN);
    // Comparators that carry no value at all.
    private static final Set<String> NO_VALUE_COMPARATORS = Set.of(CANON_IS_TRUE, CANON_IS_FALSE);

    /**
     * Validate a metadata.access COND's comparison {@code value} against the field
     * definition. Type is always enforced; length/enum/range constraints apply only
     * for full-match comparators (EQUALS/IN). A {@code null}/absent value is allowed
     * (e.g. boolean IS_TRUE/IS_FALSE carry none).
     */
    private void validateMetadataValue(String condId, Map<String, Object> field,
                                       String dataType, Map<String, Object> params, String fieldKey) {
        String comparator = params != null ? stringOrEmpty(params.get("comparator")) : "";
        if (NO_VALUE_COMPARATORS.contains(comparator)) {
            return;
        }
        Object rawValue = params != null ? params.get("value") : null;
        if (rawValue == null) {
            return;
        }

        List<Object> values = (rawValue instanceof List<?> list) ? new ArrayList<>(list) : List.of(rawValue);
        boolean fullMatch = FULL_MATCH_COMPARATORS.contains(comparator);
        Map<?, ?> validation = (field.get(KEY_VALIDATION) instanceof Map<?, ?> m) ? m : null;

        for (Object v : values) {
            if (v == null) {
                continue;
            }
            checkSingleMetadataValue(condId, dataType, v, fieldKey, fullMatch, validation);
        }
    }

    private void checkSingleMetadataValue(String condId, String dataType, Object v, String fieldKey,
                                          boolean fullMatch, Map<?, ?> validation) {
        checkValueType(condId, dataType, v, fieldKey);
        if (!fullMatch || validation == null) {
            return;
        }
        if (TYPE_NUMBER.equals(dataType)) {
            checkNumberConstraints(condId, validation, v, fieldKey);
        } else if (TYPE_STRING.equals(dataType) || "LIST".equals(dataType)) {
            checkStringConstraints(condId, validation, v, fieldKey);
        }
    }

    private void checkValueType(String condId, String dataType, Object v, String fieldKey) {
        boolean ok = switch (dataType) {
            case TYPE_NUMBER -> v instanceof Number || (v instanceof String s && isNumeric(s));
            case TYPE_BOOLEAN -> v instanceof Boolean
                    || (v instanceof String s && ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)));
            case "DATE" -> v instanceof String s && isIsoDate(s);
            default -> v instanceof String; // STRING / LIST element
        };
        if (!ok) {
            throw new InvalidRuleStructureException(
                    condId,
                    MSG_VALUE + v + "' không hợp lệ với kiểu '" + dataType + "' của trường '" + fieldKey + "'");
        }
    }

    private void checkStringConstraints(String condId, Map<?, ?> validation, Object v, String fieldKey) {
        if (!(validation.get(KEY_STRING_VALIDATION) instanceof Map<?, ?> sv)) {
            return;
        }
        String s = v.toString();
        Integer minLength = toInteger(sv.get("minLength"));
        Integer maxLength = toInteger(sv.get("maxLength"));
        Integer exactLength = toInteger(sv.get("exactLength"));
        if (minLength != null && s.length() < minLength) {
            throw new InvalidRuleStructureException(condId,
                    MSG_VALUE + s + "' ngắn hơn độ dài tối thiểu " + minLength + MSG_OF_FIELD + fieldKey + "'");
        }
        if (maxLength != null && s.length() > maxLength) {
            throw new InvalidRuleStructureException(condId,
                    MSG_VALUE + s + "' vượt độ dài tối đa " + maxLength + MSG_OF_FIELD + fieldKey + "'");
        }
        if (exactLength != null && s.length() != exactLength) {
            throw new InvalidRuleStructureException(condId,
                    MSG_VALUE + s + "' phải có đúng " + exactLength + " ký tự của trường '" + fieldKey + "'");
        }
        if (sv.get(KEY_EQUAL_TO_ANY_OF) instanceof List<?> allowed && !allowed.isEmpty()) {
            boolean member = allowed.stream().anyMatch(a -> a != null && a.toString().equals(s));
            if (!member) {
                throw new InvalidRuleStructureException(condId,
                        MSG_VALUE + s + "' không thuộc tập cho phép " + allowed + MSG_OF_FIELD + fieldKey + "'");
            }
        }
    }

    private void checkNumberConstraints(String condId, Map<?, ?> validation, Object v, String fieldKey) {
        if (!(validation.get("numberValidation") instanceof Map<?, ?> nv)) {
            return;
        }
        BigDecimal bd;
        try {
            bd = new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return; // type check already covers non-numeric
        }
        checkNumberBounds(condId, nv, bd, fieldKey);
        checkNumberEnum(condId, nv, bd, fieldKey);
    }

    private void checkNumberBounds(String condId, Map<?, ?> nv, BigDecimal bd, String fieldKey) {
        BigDecimal lt = toBigDecimal(nv.get("lessThan"));
        BigDecimal lte = toBigDecimal(nv.get("lessThanOrEqual"));
        BigDecimal gt = toBigDecimal(nv.get("greaterThan"));
        BigDecimal gte = toBigDecimal(nv.get("greaterThanOrEqual"));
        if (lt != null && bd.compareTo(lt) >= 0) {
            throw new InvalidRuleStructureException(condId,
                    MSG_VALUE_PLAIN + bd + " phải nhỏ hơn " + lt + MSG_FIELD_PAREN + fieldKey + "')");
        }
        if (lte != null && bd.compareTo(lte) > 0) {
            throw new InvalidRuleStructureException(condId,
                    MSG_VALUE_PLAIN + bd + " phải nhỏ hơn hoặc bằng " + lte + MSG_FIELD_PAREN + fieldKey + "')");
        }
        if (gt != null && bd.compareTo(gt) <= 0) {
            throw new InvalidRuleStructureException(condId,
                    MSG_VALUE_PLAIN + bd + " phải lớn hơn " + gt + MSG_FIELD_PAREN + fieldKey + "')");
        }
        if (gte != null && bd.compareTo(gte) < 0) {
            throw new InvalidRuleStructureException(condId,
                    MSG_VALUE_PLAIN + bd + " phải lớn hơn hoặc bằng " + gte + MSG_FIELD_PAREN + fieldKey + "')");
        }
    }

    private void checkNumberEnum(String condId, Map<?, ?> nv, BigDecimal bd, String fieldKey) {
        if (nv.get(KEY_EQUAL_TO_ANY_OF) instanceof List<?> allowed && !allowed.isEmpty()) {
            boolean member = allowed.stream().anyMatch(a -> toBigDecimal(a) != null && bd.compareTo(toBigDecimal(a)) == 0);
            if (!member) {
                throw new InvalidRuleStructureException(condId,
                        MSG_VALUE_PLAIN + bd + " không thuộc tập cho phép " + allowed + MSG_FIELD_PAREN + fieldKey + "')");
            }
        }
        if (nv.get("notEqualToAnyOf") instanceof List<?> excluded && !excluded.isEmpty()) {
            boolean hit = excluded.stream().anyMatch(a -> toBigDecimal(a) != null && bd.compareTo(toBigDecimal(a)) == 0);
            if (hit) {
                throw new InvalidRuleStructureException(condId,
                        MSG_VALUE_PLAIN + bd + " thuộc tập bị loại trừ " + excluded + MSG_FIELD_PAREN + fieldKey + "')");
            }
        }
    }

    private boolean isNumeric(String s) {
        try {
            new BigDecimal(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isIsoDate(String s) {
        try {
            LocalDate.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            try {
                LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
                return true;
            } catch (DateTimeParseException ex) {
                return false;
            }
        }
    }

    private Integer toInteger(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o instanceof String s && isNumeric(s)) {
            return new BigDecimal(s.trim()).intValue();
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (o instanceof String s && isNumeric(s)) {
            return new BigDecimal(s.trim());
        }
        return null;
    }

    private Map<String, Map<String, Object>> indexFieldsByName(List<Map<String, Object>> fields) {
        Map<String, Map<String, Object>> byName = new HashMap<>();
        for (Map<String, Object> f : fields) {
            byName.putIfAbsent(stringOrEmpty(f.get("name")), f);
        }
        return byName;
    }

    private void collectMetadataConds(List<RuleNode> nodes, List<RuleNode> out) {
        if (nodes == null) {
            return;
        }
        for (RuleNode n : nodes) {
            if (n == null) {
                continue;
            }
            if (n.getType() == RuleNode.NodeType.COND
                    && METADATA_ACCESS_OPERATOR.equals(n.getOperatorName())) {
                out.add(n);
            }
            collectMetadataConds(n.getChildren(), out);
        }
    }

    private Map<String, Object> findMetadataField(OperatorCategory category, String fieldName) {
        return fetchMetadataFields(category).stream()
                .filter(f -> fieldName.equals(stringOrEmpty(f.get("name"))))
                .findFirst()
                .orElse(null);
    }

    /**
     * Synthesize one rule item per metadata field, wired to the generic
     * {@code metadata.access} engine operator.
     *
     * <p>The FE rule builder requires a non-null {@code operatorName} to let the
     * user pick a rule; it then relays {@code operatorParams} verbatim into the
     * saved COND node together with the chosen comparator + value. The
     * comparators returned here are the engine-native codes understood by
     * {@code MetadataAccessOperatorTranslator}, so the FE comparator maps 1:1 to
     * the engine comparator (no suffix composition).
     */
    private RuleItemResponse synthesizeMetadataRule(OperatorCategory category,
                                                     Map<String, Object> field,
                                                     Map<String, Map<String, Map<String, String>>> labels) {
        String fieldName = stringOrEmpty(field.get("name"));
        String displayName = stringOrFallback(field.get("displayName"), fieldName);
        String rawType = stringOrFallback(field.get("type"), TYPE_STRING).toUpperCase();
        String dataType = mapMetadataDataType(rawType);
        String description = stringOrEmpty(field.get("description"));

        // A STRING field constrained to an enum (equalToAnyOf) renders as a
        // multi-select picker fed by the schema's allowed values, instead of a
        // free text box. The engine data_type stays STRING (membership via
        // in/not_in); only the FE render type becomes LIST.
        boolean stringEnum = TYPE_STRING.equals(dataType) && hasStringEnum(field);
        String feType = stringEnum ? "LIST" : dataType;
        List<String> comparators = stringEnum
                ? List.of(CANON_IN, CANON_NOT_IN)
                : metadataComparatorsForType(dataType);

        Map<String, Object> operatorParams = new LinkedHashMap<>();
        operatorParams.put("schema_type", category.getMetadataSchemaType());
        operatorParams.put("field_key", fieldName);
        operatorParams.put("data_type", dataType);

        return RuleItemResponse.builder()
                .id(category.getCode().toLowerCase() + "." + fieldName)
                .code((category.getCode() + "_" + fieldName).toUpperCase())
                .name(displayName, displayName)
                .description(description, description)
                .type(feType)
                .operatorName(METADATA_ACCESS_OPERATOR)
                .defaultComparator(comparators.isEmpty() ? null : comparators.get(0))
                .operatorParams(operatorParams)
                .inputConfig(buildMetadataInputConfig(displayName, feType, field))
                .operators(comparators.stream()
                        .map(c -> mapComparatorToOperator(c, "DATE".equals(dataType), labels))
                        .toList())
                .build();
    }

    /**
     * Read the allowed values of a STRING field's enum constraint
     * ({@code validation.stringValidation.equalToAnyOf}) from the pp-schema
     * definition. Empty when the field is not an enum.
     */
    private List<String> stringEnumValues(Map<String, Object> field) {
        if (!(field.get(KEY_VALIDATION) instanceof Map<?, ?> validation)) {
            return Collections.emptyList();
        }
        if (!(validation.get(KEY_STRING_VALIDATION) instanceof Map<?, ?> stringValidation)) {
            return Collections.emptyList();
        }
        if (!(stringValidation.get(KEY_EQUAL_TO_ANY_OF) instanceof List<?> values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(v -> v != null)
                .map(Object::toString)
                .toList();
    }

    private boolean hasStringEnum(Map<String, Object> field) {
        return !stringEnumValues(field).isEmpty();
    }

    /**
     * Build the FE input configuration for a metadata-driven rule from its
     * pp-schema definition. Without this the FE falls back to the generic
     * "Tập khách hàng" segment label/multi-select, because {@code inputConfig}
     * would be null. The label, input type and constraints are all derived
     * from the schema definition so each attribute renders with its own name
     * and the correct value editor.
     */
    private RuleInputConfigResponse buildMetadataInputConfig(String displayName, String dataType,
                                                              Map<String, Object> field) {
        RuleInputConfigResponse.Builder builder = RuleInputConfigResponse.builder();

        // Label = the attribute's own display name (from pp-schema), not the
        // segment default. Same text for en/vi since the schema carries one name.
        builder.label(displayName, displayName);
        builder.inputType(metadataInputType(dataType));
        applyMetadataPlaceholder(builder, dataType);

        // MULTIPLE cardinality (or a LIST/ARRAY type) renders a multi-value input.
        boolean multiple = "LIST".equals(dataType)
                || "MULTIPLE".equalsIgnoreCase(stringOrEmpty(field.get("cardinality")));
        if (multiple) {
            builder.multiple(Boolean.TRUE);
        }

        // Numeric range constraints come straight from the schema's NumberValidation.
        if (TYPE_NUMBER.equals(dataType)) {
            applyMetadataNumberConstraints(builder, field);
        }

        // String length constraints from StringValidation so the FE can enforce the
        // same bounds the BE checks at save time (enum is rendered as a select).
        if (TYPE_STRING.equals(dataType)) {
            applyMetadataStringConstraints(builder, field);
        }

        return builder.build();
    }

    /**
     * Map the resolved metadata {@code data_type} to an HTML-ish input type hint.
     */
    private String metadataInputType(String dataType) {
        return switch (dataType) {
            case TYPE_NUMBER -> "number";
            case TYPE_BOOLEAN -> "checkbox";
            case "DATE" -> "date";
            case "LIST" -> "select";
            default -> "text";
        };
    }

    private void applyMetadataPlaceholder(RuleInputConfigResponse.Builder builder, String dataType) {
        String en;
        String vi;
        switch (dataType) {
            case TYPE_NUMBER -> { en = "Enter a number"; vi = "Nhập số"; }
            case "DATE" -> { en = "Select a date"; vi = "Chọn ngày"; }
            case "LIST" -> { en = "Select values"; vi = "Chọn giá trị"; }
            case TYPE_BOOLEAN -> { en = ""; vi = ""; }
            default -> { en = "Enter a value"; vi = "Nhập giá trị"; }
        }
        if (!en.isEmpty() || !vi.isEmpty()) {
            builder.placeholder(en, vi);
        }
    }

    /**
     * Pull min/max from the schema's NumberValidation. Prefers the inclusive
     * bound ({@code greaterThanOrEqual}/{@code lessThanOrEqual}) and falls back
     * to the exclusive one when only that is configured.
     */
    @SuppressWarnings("unchecked")
    private void applyMetadataStringConstraints(RuleInputConfigResponse.Builder builder,
                                                Map<String, Object> field) {
        if (!(field.get(KEY_VALIDATION) instanceof Map<?, ?> validation)) {
            return;
        }
        if (!(validation.get(KEY_STRING_VALIDATION) instanceof Map<?, ?> sv)) {
            return;
        }
        builder.minLength(toInteger(sv.get("minLength")));
        builder.maxLength(toInteger(sv.get("maxLength")));
        builder.exactLength(toInteger(sv.get("exactLength")));
    }

    private void applyMetadataNumberConstraints(RuleInputConfigResponse.Builder builder,
                                                 Map<String, Object> field) {
        if (!(field.get(KEY_VALIDATION) instanceof Map<?, ?> validation)) {
            return;
        }
        if (!(validation.get("numberValidation") instanceof Map<?, ?> number)) {
            return;
        }
        Object min = firstNonNull(number.get("greaterThanOrEqual"), number.get("greaterThan"));
        Object max = firstNonNull(number.get("lessThanOrEqual"), number.get("lessThan"));
        if (min != null) {
            builder.minValue(min.toString());
        }
        if (max != null) {
            builder.maxValue(max.toString());
        }
        List<BigDecimal> allowed = toBigDecimalList(number.get(KEY_EQUAL_TO_ANY_OF));
        if (!allowed.isEmpty()) {
            builder.allowedNumbers(allowed);
        }
        List<BigDecimal> excluded = toBigDecimalList(number.get("notEqualToAnyOf"));
        if (!excluded.isEmpty()) {
            builder.excludedNumbers(excluded);
        }
    }

    private List<BigDecimal> toBigDecimalList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<BigDecimal> out = new ArrayList<>();
        for (Object o : list) {
            BigDecimal bd = toBigDecimal(o);
            if (bd != null) {
                out.add(bd);
            }
        }
        return out;
    }

    private Object firstNonNull(Object a, Object b) {
        return a != null ? a : b;
    }

    /**
     * Map a raw metadata PropertyType to the {@code data_type} understood by
     * {@code MetadataAccessOperatorTranslator} (STRING/NUMBER/BOOLEAN/DATE/LIST).
     */
    private String mapMetadataDataType(String rawType) {
        return switch (rawType) {
            case TYPE_NUMBER, "INTEGER", "DECIMAL" -> TYPE_NUMBER;
            case TYPE_BOOLEAN -> TYPE_BOOLEAN;
            case "DATE", "DATETIME", "TIMESTAMP" -> "DATE";
            case "ARRAY", "LIST" -> "LIST";
            default -> TYPE_STRING; // STRING, TEXT, ENUM, OBJECT, ...
        };
    }

    /**
     * Canonical comparator codes per metadata data type, matching the branches of
     * {@code MetadataAccessOperatorTranslator} which parses the comparator via
     * {@code ConditionOperator.valueOf()}. Date wording maps {@code before -> LESS_THAN}
     * and {@code after -> GREATER_THAN} (matrix §22). Range comparators ({@code BETWEEN})
     * are intentionally omitted because the generic rule modal renders a single value
     * input per data type, not a min/max pair.
     */
    private List<String> metadataComparatorsForType(String dataType) {
        return switch (dataType) {
            case TYPE_NUMBER -> List.of(CANON_EQUALS, CANON_GREATER_OR_EQUAL, CANON_LESS_OR_EQUAL);
            case TYPE_BOOLEAN -> List.of(CANON_IS_TRUE, CANON_IS_FALSE);
            case "DATE" -> List.of(CANON_EQUALS, CANON_LESS_THAN, CANON_GREATER_THAN);
            case "LIST" -> List.of(CANON_CONTAINS, CANON_NOT_CONTAINS);
            default -> List.of(CANON_EQUALS, CANON_NOT_EQUALS, CANON_IN, CANON_NOT_IN,
                    CANON_CONTAINS, CANON_STARTS_WITH);
        };
    }

    private String stringOrEmpty(Object o) {
        return o == null ? "" : o.toString();
    }

    private String stringOrFallback(Object o, String fallback) {
        return o == null ? fallback : o.toString();
    }

    /**
     * Map OperatorOption domain model to RuleItemResponse DTO.
     */
    private RuleItemResponse mapOptionToRuleItem(
            OperatorOption option,
            Map<String, Map<String, Map<String, String>>> labels) {
        String descEn = option.getDescription() != null ? option.getDescription() : "";
        String descVi = option.getDescriptionVi() != null ? option.getDescriptionVi() : descEn;
        return RuleItemResponse.builder()
                .id(option.getCode())
                .code(option.getCode().toUpperCase())
                .name(option.getName(), option.getNameVi() != null ? option.getNameVi() : option.getName())
                .description(descEn, descVi)
                .type(mapValueTypeToRuleType(option.getValueType(), option.getDataSourceType()))
                .autoApply(option.getAutoApply())
                .defaultOperator(option.getDefaultOperator())
                .operatorName(option.getOperatorName())
                .defaultComparator(option.getDefaultComparator())
                .inputConfig(buildInputConfig(option))
                .operators(buildOperators(option, labels))
                .build();
    }

    /**
     * Map ValueType to rule type string.
     */
    private String mapValueTypeToRuleType(OperatorOption.ValueType valueType, String dataSourceType) {
        // If dataSourceType is specified, use it as type
        if (dataSourceType != null && !dataSourceType.isBlank()) {
            return dataSourceType;
        }

        if (valueType == null) {
            return "TEXT";
        }

        return switch (valueType) {
            case NUMBER -> TYPE_NUMBER;
            case BOOLEAN -> TYPE_BOOLEAN;
            case DATE -> "DATE";
            case LIST -> "LIST";
            default -> "TEXT";
        };
    }

    /**
     * Build input configuration from option.
     */
    private RuleInputConfigResponse buildInputConfig(OperatorOption option) {
        if (option.getValueType() == OperatorOption.ValueType.BOOLEAN) {
            return null;
        }
        if (option.getInputType() == null && option.getDataSourceType() == null
                && option.getValueSource() == null && option.getDataLoaderType() == null) {
            return null;
        }

        RuleInputConfigResponse.Builder builder = RuleInputConfigResponse.builder();
        configureDataSource(builder, option);
        configureInputType(builder, option);
        configureLabels(builder, option);
        configureNumberConstraints(builder, option);
        builder.valueParamKey(option.getValueParamKey());
        return builder.build();
    }

    private void configureDataSource(RuleInputConfigResponse.Builder builder, OperatorOption option) {
        if (option.getDataSourceType() != null) {
            builder.dataSourceType(option.getDataSourceType());
        }
        if (option.getDataSourceEndpoint() != null) {
            builder.dataSourceEndpoint(option.getDataSourceEndpoint());
        }
        if (option.getDataLoaderType() != null) {
            builder.dataLoaderType(option.getDataLoaderType());
        }
        if (option.getDataLoaderConfig() != null) {
            builder.dataLoaderConfig(option.getDataLoaderConfig());
        }
    }

    private void configureInputType(RuleInputConfigResponse.Builder builder, OperatorOption option) {
        String inputType = option.getInputType();
        if (inputType == null && option.getValueType() != null) {
            inputType = switch (option.getValueType()) {
                case NUMBER -> "number";
                case BOOLEAN -> "checkbox";
                case DATE -> "date";
                default -> "text";
            };
        }
        if (inputType != null) {
            builder.inputType(inputType);
        }
        if (option.getInputMultiple() != null) {
            builder.multiple(option.getInputMultiple());
        }
        if (option.getInputSearchable() != null) {
            builder.searchable(option.getInputSearchable());
        }
    }

    private void configureLabels(RuleInputConfigResponse.Builder builder, OperatorOption option) {
        if (option.getLabelEn() != null || option.getLabelVi() != null) {
            String labelEn = option.getLabelEn() != null ? option.getLabelEn() : option.getName();
            String nameViFallback = option.getNameVi() != null ? option.getNameVi() : option.getName();
            String labelVi = option.getLabelVi() != null ? option.getLabelVi() : nameViFallback;
            builder.label(labelEn, labelVi);
        }
        if (option.getPlaceholderEn() != null || option.getPlaceholderVi() != null) {
            builder.placeholder(
                    option.getPlaceholderEn() != null ? option.getPlaceholderEn() : "",
                    option.getPlaceholderVi() != null ? option.getPlaceholderVi() : ""
            );
        }
    }

    private void configureNumberConstraints(RuleInputConfigResponse.Builder builder, OperatorOption option) {
        if (option.getMinValue() != null) {
            builder.minValue(option.getMinValue().toString());
        }
        if (option.getMaxValue() != null) {
            builder.maxValue(option.getMaxValue().toString());
        }
        if (option.getInputStep() != null) {
            builder.step(option.getInputStep());
        }
    }

    /**
     * Build operators list from the option's available comparators (canonical codes
     * after the Pha 4 re-seed). Labels are resolved from the translations table; the
     * canonical code is emitted verbatim as the operator {@code value}.
     */
    private List<OperatorResponse> buildOperators(
            OperatorOption option,
            Map<String, Map<String, Map<String, String>>> labels) {
        boolean dateLike = option.getValueType() == OperatorOption.ValueType.DATE;
        if (option.getAvailableComparators() == null || option.getAvailableComparators().isEmpty()) {
            // Return default operators based on comparison type.
            return getDefaultOperators(option.getComparisonType(), dateLike, labels);
        }

        return option.getAvailableComparators().stream()
                .map(c -> mapComparatorToOperator(c, dateLike, labels))
                .toList();
    }

    /**
     * Get default canonical operators based on comparison type (used only when an
     * option leaves available_comparators empty).
     */
    private List<OperatorResponse> getDefaultOperators(
            OperatorOption.ComparisonType comparisonType,
            boolean dateLike,
            Map<String, Map<String, Map<String, String>>> labels) {
        List<String> codes = switch (comparisonType == null
                ? OperatorOption.ComparisonType.SINGLE : comparisonType) {
            case RANGE -> List.of(CANON_EQUALS, CANON_NOT_EQUALS,
                    CANON_GREATER_OR_EQUAL, CANON_LESS_OR_EQUAL, CANON_BETWEEN);
            case LIST -> List.of(CANON_IN, CANON_NOT_IN);
            case BOOLEAN -> List.of(CANON_IS_TRUE, CANON_IS_FALSE);
            default -> List.of(CANON_EQUALS, CANON_NOT_EQUALS);
        };
        return codes.stream()
                .map(c -> mapComparatorToOperator(c, dateLike, labels))
                .toList();
    }

    /**
     * Map a comparator code to an {@link OperatorResponse} with its label resolved
     * (localized) from the translations table. Canonical codes (EQUALS, GREATER_THAN…)
     * resolve to seeded labels; non-canonical engine-native metadata codes
     * (equals/gte/before…) fall through with the code as their own label until the
     * engine adopts canonical (Pha 6).
     */
    private OperatorResponse mapComparatorToOperator(
            String comparator,
            boolean dateLike,
            Map<String, Map<String, Map<String, String>>> labels) {
        return operatorLabelService.toOperatorResponse(comparator, dateLike, labels);
    }
}
