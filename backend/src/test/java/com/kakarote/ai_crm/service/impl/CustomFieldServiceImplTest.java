package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.kakarote.ai_crm.entity.PO.CustomField;
import com.kakarote.ai_crm.mapper.CustomFieldMapper;
import com.kakarote.ai_crm.service.IDynamicSchemaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomFieldServiceImplTest {

    @Test
    void initializeSystemFieldsSyncsExistingProductUnitFieldMetadata() {
        InMemoryCustomFieldService service = new InMemoryCustomFieldService(existingProductUnitTextField());

        service.initializeSystemFields("product");

        CustomField unitField = service.field("unit");
        assertThat(unitField.getFieldType()).isEqualTo("select");
        assertThat(unitField.getFieldSource()).isEqualTo("system");
        assertThat(unitField.getPlaceholder()).isEqualTo("请选择单位");
        assertThat(unitField.getOptions()).contains("\"value\":\"个\"");
        assertThat(service.updateCount).isEqualTo(1);
    }

    @Test
    void initializeSystemFieldsCreatesCustomerSystemFields() {
        InMemoryCustomFieldService service = new InMemoryCustomFieldService();

        service.initializeSystemFields("customer");

        CustomField companyName = service.field("companyName");
        assertThat(companyName.getFieldSource()).isEqualTo("system");
        assertThat(companyName.getFieldType()).isEqualTo("text");
        assertThat(companyName.getColumnName()).isEqualTo("company_name");
        assertThat(companyName.getIsRequired()).isEqualTo(1);
        assertThat(companyName.getIsSearchable()).isEqualTo(1);
    }

    @Test
    void updateCustomFieldValueConvertsSmallintCheckboxToNumericValue() {
        InMemoryCustomFieldService service = new InMemoryCustomFieldService(existingContactPrimaryField());
        CustomFieldMapper mapper = mock(CustomFieldMapper.class);
        IDynamicSchemaService dynamicSchemaService = mock(IDynamicSchemaService.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "dynamicSchemaService", dynamicSchemaService);
        when(dynamicSchemaService.getTableName("customer_contact")).thenReturn("crm_customer_contact");
        when(dynamicSchemaService.getIdColumnName("customer_contact")).thenReturn("contact_id");
        when(dynamicSchemaService.columnExists("crm_customer_contact", "is_primary")).thenReturn(true);

        service.updateCustomFieldValue("customer_contact", 1001L, "isPrimary", true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> valuesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mapper).updateCustomFieldValues(
                eq("crm_customer_contact"),
                eq("contact_id"),
                eq(1001L),
                valuesCaptor.capture()
        );
        assertThat(valuesCaptor.getValue()).containsEntry("is_primary", 1);
    }

    private static CustomField existingProductUnitTextField() {
        CustomField field = new CustomField();
        field.setFieldId(1L);
        field.setEntityType("product");
        field.setFieldName("unit");
        field.setFieldLabel("Unit");
        field.setFieldType("text");
        field.setColumnName("unit");
        field.setColumnType("VARCHAR(50)");
        field.setPlaceholder("Input unit");
        field.setOptions(null);
        field.setFieldSource("custom");
        field.setStatus(1);
        return field;
    }

    private static CustomField existingContactPrimaryField() {
        CustomField field = new CustomField();
        field.setFieldId(2L);
        field.setEntityType("customer_contact");
        field.setFieldName("isPrimary");
        field.setFieldLabel("Primary");
        field.setFieldType("checkbox");
        field.setColumnName("is_primary");
        field.setColumnType("SMALLINT");
        field.setStatus(1);
        return field;
    }

    private static final class InMemoryCustomFieldService extends CustomFieldServiceImpl {
        private final List<CustomField> fields = new ArrayList<>();
        private int updateCount;

        private InMemoryCustomFieldService(CustomField... fields) {
            this.fields.addAll(List.of(fields));
        }

        @Override
        public List<CustomField> list(Wrapper<CustomField> queryWrapper) {
            return new ArrayList<>(fields);
        }

        @Override
        public boolean save(CustomField entity) {
            fields.add(entity);
            return true;
        }

        @Override
        public boolean updateById(CustomField entity) {
            updateCount++;
            return true;
        }

        private CustomField field(String fieldName) {
            return fields.stream()
                    .filter(field -> fieldName.equals(field.getFieldName()))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
