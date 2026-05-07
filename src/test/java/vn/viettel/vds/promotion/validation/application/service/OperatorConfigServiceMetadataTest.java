package vn.viettel.vds.promotion.validation.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.validation.application.port.out.MetadataSchemaPersistencePort;
import vn.viettel.vds.promotion.validation.application.port.out.OperatorCategoryPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.MetadataSchema;
import vn.viettel.vds.promotion.validation.domain.model.OperatorCategory;
import vn.viettel.vds.promotion.validation.domain.model.OperatorOption;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OperatorConfigService — virtual operator generation from metadata schemas (T15).
 */
@ExtendWith(MockitoExtension.class)
class OperatorConfigServiceMetadataTest {

    @Mock
    private OperatorCategoryPersistencePort categoryPort;

    @Mock
    private MetadataSchemaPersistencePort metadataSchemaPort;

    private OperatorConfigService service;

    @BeforeEach
    void setUp() {
        service = new OperatorConfigService(categoryPort, metadataSchemaPort);
    }

    // --- comparator mapping by fieldType ---

    @Test
    void enrichMetadataCategory_numberField_rangeComparators() {
        OperatorCategory cat = customerMetadataCategory();
        when(categoryPort.findAllActiveWithOptions()).thenReturn(List.of(cat));
        when(metadataSchemaPort.findActiveByTenantIdAndSchemaType("default", "customer"))
                .thenReturn(List.of(schema("loyalty_score", MetadataSchema.FieldType.NUMBER)));

        List<OperatorCategory> result = service.getAllCategoriesWithOptions("default");
        OperatorOption opt = result.get(0).getOptions().get(0);

        assertThat(opt.getAvailableComparators()).containsExactly("equals", "gte", "lte", "between");
        assertThat(opt.getDefaultComparator()).isEqualTo("equals");
        assertThat(opt.getValueType()).isEqualTo(OperatorOption.ValueType.NUMBER);
        assertThat(opt.getComparisonType()).isEqualTo(OperatorOption.ComparisonType.RANGE);
        assertThat(opt.getInputType()).isEqualTo("number");
    }

    @Test
    void enrichMetadataCategory_stringFieldNoValues_textInput() {
        OperatorCategory cat = customerMetadataCategory();
        when(categoryPort.findAllActiveWithOptions()).thenReturn(List.of(cat));
        when(metadataSchemaPort.findActiveByTenantIdAndSchemaType("default", "customer"))
                .thenReturn(List.of(schema("region", MetadataSchema.FieldType.STRING)));

        List<OperatorCategory> result = service.getAllCategoriesWithOptions("default");
        OperatorOption opt = result.get(0).getOptions().get(0);

        assertThat(opt.getAvailableComparators())
                .containsExactly("equals", "not_equals", "in", "not_in", "contains", "starts_with");
        assertThat(opt.getInputType()).isEqualTo("text");
        assertThat(opt.getValueSource()).isEqualTo(OperatorOption.ValueSource.INPUT);
        assertThat(opt.getValueOptions()).isEmpty();
    }

    @Test
    void enrichMetadataCategory_stringFieldWithValues_selectInput() {
        OperatorCategory cat = customerMetadataCategory();
        when(categoryPort.findAllActiveWithOptions()).thenReturn(List.of(cat));
        MetadataSchema s = MetadataSchema.builder()
                .id("id-vip")
                .tenantId("default")
                .schemaType(MetadataSchema.SchemaType.CUSTOMER)
                .fieldKey("vip_tier")
                .fieldName("VIP tier")
                .fieldType(MetadataSchema.FieldType.STRING)
                .availableValues(List.of("silver", "gold", "platinum"))
                .displayOrder(10)
                .active(true)
                .build();
        when(metadataSchemaPort.findActiveByTenantIdAndSchemaType("default", "customer"))
                .thenReturn(List.of(s));

        List<OperatorCategory> result = service.getAllCategoriesWithOptions("default");
        OperatorOption opt = result.get(0).getOptions().get(0);

        assertThat(opt.getInputType()).isEqualTo("select");
        assertThat(opt.getValueSource()).isEqualTo(OperatorOption.ValueSource.SELECT);
        assertThat(opt.getValueOptions()).hasSize(3)
                .extracting(OperatorOption.ValueOption::getValue)
                .containsExactly("silver", "gold", "platinum");
    }

    @Test
    void enrichMetadataCategory_booleanField_isTrueIsFalseComparators() {
        OperatorCategory cat = customerMetadataCategory();
        when(categoryPort.findAllActiveWithOptions()).thenReturn(List.of(cat));
        when(metadataSchemaPort.findActiveByTenantIdAndSchemaType("default", "customer"))
                .thenReturn(List.of(schema("is_vip", MetadataSchema.FieldType.BOOLEAN)));

        List<OperatorCategory> result = service.getAllCategoriesWithOptions("default");
        OperatorOption opt = result.get(0).getOptions().get(0);

        assertThat(opt.getAvailableComparators()).containsExactly("is_true", "is_false");
        assertThat(opt.getDefaultComparator()).isEqualTo("is_true");
        assertThat(opt.getInputType()).isEqualTo("toggle");
        assertThat(opt.getComparisonType()).isEqualTo(OperatorOption.ComparisonType.BOOLEAN);
    }

    @Test
    void enrichMetadataCategory_dateField_temporalComparators() {
        OperatorCategory cat = customerMetadataCategory();
        when(categoryPort.findAllActiveWithOptions()).thenReturn(List.of(cat));
        when(metadataSchemaPort.findActiveByTenantIdAndSchemaType("default", "customer"))
                .thenReturn(List.of(schema("first_purchase_date", MetadataSchema.FieldType.DATE)));

        List<OperatorCategory> result = service.getAllCategoriesWithOptions("default");
        OperatorOption opt = result.get(0).getOptions().get(0);

        assertThat(opt.getAvailableComparators()).containsExactly("before", "after", "between", "equals");
        assertThat(opt.getDefaultComparator()).isEqualTo("equals");
        assertThat(opt.getInputType()).isEqualTo("date");
    }

    // --- virtual option metadata ---

    @Test
    void enrichMetadataCategory_optionCode_namespacedWithSchemaType() {
        OperatorCategory cat = customerMetadataCategory();
        when(categoryPort.findAllActiveWithOptions()).thenReturn(List.of(cat));
        when(metadataSchemaPort.findActiveByTenantIdAndSchemaType("default", "customer"))
                .thenReturn(List.of(schema("loyalty_score", MetadataSchema.FieldType.NUMBER)));

        List<OperatorCategory> result = service.getAllCategoriesWithOptions("default");
        OperatorOption opt = result.get(0).getOptions().get(0);

        assertThat(opt.getCode()).isEqualTo("customer.loyalty_score");
        assertThat(opt.getOperatorName()).isEqualTo("metadata.access");
        assertThat(opt.getOperatorVersion()).isEqualTo(1);
        assertThat(opt.getId()).startsWith("virt-");
    }

    @Test
    void enrichMetadataCategory_3fields_allReturned() {
        OperatorCategory cat = customerMetadataCategory();
        when(categoryPort.findAllActiveWithOptions()).thenReturn(List.of(cat));
        when(metadataSchemaPort.findActiveByTenantIdAndSchemaType("default", "customer"))
                .thenReturn(List.of(
                        schema("vip_tier", MetadataSchema.FieldType.STRING),
                        schema("loyalty_score", MetadataSchema.FieldType.NUMBER),
                        schema("first_purchase_date", MetadataSchema.FieldType.DATE)
                ));

        List<OperatorCategory> result = service.getAllCategoriesWithOptions("default");
        assertThat(result.get(0).getOptions()).hasSize(3);
    }

    @Test
    void enrichMetadataCategory_noFields_emptyOptions() {
        OperatorCategory cat = customerMetadataCategory();
        when(categoryPort.findAllActiveWithOptions()).thenReturn(List.of(cat));
        when(metadataSchemaPort.findActiveByTenantIdAndSchemaType("default", "customer"))
                .thenReturn(List.of());

        List<OperatorCategory> result = service.getAllCategoriesWithOptions("default");
        assertThat(result.get(0).getOptions()).isEmpty();
    }

    // --- helpers ---

    private OperatorCategory customerMetadataCategory() {
        return OperatorCategory.builder()
                .id("cat-customer-meta-001")
                .code("customer_metadata")
                .name("Customer Metadata")
                .metadataCategory(true)
                .metadataSchemaType("customer")
                .active(true)
                .build();
    }

    private MetadataSchema schema(String fieldKey, MetadataSchema.FieldType fieldType) {
        return MetadataSchema.builder()
                .id("id-" + fieldKey)
                .tenantId("default")
                .schemaType(MetadataSchema.SchemaType.CUSTOMER)
                .fieldKey(fieldKey)
                .fieldName(fieldKey.replace("_", " "))
                .fieldType(fieldType)
                .availableValues(List.of())
                .displayOrder(1)
                .active(true)
                .build();
    }
}
